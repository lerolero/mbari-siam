/** 
 * @Title WetLabs ECO series instrument driver
 * @author Martyn Griffiths
 *
 * Copyright MBARI 2003
 * 
 * REVISION HISTORY:
 * $Log: WetLabsECO.java,v $
 * Revision 1.6  2009/07/16 15:07:00  headley
 * javadoc syntax fixes
 *
 * Revision 1.5  2009/01/22 00:43:05  oreilly
 * issue _toDevice.flush() after every _toDevice.write()
 *
 * Revision 1.4  2008/12/15 22:50:28  oreilly
 * beware shadowing attributes variable
 *
 * Revision 1.1.1.1  2008/11/04 19:02:04  bobh
 * Initial release of SIAM2, the release that was modularized to isolate hardware dependencies, and refactored to make most packages point to org.mbari.siam.
 *
 * Revision 1.29  2008/09/12 23:58:40  oreilly
 * 'protected' access to attributes, to allow subclass access
 *
 * Revision 1.28  2007/02/15 22:49:33  oreilly
 * Use SimpleDateFormat instead of AsciiTime
 *
 * Revision 1.27  2006/11/01 17:58:50  oreilly
 * date format is (apparently) 'mmddyy'
 *
 * Revision 1.26  2006/11/01 17:57:26  oreilly
 * if not checking cmd response, wait a bit after issuing cmd. Changed setTime() to setClock()
 *
 * Revision 1.25  2006/10/21 01:27:50  oreilly
 * Fixed format of int command - no colons
 *
 * Revision 1.24  2006/10/21 01:27:28  oreilly
 * Fixed format of int command - no colons
 *
 * Revision 1.23  2006/05/05 20:50:50  salamy
 * Added log4J comments on SAFE mode process.
 *
 * Revision 1.22  2006/04/18 18:05:13  brian
 * Commented out unused variables. removed unsed imports
 *
 * Revision 1.21  2006/04/14 23:55:31  brian
 * Added DevicePacketParser fields
 *
 * Revision 1.20  2006/03/31 22:34:03  oreilly
 * Shorten timeouts. Only try power-cycle at initialization
 *
 * Revision 1.19  2006/03/30 03:27:31  oreilly
 * Call enterCommandMode() from enterSafeMode()
 *
 * Revision 1.18  2006/03/27 22:13:55  oreilly
 * Various fixes
 *
 * Revision 1.17  2006/03/06 18:06:21  oreilly
 * resumeNormalMode() removed from Safeable
 *
 * Revision 1.16  2006/03/05 04:29:59  oreilly
 * Simplified service, added configurable properties
 *
 * Revision 1.15  2006/02/14 19:20:22  oreilly
 * Minor source code format fix
 *
 * Revision 1.14  2004/12/28 21:20:16  oreilly
 * Fixed error message
 *
 * Revision 1.13  2004/10/15 20:20:25  oreilly
 * utilizes ServiceAttributes framework
 *
 * Revision 1.12  2004/08/18 22:18:38  oreilly
 * Use _toDevice.write(), _fromDevice.flush()
 *
 * Revision 1.11  2004/05/20 00:45:44  oreilly
 * flush input buffer while in getInstrumentStateMetadata()
 *
 * Revision 1.10  2004/05/18 22:43:00  mrisi
 * flush input buffer before taking a sample, Tom and Kent said it was OK
 *
 * Revision 1.9  2004/02/27 01:23:47  headley
 * changed schedule specifier to ms
 *
 * Revision 1.8  2003/10/11 00:10:41  martyn
 * Refined log message types
 *
 * Revision 1.7  2003/10/10 18:24:24  martyn
 * Adjusted debug some messages - cosmetic.
 *
 * Revision 1.6  2003/10/06 23:06:20  mrisi
 * modified to use StreamUtils.skipUntil
 *
 * Revision 1.5  2003/10/03 00:50:29  martyn
 * Mainly updating print statements to use log4j logging
 * Some tweaks.
 *
 * Revision 1.4  2003/09/17 00:07:04  martyn
 * Added postSample - this waits until all trailing (uncaptured) data has been sent
 * Increased SYNC_TIME
 *
 * Revision 1.3  2003/09/15 23:38:46  martyn
 * Improved AsciiTime utility class
 * Updated all drivers to use revised class.
 * Removed registryName service property
 * Moved service property keys to ServiceProperties
 *
 * Revision 1.2  2003/09/10 22:38:14  martyn
 * Added averageCount as a new property
 *
 * Revision 1.1  2003/08/29 01:34:55  martyn
 * Initial version - initializing and sampling w/ FL series
 *
 *
 */

package org.mbari.siam.devices.wetlabs;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.utils.AsciiTime;
import org.mbari.siam.utils.StreamUtils;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.DevicePacketParser;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.Safeable;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.TimeoutException;

/**
   Implements instrument service for WET Labs ECO instrument. 
   Instrument begins emitting samples at power-up or when the "$run" 
   command is issued; the number of samples that are emitted can be 
   modified by configuration. Thus the instrument has both "streaming" and
   "polled" characteristics. This implementation extends the 
   PolledInstrumentService class.
 */

public class WetLabsECO 
    extends PolledInstrumentService 
    implements Instrument, Safeable {

    SimpleDateFormat _dateFormatter = new SimpleDateFormat("MMddyy");
    SimpleDateFormat _timeFormatter = new SimpleDateFormat("HHmmss");

    // Max constants
    static final int MAX_SAMPLE_TRIES = 3;

    static final int MAX_COMMAND_TRIES = 3;

    static final int MAX_SAMPLE_BYTES = 100;

    // Must wait 60 seconds before turning on power
    static final int POWER_OFF_SEC = 60;

    static final int RESPONSE_TIME = 60000; // This could be very long but
    // realistically 1 min should
    // suffice

    static final int SAMPLE_RESPONSE_TIME = RESPONSE_TIME;

    // Commands
    static final String ENABLE_LOG_CMD = "$rec 1";
    static final String DISABLE_LOG_CMD = "$rec 0";

    static final String SET_DATE_CMD = "$dat ";

    static final String SET_TIME_CMD = "$clk ";

    static final String PKTS_PER_SET_CMD = "$set ";

    static final String SAMPLES_PER_PKT_CMD = "$pkt ";

    static final String AVERAGE_COUNT_CMD = "$ave ";

    static final String PACKET_INTERVAL_CMD = "$int ";

    static final String MENU_CMD = "$mnu";

    static final String STORE_CMD = "$sto";

    static final String RUN_CMD = "$run";

    static final String DATA_SYNC = "\r\n"; // End of Line

    static final String RESPONSE_DONE = "done";

    // Scratch buffer
    byte[] _scratch = new byte[512];

    // log4j Logger
    static private Logger _log4j = Logger.getLogger(WetLabsECO.class);

    protected Attributes _attributes = new Attributes(this);

    /**
     * Allocates a new <code>WetLabsECO</code>
     * 
     * @throws RemoteException .
     */
    public WetLabsECO() throws RemoteException {
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
	return menuPrefix().getBytes();
    }

    /** Specify maximum bytes in raw sample. */
    protected int initMaxSampleBytes() {
	
	return MAX_SAMPLE_BYTES * 
	    _attributes.samplesPerPacket * 
	    _attributes.packetsPerSet;
    }

    /** Specify current limit in increments of 120 mA upto 11880 mA. */
    protected int initCurrentLimit() {
	return 1000; //!! to do
    }

    /** Return initial value of instrument power policy. */
    protected PowerPolicy initInstrumentPowerPolicy() {
	return PowerPolicy.WHEN_SAMPLING;
    }

    /** Return initial value of communication power policy. */
    protected PowerPolicy initCommunicationPowerPolicy() {
	return PowerPolicy.WHEN_SAMPLING;
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

	return new SerialPortParameters(19200, SerialPort.DATABITS_8,
					SerialPort.PARITY_NONE, 
					SerialPort.STOPBITS_1);
    }

    /**
     * Called by the framework to initialize the instrument prior to sampling.
     * 
     * @exception InitializeException
     * @exception Exception
     */
    protected void initializeInstrument() 
	throws InitializeException, Exception {

	_log4j.info("Initializing...");

	try {
	    setMaxSampleBytes(initMaxSampleBytes());
	} catch (RangeException e) {
	    throw new InitializeException("max sample bytes out of range");
	}

	//String response, property;
	//String[] token;

	setMaxSampleTries(MAX_SAMPLE_TRIES);
	setSampleTimeout(SAMPLE_RESPONSE_TIME);

	try {
	    enterCommandMode();
	}
	catch (Exception e) {
	    // Turn off power for prescribed time. If this doesn't
	    // we'll leave this method with an exception
	    _log4j.info("Power off for " + POWER_OFF_SEC + " sec");
	    _instrumentPort.disconnectPower();
	    snooze(POWER_OFF_SEC);
	    _log4j.info("Power on instrument");
	    _instrumentPort.connectPower();
	
	    enterCommandMode();
	}

	try {

	    if (_instrumentAttributes.timeSynch) {
		setClock();
	    }

	    // Set to 1 packet per set
	    doCommand(PKTS_PER_SET_CMD + 
		      ((Attributes)_instrumentAttributes).packetsPerSet);

	    // Set to x measurements per pkt
	    doCommand(SAMPLES_PER_PKT_CMD + 
		      ((Attributes)_instrumentAttributes).samplesPerPacket);

	    // Set to x measurements / sample
	    _log4j.debug("initalizeInstrument(): " + 
			 AVERAGE_COUNT_CMD + 
			 ((Attributes)_instrumentAttributes).averageCount);

	    doCommand(AVERAGE_COUNT_CMD + 
		      ((Attributes)_instrumentAttributes).averageCount);

	    // Set interval between packets
	    doCommand(PACKET_INTERVAL_CMD + 
		      ((Attributes)_instrumentAttributes).packetInterval);

	    // Enable/disable logging to instrument FLASH
	    if (((Attributes)_instrumentAttributes).logEnabled) {
		doCommand(ENABLE_LOG_CMD);
	    }
	    else {
		doCommand(DISABLE_LOG_CMD);
	    }

	    doCommand(STORE_CMD, RESPONSE_DONE);

	} 
	catch (Exception e) {
	    _log4j.error("initializeInstrument(): " + e);
	    throw new InitializeException(e.getMessage());
	} 

	_log4j.info("Initializing completed");
    }


    /** Set instrument's time and date. */
    public synchronized void setClock() throws NotSupportedException {

	Date date = new Date(System.currentTimeMillis());


	try {
	    _log4j.debug(SET_DATE_CMD + " " + _dateFormatter.format(date));
	    doCommand(SET_DATE_CMD + " " + _dateFormatter.format(date));
	}
	catch (Exception e) {
	    throw new NotSupportedException("error while setting date");
	}

	try {
	    _log4j.debug(SET_TIME_CMD + " " + _timeFormatter.format(date));
	    doCommand(SET_TIME_CMD + " " + _timeFormatter.format(date));
	}
	catch (Exception e) {
	    throw new NotSupportedException("error while setting time");
	}	
    }

    /**
     * 
     * @exception TimeoutException
     *                thrown if no data is detected for a period of twice the
     *                _preemptionTime
     * @exception Exception
     *                not thrown
     * see setNumSensors
     * see _preemptionTime
     */
    protected void requestSample() throws TimeoutException, Exception {

	_log4j.debug("Sampling...");
	_fromDevice.flush();
	doCommand(RUN_CMD);
    }


    /** Get instrument state information from the device itself. */
    protected byte[] getInstrumentStateMetadata() {

	try {
	    enterCommandMode();
	    try {
		Thread.sleep(1000);
	    }
	    catch (Exception e) {
	    }
	    _fromDevice.flush();

	    // Return menu values
	    doCommand(MENU_CMD);

	    int nBytes = 
		StreamUtils.readBytes(_fromDevice, 
				      _scratch, 
				      0, _scratch.length, 
				      3000);

	    // Trim buffer
	    String tmp = new String(_scratch, 0, nBytes);

	    return tmp.getBytes();
	}
	catch (Exception e) {
	    return e.getMessage().getBytes();
	}
    }


    /**
     * Not implemented. Samples are locally timestamped
     * 
     * @param t
     */
    public void setClock(long t) {
	return;
    }


    /**
     * Method to send commands to the instrument. doCommand makes every
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
    protected void doCommand(String cmd, String response) 
	throws Exception {

	for (int i = 0; i < MAX_COMMAND_TRIES; i++) {
	    // Prepare to send message
	    try {
		_fromDevice.flush();
		_toDevice.write(mkCmd(cmd));
		_toDevice.flush();

		if (response.length() == 0) {
		    // Done 
		    return;
		}

		StreamUtils.skipUntil(_fromDevice, 
				      response.getBytes(),
				      5000);

		// Clear rest of response
		_fromDevice.flush();
		return;
	    } catch (IOException e) { // This is bad - not sure a retry would
		// help
		_log4j.error("IOException " + e);
		throw new Exception("doCommand(" + cmd
				    + ") - Stream I/O failure");
	    } catch (NullPointerException e) { // This is bad - a retry isn't
		// going to make this one batter
		_log4j.error("Null Pointer Exception " + e);
		throw new Exception("doCommand(" + cmd + ") - Null pointer");
	    } catch (TimeoutException e) {
		_log4j.error("Timeout Exception: " + e + "Cmd=" + cmd);
	    } catch (Exception e) { // Probably exceeded max bytes - bad command
		// maybe
		_log4j.error("Exception " + e);
		//incBadResponseCount();
	    }
	}
	//incRetryExceededCount();
	throw new Exception("doCommand(" + cmd
			    + ") - Maximum retries attempted");
    }


    /** Execute specified command, don't verify response. */
    protected void doCommand(String command) 
	throws Exception {

	final int waitMsec = 2000;

	doCommand(command, "");

	// Wait a little bit for instrument to process command
	_log4j.debug("doCommand() - wait " + waitMsec + 
		     " msec for processing");

	Thread.sleep(waitMsec);
    }

    /**
       Put instrument into command mode. Note that it may be necessary to
       cycle instrument power to get into command mode.
     */
    protected void enterCommandMode() throws Exception {

	doCommand(MENU_CMD);
	try {
	    StreamUtils.skipUntil(_fromDevice, menuPrefix().getBytes(),
				  1000);

	    _fromDevice.flush();

	    // Already in command mode
	    _log4j.debug("enterCommandMode() - already in command mode");
	    return;
	}
	catch (Exception e) {
	    // Now try to get instrument into command mode
	}

	try {
	    _log4j.debug("enterCommandMode() - doSoftBreak()");
	    doSoftBreak();
	    return;
	}
	catch (Exception e) {
	    _log4j.debug("enterCommandMode() - no menu");
	    throw new Exception("Couldn't get to menu mode");
	}

    }


    /** Issue "soft break" until instrument goes into command mode. */
    protected void doSoftBreak() throws Exception {
	final int maxTries = 5;
	for (int i = 0; i < maxTries; i++) {
	    try {
		// Issue 7 since first 2 get lost while ECO powering up
		_log4j.debug("doSoftBreak() - issue !s");
		for (int j = 0; j < 7; j++) {
		    _toDevice.write("!".getBytes());
		    _toDevice.flush();
		    Thread.sleep(200);
		}
		// Look for menu 
		StreamUtils.skipUntil(_fromDevice, 
				      menuPrefix().getBytes(),
				      5000);
		// Got menu - return
		_log4j.debug("doSoftBreak() - now in menu");
		return;

	    } 
	    catch (TimeoutException e) {
		_log4j.info("doSoftBreak() - no menu prompt yet");
	    } 
	    catch (Exception e) {
		// maybe
		_log4j.error("Exception " + e);
	    }
	}
	_log4j.error("doSoftBreak() failed");
	throw new Exception("doSoftBreak() failed");
    }



    /**
       Called after power is applied; return when instrument is ready for use.
       When instrument is powered on, it will stream data in accordance
       with instrument settings. Since we are treating this as a 
       "polled" instrument, we disable streaming when the instrument 
       powers up.
     */
    protected void powerOnCallback() {

	try {
	    // Stop streaming
	    enterCommandMode();
	}
	catch (Exception e) {
	    _log4j.error("powerOnCallback(): " + e);
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
    protected byte[] mkCmd(String cmd) {
	return (new String(cmd + "\r")).getBytes();
    }


    public int test() {
	return 0;
    }


    /** Enter mode for resource-restricted environement. */
    public synchronized void enterSafeMode() throws Exception {

        _log4j.info("enterSafeMode() - Setting WetLab ECO-Triplet Instrument to SAFE mode.");


        _log4j.info("enterSafeMode() - Getting WetLab ECO-Triplet attention.");
	enterCommandMode();
        _log4j.info("enterSafeMode() - Setting Workhorse to log internally for SAFE mode.");
	doCommand(ENABLE_LOG_CMD);
	doCommand(SAMPLES_PER_PKT_CMD + " 10");
	doCommand(PACKET_INTERVAL_CMD + " 006000");

	// Store new settings
        _log4j.info("enterSafeMode() - Saving WetLab ECO-Triplet Parameters.");
	doCommand(STORE_CMD, RESPONSE_DONE);
        _log4j.info("enterSafeMode() - Deploying WetLab ECO-Triplet in SAFE mode NOW.");
    }

    /** Return message regarding power-cycling. */
    protected String shutdownInstrument() throws Exception {
	return "Leave instrument power off for at least one minute before" + 
	    " switching on again";
    }


    /** Return leading string of menu */
    String menuPrefix() {
	return "Ver";
    }
    
    public DevicePacketParser getDevicePacketParser() throws NotSupportedException {
        // TODO Auto-generated method stub
        return super.getDevicePacketParser();
    }

    /** Configurable attributes */
    protected class Attributes extends InstrumentServiceAttributes {

	/** Number of measurements in each packet ("$ave") */
	int averageCount = 50;

	/** Number of samples in a packet ("$pkt"). */
	int samplesPerPacket = 3;

	/** Number of times to repeat packet ("$set") */
	int packetsPerSet = 1;

	/** Interval between packets - must be expressed as hhmmss - 
	 NO COLONS! */
	String packetInterval = "000000";

	/** Enable or disable data logging to instrument FLASH. */
	boolean logEnabled = true;

	/** Constructor, with required InstrumentService argument */
	Attributes(DeviceServiceIF service) {
	    super(service);
	}


	/**
	 * Throw InvalidPropertyException if any invalid attribute values found
	 */
	public void checkValues() throws InvalidPropertyException {

	    if (samplesPerPacket <= 0) {
		throw new InvalidPropertyException(samplesPerPacket
						   + ": invalid samplesPerPacket. " + "must be > 0");
	    }
	    if (averageCount <= 0) {
		throw new InvalidPropertyException(averageCount
						   + ": invalid averageCount. " + "must be > 0");
	    }

	}
    }
}

