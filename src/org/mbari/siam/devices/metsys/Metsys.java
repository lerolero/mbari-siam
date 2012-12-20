/** 
 * @Title Metsys
 * @author Martyn Griffiths
 *
 * Copyright MBARI 2003
 * 
 * REVISION HISTORY:
 * $Log: Metsys.java,v $
 * Revision 1.4  2009/07/16 17:43:02  headley
 * javadoc syntax fixes
 *
 * Revision 1.3  2009/07/16 15:56:09  headley
 * javadoc syntax fixes
 *
 * Revision 1.2  2009/07/16 15:01:02  headley
 * javadoc syntax fixes
 *
 * Revision 1.1  2008/11/04 22:17:55  bobh
 * Initial checkin.
 *
 * Revision 1.1.1.1  2008/11/04 19:02:04  bobh
 * Initial release of SIAM2, the release that was modularized to isolate hardware dependencies, and refactored to make most packages point to org.mbari.siam.
 *
 * Revision 1.13  2004/10/15 20:15:31  oreilly
 * utilizes ServiceAttributes framework
 *
 * Revision 1.12  2004/08/18 22:13:29  oreilly
 * Use _toDevice.write(), _fromDevice.flush()
 *
 * Revision 1.11  2004/05/04 19:23:20  oreilly
 * Modified log4j levels
 *
 * Revision 1.10  2004/02/27 01:21:44  headley
 * changed schedule specifier to ms
 *
 * Revision 1.9  2003/10/10 23:58:52  martyn
 * Refined log message types
 *
 * Revision 1.8  2003/10/10 18:24:24  martyn
 * Adjusted debug some messages - cosmetic.
 *
 * Revision 1.7  2003/10/10 17:19:57  martyn
 * Changed power policy to power always w/ suitable delay before attempting to enter command state
 *
 * Revision 1.6  2003/10/06 23:05:42  mrisi
 * modified to use StreamUtils.skipUntil
 *
 * Revision 1.5  2003/10/03 23:05:19  mrisi
 * switched readUntil to moos.utils.StreamUtils readUntil
 *
 * Revision 1.4  2003/10/03 20:31:22  martyn
 * Documentation
 *
 * Revision 1.3  2003/10/03 00:50:28  martyn
 * Mainly updating print statements to use log4j logging
 * Some tweaks.
 *
 * Revision 1.2  2003/09/24 18:38:12  martyn
 * Added some refinements:
 * 1. Used siam_home to determine siam root.
 * 2. Now saves metloc in <siam_home>/logs
 * 3. Added retroSave property to define how many unsaved arrays to save.
 *
 * Revision 1.1  2003/09/24 01:20:32  martyn
 * Initial submission of working driver
 *
 *
 */

package org.mbari.siam.devices.metsys;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.util.Properties;
import java.util.StringTokenizer;

import org.mbari.siam.core.InstrumentService;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.utils.AsciiTime;
import org.mbari.siam.utils.PrintUtils;
import org.mbari.siam.utils.StreamUtils;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.InvalidDataException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.TimeoutException;

/**
 * A Campbell Scientific CR10X, an embedded microprocessor based datalogger,
 * coordinates the sampling of the Metsys sensors. The embedded system also
 * provides serial communications to the host system so the sampled data can
 * also be transmitted to a PC or mooring controller. The subsystem is
 * preconfigured to take a sample set of all sensors every 10 minutes (1 minute
 * in test mode). The instrument has three distinct operating modes "awake" (or
 * command mode), "asleep" - a low power mode but able to wake itself up
 * automatically at sampling time and "off" - powered down completely. This last
 * operating mode is not a deployment option. To wake the instrument, two
 * carriage returns are sent to the communications port spaced by a small delay
 * to allow the CR10X to wake up before receiving the second character. Sending
 * this second character to the unit causes the unit to enter command mode. The
 * instrument responds with the prompt "*". When in command mode the unit can be
 * interrogated and any number of previous samples can be requested from the
 * instrument's circular buffer. To exit command mode an "E" command is sent and
 * the unit returns to the low power sleep mode.
 * 
 * Sample Buffer Management The CR10X's sample buffer is controlled by 2 main
 * pointers, the write pointer (reference) and the read pointer (location). Once
 * initialized, it is the responsibility of the Sidearm to control the position
 * of the "location" pointer; the "reference" pointer is exclusively controlled
 * by the Metsys. The first time the Sidearm initializes the Metsys, the
 * "location" pointer is set to the "reference" pointer. A persistent copy of
 * the "location" pointer is always maintained in "SIAM_HOME/logs/metloc".
 * Should the Sidearm power fail after initialization, the stored location
 * pointer is recovered and used to determine how many records to acquire. There
 * is a limit to the number of past records that the Sidearm can acquire. This
 * limit is programmable using the "retroSave" property (see below).
 */
public class Metsys extends InstrumentService implements Instrument {

	// Data constants
	static final int CHARS_PER_RECORD = 10;

	static final int RECORDS_PER_ARRAY = 15;

	static final int CHARS_PER_ARRAY = (CHARS_PER_RECORD * RECORDS_PER_ARRAY) + 10; // 10 =
																					// "/r/n"
																					// delimiter
																					// +
																					// others

	// Max constants
	static final int MAX_RESPONSE_BYTES = 256; // !! to be determined

	static final int MAX_ARRAYS = 4; // 100; // 100 arrays = 16 hours (@ 10
									 // mins/sample)

	static final int MAX_SAMPLE_BYTES = CHARS_PER_ARRAY * MAX_ARRAYS;

	static final int MAX_SAMPLE_TRIES = 3;

	static final int MAX_COMMAND_TRIES = 3;

	//    static final int MAX_ATTENTION_TRIES = 20; // Once a minute

	// Timeouts
	static final int WAKEUP_TIME = 3000;

	static final int RESPONSE_TIME = WAKEUP_TIME;

	static final int SAMPLE_RESPONSE_TIME = 20000; // Unfortunately large to
												   // deal w/ worst case
												   // retroSave 100*152*1mS =
												   // 15.2 secs

	static final int ECHO_RESPONSE_TIME = 2000;

	static final int POWER_UP_TIME = 10000;

	// Default Properties
	static final String DEFAULT_TIME_SYNC = "Y";

	static final String DEFAULT_RETRO_SAVE = "100";

	// Commands
	static final String COMMAND_PROMPT = "";

	static final String COMMAND_SET_DATE = "C";

	static final String COMMAND_ACQUIRE = "D";

	static final String COMMAND_BACKUP = "B";

	static final String COMMAND_GOTO = "G";

	static final String COMMAND_SLEEP = "E";

	static final String COMMAND_STATUS = "A";

	// Responses
	static final String SAMPLE_TERMINATOR = "\r\n A";

	static final String RESPONSE_PROMPT = "*";

	// Others
	static final int BYTES_PER_ARRAY = 16; // # of internal bytes of memory used
										   // per array

	static final int CHECKSUM_MODULUS = 8192;

	static final String SIAM_HOME_KEY = "siam_home";

	// log4j Logger
	static private Logger _logger = Logger.getLogger(Metsys.class);

	// Configurable MetSys attributes
	Attributes _attributes = new Attributes(this);

	// Private fields
	private int _reference = 0;

	private int _arraysToFetch = 0;

	private File _metLoc;

	/**
	 * 
	 * @exception RemoteException .
	 */
	public Metsys() throws RemoteException {
	}

	/** Specify startup delay (millisec) */
	protected int initInstrumentStartDelay() {
		return 2000;
	}

	/** Specify prompt string. */
	protected byte[] initPromptString() {
		return "".getBytes();
	}

	/** Specify sample terminator. */
	protected byte[] initSampleTerminator() {
		return SAMPLE_TERMINATOR.getBytes();
	}

	/** Specify maximum bytes in raw sample. */
	protected int initMaxSampleBytes() {
		return MAX_SAMPLE_BYTES;
	}

	/** Specify current limit in increments of 120 mA upto 11880 mA. */
	protected int initCurrentLimit() {
		return 1000; // Powered externally
	}

	/**
	 * Return instrument power policy.
	 * 
	 * @return power policy
	 */
	protected PowerPolicy initInstrumentPowerPolicy() {
		return PowerPolicy.ALWAYS;
	}

	/**
	 * Return communications interface power policy.
	 * 
	 * @return power policy
	 */
	protected PowerPolicy initCommunicationPowerPolicy() {
		return PowerPolicy.WHEN_SAMPLING;
	}

	/**
	 * Return specifier for default sampling schedule.
	 * 
	 * @return default sample schedule
	 * @exception ScheduleParseException
	 */
	protected ScheduleSpecifier createDefaultSampleSchedule()
			throws ScheduleParseException {
		return new ScheduleSpecifier(600000); // every 10 minutes
	}

	/** Return parameters to use on serial port. */
	public SerialPortParameters getSerialPortParameters()
			throws UnsupportedCommOperationException {

		return new SerialPortParameters(9600, SerialPort.DATABITS_8,
				SerialPort.PARITY_NONE, SerialPort.STOPBITS_1);
	}

	/**
	 * Called by the framework to initialize the instrument prior to sampling.
	 * Normally this method will use properties determined by the setProperties
	 * method.
	 * 
	 * @exception InitializeException
	 * @exception Exception
	 */
	protected void initializeInstrument() throws InitializeException, Exception {
		_logger.debug("Initializing ...");

		// Get siam home path
		Properties props = System.getProperties();
		String siamHome = props.getProperty(SIAM_HOME_KEY);
		_metLoc = new File(siamHome + "/logs/metloc");

		setMaxSampleTries(MAX_SAMPLE_TRIES);
		setSampleTimeout(SAMPLE_RESPONSE_TIME);
		setMaxSampleBytes(CHARS_PER_ARRAY * _attributes.retroSave);

		// turn on instrument and communications/power
		managePowerWake();

		// Wait for instrument to initialize
		sleep(POWER_UP_TIME);

		try {
			enterCommandMode();
			if (_attributes.timeSynch)
				setTime();
			getCurrentLocation(); // Initialize location counter

		} catch (Exception e) {
			throw new InitializeException("initializeInstrument() " + e);
		} finally {
			exitCommandMode();
			// turn off communications/power
			managePowerSleep();
		}

		_logger.debug("Initializing completed");
	}

	/**
	 * Set the Metsys date and time to the Sidearm's date and time.
	 * 
	 * 
	 * @exception Exception
	 */
	private void setTime() throws Exception {
		String now = AsciiTime.getDate("YY:ddd") + ":"
				+ AsciiTime.getTime("HH:mm:ss");
		//Format 03:265:19:28:56
		sendCommand(now + COMMAND_SET_DATE);
	}

	/**
	 * Wakeup the Metsys in preparation to sample. Determine how many records we
	 * need to retrieve based on our current position and the position that's
	 * been reached by the Metsys
	 * 
	 * @exception Exception
	 */
	protected void prepareToSample() throws Exception {
		_logger.debug("Preparing to sample...");
		enterCommandMode();

		// Issue a "A" command
		String response = sendRequest(COMMAND_STATUS);
		// Typical response: R+37285. F+62140. V4 A1 L+37285. E00 99 28 M0256
		// B+3.0784 C3185

		_reference = getParameter(response, 'R');
		int limit = getParameter(response, 'F');
		int location = getCurrentLocation();
		//int location = getParameter(response,'L');
		int offset = _reference - location;
		if (offset < 0)
			offset += limit;
		_arraysToFetch = offset / BYTES_PER_ARRAY;

		_logger.debug("R=" + _reference + " F=" + limit + " L=" + location);

		if (_arraysToFetch == 0) {
			exitCommandMode();
			throw new Exception("No Samples to fetch!");
		} else if (_arraysToFetch > _attributes.retroSave) {
			_logger.warn("loss of " + (_arraysToFetch - _attributes.retroSave)
					+ " arrays!");

			_arraysToFetch = _attributes.retroSave;

		}

		_logger.debug("Preparing to sample completed");
	}

	/**
	 * Issue command to Metsys to acquire lastest sample(s)
	 * 
	 * @exception TimeoutException
	 * @exception Exception
	 *                not thrown
	 */
	protected void requestSample() throws TimeoutException, Exception {
		_logger.debug("Sampling - fetching " + _arraysToFetch + " arrays...");
		sendCommand(_arraysToFetch + COMMAND_BACKUP);
		sendCommand(_arraysToFetch + COMMAND_ACQUIRE, _arraysToFetch
				+ COMMAND_ACQUIRE + "\r\n");
	}

	/**
	 * Called by the framwwork after receiving sample. Fetches last line
	 * appended to sample data containing checkusm information. Calculates
	 * checksum of received sample and compares w/ expected checksum in trailer.
	 * 
	 * @param _sampleBuf
	 *            sample data
	 * @param nBytes
	 *            number of bytes captured
	 * 
	 * @exception InvalidDataException
	 *                if checksum failed
	 */
	protected void validateSample(byte[] _sampleBuf, int nBytes)
			throws InvalidDataException {
		// Get remaining bytes
		// example .. L+32661 C4055

		int actualChecksum = 0;
		// Get checksum line...
		byte[] buf = new byte[MAX_RESPONSE_BYTES];
		try {
			StreamUtils.readUntil(_fromDevice, buf, RESPONSE_PROMPT.getBytes(),
					RESPONSE_TIME);
		} catch (Exception e) {
			throw new InvalidDataException("Failed to read checksum line");
		}

		int expectedChecksum = getParameter(new String(buf), 'C');

		for (int i = 0; i < nBytes; i++) {
			actualChecksum += _sampleBuf[i];
		}
		// Add SAMPLE_TERMINATOR bytes to checksum
		byte[] temp = SAMPLE_TERMINATOR.getBytes();
		for (int i = 0; i < temp.length; i++)
			actualChecksum += temp[i];
		// Add command echo bytes to checksum
		temp = new String(_arraysToFetch + COMMAND_ACQUIRE + "\r\n").getBytes();
		for (int i = 0; i < temp.length; i++)
			actualChecksum += temp[i];
		// Add buf upto and including "C".
		for (int i = 0; i < buf.length; i++) {
			actualChecksum += buf[i];
			if (buf[i] == 'C')
				break;
		}

		_logger.debug("Checksum: Is= " + (actualChecksum % CHECKSUM_MODULUS)
				+ " Was= " + expectedChecksum);

		if ((actualChecksum % CHECKSUM_MODULUS) != expectedChecksum) {
			_logger.error("Bad checksum");
			throw new InvalidDataException("Bad Checksum");
		}
	}

	/**
	 * Deal with Metsys after sample received or on error during
	 * sampling/validating... If no error saves the current reference pointer as
	 * the new location pointer for next time. If an error was detected then the
	 * location pointer isn't updated and an attempt will be made to recover it
	 * next time. Switches metsys to sleep mode
	 * 
	 */
	protected void postSample() {
		try {
			//update location with current reference if no acquisition error
			if (getStatus() != Device.ERROR) {
				setCurrentLocation(_reference);
			}
			exitCommandMode();
		} catch (Exception e) {
			_logger.error("postSample() failed: " + e);
		}
	}

	/**
	 * Not implemented
	 * 
	 * @return instrument metadata (byte[])
	 */
	protected byte[] getInstrumentMetadata() {

		// This method should return any available configuration
		// information that is available from the instrument

		return "Not supported".getBytes();
	}//end getInstrumentMetadata()

	/**
	 * Not implemented. Samples are locally timestamped
	 * 
	 * @param t
	 */
	public void setClock(long t) {
		return;
	}

	/** PRIVATE METHODS * */

	/**
	 * Method to send commands to the instrument. sendCommand makes every
	 * attempt to communicate with the unit by a process of resetting, flushing
	 * input buffer and resending.
	 * 
	 * Note: Trailing '\r' is automatically added to command string.
	 * 
	 * @param cmd
	 *            Command string to send
	 * 
	 * @exception Exception
	 *                thrown if the method fails to send the command.
	 */
	private void sendCommand(String cmd) throws Exception {

		for (int i = 0; i < MAX_COMMAND_TRIES; i++) {
			// Prepare to send message
			try {
				_fromDevice.flush();
				_toDevice.write(mkCmd(cmd));
				StreamUtils.skipUntil(_fromDevice, RESPONSE_PROMPT.getBytes(),
						RESPONSE_TIME, MAX_RESPONSE_BYTES);
				return;
			} catch (IOException e) { // This is bad - not sure a retry would
									  // help
				_logger.error("IOException " + e);
				throw new Exception("sendCommand(" + cmd
						+ ") - Stream I/O failure");
			} catch (NullPointerException e) { // This is bad - a retry isn't
											   // going to make this one batter
				_logger.error("Null Pointer Exception " + e);
				throw new Exception("sendCommand(" + cmd + ") - Null pointer");
			} catch (TimeoutException e) {
				_logger.error("Timeout Exception: " + e + "Cmd=" + cmd);
			} catch (Exception e) { // Probably exceeded max bytes - bad command
									// maybe
				_logger.error("Exception " + e);
				//incBadResponseCount();
			}
		}
		//incRetryExceededCount();
		throw new Exception("sendCommand(" + cmd
				+ ") - Maximum retries attempted");
	}

	/**
	 * Method to send commands to the Instrument. sendCommand makes every
	 * attempt to communicate with the unit by a process of resetting, flushing
	 * input buffer and resending.
	 * 
	 * Note: Trailing '\r' is automatically added to command string.
	 * 
	 * @param cmd
	 *            Command string to send
	 * 
	 * @exception Exception
	 *                thrown if the method fails to send the command.
	 */
	private void sendCommand(String cmd, String rsp) throws Exception {

		for (int i = 0; i < MAX_COMMAND_TRIES; i++) {
			// Prepare to send message
			try {
				_fromDevice.flush();
				_toDevice.write(mkCmd(cmd));
				StreamUtils.skipUntil(_fromDevice, rsp.getBytes(),
						RESPONSE_TIME, MAX_RESPONSE_BYTES);
				return;
			} catch (IOException e) { // This is bad - not sure a retry would
									  // help
				_logger.error("IOException " + e);
				throw new Exception("sendCommand(" + cmd
						+ ") - Stream I/O failure");
			} catch (NullPointerException e) { // This is bad - a retry isn't
											   // going to make this one batter
				_logger.error("Null Pointer Exception " + e);
				throw new Exception("sendCommand(" + cmd + ") - Null pointer");
			} catch (TimeoutException e) {
				_logger.error("Timeout Exception: " + e + "Cmd=" + cmd);
				// incTimeoutCount(); Don't include these as they occur often
				// when resyncing
			} catch (Exception e) { // Probably exceeded max bytes - bad command
									// maybe
				_logger.error("Exception " + e);
				//incBadResponseCount();
			}
		}
		//incRetryExceededCount();
		throw new Exception("sendCommand(" + cmd
				+ ") - Maximum retries attempted");
	}

	/**
	 * Sends a request to the Metsys for parametric data. The response is
	 * returned as a string.
	 * 
	 * sendRequest makes every attempt to communicate with the unit by a process
	 * of resetting, flushing input buffer and resending.
	 * 
	 * @param request
	 *            string
	 * 
	 * @return Response returned by the Metsys
	 * @exception Exception
	 */
	private String sendRequest(String request) throws Exception {

		byte[] response = new byte[MAX_RESPONSE_BYTES];
		for (int i = 0; i < MAX_COMMAND_TRIES; i++) {
			// Prepare to send message
			try {
				_fromDevice.flush();
				_toDevice.write(mkCmd(request));
				StreamUtils.skipUntil(_fromDevice, "\r\n".getBytes(),
						ECHO_RESPONSE_TIME, mkCmd(request).length + 2);
				StreamUtils.readUntil(_fromDevice, response, RESPONSE_PROMPT
						.getBytes(), RESPONSE_TIME);
				return new String(response);
			} catch (IOException e) { // This is bad - not sure a retry would
									  // help
				_logger.error("IOException " + e);
				throw new Exception("sendCommand(" + request
						+ ") - Stream I/O failure");
			} catch (NullPointerException e) { // This is bad - a retry isn't
											   // going to make this one batter
				_logger.error("Null Pointer Exception " + e);
				throw new Exception("sendCommand(" + request
						+ ") - Null pointer");
			} catch (TimeoutException e) {
				_logger.error("Timeout Exception: " + e + "Request=" + request);
				// incTimeoutCount(); Don't include these as they occur often
				// when resyncing
			} catch (Exception e) { // Probably exceeded max bytes - bad command
									// maybe
				_logger.error("Exception " + e);
				//incBadResponseCount();
			}
			// Reset interface using
			_toDevice.write("\r".getBytes());
			sleep(500);
		}
		//incRetryExceededCount();
		throw new Exception("sendCommand(" + request
				+ ") - Maximum retries attempted");
	}

	/**
	 * Wakeup Metsys.
	 * 
	 * @exception Exception
	 */
	private void enterCommandMode() throws Exception {
		_toDevice.write("\r".getBytes());
		// Allow Metsys to wake up
		sleep(100);
		_fromDevice.flush(); // In case already awake
		sendCommand(COMMAND_PROMPT, RESPONSE_PROMPT);
	}

	/**
	 * Put Metsys in low power sleep mode
	 * 
	 * @exception Exception
	 */
	private void exitCommandMode() throws Exception {
		sendCommand(COMMAND_SLEEP, COMMAND_SLEEP);
	}

	/**
	 * Method for suspending task.
	 * 
	 * @param msDelay
	 *            number of milliseconds to delay task
	 */
	private void sleep(int msDelay) {
		for (;;) {
			try {
				Thread.sleep(msDelay);
				return;
			} catch (InterruptedException e) {

			}
		}
	}

	/**
	 * Utility method to construct a message of the form: -
	 * <p>
	 * <BLOCKQUOTE>
	 * 
	 * <PRE>
	 * 
	 * "cmd + \r" </BLOCKQUOTE>
	 * 
	 * </PRE>
	 * 
	 * and returns this as a byte array for transmission
	 * 
	 * @param cmd
	 *            basic command string to construct
	 * 
	 * @return byte array of command
	 */
	private byte[] mkCmd(String cmd) {
		return (new String(cmd + "\r")).getBytes();
	}

	/**
	 * Called by the framework after the sample data has been captured to
	 * optionally print the contents of the sample buffer for debug purposes.
	 * 
	 * @param buf
	 *            sample buffer
	 */
	protected void printData(byte[] buf) {
		PrintUtils.printFull(buf, 0, 64);
	}

	public int test() {
		return 0;
	}

	/**
	 * Save the location counter in persistent storage. If it doesn't exist
	 * create it first.
	 * 
	 * @param location
	 * 
	 * @exception Exception
	 * @exception IOException
	 */
	private void setCurrentLocation(int location) throws Exception, IOException {
		//File metLoc = new File(_siamHome + "/logs/metloc");

		if (!_metLoc.exists())
			_metLoc.createNewFile();

		OutputStream os = new FileOutputStream(_metLoc);
		DataOutputStream dout = new DataOutputStream(os);
		dout.writeInt(location);
		dout.close();
	}

	/**
	 * Gets the current location (read pointer) by 1. Reading in the location if
	 * persisent storage is found. 2. If no persisent storage found then get the
	 * current status from the Metsys and make persistent.
	 * 
	 * @return current location
	 * @exception Exception
	 * @exception IOException
	 */
	private int getCurrentLocation() throws Exception, IOException {

		// If location counter found then retrieve
		// Else read metsys and get current location (L).

		//File metLoc = new File(_siamHome + "/logs/metloc");

		int location = 0;
		if (_metLoc.exists()) {
			InputStream is = new FileInputStream(_metLoc);
			DataInputStream din = new DataInputStream(is);
			location = din.readInt();
			din.close();
		} else {
			// Get current status

			String response = sendRequest(COMMAND_STATUS);
			// Typical response: R+37285. F+62140. V4 A1 L+37285. E00 99 28
			// M0256 B+3.0784 C3185

			location = getParameter(response, 'R');

			// Make persistent
			setCurrentLocation(location);

		}
		return location;
	}

	/**
	 * Extracts the parameter associated with character ch from the string buf.
	 * For example
	 * <p>
	 * <BLOCKQUOTE>
	 * 
	 * <PRE>
	 * 
	 * if buf = R+37285. F+62140. V4 A1 L+37285. E00 99 28 M0256 B+3.0784 C3185
	 * and ch = 'L' The returned parameter would be 37285 converted from a
	 * string to an integer. </BLOCKQUOTE>
	 * 
	 * </PRE>
	 * 
	 * @param buf
	 *            The string to parse
	 * @param ch
	 *            The parameter reference
	 * 
	 * @return The parmeter requested convert from a string to an integer 0 if
	 *         parse failed
	 */
	private int getParameter(String buf, char ch) {

		String format = new Character(ch).toString();

		// strip +'s and .'s as these break integer parser and are irrelevant
		buf = buf.replace('+', '0');
		buf = buf.replace('.', ' ');
		StringTokenizer st = new StringTokenizer(buf, format + " ", true);
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (token.equals(format))
				return Integer.parseInt(st.nextToken());
		}
		return 0;
	}

	/** Configurable MetSys attributes */
	class Attributes extends InstrumentServiceAttributes {

		/** Constructor, with required InstrumentService argument */
		Attributes(InstrumentService service) {
			super(service);
		}

		int retroSave = 1;

		/**
		 * Throw MissingPropertyException if specified attribute is mandatory.
		 */
		public void missingAttributeCallback(String attributeName)
				throws MissingPropertyException {

			if (attributeName.equals("retroSave")) {
				throw new MissingPropertyException(attributeName);
			}
		}

		/**
		 * Throw InvalidPropertyException if any invalid attribute values found
		 */
		public void checkValues() throws InvalidPropertyException {

			if (retroSave <= 0) {
				throw new InvalidPropertyException(
						"Invalid 'retroSave' value: "
								+ "must be positive integer");
			}
		}
	}

}

