/**
 * @Title GasHound Sub-system driver
 * @author Martyn Griffiths
 *
 * Copyright MBARI 2003
 * 
 * REVISION HISTORY:
 * $Log: GashoundSubsystem.java,v $
 * Revision 1.5  2009/07/16 17:43:17  headley
 * javadoc syntax fixes
 *
 * Revision 1.4  2009/07/16 15:55:30  headley
 * javadoc syntax fixes
 *
 * Revision 1.3  2009/07/16 15:07:40  headley
 * javadoc syntax fixes
 *
 * Revision 1.2  2009/07/16 05:33:59  headley
 * javadoc syntax fixes
 *
 * Revision 1.1  2008/11/04 22:17:51  bobh
 * Initial checkin.
 *
 * Revision 1.1.1.1  2008/11/04 19:02:04  bobh
 * Initial release of SIAM2, the release that was modularized to isolate hardware dependencies, and refactored to make most packages point to org.mbari.siam.
 *
 * Revision 1.23  2006/04/21 07:04:00  headley
 * converted System.x.println to log4j  and/or change _logger to _log4j
 *
 * Revision 1.22  2006/03/21 22:25:17  salamy
 * Removed references to deprecated MAX_SAMPLE_BYTES and added a toDevice.flush() in sendCommand() and enterCommand().
 *
 * Revision 1.21  2006/03/06 19:50:20  salamy
 * Changed a few comments.
 *
 * Revision 1.20  2006/03/06 18:43:37  salamy
 * Added toDevice.flush() command within sendCommand().
 *
 * Revision 1.19  2006/02/10 18:15:06  headley
 * moved variable "now" which was breaking build
 *
 * Revision 1.18  2006/02/09 22:48:38  salamy
 * Added a few info statements during initialization.
 *
 * Revision 1.17  2006/02/08 19:32:42  salamy
 * Enabled initialization of instrument by service.
 *
 * Revision 1.16  2006/01/31 23:20:46  salamy
 * Changed pCO2 Instrument Power Policy to ALWAYS.
 *
 * Revision 1.15  2005/09/01 00:48:40  headley
 * minor cleanup --
 * - removed unused methods
 * - replaced setTime() with setClock()
 *
 * Revision 1.14  2004/10/15 20:14:41  oreilly
 * Reformatted, organized imports
 *
 * Revision 1.13  2004/08/18 22:12:32  oreilly
 * Use _toDevice.write(), _fromDevice.flush()
 *
 * Revision 1.12  2004/03/16 17:05:05  headley
 * fixed newline (previously checked in via windows)
 *
 * Revision 1.11  2003/10/15 00:21:24  mrisi
 * changed Thread.yield() to StopWatch.delay(50) to try to fix threading problem
 *
 * Revision 1.10  2003/10/10 23:45:23  martyn
 * Refined log message types
 *
 * Revision 1.9  2003/10/10 18:24:24  martyn
 * Adjusted debug some messages - cosmetic.
 *
 * Revision 1.8  2003/10/09 22:29:29  mrisi
 * added a Thread.yield() to a polling loop
 *
 * Revision 1.7  2003/10/06 23:05:29  mrisi
 * modified to use StreamUtils.skipUntil
 *
 * Revision 1.6  2003/10/03 20:31:22  martyn
 * Documentation
 *
 * Revision 1.5  2003/10/03 00:50:28  martyn
 * Mainly updating print statements to use log4j logging
 * Some tweaks.
 *
 * Revision 1.4  2003/09/18 20:37:45  martyn
 * Improved documentation
 * Fixed enterCommandMode - failed when Gashound sampling
 * Miscellaneous cleanup
 *
 * Revision 1.3  2003/09/15 23:38:46  martyn
 * Improved AsciiTime utility class
 * Updated all drivers to use revised class.
 * Removed registryName service property
 * Moved service property keys to ServiceProperties
 *
 * Revision 1.2  2003/09/11 21:29:07  martyn
 * Gashound logging stopped before updating time and date
 * Restarted in postSample()
 *
 * Revision 1.1  2003/09/10 22:53:14  martyn
 * Initial submission of Gashound subsystem instrument driver.
 * Note: This version successfully captures data
 *
 */

package org.mbari.siam.devices.gashound;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.rmi.RemoteException;

import org.mbari.siam.core.InstrumentService;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.utils.AsciiTime;
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
import org.mbari.siam.distributed.NotSupportedException;

/**
 * An Onset Tattletale, an embedded microprocessor based datalogger, coordinates
 * the Gashound sensor and associated pump and gas valves. The embedded system
 * also provides serial communications to the host system so the sampled data
 * can also be transmitted to a PC or mooring controller. The subsystem is
 * preconfigured to take a sample set of all channels on the hour every hour.
 * The sampling cycle can take upto 10 minutes to perform during which time no
 * communications with the outside world is possible. The instrument has three
 * distinct operating modes "awake" (or command mode), "asleep" - a low power
 * mode but able to wake itself up automatically at sampling time and "off" -
 * powered down completely. This last operating mode is not a deployment option.
 * To wake the instrument, a single character is sent to the communications
 * port. Provided the unit isn't in the sampling cycle, a response in the form
 * of scheduling information is transmiited to the host. As soon as the host
 * receives the first character of the response the host sends a second
 * character. Sending this second character to the unit causes the unit to enter
 * command mode. The instrument responds with the prompt "SOON>". When in
 * command mode the unit can be interrogated and the last sample retrieved. To
 * exit command mode a "quit" command is sent and the unit returns to the low
 * power sleep mode.
 * 
 * @see org.mbari.siam.distributed.Device
 * @see org.mbari.siam.distributed.Instrument
 * @see org.mbari.siam.distributed.PowerPort
 * @see org.mbari.siam.core.InstrumentService
 */

public class GashoundSubsystem extends InstrumentService implements Instrument {

    // Max constants
    static final int MAX_RESPONSE_BYTES = 256;

    static final int MAX_SAMPLE_BYTES = 512; // This is about twice the required
    // buffer space

    static final int MAX_SAMPLE_TRIES = 3;

    static final int MAX_COMMAND_TRIES = 3;

    static final int MAX_ATTENTION_TRIES = 20; // Once a minute

    // Timeouts
    static final int WAKEUP_TIME = 60000;

    static final int SYNC_TIME = 2000;

    static final int RESPONSE_TIME = 60000; // This could be very long but
    // realistically 1 min should
    // suffice

    static final int SAMPLE_RESPONSE_TIME = RESPONSE_TIME;

    // Default Properties
    static final String DEFAULT_SCHEDULE = "A 0 50 * * * * * * GMT *"; // sample
    // 10mins
    // before
    // the
    // hour

    // Commands
    static final String COMMAND_PROMPT = "";

    static final String COMMAND_SET_DATE = "date"; // Set Date & Time.

    static final String COMMAND_ACQUIRE = "data"; // Display last Data Buffer.

    static final String COMMAND_SLEEP = "quit"; // Exit user interface and go back to logging.

    static final String COMMAND_STOPLOG = "stoplog"; // Stop deployment mode, close log file.

    static final String COMMAND_DEPLOY = "deploy"; // Deploy - Go into deployment mode (start sampling).

    static final String COMMAND_RESET = "reset"; // Reset system - will restart logger.
  
    static final String COMMAND_CONTINUE = "e"; // During reset - continues initialization.

    // Command Sequence

    // Responses
    static final String SAMPLE_TERMINATOR = "<<";

    static final String RESPONSE_PROMPT = "SOON>";

    // Others
    static final String DATA_SYNC = ">>";

    // Private vars

    // log4j Logger
    static private Logger _log4j = Logger.getLogger(GashoundSubsystem.class);

    private boolean _bInitialized = false;

    /**
     * 
     * @exception RemoteException .
     */
    public GashoundSubsystem() throws RemoteException {
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
     * @return Instrument Power Policy
     */
    protected PowerPolicy initInstrumentPowerPolicy() {
	return PowerPolicy.ALWAYS;
    }

    /**
     * Return communications interface power policy.
     * 
     * @return Instrument Power Policy

     */
    protected PowerPolicy initCommunicationPowerPolicy() {
	return PowerPolicy.WHEN_SAMPLING;
    }

    /**
     * Return specifier for default sampling schedule.
     * 
     * @return Schedule specifier
     * @exception ScheduleParseException
     */
    protected ScheduleSpecifier createDefaultSampleSchedule()
	throws ScheduleParseException {
	return new ScheduleSpecifier(DEFAULT_SCHEDULE);
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
	_log4j.info("Initializing pCO2 Instrument...");
	String now="";
	try{
	   setMaxSampleTries(MAX_SAMPLE_TRIES);
	   setSampleTimeout(SAMPLE_RESPONSE_TIME);

    	   // Get to a command prompt (SOON>).
	   _log4j.debug("Entering Command Mode.");
	   enterCommandMode();

           // Upon power up, at command prompt type reset.
	   _log4j.debug("Sending a RESET command.");
	   sendCommand(COMMAND_RESET);

	   // First, set the date and time.
           now = AsciiTime.getDate("MM/DD/YYYY") + " "
		    + AsciiTime.getTime("HH:mm:ss");
	   _log4j.debug("Setting Date and Time.");
           sendCommand(now, "?");
	   _log4j.info("pCO2 Date & Time set to RTC: " + now);

           // Next, accept prompt with an "e".
	   _log4j.debug("Not setting additional pCO2 parameters, continuing with initialization.");
           sendCommand(COMMAND_CONTINUE, "PARAMETER.");

           // Finally, deploy the instrument.
	   sendCommand(COMMAND_DEPLOY);

	    }catch(Exception e){
		_log4j.error(e);
	    }

	_log4j.info("pCO2 Initialization Complete.");
    }

    /**
     * Set the Gashound date and time to the sidearm's date and time.
     * Note: if t>0L, currently does nothing
     * 
     * @exception NotSupportedException
     */
    public void setClock(long t) throws NotSupportedException {
	if(t<0L){
	    try{
		String now = AsciiTime.getDate("MM/DD/YY") + " "
		    + AsciiTime.getTime("HH:mm:ss");
		sendCommand(COMMAND_SET_DATE, "?");
		sendCommand(now);
	    }catch(Exception e){
		_log4j.error(e);
	    }
	}
    }

    /**
     * Wakeup the Gashound in preparation to sample. If this is the first time a
     * sample has been taken then synchronize Gashound time w/ Sidearm time.
     * 
     * @exception Exception
     */
    protected void prepareToSample() throws Exception {
	_log4j.info("Preparing to sample...");
	enterCommandMode();
	if (!_bInitialized) {
	    sendCommand(COMMAND_STOPLOG); // Note: Restarted in postSample()
	    // can/should setClock() be done each time it samples?
	    // If only done at init, clock drift is more likely to occur.
	    //  - headley 8/31/2005
	    setClock(-1L);
	}
	_log4j.info("Preparing to sample completed");
    }

    /**
     * Issue command to Gashound to acquire last sampled data and wait until the
     * data sync characters have been received.
     * 
     * @exception TimeoutException
     * @exception Exception
     *                not thrown
     */
    protected void requestSample() throws TimeoutException, Exception {
	_log4j.info("Sampling...");
	sendCommand(COMMAND_ACQUIRE, DATA_SYNC);
    }

    /**
     * Deal with Gashound after sample received... If this is the first time a
     * sample has been taken then re-deploy the Gashound otherwise set the
     * Gashound to sleep mode.
     * 
     */
    protected void postSample() {
	try {
	    sendCommand(COMMAND_PROMPT);
	    if (!_bInitialized) {
		sendCommand(COMMAND_DEPLOY, COMMAND_DEPLOY);// just sync on echo
		// as no prompt
		// returned
		_bInitialized = true;
	    } else
		sendCommand(COMMAND_SLEEP, COMMAND_SLEEP);// just sync on echo
	    // as no prompt
	    // returned
	} catch (Exception e) {
	    _log4j.error("postSample() failed: " + e);
	}
    }

    /**
     * getInstrumentMetadata Not implemented fror gashound
     * 
     */
    protected byte[] getInstrumentMetadata() {

	// This method should return any available configuration
	// information that is available from the instrument

	return "Not supported".getBytes();
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
		_fromDevice.flush();
		_toDevice.write(mkCmd(cmd));
		_toDevice.flush();  // Tom says you must flush when you are done.
		StreamUtils.skipUntil(_fromDevice, RESPONSE_PROMPT.getBytes(), RESPONSE_TIME);
		return;
	    } catch (IOException e) { // This is bad - not sure a retry would
		// help
		_log4j.error("IOException" + e);
		throw new Exception("sendCommand(" + cmd
				    + ") - Stream I/O failure");
	    } catch (NullPointerException e) { // This is bad - a retry isn't
		// going to make this one better
		_log4j.error("Null Pointer Exception " + e);
		throw new Exception("sendCommand(" + cmd + ") - Null pointer");
	    } catch (TimeoutException e) {
		_log4j.error("Timeout Exception: " + e + "Cmd=" + cmd);
	    } catch (Exception e) { // Probably exceeded max bytes - bad command
		// maybe
		_log4j.error("Exception " + e);
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
		_toDevice.flush();  // Tom says you must flush when you are done.
		StreamUtils.skipUntil(_fromDevice, rsp.getBytes(), RESPONSE_TIME);
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
	//incRetryExceededCount();
	throw new Exception("sendCommand(" + cmd
			    + ") - Maximum retries attempted");
    }

    /**
     * Wakeup Gashound. If the Gashound is not acquiring data then sending any
     * character out, waiting for a response then resending the character puts
     * the Gashound into command mode. If the Gashound is acquiring data, any
     * response could take upto 10 minutes.
     * 
     * 
     * @exception Exception
     */
    private void enterCommandMode() throws Exception {

	for (int i = 0; i < MAX_ATTENTION_TRIES; i++) {
	    _fromDevice.flush();

	    byte[] ch = new byte[1];
	    ch[0] = '!';// Any character will do to wake the hound up
	    _toDevice.write(ch);
	    _toDevice.flush();  // Tom says you must flush when you are done.
	    long t0 = System.currentTimeMillis();
	    while ((System.currentTimeMillis() - t0) < WAKEUP_TIME) {
		if (_fromDevice.available() != 0) {
		    // wack it again to get the command prompt
		    _toDevice.write(ch);
		    StreamUtils.skipUntil(_fromDevice, RESPONSE_PROMPT.getBytes(), RESPONSE_TIME);
		    return;
		} else {
		    //Thread.yield();
		    StopWatch.delay(50);
		}
	    }
	}
	throw new Exception("Failed to wake up Gashound!");
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

}

