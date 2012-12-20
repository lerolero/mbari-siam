/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.waveSensor;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.core.PuckSerialInstrumentPort;
import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.core.SerialInstrumentPort;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.utils.StopWatch;
import org.mbari.siam.utils.StreamUtils;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.TimeoutException;

/**
 * Implementation of TriAxis heave sensor.
 * 
 * Note that the instrument has the rather unpleasant behavior of automatically
 * entering "sampling" mode after 20 seconds of user inactivity. When in
 * sampling mode, the instrument is incommunicado for at least six minutes.
 * 
 * Hence this service cycles instrument power (via RTS line) as part of the 
 * initializeInstrument() method.
 */
public class HeaveSensor 
    extends PolledInstrumentService 
    implements Instrument {

    static private Logger _log4j = Logger.getLogger(HeaveSensor.class);

    static final byte[] RETRIEVE_SAMPLE = "?MWC\r".getBytes();

    static final byte[] RETRIEVE_SAMPLE_ECHO = "?MWC\r\n".getBytes();

    static final byte[] RETRIEVE_HOUSEKEEPING = "?MWB\r".getBytes();

    static final byte[] RETRIEVE_HOUSEKEEPING_ECHO = "?MWB\r\n".getBytes();

    static final byte[] RETRIEVE_OFFSETS = "?O\r".getBytes();

    static final byte[] RETRIEVE_OFFSETS_ECHO = "?O\r\n".getBytes();

    SerialInstrumentPort _port = null;

    // Time to allow for sample processing (millisec)
    static final int MAX_PROCESS_DURATION = 180 * 1000;

    int _maxTries = 3;

    // Configurable attributes
    Attributes _attributes = new Attributes(this);

    /** Specify device startup delay (millisec) */
    protected int initInstrumentStartDelay() {
	return 20000;
    }

    /** Specify prompt string. */
    protected byte[] initPromptString() {
	return "..".getBytes();
    }

    /** Specify sample terminator. */
    protected byte[] initSampleTerminator() {
	return "..".getBytes();
    }

    /** Specify maximum bytes in raw sample. */
    protected int initMaxSampleBytes() {
	return 1000;
    }

    /** Specify maximum bytes in configuration information. */
    protected int initMaxConfigBytes() {
	return 512;
    }

    /** Specify current limit. */
    protected int initCurrentLimit() {
	return 10000;
    }

    /** Return initial value of instrument power policy. */
    protected PowerPolicy initInstrumentPowerPolicy() {
	return PowerPolicy.ALWAYS;
    }

    /** Return initial value of communication power policy. */
    protected PowerPolicy initCommunicationPowerPolicy() {
	return PowerPolicy.ALWAYS;
    }

    /**
     * Wave sensor constructor.
     */
    public HeaveSensor() throws RemoteException {

	// Initialize member variables from Parents

	try {
	    setSampleTimeout(10000);
	    setMaxSampleTries(3);
	} catch (RangeException e) {
	    _log4j.error(e);
	}

	try {
	    // Only attempt one sample retrieval on any acquisition cycle
	    setMaxSampleTries(1);
	} catch (Exception e) {
	}

    }

    /** Initialize instrument. */
    protected void initializeInstrument() {

	if (_instrumentPort instanceof PuckSerialInstrumentPort) {
	    _log4j.warn("capacitance of Triaxys instrument may cause "
			 + " problems when used with puck");
	}

	_port = (SerialInstrumentPort) _instrumentPort;

	_log4j.info("initializeInstrument(): clear RTS");

	// At startup, disconnect power to the instrument via the 
	// RTS line, since it may be in the midst of sampling, and hence 
	// incommunicado.
	if (_attributes.rtsPower) {
	    _port.setRTS(false);
	}
	else {
	    _instrumentPort.disconnectPower();
	}
	 
	int msec = 10000;
	_log4j.debug("initializeInstrument() - delay " + msec
		     + " before power up");

	StopWatch.delay(msec);

	// Reconnect power
	if (_attributes.rtsPower) {
	    _log4j.info("initializeInstrument(): set RTS");
	    _port.setRTS(true);
	}
	else {
	    _instrumentPort.connectPower();
	}

	try {
	    powerOnCallback();
	}
	catch (Exception e) {
	    _log4j.error("initializeInstrument() - powerOnCallback(): " + e);
	}

	// Set the device's clock
	setClock(System.currentTimeMillis());

	try {
	    _log4j.debug("disable un-needed files");
	    // Disable un-needed file types
	    writeCommand("!F0,0,0,0,0,0,0");
	    StreamUtils.skipUntil(_fromDevice, getPromptString(), 10000);

	    // Set x, y, z offsets
	    _log4j.debug("set x,y,z offsets");
	    writeCommand("!O" + _attributes.xyzOffsets[0] + ","
			 + _attributes.xyzOffsets[1] + ","
			 + _attributes.xyzOffsets[2]);

	    StreamUtils.skipUntil(_fromDevice, getPromptString(), 10000);
	} catch (IOException e) {
	    _log4j.error("initializeInstrument()", e);
	} catch (Exception e) {
	    _log4j.error("while setting file types and offsets", e);
	}
    }

    /** Request a data sample from the instrument. */
    
    protected void requestSample() throws Exception {
	
	// Serial port may be closed during sleep, deasserting RTS.
	// This would cause the instrument to be power cycled.
	// Here it is reinitialized in a controlled way before sampling.
	// klh 07/15/2005
	try{
	    initializeInstrument();
	}catch (Exception e) {
	    _log4j.error("requestSample: initialization failed:", e);
	}

	if (!startSample()) {
	    throw new IOException("HeaveSensor.startSample() failed");
	}
	    
	int wakeupPadSec = 60;

	// Sleep during sample acquisition (NOTE: wake up a little early)
	int sleepSec = _attributes.sampleMinutes * 60 - wakeupPadSec;
	    
	_log4j.debug("Snooze for " + sleepSec + 
		      " sec while Triaxys samples...");
	    
	// Snooze (input sleeptime argument in seconds)
	snooze(sleepSec);

	_log4j.debug("Getting sample results from Triaxys");
	    
	// Set it up so that readSample() just reads the response
	//
	try {
	    _log4j.debug("getPrompt from wave sensor");
		
	    // Wait for on-instrument processing to complete
	    StreamUtils.skipUntil(_fromDevice, getPromptString(),
				  MAX_PROCESS_DURATION + wakeupPadSec*1000);
	    _fromDevice.flush();
		
	    // Get processed sample
	    _log4j.debug("Got prompt, ask for the sample");
	    writeCommand("?MWC");
	    StreamUtils.skipUntil(_fromDevice, RETRIEVE_SAMPLE_ECHO, 10000);
		
	    _log4j.debug("Got sample, now read it");
	    return; // Success
		
	} catch (TimeoutException e) {
	    _log4j.error("Triaxys got timeout");
	    throw e;
	}
    }

    /** Ask device to start acquiring a sample. */
    private boolean startSample() throws Exception {
	boolean sampleStarted = false;

	int ntries = 1;
	while (ntries < 4) {

	    ntries++;

	    try {
		StreamUtils.skipUntil(_fromDevice, getPromptString(), 10000);

		_log4j.debug("startSample(): Prompt detected: "
			      + new String(getPromptString()));

		_fromDevice.flush(); // Probably more dots or stars in the
		// buffer

		writeCommand("!R" + _attributes.sampleMinutes);

		byte[] takeSampleEcho = ("!R" + _attributes.sampleMinutes)
		    .getBytes();

		StreamUtils.skipUntil(_fromDevice, takeSampleEcho, 10000);

		// Prompt string may change after sample acquisition
		setPostSamplePrompt();

		sampleStarted = true;
		break;

	    } catch (Exception toe) {
		_log4j.debug("timeout in startSample(), retry...");

		Thread.sleep(getSampleTimeout());
	    }
	}

	if (sampleStarted)
	    _log4j.debug("startSample() completed successfully...");

	return sampleStarted;
    }

    /** Set prompt string following acquisition of first sample. */
    void setPostSamplePrompt() {

	setPromptString("**".getBytes());
	setSampleTerminator("**".getBytes());
    }

    /** Return parameters to use on serial port. */
    public SerialPortParameters getSerialPortParameters()
	throws UnsupportedCommOperationException {

	return new SerialPortParameters(9600, SerialPort.DATABITS_8,
					SerialPort.PARITY_NONE, SerialPort.STOPBITS_1);
    }

    /** Set Triaxys clock to specified millisec past epoch. */
    public void setClock(long t) {

	// Build the SetTime command for the Triaxsys
	// The format is "!TYY-MM-DD HH:MM:SS", with zeros padded on the left
	// of the 2-digit values
	//

	Date date = new Date(t);

	SimpleDateFormat dateFormatter = new SimpleDateFormat(
							      "yy-MM-dd HH:mm:ss");

	try {
	    String dateString = dateFormatter.format(date);
	    String cmd = "!T" + dateString;
	    _log4j.debug("set time cmd: " + cmd);
	    writeCommand(cmd);
	    // Read back echoed date
	    _log4j.debug("Look for echoed time: " + dateString);

	    StreamUtils.skipUntil(_fromDevice, dateString.getBytes(), 5000);
	} catch (Exception e) {
	    _log4j.error("setClock() failed", e);
	}

	return;
    }

    /** Self-test not implemented. */
    public int test() {
	return Device.OK;
    }

    /** Return specifier for default sampling schedule. */
    protected ScheduleSpecifier createDefaultSampleSchedule()
	throws ScheduleParseException {

	// Sample every 30 minutes by default
	return new ScheduleSpecifier(60 * 30 * 1000);
    }

    /**
     * Get device's notion of its state: a Triaxys housekeeping packet.
     */
    protected byte[] getInstrumentStateMetadata() {

	try {
	    _log4j.debug("_fromDevice.flush - " + _fromDevice.available()
			  + " avail chars");

	    _fromDevice.flush();

	    _log4j.debug("post _fromDevice.flush - " + _fromDevice.available()
			  + " avail chars");

	    _log4j
		.debug("write " + new String(RETRIEVE_HOUSEKEEPING)
		       + " cmd");

	    writeCommand("?MWB");

	    _log4j.debug("skip past '" + new String(RETRIEVE_HOUSEKEEPING)
			  + "' command echo");

	    StreamUtils.skipUntil(_fromDevice, RETRIEVE_HOUSEKEEPING_ECHO,
				  10000);

	    _log4j.debug("readUntil '" + new String(getSampleTerminator())
			  + "' delim");

	    int nBytes = StreamUtils.readUntil(_fromDevice, getSampleBuf(),
					       getSampleTerminator(), getSampleTimeout());

	    String msg = new String(getSampleBuf(), 0, nBytes);

	    _fromDevice.flush();
	    // Retrieve offsets
	    writeCommand("?O");

	    _log4j.debug("skip past '" + new String(RETRIEVE_OFFSETS)
			  + "' command echo");

	    StreamUtils.skipUntil(_fromDevice, RETRIEVE_OFFSETS_ECHO, 10000);

	    nBytes = StreamUtils.readUntil(_fromDevice, getSampleBuf(),
					   getSampleTerminator(), getSampleTimeout());

	    msg += new String(getSampleBuf(), 0, nBytes);
	    _log4j.debug("metadata: " + msg);
	    return msg.getBytes();
	} catch (Exception e) {
	    String err = "Got exception reading Triaxys housekeeping data:\n"
		+ e;

	    _log4j.error(err, e);

	    return err.getBytes();
	}
    }

    /**
     * Called after power is applied; return when instrument is ready for use.
     */
    protected void powerOnCallback() throws Exception {

	// After power-on, prompt is "...."
	setPromptString("..".getBytes());
	setSampleTerminator("..".getBytes());

	try {
	    _log4j.debug("powerOnCallback() - skipUntil() '"
			  + new String(getPromptString()) + "'");

	    StreamUtils.skipUntil(_fromDevice, getPromptString(),
				  getInstrumentStartDelay());

	    _log4j.debug("powerOnCallback() - device is ready");
	} catch (Exception e) {
	    _log4j.error("powerOnCallback(): ", e);
	}
    }

    /**
     * Write specified command to instrument, throttling character flow. NOTE:
     * 'cmd' argument does NOT include carriage-return
     */
    protected void writeCommand(String cmd) throws IOException {

	_log4j.debug("writeCmd(" + cmd + ")");
	cmd += "\r";

	byte[] cmdBytes = cmd.getBytes();

	_toDevice.write(cmdBytes);
    }

    /** Configurable attributes */
    class Attributes extends InstrumentServiceAttributes {

	/** Center of mass translation offsets */
	float[] xyzOffsets = { 0.f, 0.f, 0.f };

	/** Device serial number. */
	String serialNo = null;

	/** Minutes per sample. */
	int sampleMinutes = 5;

	/** Serial RTS line provides power to instrument by default. */
	boolean rtsPower = true;

	/** Constructor, with required InstrumentService argument */
	Attributes(DeviceServiceIF service) {
	    super(service);
	}

	/**
	 * Throw MissingPropertyException if specified attribute is mandatory.
	 */
	public void missingAttributeCallback(String attributeName)
	    throws MissingPropertyException {
	    if (attributeName.equals("serialNo")) {
		throw new MissingPropertyException(attributeName);
	    }
	}

	/**
	 * Throw InvalidPropertyException if any invalid attribute values found
	 */
	public void checkValues() throws InvalidPropertyException {

	    if (xyzOffsets.length != 3) {
		throw new InvalidPropertyException("xyzOffsets: must specify "
						   + "exactly 3 numbers");
	    }

	    if (sampleMinutes < 5 || sampleMinutes > 35) {
		throw new InvalidPropertyException(sampleMinutes
						   + ": invalid sampleMinutes. " + "must be > 5 and < 35");
	    }
	}
    }

    /** Return message regarding power-cycling. */
    protected String shutdownInstrument() throws Exception {
	return "Leave Triaxis power off for at least one minute before" + 
	    " switching on again";
    }
}
