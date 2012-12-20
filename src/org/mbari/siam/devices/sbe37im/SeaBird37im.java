/** 
 * @Title Generalized Sea-Bird Electronics Inductive Modem Instrument Driver
 * @author Martyn Griffiths
 * @version 1.0
 * @date 7/15/2003
 *
 * Copyright MBARI 2003
 * 
 * REVISION HISTORY:
 * $Log: SeaBird37im.java,v $
 * Revision 1.3  2009/07/16 15:56:28  headley
 * javadoc syntax fixes
 *
 * Revision 1.2  2009/07/16 15:08:35  headley
 * javadoc syntax fixes
 *
 * Revision 1.1  2008/11/04 22:17:57  bobh
 * Initial checkin.
 *
 * Revision 1.1.1.1  2008/11/04 19:02:04  bobh
 * Initial release of SIAM2, the release that was modularized to isolate hardware dependencies, and refactored to make most packages point to org.mbari.siam.
 *
 * Revision 1.19  2006/06/03 19:00:36  oreilly
 * extends PolledInstrumentService
 *
 * Revision 1.18  2004/10/15 20:18:11  oreilly
 * utilizes ServiceAttributes framework
 *
 * Revision 1.17  2004/08/18 22:15:50  oreilly
 * Use _toDevice.write(), _fromDevice.flush()
 *
 * Revision 1.16  2004/04/21 23:25:44  headley
 * changed StringTokenizer in validateSample to one that worked.
 *  changed to UNIX line ends
 *
 * Revision 1.15  2003/10/11 00:10:41  martyn
 * Refined log message types
 *
 * Revision 1.14  2003/10/10 18:24:24  martyn
 * Adjusted debug some messages - cosmetic.
 *
 * Revision 1.13  2003/10/06 23:05:51  mrisi
 * modified to use StreamUtils.skipUntil
 *
 * Revision 1.12  2003/10/03 20:31:22  martyn
 * Documentation
 *
 * Revision 1.11  2003/10/03 00:50:29  martyn
 * Mainly updating print statements to use log4j logging
 * Some tweaks.
 *
 * Revision 1.10  2003/09/17 01:39:53  martyn
 * Cosmetic
 *
 * Revision 1.9  2003/09/17 00:02:19  martyn
 * MicroCAT initialization progress indicators added
 *
 * Revision 1.8  2003/09/15 23:38:46  martyn
 * Improved AsciiTime utility class
 * Updated all drivers to use revised class.
 * Removed registryName service property
 * Moved service property keys to ServiceProperties
 *
 * Revision 1.7  2003/08/20 00:18:09  martyn
 * Updated to use postSample() in framework
 * Made debug strings in initializeInstrument and loadProperties consistent and more informative.
 *
 * Revision 1.6  2003/07/25 02:23:33  martyn
 * Improved instrumentation
 * Now checks timesync flag before setting time
 *
 * Revision 1.5  2003/07/24 21:17:44  martyn
 * Eliminated bad response count occuring during board power up:-
 * Increased MAX_SIM_RESPONSE_BYTES to 100 to take into account SIM power on message.
 * Added statement in requestSample  to ensure return of control of SIM before sample request command issued.
 *
 * Revision 1.4  2003/07/24 02:33:16  martyn
 * Cleanup after doing a full checkout and synching up with Kent's property changes
 *
 * Revision 1.3  2003/07/23 23:16:01  martyn
 * Added documentation, removed redundant code.
 *
 * Close to operational version. Runs reliably on 3 MicroCATs.
 *
 * Revision 1.2  2003/07/23 02:35:39  martyn
 * Changes: added sample validation, added sample concatenation and logging,
 *                  improved comms recoverey, added comms stats dump using gp test method
 *
 * 
 */

package org.mbari.siam.devices.sbe37im;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.StringTokenizer;

import org.mbari.siam.core.AggregateInstrumentService;
import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.utils.AsciiTime;
import org.mbari.siam.utils.PrintUtils;
import org.mbari.siam.utils.StreamUtils;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.InvalidDataException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.TimeoutException;

/**
 * The Sea-Bird Inductive Modem MicroCAT is an autonomous CT /CTD logger that
 * communicates with a base station (normally at sea level), the SIM (surface
 * inductive modem). The base station can supervise up to 100 inductively
 * coupled MicroCAT's. In practice no more than 30 are used to monitor the
 * physical characteristics of a water column. The SIM unit in turn communicates
 * upon request with the Sidearm instrument controller through a low speed RS232
 * channel.
 * 
 * 
 * Pre deployment setup
 * 
 * Instrument setup Since a MicroCAT's Id cannot be set with multiple units on
 * the IM loop, each module to be deployed at a single site must be
 * preconfigured with it's own unique id. Ideally the module's id also
 * represents its position in the water column w/ 0 being closest to the surface
 * and the highest representing the deepest module. In any event the modules
 * id's should be consecutively numbered. It is expect that other relevant
 * properties such as the reference pressure for modules without pressure
 * sensors would be setup at this time. The service driver would not initialize
 * such properties.
 * 
 * Note: When deploying a chain of modules one of them can fail to respond
 * during the pre deployment test. For such an eventuality a spare is taken
 * along. This spare is programmed with the next logical id leaving an id
 * discontinuity. The new id configuration should be added to the puck property
 * idMicroCATs and the service reinitialized.
 */
public class SeaBird37im extends AggregateInstrumentService implements
		Instrument {

	// Messages
	static final String MSG_SIM_RESPONSE_FAIL = "SIM failed to respond!";

	static final String MSG_SIM_PWRONCMD_FAIL = "PWRON command failed to respond!";

	static final String MSG_SIM_ECHOOFF_FAIL = "ECHOOFF command failed to respond!";

	static final String MSG_CAT_RESPONSE_FAIL = "CAT failed to respond!";

	// Max constants
	static final int MAX_SAMPLE_TYPE1_LEN = 49;

	static final int MAX_SAMPLE_TYPE2_LEN = 57;

	static final int MAX_SAMPLE_TYPE3_LEN = 67;

	static final int MAX_RETRIES = 3;

	static final int MAX_SENSORS = 30;

	static final int MAX_CAT_RESPONSE_BYTES = MAX_SAMPLE_TYPE3_LEN;

	static final int MAX_SIM_RESPONSE_BYTES = 100;

	// Timeouts
	static final int SIM_RESPONSE_TIME = 8000;

	static final int CAT_RESPONSE_TIME = 10000;

	static final int ECHO_RESPONSE_TIME = 4000;

	// Commands
	static final String COMMAND_FORCE_PROMPT = "\033";

	static final String COMMAND_ECHO_OFF = "ECHOOFF";

	static final String COMMAND_PWR_ON = "pwron";

	static final String COMMAND_PWR_OFF = "pwroff";

	static final String COMMAND_STOP_LOGGING = "stop";

	static final String COMMAND_START_LOGGING = "startnow";

	static final String COMMAND_SET_INTERVAL = "interval=";

	static final String COMMAND_SET_DATE = "mmddyy=";

	static final String COMMAND_SET_TIME = "hhmmss=";

	static final String COMMAND_GET_LAST_SAMPLE = "sl";

	static final String COMMAND_GET_SIM_STATUS = "ds";

	static final String COMMAND_GET_CAT_STATUS = "ds";

	static final String RESPONSE_PROMPT = "\r\nS>";

	static final String RESPONSE_OK = "ok\r\nS>";

	static final String RESPONSE_PWRON = "ds\r\nS>"; //End of "wait 4 seconds"

	static final String RESPONSE_START_LOGGING = "t now\r\nS>";// "Start now"
															   // response

	static final String RESPONSE_SAMPLE = "\r\nS>";

	// log4j Logger
	static private Logger _logger = Logger.getLogger(SeaBird37im.class);

	// Configurable Seabird attributes
	Attributes _attributes = new Attributes(this);

	private MicroCAT _sensor[];

	/**
	 * Allocates a new <code>SeaBird37im</code>
	 * 
	 * @throws RemoteException .
	 */
	public SeaBird37im() throws RemoteException {
		_numSensors = 10; //default number of MicroCATs
	}

	/**
	 * Override of abstract method which sets the number of sensors this driver
	 * has determined it needs to support. The super class needs to know this in
	 * order to control acquireSample.
	 * 
	 * @param numSensors
	 *            the number of slaved instruments supported by the driver.
	 */
	protected void setNumSensors(int numSensors) {
		_numSensors = numSensors; // Update super class
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
		return RESPONSE_SAMPLE.getBytes();
	}

	/** Specify maximum bytes in raw sample. */
	protected int initMaxSampleBytes() {
		return (MAX_CAT_RESPONSE_BYTES + 2) * _numSensors;
	}

	/** Specify current limit in increments of 120 mA upto 11880 mA. */
	protected int initCurrentLimit() {
		return 1000;
	}

	/** Return initial value of instrument power policy. */
	protected PowerPolicy initInstrumentPowerPolicy() {
		return PowerPolicy.WHEN_SAMPLING;
	}

	/** Return initial value of communication power policy. */
	protected PowerPolicy initCommunicationPowerPolicy() {
		return PowerPolicy.ALWAYS;
	}

	/** Return specifier for default sampling schedule. */
	protected ScheduleSpecifier createDefaultSampleSchedule()
			throws ScheduleParseException {
		// Sample every 10 minutes by default
		return new ScheduleSpecifier(600000);
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
		_logger.info("Initializing " + _numSensors + " MicroCATs... ");

		setSampleTimeout(CAT_RESPONSE_TIME);
		setMaxSampleTries(MAX_RETRIES);
		setMaxSampleBytes((MAX_CAT_RESPONSE_BYTES + 2) * _numSensors);

		// Turn on DPA power/comms
		managePowerWake();

		// Gain attention of SIM
		try {
			sendCommand(COMMAND_FORCE_PROMPT);
			sendCommand(COMMAND_PWR_ON);
		} catch (Exception e) {
			// No point in carrying on if can't communicate w/ SIM
			throw new InitializeException("initializeInstrument(): " + e);
		}

		// If timeSynch then set time
		if (_attributes.timeSynch || _attributes.sampleInterval != 0) {
			for (int i = 0; i < _numSensors; i++) {
				try {
					// Stop logging
					sendCommand(_sensor[i].getUnitId(), COMMAND_STOP_LOGGING,
							RESPONSE_OK);
					sleep(500);
					_sensor[i].setStatus(MicroCAT.Status.IDLE);

					if (_attributes.sampleInterval != 0) {
						// Set sampling intervals
						String cmdStr = COMMAND_SET_INTERVAL
								+ _attributes.sampleInterval / 10
								+ _attributes.sampleInterval % 10;

						sendCommand(_sensor[i].getUnitId(), cmdStr, RESPONSE_OK);
						sleep(500);
					}

					if (_attributes.timeSynch) {
						String cmdStr = COMMAND_SET_DATE
								+ AsciiTime.getDate("MMDDYY");
						sendCommand(_sensor[i].getUnitId(), cmdStr, RESPONSE_OK);
						sleep(500);
						cmdStr = COMMAND_SET_TIME + AsciiTime.getTime("HHmmss");

						sendCommand(_sensor[i].getUnitId(), cmdStr, RESPONSE_OK);
						sleep(500);
						_logger.debug("Date: " + AsciiTime.getDate("MMDDYY")
								+ " Time: " + AsciiTime.getTime("HHmmss"));
					}

					// Start logging
					sendCommand(_sensor[i].getUnitId(), COMMAND_START_LOGGING,
							RESPONSE_START_LOGGING);

					_sensor[i].setStatus(MicroCAT.Status.LOGGING);
					// Output confirmation that a MicroCAT has been
					// initialized
					_logger.debug("Initialized unit " + _sensor[i].getUnitId());
				} catch (Exception e) {
					_sensor[i].setStatus(MicroCAT.Status.NOT_RESPONDING);
					// Output confirmation that a MicroCAT has failed
					// to be initialized
					_logger.error("Failed to initialize unit "
							+ _sensor[i].getUnitId());
				}
			}
		}

		try {
			sendCommand(COMMAND_PWR_OFF);
		} catch (Exception e) {
			throw new InitializeException("initializeInstrument(): " + e);
		}

		_logger.info("Initializing completed");
	}

	/**
	 * Called by the framework before sampling. Here we use it to power up the
	 * MicroCATs.
	 * 
	 * @exception Exception
	 */
	protected void prepareToSample() throws Exception {
		// Power up the MicroCAT's prior to sampling
		sendCommand(COMMAND_FORCE_PROMPT);
		sleep(500);
		sendCommand(COMMAND_PWR_ON);
		sleep(500);
	}

	/**
	 * Called repetitively (once per sensor) by the framework at the scheduled
	 * interval.
	 * 
	 * Note: The SIM could be in a state where it still waiting for a response
	 * from a silent MicroCAT. COMMAND_FORCE_PROMPT is sent to ensure we get the
	 * attention of the SIM from any state.
	 * 
	 * @param moduleRef
	 *            Value between 0 and numSensors-1
	 * 
	 * @exception TimeoutException
	 * @exception Exception
	 * see setNumSensors(..)
	 */
	protected void requestSample(int moduleRef) throws TimeoutException,
			Exception {

		_logger.info("Sampling ...");
		sendCommand(COMMAND_FORCE_PROMPT);
		if (_sensor[moduleRef].getStatus() == MicroCAT.Status.LOGGING) {

			sendCommand(_sensor[moduleRef].getUnitId(), COMMAND_GET_LAST_SAMPLE);
		}
	}

	/**
	 * Validates the following records from MicroCATs:-
	 * 
	 * <p>
	 * <BLOCKQUOTE>
	 * 
	 * <PRE>
	 * 
	 * TYPE1 = " 00820, 23.6730, 0.00010, 21 Jul 2003, 23:01:28" TYPE2 = "
	 * 00470, 24.3894, 0.00001, 07 Jan 1980, 08:18:24, 25075" TYPE3 = " 01247,
	 * 23.3810, 0.08525, -0.739, 21 Jul 2003, 23:02:50, 24941"
	 * </p>
	 * </BLOCKQUOTE>
	 * 
	 * </PRE>
	 * 
	 * Validation is based on two parameters - sample length and number of
	 * fields
	 * 
	 * @param buffer
	 *            sample data
	 * @param nBytes
	 *            Length of sample data
	 * 
	 * @exception InvalidDataException
	 */

	protected void validateSample(byte[] buffer, int nBytes)
			throws InvalidDataException {

		// Test response lengths and number of fields for type 1,2,3

		//StringTokenizer temp = new StringTokenizer(new
		// String(buffer),",\\r");
		StringTokenizer temp = new StringTokenizer(new String(buffer), ",");
		if ((nBytes == MAX_SAMPLE_TYPE1_LEN && temp.countTokens() == 5)
				|| (nBytes == MAX_SAMPLE_TYPE2_LEN && temp.countTokens() == 6)
				|| (nBytes == MAX_SAMPLE_TYPE3_LEN && temp.countTokens() == 7)) {
			return;
		} else {
			throw new InvalidDataException("Data=" + new String(buffer)
					+ " Length=" + nBytes + " Fields=" + temp.countTokens());
		}
	}

	/**
	 * Called by the framework after sampling. Here we use it instruct the
	 * MicroCATs to power down.
	 * 
	 */
	protected void postSample() {
		try {
			sendCommand(COMMAND_PWR_OFF);
		} catch (Exception e) {
			// do nothing
		}
	}

	protected void printData(byte[] buf) {
		PrintUtils.printFull(buf, 0, buf.length);
	}

	/**
	 * Not implemented
	 * 
	 * @return instrument metadata (byte array)
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
	 * Method to send commands to the SIM card (Surface Inductive Modem).
	 * sendCommand makes every attempt to communicate with the SIM by a process
	 * of resetting, flushing input buffer and resending.
	 * 
	 * Note: Trailing '\r' is automatically added to command string.
	 * 
	 * @param cmd
	 *            Command string to send to SIM
	 * 
	 * @exception Exception
	 *                thrown if the method fails to send the command.
	 */
	private void sendCommand(String cmd) throws Exception {

		for (int i = 0; i < getMaxSampleTries(); i++) {
			// Prepare to send message
			try {
				_fromDevice.flush();
				_toDevice.write(mkCmd(cmd));
				StreamUtils.skipUntil(_fromDevice, RESPONSE_PROMPT.getBytes(),
						SIM_RESPONSE_TIME, MAX_SIM_RESPONSE_BYTES);
				//_logger.debug("sendCommand()1: sent "+new
				// String(mkCmd(cmd))+" did skipUntil "+RESPONSE_PROMPT);
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
				incBadResponseCount();
			}
			// Reset interface using Esc + \r
			_toDevice.write(mkCmd(COMMAND_FORCE_PROMPT));
			sleep(500);
		}
		incRetryExceededCount();
		throw new Exception("sendCommand(" + cmd
				+ ") - Maximum retries attempted");
	}

	/**
	 * Method to send a command to a CAT module (unitId). sendCommand makes
	 * every attempt to communicate with the CAT by a process of resetting,
	 * flushing input buffer and resending.
	 * 
	 * Note: Trailing '\r' is automatically added to command string.
	 * 
	 * @param unitId
	 *            unit id of MicroCAT
	 * @param cmd
	 *            command string to send to MicroCAT
	 * 
	 * @exception Exception
	 * @exception TimeoutException
	 *                thrown if no response from MicroCAT after retries.
	 * @exception NullPointerException
	 *                thrown if passed parameters are uninitialized references -
	 *                retries not attempted in this case.
	 * @exception IOException
	 *                thrown if output stream has problems - retries not
	 *                attempted in this case.
	 */
	private void sendCommand(int unitId, String cmd) throws Exception,
			TimeoutException, NullPointerException, IOException {
		_fromDevice.flush();
		_toDevice.write(mkCmd(unitId, cmd));
		StreamUtils.skipUntil(_fromDevice, "\r\n".getBytes(),
				ECHO_RESPONSE_TIME, mkCmd(unitId, cmd).length + 2);
	}

	/**
	 * Method to send a command to a CAT module (unitId). sendCommand makes
	 * every attempt to communicate with the CAT by a process of resetting,
	 * flushing input buffer and resending.
	 * 
	 * Note: Trailing '\r' is automatically added to command string.
	 * 
	 * @param unitId
	 *            unit id of MicroCAT
	 * @param cmd
	 *            Command string to send to MicroCAT
	 * @param response
	 *            Response string expect from MicroCAT
	 * 
	 * @exception Exception
	 *                thrown if the method fails send the command
	 */
	private void sendCommand(int unitId, String cmd, String response)
			throws Exception {
		for (int i = 0; i < getMaxSampleTries(); i++) {
			// Prepare to send message
			try {
				_fromDevice.flush();
				_toDevice.write(mkCmd(unitId, cmd));
				StreamUtils.skipUntil(_fromDevice, response.getBytes(),
						CAT_RESPONSE_TIME, 0);
				return;
			} catch (IOException e) {
				// This is bad - not sure a retry would help
				_logger.error("IOException " + e);
				throw new Exception("sendCommand(" + cmd
						+ ") - Stream I/O failure");
			} catch (NullPointerException e) { // This is bad - a retry isn't
											   // going to make this one batter
				_logger.error("Null Pointer Exception " + e);
				throw new Exception("sendCommand(" + cmd + ") - Null pointer");
			} catch (TimeoutException e) {
				_logger.error("Timeout Exception: " + e + "Cmd=" + cmd);
				incTimeoutCount();
			} catch (Exception e) { // Probably exceeded max bytes - bad command
									// maybe
				_logger.error("Exception " + e);
				incBadResponseCount();
			}
			// Reset interface using Esc + \r
			_toDevice.write(mkCmd(COMMAND_FORCE_PROMPT));
			sleep(500);
		}
		incRetryExceededCount();
		throw new Exception("sendCommand(" + cmd
				+ ") - Maximum retries attempted");
	}

	/**
	 * Mothod for suspending task.
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
	 * Method to convert a string of unit id's in the following form: -
	 * <p>
	 * <BLOCKQUOTE>
	 * 
	 * <PRE>
	 * 
	 * "1-5,7,9-11" (shorthand for "1,2,3,4,5,7,9,10,11") </BLOCKQUOTE>
	 * 
	 * </PRE>
	 * 
	 * @param idProperty
	 * 
	 * @return Unit IDs (int array)
	 * @exception NumberFormatException
	 * @exception Exception
	 */
	private int[] parseIdProperty(String idProperty)
			throws NumberFormatException, Exception {
		boolean bRange = false;
		int[] ids = new int[MAX_SENSORS];
		StringTokenizer str = new StringTokenizer(idProperty,
				"-, \\t\\n\\r\\f", true);

		String token;
		int i;
		for (i = 0; i < ids.length && str.hasMoreTokens();) {

			token = str.nextToken();
			if (token.equals("-")) {
				bRange = true;
			} else if (token.equals(",") || token.equals(" ")) {
				bRange = false;
			} else {
				int currentId = (new Integer(token)).intValue();
				if (bRange) {
					if (currentId < ids[i - 1]) {
						throw new Exception("Range error");
					}
					for (int from = ids[i - 1] + 1; from <= currentId; from++) {
						ids[i++] = from;
					}
					bRange = false;
				} else {
					ids[i++] = currentId;
				}
			}
		}
		// Truncate array to fit
		int[] temp = new int[i];
		System.arraycopy(ids, 0, temp, 0, i);
		return temp;
	}

	/**
	 * Utility method to construct a CAT message of the form: -
	 * <p>
	 * <BLOCKQUOTE>
	 * 
	 * <PRE>"# + unitId + cmd + \r" </BLOCKQUOTE>
	 * 
	 * </PRE>
	 * 
	 * and returns this as a byte array for transmission
	 * 
	 * @param i
	 *            unit id
	 * @param cmd
	 *            basic command string to construct
	 * 
	 * @return byte array of command
	 */
	private byte[] mkCmd(int i, String cmd) {
		return (new String("#" + i / 10 + i % 10 + cmd + "\r")).getBytes();
	}

	/**
	 * Utility method to construct a SIM message of the form: -
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
	 * @return command (byte array)
	 */
	private byte[] mkCmd(String cmd) {
		return (new String(cmd + "\r")).getBytes();
	}

	/** Configurable Seabird attributes */
	class Attributes extends InstrumentServiceAttributes {

		int sampleInterval = 0;

		String idMicroCATs = "0-9";

		/** Constructor, with required InstrumentService argument */
		Attributes(DeviceServiceIF service) {
			super(service);
		}

		/**
		 * Throw InvalidPropertyException if any invalid attribute values found
		 */
		public void checkValues() throws InvalidPropertyException {

			if (sampleInterval < 0) {
				throw new InvalidPropertyException(sampleInterval
						+ ": invalid sampleInterval. " + "Must be >= 0");
			}

			// Set up sensors
			int[] sensorId;
			try {
				sensorId = parseIdProperty(idMicroCATs);
			} catch (NumberFormatException e) {
				throw new InvalidPropertyException("idMicroCats"
						+ ": Syntax error! [" + idMicroCATs + "]");
			} catch (Exception e) {
				throw new InvalidPropertyException("idMicroCats"
						+ ":Range exception! [" + idMicroCATs + "]");
			}
			_sensor = new MicroCAT[sensorId.length];

			// Initialize sensor array
			for (int i = 0; i < _sensor.length; i++) {
				_sensor[i] = new MicroCAT(sensorId[i]);
				// Assume we're up and running
				_sensor[i].setStatus(MicroCAT.Status.LOGGING);
			}

			// Let base class know this
			setNumSensors(_sensor.length);
		}
	}

	/**
	 * MicroCAT class for containing attributes of each MicroCAT under
	 * supervision
	 * 
	 * @author Martyn Griffiths
	 * @version 1.0
	 */
	class MicroCAT {

		/**
		 * Status of MicroCAT
		 */
		class Status {
			static final int IDLE = 0;

			static final int LOGGING = 1;

			static final int LOW_BATTERY = 2;

			static final int UNKNOWN = 3;

			static final int NOT_RESPONDING = 4;
		}

		/**
		 * Unit id of MicroCAT
		 */
		private int _unitId;

		/**
		 * serial # of MicroCAT
		 */
		private String _serialNum;

		/**
		 * current status of MicroCAT
		 */
		private int _status;

		public MicroCAT() {
			_status = Status.IDLE;
		}

		public MicroCAT(int unitId) {
			this();
			_unitId = unitId;
		}

		public int getStatus() {
			return _status;
		}

		public void setStatus(int status) {
			_status = status;
		}

		public void setSerialNum(String serialNum) {
			_serialNum = serialNum;
		}

		public String getSerialNum() {
			return _serialNum;
		}

		public void setUnitId(int unitId) {
			_unitId = unitId;
		}

		public int getUnitId() {
			return _unitId;
		}
	}

}

