/** 
 * @Title HOBILabs HydroRad instrument driver
 * @author Martyn Griffiths
 * @version 1.0
 * @date 8/13/2003
 *
 * Copyright MBARI 2003
 * 
 * REVISION HISTORY:
 * $Log: HydroRad.java,v $
 * Revision 1.4  2009/07/16 17:43:33  headley
 * javadoc syntax fixes
 *
 * Revision 1.3  2009/07/16 15:55:52  headley
 * javadoc syntax fixes
 *
 * Revision 1.2  2009/07/16 05:33:49  headley
 * javadoc syntax fixes
 *
 * Revision 1.1  2008/11/04 22:17:51  bobh
 * Initial checkin.
 *
 * Revision 1.1.1.1  2008/11/04 19:02:04  bobh
 * Initial release of SIAM2, the release that was modularized to isolate hardware dependencies, and refactored to make most packages point to org.mbari.siam.
 *
 * Revision 1.18  2006/06/03 18:57:09  oreilly
 * extends PolledInstrumentService
 *
 * Revision 1.17  2006/04/21 07:06:11  headley
 * converted System.x.println to log4j  and/or change _logger to _log4j
 *
 * Revision 1.16  2004/10/15 20:15:17  oreilly
 * utilizes ServiceAttributes framework
 *
 * Revision 1.15  2004/08/18 22:12:48  oreilly
 * Use _toDevice.write(), _fromDevice.flush()
 *
 * Revision 1.14  2003/10/16 22:06:48  martyn
 * Increased power settling time from 10 to 15 secs
 *
 * Revision 1.13  2003/10/10 23:49:31  martyn
 * Refined log message types
 *
 * Revision 1.12  2003/10/10 18:24:24  martyn
 * Adjusted debug some messages - cosmetic.
 *
 * Revision 1.11  2003/10/07 18:20:52  oreilly
 * Removed carriage returns from code
 *
 * Revision 1.10  2003/10/06 23:05:34  mrisi
 * modified to use StreamUtils.skipUntil
 *
 * Revision 1.9  2003/10/03 19:16:21  martyn
 * Mostly documentation.
 * enterCommandMode() should always be called in prepareToSample()
 *
 * Revision 1.8  2003/10/03 00:50:28  martyn
 * Mainly updating print statements to use log4j logging
 * Some tweaks.
 *
 * Revision 1.7  2003/09/17 00:03:36  martyn
 * Mainly cosmetic - commenting out println etc
 *
 * Revision 1.6  2003/09/15 23:38:46  martyn
 * Improved AsciiTime utility class
 * Updated all drivers to use revised class.
 * Removed registryName service property
 * Moved service property keys to ServiceProperties
 *
 * Revision 1.5  2003/09/10 22:42:16  martyn
 * Now uses AsciiTime utility class
 * Removed unused methods
 * Some documentation added
 *
 * Revision 1.4  2003/08/29 22:25:29  martyn
 * Added delays during initialization and sampling
 * needed when powered by Sidearm.
 *
 * Revision 1.3  2003/08/26 00:40:21  martyn
 * Added printData method
 * Revised power policy
 * Revised current limit
 *
 * Revision 1.2  2003/08/20 22:06:48  martyn
 * Doubled capture buffer size for other data formats -
 * Warning: still too small for ASCII records
 *
 * Revision 1.1  2003/08/20 00:19:35  martyn
 * Initial submission - working driver but some cleaning up to do.
 *
 *
 */

package org.mbari.siam.devices.hobilabs;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.StringTokenizer;

import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.utils.AsciiTime;
import org.mbari.siam.utils.PrintUtils;
import org.mbari.siam.utils.StreamUtils;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.TimeoutException;

/**
 * The <code>HydroRad</code> class represents the
 * <code>InstrumentServices</code> driver for controlling multichannel
 * radiometers. The class duties is to capture and verify sample data from the
 * instrument.
 * 
 * @see org.mbari.siam.distributed.Device
 * @see org.mbari.siam.distributed.Instrument
 * @see org.mbari.siam.distributed.PowerPort
 * @see org.mbari.siam.core.InstrumentService
 */

public class HydroRad extends PolledInstrumentService implements Instrument {

	// Max constants
	static final int MAX_RESPONSE_BYTES = 1000; // 559 old 736 new

	static final int MAX_SAMPLE_TRIES = 3;

	static final int MAX_COMMAND_TRIES = 5; // In command mode we need to be

	// more rigourous

	static final int MAX_REQUEST_BYTES = 80; // Max number of bytes sent back

	// from a request

	static final int MAX_SAMPLE_BYTES = 4200 * 4 * 2;//!!change // (2048 pixels

	// * 2 + overhead) * #
	// channels

	// Timeouts
	static final int RESPONSE_TIME = 10000; // This needs to be longer than the

	// longest likely intercharacter
	// delay

	static final int PROMPT_RESPONSE_TIME = 1000;

	static final int ECHO_RESPONSE_TIME = 2000;

	static final int SAMPLE_RESPONSE_TIME = 30000 * 4 * 3; // 30 secs, 4

	// channels, 3
	// samples - !!Todo:
	// depends on
	// parameter string

	static final int POWER_UP_TIME = 15000;

	// Property keys

	// Property defaults
	static final String DEFAULT_TIME_SYNC = "N";

	static final String DEFAULT_ACQUIRE_PARAM = "AUTO,3,1,,0,0,2,1234";

	// Commands
	static final String COMMAND_FORCE_PROMPT = ""; // "\r" implied

	static final String COMMAND_CONTROL_C = "\003";

	static final String COMMAND_SLEEP = "SLEEP";

	static final String COMMAND_SET_RTC = "DATE";

	static final String COMMAND_ACQUIRE = "ACQUIRE";

	static final String COMMAND_POWER_ON = "POWER ON";

	static final String COMMAND_SHUTTER_OPEN = "SHUTTER OPEN";

	static final String COMMAND_SHUTTER_CLOSED = "SHUTTER CLOSE";

	// Responses
	static final String SAMPLE_TERMINATOR = "Hydro";

	static final String RESPONSE_PROMPT = "Rad>";

	static final String RESPONSE_EOL = "\r\n";

	// Others
	static final String DATA_SYNC = ", .";

	// Configurable attributes
	Attributes _attributes = new Attributes(this);

	// log4j Logger
	static private Logger _log4j = Logger.getLogger(HydroRad.class);

	/**
	 * Initializes a new <code>HydroRad</code> instance
	 * 
	 * @exception RemoteException .
	 */
	public HydroRad() throws RemoteException {
	}

	/** Specify startup delay (millisec) */
	protected int initInstrumentStartDelay() {
		return 2000;
	}

	/** Specify prompt string. */
	protected byte[] initPromptString() {
		return RESPONSE_PROMPT.getBytes();
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
		return 2000; //!! to do
	}

	/** Return initial value of instrument power policy. */
	protected PowerPolicy initInstrumentPowerPolicy() {
		return PowerPolicy.WHEN_SAMPLING; // Worse case
	}

	/** Return initial value of communication power policy. */
	protected PowerPolicy initCommunicationPowerPolicy() {
		return PowerPolicy.WHEN_SAMPLING;
	}

	/** Return specifier for default sampling schedule. */
	protected ScheduleSpecifier createDefaultSampleSchedule()
			throws ScheduleParseException {
		// Sample every 10 minutes by default
		return new ScheduleSpecifier(600);
	}

	/** Return parameters to use on serial port. */
	public SerialPortParameters getSerialPortParameters()
			throws UnsupportedCommOperationException {

		return new SerialPortParameters(9600, SerialPort.DATABITS_8,
				SerialPort.PARITY_NONE, SerialPort.STOPBITS_1);
	}

	/**
	 * The HydroRad is initialized as follows: - 1. Shutter (if open) is closed.
	 * 2. Instrument time is optionally set.
	 * 
	 * @exception InitializeException
	 * @exception Exception
	 */
	protected void initializeInstrument() throws InitializeException, Exception {
		_log4j.info("Initializing...");

		String response, property;
		String[] token;

		setMaxSampleTries(MAX_SAMPLE_TRIES);
		setSampleTimeout(SAMPLE_RESPONSE_TIME);

		// turn on communications/power
		managePowerWake();

		// wait for instrument to power up
		if (getInstrumentPowerPolicy() == PowerPolicy.WHEN_SAMPLING) {
			if (!lineSilent(POWER_UP_TIME))
				throw new InitializeException("instrument not stable");
		}

		try {
			enterCommandMode();
			sendCommand(COMMAND_SHUTTER_CLOSED);
			if (_attributes.timeSynch) {
				setTime();
			}

		} catch (Exception e) {
			throw new InitializeException("initializeInstrument() " + e);
		} finally {
			exitCommandMode();
			// turn off communications/power
			managePowerSleep();
		}

		_log4j.info("Initializing complete");
	}

	/**
	 * Method to set the Instrument to Sidearm Time
	 * 
	 * @exception Exception
	 * @see org.mbari.siam.utils.AsciiTime
	 */
	private void setTime() throws Exception {
		// Format - 08/11/2003 20:31:50
		String now = AsciiTime.getDate("MM/DD/YYYY") + " "
				+ AsciiTime.getTime("HH:mm:ss");
		sendCommand(COMMAND_SET_RTC + " " + now);
	}

	/**
	 * When this method is called the instrument may just have been powered up
	 * (POWER_WHEN_SAMPLIN). If this is the case, communications is halted until
	 * the instrument has finished sending power up information (lineSilent(...)
	 * returning true). The time is the unconditionally set. In all power modes
	 * the spectrometer is powered up and the shutters are opened.
	 * 
	 * @exception Exception
	 *                If there is a problem communicating w/ the instrument
	 */
	protected void prepareToSample() throws Exception {
		_log4j.info("Preparing to sample...");

		if (!lineSilent(POWER_UP_TIME))
			throw new Exception("Instrument not stable");
		enterCommandMode(); // Must have prompt

		// If we've just powered up then reset the time
		if (getInstrumentPowerPolicy() == PowerPolicy.WHEN_SAMPLING) {
			setTime(); // Set the time since the HydroRad has been powered off
		}
		sendCommand(COMMAND_POWER_ON);
		sendCommand(COMMAND_SHUTTER_OPEN);
	}

	/**
	 * The HydroRad is sent the ACQUIRE command followed by the "acquireParams"
	 * property string. The instrument can take 4 - 5 minutes to determine the
	 * correct he integration time and return data. Control isn't handed back to
	 * the framework until the DATA_SYNC string has been detected. This filters
	 * out responses that are not part of the sample data stream.
	 * 
	 * @exception TimeoutException
	 *                thrown if no response to the ACQUIRE command is detected
	 *                or the DATA_SYNC sequence is not detected.
	 * @exception Exception
	 *                thrown if any other Exceptions are returned
	 */
	protected void requestSample() throws TimeoutException, Exception {
		_log4j.info("Sampling...");

		String cmd = COMMAND_ACQUIRE + " " + _attributes.acquireParams;
		sendCommand(cmd, RESPONSE_EOL);
		// Wait for start of sample data ...
		StreamUtils.skipUntil(_fromDevice, DATA_SYNC.getBytes(),
				SAMPLE_RESPONSE_TIME, 1000); //!! Tidy this
	}

	/**
	 * Called by the framework after sampling. The HydroRad is put into a low
	 * power mode (this may be superceeded by a power down depending on power
	 * policy).
	 * 
	 */
	protected void postSample() {
		try {
			sendCommand(COMMAND_SHUTTER_CLOSED);
			exitCommandMode();
		} catch (Exception e) {
			_log4j.error("postSample failed: " + e);
		}
	}

	/**
	 * Not implemented
	 * 
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
				_log4j.error("IOException " + e);
				throw new Exception("sendCommand(" + cmd
						+ ") - Stream I/O failure");
			} catch (NullPointerException e) { // This is bad - a retry isn't
				// going to make this one batter
				_log4j.error("Null Pointer Exception " + e);
				throw new Exception("sendCommand(" + cmd + ") - Null pointer");
			} catch (TimeoutException e) {
				_log4j.error("Timeout Exception: " + e + "Cmd=" + cmd);
				// incTimeoutCount(); Don't include these as they occur often
				// when resyncing
			} catch (Exception e) { // Probably exceeded max bytes - bad command
				// maybe
				_log4j.error("Exception " + e);
				//incBadResponseCount();
			}
			// Reset interface using "\r"
			_toDevice.write(mkCmd(COMMAND_FORCE_PROMPT));
			sleep(500);
		}
		//incRetryExceededCount();
		throw new Exception("sendCommand(" + cmd
				+ ") - Maximum retries attempted");
	}

	/**
	 * Method to send commands to the Workhorse. sendCommand makes every attempt
	 * to communicate with the unit by a process of resetting, flushing input
	 * buffer and resending.
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
				_log4j.error("IOException " + e);
				throw new Exception("sendCommand(" + cmd
						+ ") - Stream I/O failure");
			} catch (NullPointerException e) { // This is bad - a retry isn't
				// going to make this one batter
				_log4j.error("Null Pointer Exception " + e);
				throw new Exception("sendCommand(" + cmd + ") - Null pointer");
			} catch (TimeoutException e) {
				_log4j.error("Timeout Exception: " + e + "Cmd=" + cmd);
				// incTimeoutCount(); Don't include these as they occur often
				// when resyncing
			} catch (Exception e) { // Probably exceeded max bytes - bad command
				// maybe
				_log4j.error("Exception " + e);
				//incBadResponseCount();
			}
			// Reset interface using "\r"
			_toDevice.write(mkCmd(COMMAND_FORCE_PROMPT));
			sleep(500);
		}
		//incRetryExceededCount();
		throw new Exception("sendCommand(" + cmd
				+ ") - Maximum retries attempted");
	}

	/**
	 * sends a Ctrl C to the HydroRad and wait for a prompt
	 * 
	 * @exception Exception
	 */
	private void enterCommandMode() throws Exception {
		for (int i = 0; i < MAX_COMMAND_TRIES; i++) {
			try {
				_fromDevice.flush();
				_toDevice.write(COMMAND_CONTROL_C.getBytes());
				StreamUtils.skipUntil(_fromDevice, RESPONSE_PROMPT.getBytes(),
						RESPONSE_TIME, MAX_RESPONSE_BYTES);
				return;
			} catch (IOException e) { // This is bad - not sure a retry would
				// help
				_log4j.error("IOException " + e);
				throw new Exception("StreamUtils.skipUntil() " + e);
			} catch (NullPointerException e) { // This is bad - a retry isn't
				// going to make this one batter
				_log4j.error("Null Pointer Exception " + e);
				throw new Exception("StreamUtils.skipUntil() " + e);
			} catch (TimeoutException e) {
				_log4j.error("Timeout Exception: " + e);
			} catch (Exception e) { // Probably exceeded max bytes - bad command
				// maybe
				_log4j.error("Exception " + e);
			}
		}
		throw new Exception("enterCommandMode() - Maximum retries attempted");
	}

	/**
	 * restarts sampling and exits command mode
	 * 
	 * @exception IOException
	 * @exception Exception
	 */
	private void exitCommandMode() throws IOException, Exception {
		_toDevice.write(mkCmd(COMMAND_SLEEP));
	}

	/**
	 * Utility method which waits for activity on the receive channel to stop by
	 * sampling the receive buffer every 2 seconds. This inspection is aborted
	 * if activity constinues for more than "waitTime".
	 * 
	 * @param waitTime
	 *            The maximum period to wait for activity to stop.
	 * 
	 * @return true - if the line is now silent false - if activity remained
	 *         after the waitTime.
	 * @exception Exception
	 *                if there is a problem with the communications channel
	 */
	private boolean lineSilent(int waitTime) throws Exception {
		long t0 = System.currentTimeMillis();
		do {
			_log4j.debug("Waiting");
			_fromDevice.flush();
			sleep(2000);
			if ((System.currentTimeMillis() - t0) > waitTime) {
				return false;
			}
		} while (_fromDevice.available() > 0);
		return true;
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
	 * Cludge to create an int from 2 bytes (There is probably a better way of
	 * doing this but this works)
	 * 
	 * @param loByte
	 *            ls byte
	 * @param hiByte
	 *            ms byte
	 * 
	 * @return int value of hiByte:loByte
	 */
	private int mkInt(byte loByte, byte hiByte) {
		int temp1 = loByte & 0xff;
		int temp2 = hiByte & 0xff;
		return temp1 + (temp2 << 8);
	}

	/**
	 * Returns the property value from the HydroRad response
	 * 
	 * @param response
	 *            response string return from HydroRad
	 * 
	 * @return string representing the property (rhs of ='s)
	 * @exception InvalidPropertyException
	 */
	private String getProperty(String response) throws InvalidPropertyException {
		StringTokenizer tkzResponse = new StringTokenizer(response, " ", false);
		while (tkzResponse.countTokens() > 0) {
			String property = tkzResponse.nextToken();
			if (property.equals("=")) {
				property = tkzResponse.nextToken();
				return property;
			}
		}
		throw new InvalidPropertyException("Invalid HydroRad property");
	}

	/**
	 * splits the response into fields defined by the delim string and returns
	 * these in an array. fieldCount limits the number of fields generated.
	 * 
	 * @param response
	 *            string to parse
	 * @param delim
	 *            field delimiter(s)
	 * @param fieldCount
	 *            number of expected fields
	 * 
	 * @return array of Strings of each field detected
	 * @exception InvalidPropertyException
	 *                an illegal response string has been determined
	 */
	private String[] parseProperty(String response, String delim, int fieldCount)
			throws InvalidPropertyException {
		StringTokenizer tkzResponse = new StringTokenizer(response, delim,
				false);
		if (fieldCount > tkzResponse.countTokens())
			throw new InvalidPropertyException("Invalid HydroRad property");

		String[] property = new String[fieldCount];
		for (int i = 0; i < fieldCount; i++) {
			property[i] = tkzResponse.nextToken();
		}
		return property;
	}

	/**
	 * Called by the framework after the sample data has been captured to
	 * optionally print the contents of the sample buffer for debug purposes.
	 * 
	 * @param buf
	 *            sample buffer
	 */
	protected void printData(byte[] buf) {
		PrintUtils.printFull(buf, 0, 64); // First 64 bytes
	}

	public int test() {
		return 0;
	}

}

/** Configurable HydroRad attributes */

class Attributes extends InstrumentServiceAttributes {

	/** Constructor, with required InstrumentService argument */
	Attributes(DeviceServiceIF service) {
		super(service);
	}

	/**
	 * This string is sent with the Acquire command to trigger a sampling cycle.
	 * See HydroRad manual for explanation of fields
	 */
	String acquireParams = null;

	/**
	 * Throw MissingPropertyException if specified attribute is mandatory.
	 */
	public void missingAttributeCallback(String attributeName)
			throws MissingPropertyException {

		if (attributeName.equals("acquireParams")) {
			throw new MissingPropertyException(attributeName);
		}
	}

	/**
	 * Throw InvalidPropertyException if any invalid attribute values found
	 */
	public void checkValues() throws InvalidPropertyException {

	}
}
