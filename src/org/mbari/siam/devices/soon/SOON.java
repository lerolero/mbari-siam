/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/**
 * @Title SOON Sub-system driver
 * @author Martyn Griffiths, Karen Salamy, Tom O'Reilly, Kent Headley
 *

 */

package org.mbari.siam.devices.soon;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.utils.PrintUtils;
import org.mbari.siam.utils.StopWatch;
import org.mbari.siam.utils.StreamUtils;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.DeviceServiceIF;

/**
 * An embedded microprocessor based datalogger (Onset TattleTale)
 * coordinates the SOON sensors and associated pump and gas valves. 
 * The embedded system also provides serial communications to the host 
 * system so the sampled data can also be transmitted to a PC or mooring 
 * controller. 
 *
 * The subsystem is preconfigured to take a sample set of all 
 * channels on the hour every hour. The sampling cycle can take up to 
 * 10 minutes to perform during which time no serial communication with
 * the instrument should NOT be attempted, as this causes errors in the
 * instrument's operation. 
 * 
 * The instrument has three distinct operating modes 
 * "awake" (or command mode), "asleep" - a low power mode but able to wake 
 * itself up automatically at sampling time and "off" -
 * powered down completely. This last operating mode is not a deployment 
 * option.
 * 
 * To wake the instrument, a single character is sent to the communications
 * port. Provided the unit isn't in the sampling cycle, a response in the form
 * of scheduling information is transmiited to the host. 
 * Then sending a carriage return ('\r') to the unit causes the unit to 
 * enter command mode. The instrument responds with the prompt "SOON>". When in
 * command mode the unit can be interrogated and the last sample retrieved. To
 * exit command mode a "quit" command is sent and the unit returns to the low
 * power sleep mode.
 * 
 * @see org.mbari.siam.distributed.Device
 * @see org.mbari.siam.distributed.Instrument
 * @see org.mbari.siam.distributed.PowerPort
 * @see org.mbari.siam.core.PolledInstrumentService
 */

public class SOON 
    extends PolledInstrumentService implements Instrument {


    private static String _versionID = "$Revision: 1.4 $";

    // Max constants
    static final int MAX_RESPONSE_BYTES = 256;

    // This is about twice the required buffer space
    static final int MAX_SAMPLE_BYTES = 512; 

    static final int MAX_SAMPLE_TRIES = 3;

    static final int MAX_COMMAND_TRIES = 3;

    // Timeouts
    static final int WAKEUP_TIME = 60000;

    static final int RESPONSE_TIME = 60000; // This could be very long but
    // realistically 1 min should suffice

    /** It is only safe to communicate with instrument when it is not sampling.
	Instrument samples for roughly 8 minutes, starting at the top of 
	the hour. We define a "safe" period in terms of minutes past the 
	hour - we've added some pad for safety. Thus, it's OK to communicate 
	from 15 minutes past the hour until 55 minutes past. */
    static final int START_SAFE_PERIOD = 12;
    static final int END_SAFE_PERIOD = 57;

    static final int SAMPLE_RESPONSE_TIME = RESPONSE_TIME;

    /** Instrument collects a sample at the top of every hour. The
	service must NOT attempt to communicate with the device while 
	the device is collecting a sample. This services sets a FIXED,
	absolute schedule which samples at a "safe" time and will override 
	any property value. 
    */
    static final String FIXED_SCHEDULE = "A 0 30 * * * * * * GMT *"; 

    // Commands
    static final String PROMPT_CMD = "";

    // Set Date & Time.
    static final String SET_DATE_CMD = "date"; 

    // Display last Data Buffer.
    static final String ACQUIRE_CMD = "data";

    // Exit user interface and go back to logging.
    static final String SLEEP_CMD = "quit"; 

    // Stop deployment mode, close log file.
    static final String STOPLOG_CMD = "stoplog"; 

    // Deploy - Go into deployment mode (start sampling).
    static final String DEPLOY_CMD = "deploy"; 

    // Deploy - Go into deployment mode (start sampling).
    static final String DEPLOY_CMD_AT = "deployat"; 

    // Reset system - will restart logger.
    static final String RESET_CMD = "reset"; 
  
    // During reset - continues initialization.
    static final String CONTINUE_CMD = "e";

    // Stop internal telemtry
    static final String STOP_TELEM_CMD = "stoptlm";


    static final String DATA_SYNC = ">>";
    static final String SAMPLE_TERMINATOR = "<<";

    static final String RESPONSE_PROMPT = "SOON>";

    // Others

    protected Attributes _attributes = new Attributes(this);

    SimpleDateFormat _dateFormatter = 
	new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");


    protected boolean _initialized = false;

    // Private vars

    // log4j Logger
    static protected Logger _log4j = Logger.getLogger(SOON.class);

    // Scratch buffer
    private byte[] _scratch = new byte[1024];


    /**
     * 
     * @exception RemoteException .
     */
    public SOON() throws RemoteException {
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
	return 1000; //!! to do
    }

    /**
     * Return instrument power policy.
     * 
     * @return instrument power policy
     */
    protected PowerPolicy initInstrumentPowerPolicy() {
	return PowerPolicy.ALWAYS;
    }

    /**
     * Return communications interface power policy.
     * 
     * @return instrument power policy
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
	return new ScheduleSpecifier(FIXED_SCHEDULE);
    }

    /** Return parameters to use on serial port. */
    public SerialPortParameters getSerialPortParameters()
	throws UnsupportedCommOperationException {

	return new SerialPortParameters(9600, SerialPort.DATABITS_8,
					SerialPort.PARITY_NONE, 
					SerialPort.STOPBITS_1);
    }


    /**
     * Called by the framework to initialize the instrument prior to sampling.
     * Normally this method will use properties determined by the setProperties
     * method.
     * 
     * @exception InitializeException
     * @exception Exception
     */
    protected void initializeInstrument() 
	throws InitializeException, Exception {

	setMaxSampleTries(MAX_SAMPLE_TRIES);
	setSampleTimeout(SAMPLE_RESPONSE_TIME);

	initializeDevice();
	// Since initialization can take a long time, launch a worker 
	// thread to initialize instrument and return
	// _log4j.debug("initializeInstrument() - launch initialization thread");
	// new InitializationWorker(this);

	return;
    }


    /**
     * Set the SOON date and time to current date and time.
     * 
     */
    public void setClock(long t) throws NotSupportedException {

	/* NOTE: Ignore 't' argument - since it makes more sense to 
	   get current host time and use that. */
	try {
	    enterCommandMode();

	    String timeString = 
		_dateFormatter.format(new Date(System.currentTimeMillis()));

	    sendCommand(SET_DATE_CMD, "?");
	    sendCommand(timeString);
	}
	catch (Exception e) {
	    _log4j.error("setClock() failed: " + e);
	}
    }


    /**
     * Prepare the device for sampling; called before requestSample(). 
     */
    protected void prepareToSample() throws Exception {
	if (!_initialized) {
	    throw new Exception("Waiting for instrument initialization");
	}
	int waitSec = secondsUntilSafeComms();

	if (waitSec > 30) {
	    throw new Exception("Not safe to sample instrument; try again in  "
				+ waitSec + " seconds");
	}
    }


    /**
     * Issue command to SOON to acquire last sampled data and wait until the
     * data sync characters have been received.
     * 
     * @exception TimeoutException
     * @exception Exception
     *                not thrown
     */
    protected void requestSample() throws TimeoutException, Exception {
	enterCommandMode();
	_log4j.info("Sampling...");
	sendCommand(ACQUIRE_CMD);
	//	sendCommand(ACQUIRE_CMD, DATA_SYNC);
    }


    /**
     * Not implemented
     * 
     * @return instrument metadata (byte array)
     */
    protected byte[] getInstrumentStateMetadata() {

	if (!_initialized) {
	    return "Device not yet initialized".getBytes();
	}

	try {
	    // This method should return any available configuration
	    // information that is available from the instrument
	    enterCommandMode();
	    sendCommand("type config.cfg");
	    int nBytes = StreamUtils.readUntil(_fromDevice, _scratch,
					       RESPONSE_PROMPT.getBytes(),
					       15000);

	    byte[] returnBuf = new byte[nBytes];
	    System.arraycopy(_scratch, 0, returnBuf, 0, nBytes);

	    return returnBuf;
	}
	catch (Exception e) {
	    String err = "Exception while getting instrument metadata: " + 
		e.getMessage();

	    return err.getBytes();
	}

    }//end getInstrumentMetadata()


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
		waitForInstrument();

		_fromDevice.flush();
		_toDevice.write(mkCmd(cmd));
		_toDevice.flush();  // Tom says you must flush when you are done.
		return;

	    } catch (IOException e) { // This is bad - not sure a retry would
		// help
		_log4j.error("IOException" + e);
		throw new Exception("sendCommand(" + cmd
				    + ") - Stream I/O failure");
	    }
	    catch (Exception e) {
		_log4j.error("Exception " + e);
		throw e;
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
		waitForInstrument();
		_fromDevice.flush();
		_toDevice.write(mkCmd(cmd));
		StreamUtils.skipUntil(_fromDevice, rsp.getBytes(),
				      RESPONSE_TIME);
		return;
	    } catch (IOException e) { // This is bad - not sure a retry would
		// help
		_log4j.error("IOException" + e);
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
	}
	// incRetryExceededCount();
	throw new Exception("sendCommand(" + cmd
			    + ") - Maximum retries attempted");
    }

    /**
     * Wakeup SOON. If the instrument is not acquiring data then sending any
     * character out, waiting for a response then resending the character puts
     * the instrument into command mode. If the instrument is acquiring data, 
     * this method will wait until it's safe to communicate.
     * 
     * @exception Exception
     */
    private void enterCommandMode() throws Exception {

	waitForInstrument();

	final int MAX_TRIES = 60;
	_log4j.debug("enterCommandMode() - try to wake instrument");
	for (int i = 0; i < MAX_TRIES; i++) {
	    _fromDevice.flush();

	    // _log4j.debug("enterCommandMode() - send cr");
	    _toDevice.write("\r".getBytes());
	    _toDevice.flush();
	    _toDevice.write("\r".getBytes());
	    _toDevice.flush();

	    try {
		StreamUtils.skipUntil(_fromDevice, 
				      RESPONSE_PROMPT.getBytes(), 
				      500);

		// Got prompt
		_log4j.debug("enterCommandMode() - got prompt");
		return;
	    }
	    catch (TimeoutException e) {
		// Thread.sleep(100);
	    }
	}
	throw new Exception("Failed to wake up SOON");
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

    public int test() {
	return 0;
    }



    /** Waits until it is safe to communicate with instrument. It is NOT
	safe to communicate with instrument near the top of the hour.*/
    protected void waitForInstrument() throws InterruptedException {
	int waitSec = secondsUntilSafeComms();

	if (waitSec > 0) {
	    _log4j.info("waitForInstrument() - wait " + waitSec + 
			" sec before communicating with instrument");

	    snooze(waitSec);

	    _log4j.debug("waitForInstrument() - done with snooze()");
	}

	return;
    }

    /** Return number of seconds before it is safe to communicate with 
	device */
    protected int secondsUntilSafeComms() {

	int minutesPastHour = 
	    (int )((System.currentTimeMillis() / 1000 / 60) % 60);

	_log4j.debug("secondsUntilSafeComms() - minutesPastHour=" + 
		     minutesPastHour);

	if (minutesPastHour > START_SAFE_PERIOD &&
	    minutesPastHour < END_SAFE_PERIOD) {
	    // No need to wait
	    return 0;
	}

	// Compute seconds we must wait to re-enter safe period
	int waitSec = 0;
	if (minutesPastHour > END_SAFE_PERIOD) {
	    waitSec = (60 - END_SAFE_PERIOD + START_SAFE_PERIOD) * 60;
	}
	else {
	    waitSec = (START_SAFE_PERIOD - minutesPastHour) * 60;
	}
	_log4j.info("secondsUntilSafeComms() - " + waitSec + 
		    " sec before safe to communicate");

	return waitSec;
    }

    /** Initialize the device. */
    protected void initializeDevice() throws InitializeException {
	_log4j.info("Initializing pCO2 Instrument...");

	managePowerWake();

	try {
	    // Get to a command prompt (SOON>).
	    _log4j.debug("Entering Command Mode.");
	    enterCommandMode();

	    // Upon power up, at command prompt type reset.
	    _log4j.info("Sending a RESET command.");
	    sendCommand(RESET_CMD, "?");

	    long now = System.currentTimeMillis();

	    String timeString = _dateFormatter.format(new Date(now));

	    _log4j.info("Setting Date and Time to " + timeString);
	    sendCommand(timeString, "PARAMETER");
	    _log4j.info("pCO2 Date & Time set to RTC: " + timeString);

	    // Next, accept prompt with an "e".
	    _log4j.info("Handshaking with SOON instruments - wait...");
	    sendCommand(CONTINUE_CMD, RESPONSE_PROMPT);

	    // Finally, deploy the instrument.
	    _log4j.info("Start deploy now"); 
	    sendCommand(DEPLOY_CMD, "Sampling and telemetry will start");
	    _fromDevice.flush();

	    // Tell controller to cease talking to modem
	    _log4j.info("Stop device modem telemetry"); 
	    sendCommand(STOP_TELEM_CMD);

	    _log4j.info("SOON initialization Complete.");
	    _initialized = true;

	}
	catch(Exception e) {
	    _log4j.error("InitializationWorker.run() - " + e);
	    throw new InitializeException(e.getMessage());
	}
	managePowerSleep();
	return;
    }


    /** Configurable service attributes. */
    class Attributes extends InstrumentServiceAttributes {
		
	Attributes(DeviceServiceIF service) {
	    super(service);
	}

	/** Instrument uses onboard modem. */
	boolean hasModem = false;

	/** Deployment start delay seconds */
	int deployStartSec = 60;

	/** Check attributes and values. Sampling schedule must remain 
	    fixed at default hardcoded value - thus throw 
	    InvalidPropertyException if property attempts to override. */
	protected void setAttributeCallback(String attributeName, 
					    String valueString)
        throws InvalidPropertyException {

	}


    }

    /** This class runs in a separate thread to initialize the SOON, since
     communication opportunities with the instrument are limited. */
    class InitializationWorker implements Runnable {

	SOON _soon = null;

	InitializationWorker(SOON soon) {
	    _soon = soon;
	    Thread thread = new Thread(this);
	    thread.start();
	}


	public void run() {
	    try {
		_soon.initializeDevice();
	    }
	    catch (InitializeException e) {
		_log4j.error(e);
	    }
	}

    }
}

