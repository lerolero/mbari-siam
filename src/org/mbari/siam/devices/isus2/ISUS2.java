/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.isus2;

import java.util.StringTokenizer;
import java.rmi.RemoteException;
import java.io.IOException;
import org.apache.log4j.Logger;
import gnu.io.UnsupportedCommOperationException;
import gnu.io.SerialPort;
import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.utils.StreamUtils;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.NotSupportedException;

/**
Satlantic ISUS-2 instrument service, as operated in TRIGGERED mode. 
Compatible with ISUS firmware version 2.7.1.
This instrument serial interface is menu-driven, resulting in some tricky
state transitions. 
States include:
<pre>
  - Ready: instrument awake, ready to accept single-letter commands
  - InMenu: instrument awake, in main menu mode
  - Asleep: instrument is sleeping
  - Sampling: instrument is awake, aquiring samples
  - Charging: instrument is waking up
</pre>
@author Tom O'Reilly
*/
public class ISUS2 
    extends PolledInstrumentService 
    implements Instrument {

    protected Attributes _attributes = new Attributes(this);

    static private Logger _log4j = Logger.getLogger(ISUS2.class);

    static final String MAIN_MENU_CMD = "M\r";
    static final String MAIN_MENU_PROMPT = "ISUS> [H] ? ";
    static final String MENU_PROMPT = "> [H] ? ";
    static final String ASK_YES = "[Y] ? ";
    static final String ASK_NO = "[N] ? ";
    static final String PASSWORD_PROMPT = "ISUS Password:  ? ";

    static final byte[] TRIGGER_PROMPT = "Waiting for 'g'".getBytes();
    static final byte[] CHARGING_MSG = "Charging power protection".getBytes();
    static final byte[] CHARGING_DONE = "charged".getBytes();
    static final byte[] ISUS_STARTING_MSG = "ISUS will start in".getBytes();
    static final byte[] ISUS_AWAKENED_MSG = "ISUS awakened".getBytes();
    static final byte[] ENTER_MENU_MSG = "Enter 'M' to enter".getBytes();

    static final String FRAME_PREFIX = "SATN";

    static final int MAX_CHARGE_MSEC = 30000;

    // Time to setup sample acquisition
    static final int SAMPLE_SETUP_MSEC = 15000;

    // Time to keep power off
    static final int POWER_OFF_SEC = 30;

    // Approximate time to boot
    static final int BOOT_SEC = 20;

    // Time to go to low-power mode
    static final int LOW_POWER_TRANSITION_SEC = 15;

    /** Allocate plenty of space for each frame */
    final static int MAX_ASCII_FRAME_BYTES = 4096;
    protected byte[] _frameBuffer = new byte[MAX_ASCII_FRAME_BYTES];
    protected byte[] _buffer = new byte[1024];

    // Tracks whether instrument is in "menu mode"
    private boolean _inMenu = false;

    /** Required constructor. */
    public ISUS2() throws RemoteException {
    }

    /** Return sample terminator */
    protected byte[] initSampleTerminator() {
	// Sample is terminated by "ISUS will start in..." message
	return "\r\n".getBytes();
    }

    /** Specify prompt string. */
    protected byte[] initPromptString() {
	return MAIN_MENU_PROMPT.getBytes();
    }

    /** Specify current limit in increments of 120 mA upto 11880 mA. */
    protected int initCurrentLimit() {
	return 4000; //!! to do
    }

    /** Return initial value of instrument power policy. */
    protected PowerPolicy initInstrumentPowerPolicy() {
	return PowerPolicy.ALWAYS;
    }

    /** Return initial value of communication power policy. */
    protected PowerPolicy initCommunicationPowerPolicy() {
	return PowerPolicy.WHEN_SAMPLING;
    }

    /** Specify startup delay (millisec) */
    protected int initInstrumentStartDelay() {
	return 20000;
    }


    /** For some reason, this method is required by the framework */
    public int test() {
	return 0;
    }


    /** Specify maximum bytes in raw sample. */
    protected int initMaxSampleBytes() {
	return _attributes.sampleFrames * MAX_ASCII_FRAME_BYTES;
    }

    /** Return parameters to use on serial port. */
    public SerialPortParameters getSerialPortParameters()
	throws UnsupportedCommOperationException {

	return new SerialPortParameters(38400, SerialPort.DATABITS_8,
					SerialPort.PARITY_NONE, 
					SerialPort.STOPBITS_1);
    }


    /** Initialize the device. */
    protected synchronized void initializeInstrument()
	throws InitializeException, Exception {

	setSampleTimeout(30000);

	// Sample sequence is not amenable to multiple tries
	setMaxSampleTries(1);

	try {

	    // Configure the instrument
	    configureInstrument();

	    // Set the instrument's internal clock
	    setClock();

	    // Get ready for sampling by exiting main menu
	    if (_attributes.rebootOnInit) {
		_log4j.info("Rebooting instrument");
		doCommand("boot", "");
		_inMenu = false;
		snooze(BOOT_SEC);
	    }
	    else {
		_log4j.info("NOT rebooting instrument");
	    }

	    exitMainMenu();
	}
	catch (Exception e) {
	    throw new InitializeException("ISUS2.initializeInstrument() - caught exception" + e);
	}

	_log4j.debug("initializeInstrument() - DONE");

    }


    /** Put instrument in correct mode. */
    protected void configureInstrument() throws Exception {
	_log4j.debug("initializeInstrument() - goto main menu");
	gotoMainMenu();

	// Disable OASIS mode
	_log4j.debug("initializeInstrument() - ensure OASIS mode DISABLED");
	doCommand("config", MENU_PROMPT);
	doCommand("admin", PASSWORD_PROMPT);
	doCommand("dunlin", MENU_PROMPT);
	doCommand("oasis", "?");
	doCommand("0", MENU_PROMPT);
	doCommand("quit", MENU_PROMPT);
	

	// Set to TRIGGERED mode
	_log4j.debug("initializeInstrument() - set TRIGGERED mode");
	doCommand("setup", MENU_PROMPT);
	doCommand("deploy", MENU_PROMPT);
	doCommand("operational", ASK_NO);
	doCommand("yes", "?");
	doCommand("4", MENU_PROMPT);
	doCommand("quit", MENU_PROMPT);
	doCommand("quit", ASK_YES);
	doCommand("yes", ASK_NO);
	doCommand("yes", MENU_PROMPT);

	// Set sampling duration based on assumed 1 frame per second,
	// plus pad with additional seconds. 
	// This service will subsample a specified number of frames,
	// discarding the rest.
	_log4j.debug("initializeInstrument() - set sampling duration");
	doCommand("setup", MENU_PROMPT);
	doCommand("deploy", MENU_PROMPT);
	doCommand("fixed", ASK_NO);
	doCommand("yes", " ? ");
	doCommand(Integer.toString(_attributes.sampleFrames + 60), 
		  MENU_PROMPT);

	doCommand("quit", MENU_PROMPT);
	doCommand("quit", ASK_YES);
	doCommand("yes", ASK_NO);
	doCommand("yes", MENU_PROMPT);
			 
	// Enable output of frames to serial port
	_log4j.debug("initializeInstrument() - enable frames output");
	doCommand("setup", MENU_PROMPT);
	doCommand("output", MENU_PROMPT);
	doCommand("output", ASK_NO);
	// Enable light output
	doCommand("yes", " ? ");
	doCommand("1", MENU_PROMPT);
	// Enable dark output
	doCommand("yes", " ? ");
	doCommand("1", MENU_PROMPT);

	doCommand("quit", MENU_PROMPT);
	doCommand("quit", ASK_YES);
	doCommand("yes", ASK_NO);
	doCommand("yes", MENU_PROMPT);

	// Set frame format to full ascii
	_log4j.debug("initializeInstrument() - set frame format");
	doCommand("setup", MENU_PROMPT);
	doCommand("output", MENU_PROMPT);
	doCommand("frame", ASK_NO);
	doCommand("yes", " ? ");
	doCommand("0", MENU_PROMPT);
	doCommand("quit", MENU_PROMPT);
	doCommand("quit", ASK_YES);
	doCommand("yes", ASK_NO);
	doCommand("yes", MENU_PROMPT);

	// Turn off status messages
	_log4j.debug("initializeInstrument() - turn off status msgs");
	doCommand("setup", MENU_PROMPT);
	doCommand("output", MENU_PROMPT);
	doCommand("status", ASK_NO);
	doCommand("yes", " ? ");
	doCommand("0", MENU_PROMPT);
	doCommand("quit", MENU_PROMPT);
	doCommand("quit", ASK_YES);
	doCommand("yes", ASK_NO);
	doCommand("yes", MENU_PROMPT);
    }


    /** Put instrument in "main menu mode" */
    protected void gotoMainMenu() throws Exception {

	// Possible states
	final int UNKNOWN = -1;
	final int IN_MENU = 0;
	final int WAITING_FOR_TRIGGER = 1;
	final int READY = 2;

	// Determine device state
	int state = UNKNOWN;
	int maxTries = 3;
	for (int i = 0; i < maxTries && state == UNKNOWN; i++) {

	    _fromDevice.flush();
	    _toDevice.write("\r".getBytes());
	    _toDevice.write("\r".getBytes());
	    _toDevice.write("\r".getBytes());
	    _toDevice.flush();

	    int nBytes = 0;

	    _log4j.debug("gotoMainMenu() - readBytes()");
	    nBytes = StreamUtils.readBytes(_fromDevice, _buffer, 
					   0, _buffer.length,  5000);

	    _log4j.debug("gotoMainMenu() - readBytes() returned " + nBytes);

	    String buf = new String(_buffer, 0, nBytes);
	    if (buf.indexOf(MENU_PROMPT) >= 0) {
		// In menu 
		_log4j.debug("gotoMainMenu() - state=IN_MENU");
		state = IN_MENU;
	    }
	    else if (buf.indexOf(new String(TRIGGER_PROMPT)) >= 0) {
		_log4j.debug("gotoMainMenu() - state=WAITING_FOR_TRIGGER");
		state = WAITING_FOR_TRIGGER;
	    }
	    else if (buf.indexOf(new String(ISUS_STARTING_MSG)) >= 0 ||
		     buf.indexOf(new String(ISUS_AWAKENED_MSG)) >= 0 ||
		     buf.indexOf(new String(ENTER_MENU_MSG)) >= 0) {
		    
		_log4j.debug("gotoMainMenu() - state=READY");
		state = READY;
	    }
	    else if (buf.indexOf(new String(CHARGING_MSG)) >= 0) {
		waitForCharge();
		state = READY;
	    }
	    else if (buf.indexOf(FRAME_PREFIX) >= 0) {
		// Looks like a frame - maybe sampling - try to stop it
		_log4j.debug("gotoMainMenu() - issue stop-sampling cmd");
		try {
		    stopSampling();
		    state = READY;
		}
		catch (Exception e2) {
		    state = UNKNOWN;
		    _log4j.info("gotoMainMenu() - caught exception from stopSampling(): " + e2);
		}
	    }

	    else {
		// Unknown state
		try {
		    gotoReady();
		    state = READY;
		}
		catch (Exception e) {
		    _log4j.warn("gotoMainMenu() - state is still unknown");
		}
	    }
	}

	_log4j.debug("gotoMainMenu() - state=" + state);

	switch (state) {

	case IN_MENU:
	    // Go up menu stack until we get to top (main menu)
	    _fromDevice.flush();
	    _toDevice.write("\r".getBytes());
	    _toDevice.flush();
	    while (true) {
		
		int nBytes = StreamUtils.readUntil(_fromDevice, _buffer, 
						   "?".getBytes(), 1500);

		_fromDevice.flush();

		String buf = new String(_buffer, 0, nBytes);
		if (buf.indexOf("ISUS>") >= 0) {
		    // We are at the top menu
		    break;
		}
		else if (buf.indexOf("[H] ") >= 0) {
		    _toDevice.write("quit\r".getBytes());
		    _toDevice.flush();
		}
		else {
		    _toDevice.write("no\r".getBytes());
		    _toDevice.flush();
		}
				      
	    }

	    break;

	case READY:
	    // Got to main menu
	    _log4j.debug("gotoMainMenu() - issue 'M'");
	    doCommand(MAIN_MENU_CMD, MAIN_MENU_PROMPT, 3, 10000);
	    break;

	case WAITING_FOR_TRIGGER:
	    // Trigger acquisition, wait for first frame 
	    // (can't abort until frames start)
	    _log4j.debug("gotoMainMenu() - trigger sampling");
	    requestSample();

	    // Stop acquisition
	    _log4j.debug("gotoMainMenu() - stop acquisition");
	    stopSampling();

	    // Issue "M" to get to main menu
	    _log4j.debug("gotoMainMenu() - issue 'M'");
	    doCommand(MAIN_MENU_CMD, MAIN_MENU_PROMPT, 3, 10000);

	    break;


	default:

	    throw new Exception("ISUS in unknown state");
	}

	_inMenu = true;
    }


    /** Exit main menu */
    protected void exitMainMenu() throws Exception {
	if (!inMenu()) {
	    // Not in menu mode
	    return;
	}

	// Go to the top of the main menu
	gotoMainMenu();

	// Now quit menu mode
	doCommand("quit", "Exit");
	_inMenu = false;
    }


    /** Return true if in menu mode. */
    protected boolean inMenu() {
	return _inMenu;
    }


    /** Return metadata from device. */
    protected byte[] getInstrumentStateMetadata() {

	StringBuffer metadata = new StringBuffer();
	_log4j.debug("getInstrumentStateMetadata()");

	try {
	    _log4j.debug("getInstrumentStateMetadata() - goto main menu");
	    gotoMainMenu();
	    _log4j.debug("getInstrumentStateMetadata() - configure");
	    doCommand("configure", MENU_PROMPT);
	    _fromDevice.flush();
	    _log4j.debug("getInstrumentStateMetadata() - show");
	    _toDevice.write("show\r".getBytes());
	    int nBytes = StreamUtils.readUntil(_fromDevice, _buffer, 
					       "ISUS_CONFIG>".getBytes(),
					       5000);

	    _log4j.debug("getInstrumentStateMetadata() - got show response");
	    metadata.append(new String(_buffer, 0, nBytes));
	    // Go back to top of menu
	    doCommand("quit", MENU_PROMPT);

	    _log4j.debug("getInstrumentStateMetadata() - info");
	    doCommand("info", MENU_PROMPT);
	    _log4j.debug("getInstrumentStateMetadata() - get disk info");
	    _toDevice.write("disk\r".getBytes());
	    nBytes = StreamUtils.readUntil(_fromDevice, _buffer, 
					   "ISUS_INFO>".getBytes(),
					   5000);

	    metadata.append(new String(_buffer, 0, nBytes));
	    doCommand("quit", MENU_PROMPT);
	}
	catch (Exception e) {
	    _log4j.error("getInstrumentStateMetadata() - got exception: " + e);
	}

	try {
	    _log4j.debug("getInstrumentStateMetadata() - exit main menu");
	    exitMainMenu();
	}
	catch (Exception e) {
	    _log4j.error("Caught exception from exitMainMenu(): " + 
			 e.getMessage());
	}

	return new String(metadata).getBytes();
    }


    /** Request a data sample. */
    protected synchronized void requestSample() throws IOException {

	boolean gotIt = false;
	int maxTries = 15;
	for (int i = 0; i < maxTries; i++) {
	    _toDevice.write("x".getBytes());  // Get instrument attention
	    _toDevice.flush();

	    try {
		StreamUtils.skipUntil(_fromDevice, TRIGGER_PROMPT, 1000);
		_log4j.debug("Got trigger prompt");
		gotIt = true;
		break;
	    }
	    catch (Exception e) {
		_log4j.debug("Couldn't get trigger prompt; try #" + i);
	    }
	}
	if (!gotIt) {
	    throw new IOException("Couldn't get trigger prompt after " + 
				  maxTries + " tries");
	}
	boolean charging = false;
	for (int i = 0; i < maxTries; i++) {
	    _toDevice.write("g".getBytes());
	    _toDevice.flush();
	    try {
		StreamUtils.skipUntil(_fromDevice, CHARGING_MSG, 2000);
		// Got it
		_log4j.debug("Triggered sample...");
		charging = true;
		break;
	    }
	    catch (Exception e) {
		_log4j.debug("Didn't trigger acq, try #" + i);
	    }
	}

	if (!charging) {
	    throw new IOException("Couldn't trigger sample");
	}
    
	try {
	    waitForCharge();
	    _log4j.debug("Got charged message");
	}
	catch (Exception e) {
	    throw new IOException("Couldn't get charge-up message");
	}
	_fromDevice.flush();
    }


    /**
     * Read raw sample bytes from serial port into buffer, return number of
     * bytes read. ISUS service reads specified number of frames from 
     * serial port.
     * 
     * @param sample
     *            output buffer
     */
    protected int readSample(byte[] sample) throws TimeoutException,
						   IOException, Exception {


	final byte[] frameTerminator = getSampleTerminator();
	int totalBytes = 0;
	// First frame timeout includes warmup time
	long timeout = SAMPLE_SETUP_MSEC + getSampleTimeout();

	for (int frame = 0; frame < _attributes.sampleFrames; frame++) {

	    _log4j.debug("readSample() - get frame " + frame);

	    // Read next frame into frame buffer
	    int nBytes = StreamUtils.readUntil(_fromDevice, _frameBuffer,
					       frameTerminator,
					       timeout);
		
	    // Subsequent frame timeouts don't include warmup time
	    timeout = getSampleTimeout();

	    _log4j.debug("readSample() - GOT frame " + frame);

	    // Copy from frame buffer to sample buffer
	    System.arraycopy(_frameBuffer, 0, sample, totalBytes, nBytes);

	    totalBytes += nBytes;

	    // Append terminator to each frame (since it's discarded by
	    // StreamUtils.readUntil()).
	    System.arraycopy(frameTerminator, 0, sample, totalBytes,
			     frameTerminator.length);

	    totalBytes += frameTerminator.length;
	}

	// Don't need any more frames - try to stop sampling
	_log4j.debug("readSample() - stop acquisition");
	boolean stopped = false;
	int maxTries = 10;
	for (int i = 0; i < maxTries; i++) {
	    try {
		_fromDevice.flush();
		stopSampling();

		stopped = true;
		break;
	    }
	    catch (Exception e) {
		_log4j.debug("readSample() - didn't stop sampling yet");
	    }
	}
	if (!stopped) {
	    _log4j.error("readSample() - couldn't stop sampling");
	}

	// Wait for low power mode
	_log4j.debug("Wait for low-power mode");
	Thread.sleep(LOW_POWER_TRANSITION_SEC * 1000);

	return totalBytes;
    }


    /** Wait for instrument to complete charging */
    protected void waitForCharge() throws Exception {
	_log4j.debug("Wait for '" + new String(CHARGING_DONE) + "'");
	StreamUtils.skipUntil(_fromDevice, CHARGING_DONE, MAX_CHARGE_MSEC);
    }


    protected void doCommand(String cmd, String response) throws Exception {
	doCommand(cmd, response, 3, 10000);
    }

    /**
     * Method to send commands to the Workhorse; makes every attempt
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
    protected void doCommand(String cmd, String response, int maxTries,
			   int timeoutMsec) throws Exception {

	for (int i = 0; i < maxTries; i++) {
	    // Prepare to send message
	    try {
		_fromDevice.flush();
		_toDevice.write(mkCmd(cmd));
		_toDevice.flush();
		if (response != null && response.length() > 0) {
		    StreamUtils.skipUntil(_fromDevice, response.getBytes(), 
					  timeoutMsec);
		}
		return;

	    } 
	    catch (TimeoutException e) {
		_log4j.error("TimeoutException: " + e + " Cmd=" + cmd +
			     " looking for resp=" + response);
	    }

	    // Reset interface using "\r"
	    _toDevice.write("\r".getBytes());
	    Thread.sleep(500);
	}

	throw new Exception("doCommand(" + cmd + "," + response + 
			    ") - Maximum retries attempted");
    }

    /** Get instrument out of sampling mode. */
    protected void stopSampling() throws Exception {
	int maxTries = 10;
	for (int i = 0; i < maxTries; i++) {
	    Thread.sleep(1000);
	    _toDevice.write("s".getBytes());
	    _toDevice.flush();
	    try {
		StreamUtils.skipUntil(_fromDevice, ISUS_STARTING_MSG, 3000);
		_log4j.debug("stopSampling() - stopped sampling");
		return;
	    }
	    catch (Exception e) {
		_log4j.debug("stopSampling() - not stopped yet");
	    }
	    Thread.sleep(1000);
	}
    }


    /**
     * Utility method to construct a message of the form: -
     * "cmd + \r" and returns this as a byte array for transmission
     * 
     * @param cmd
     *            basic command string to construct
     * 
     * @return byte array of command
     */
    private byte[] mkCmd(String cmd) {
	return (new String(cmd + "\r")).getBytes();
    }


    /** Return specifier for default sampling schedule. */
    protected ScheduleSpecifier createDefaultSampleSchedule()
	throws ScheduleParseException {
	return new ScheduleSpecifier(600 * 1000);
    }


    /** Set device's clock to current time; can throw NotSupportedException. */
    public void setClock() 
	throws NotSupportedException {
	
	try {
	    gotoReady();
	    setDeviceClock();
	}
	catch (Exception e) {
	    _log4j.error("setClock() - caught exception: " + e.getMessage());
	}
    }


    /** Set device clock to current time; assumes current instrument
	state accepts 'T' command. */
    protected void setDeviceClock() throws Exception {
	String cmd = "t," + System.currentTimeMillis()/1000;
	doCommand(cmd, "ACK");
    }


    /** Return message regarding power-cycling. */
    protected String shutdownInstrument() throws Exception {
	return "Leave ISUS power off for at least one minute before" + 
	    " switching on again";
    }


    /** Go to "ready" state */
    protected void gotoReady() throws Exception {

	if (inMenu()) {
	    exitMainMenu();
	    return;
	}

	int maxTries = 3;
	for (int i = 0; i < maxTries; i++) {
	    _fromDevice.flush();
	    _toDevice.write("W".getBytes());
	    _toDevice.flush();

	    try {
		StreamUtils.skipUntil(_fromDevice, "ACK".getBytes(), 1000);
		// Ready
		_inMenu = false;
		return;
	    }
	    catch (Exception e) {
		_log4j.debug("gotoReady() - not ready yet");
	    }

	    
	    try {
		// Might be charging
		StreamUtils.skipUntil(_fromDevice, "+++".getBytes(), 4000);
		// Powering up
		_log4j.debug("gotoReady() - wait for charge");
		waitForCharge();

	    }
	    catch (Exception e) {
		_log4j.warn("gotoReady() - Didn't get 'charged' message");
		// Try again
		continue;
	    }

	    try {
		// Expect to see this - but not always???
		StreamUtils.skipUntil(_fromDevice,
				      "Enter 'M' to enter Menu".getBytes(), 
				      2000);
	    }
	    catch (Exception e) {
		_log4j.debug("gotoReady() - didn't get 'Enter M' prompt");
	    }

	    _log4j.debug("gotoReady() - Ready");
	    _inMenu = false;
	    return;
	}

	throw new Exception("gotoReady() failed");
    }


    /** 
     * Configurable ISUS attributes.
     * @author oreilly
     *
     */	
    class Attributes extends InstrumentServiceAttributes {
		
	Attributes(DeviceServiceIF service) {
	    super(service);
	}

	/** Frames per sample; note that every 11th frame is dark. */
	int sampleFrames = 4;

	/** Reboot after intializing */
	boolean rebootOnInit = false;

    }

}

