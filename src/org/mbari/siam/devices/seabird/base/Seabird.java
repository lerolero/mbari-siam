/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.seabird.base;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.distributed.RangeException;
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
import org.mbari.siam.distributed.DevicePacketParser;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.PacketParser;

import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.utils.StreamUtils;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

public abstract class Seabird
    extends PolledInstrumentService
    implements Instrument {

    // CVS revision 
    private static String _versionID = "$Revision: 1.7 $";

    static private Logger _log4j = Logger.getLogger(Seabird.class);

    protected byte[] _getPrompt = "\r\n".getBytes();

    SimpleDateFormat _dateFormatter = new SimpleDateFormat("MMddyy");
    SimpleDateFormat _timeFormatter = new SimpleDateFormat("HHmmss");

    /**
     * Command to sample depends on whether sample is logged to instrument
     * FLASH (see initializeInstrument())
     */
    protected byte[] _requestSample = null;

    protected byte[] _requestSampleEcho = null;

    protected byte[] _sendLast = "SL\r".getBytes();

    protected byte[] _sendLastEcho = "SL\r\n".getBytes();

    protected byte[] _quitSession = "QS\r".getBytes();

    protected byte[] _quitSessionEcho = "QS".getBytes();

    protected byte[] _getStatusInfo = "DS\r".getBytes();

    protected byte[] _getStatusEcho = "DS\r\n".getBytes();

    protected byte[] _haltSample = "STOP\r".getBytes();

    protected byte[] _haltSampleEcho = "STOP".getBytes();

    protected byte[] _startNow = "Startnow\r".getBytes();

    protected byte[] _startNowEcho = "start now".getBytes();

    protected String _setSampleInterval = "INTERVAL=";// is this the same for SBE37SM and SBE16plus?

    protected int _maxStatusBytes = 2000;

    protected int _maxCalBytes = 2000;

    protected long MIN_AUTO_SAMPLE_INTERVAL_SEC = 6;
    protected long MAX_AUTO_SAMPLE_INTERVAL_SEC = 21600;

    protected Attributes _attributes = new Attributes(this);

    private DevicePacketParser devicePacketParser;

    protected boolean _safeMode=false;

    /**
     * Constructor.
     */
    public Seabird() throws RemoteException {

        try {
	    // timeout for getting a sample
            setSampleTimeout(_attributes.sampleTimeoutMsec);
            // fix
            setMaxSampleTries(3);
        }
        catch (RangeException e) {
            _log4j.error("RangeException: ", e);
        }
    }

    /**
     * Return initial value for instrument's "prompt" character.
     */
    protected byte[] initPromptString() {
        return "S>".getBytes();
    }

    /**
     * Return initial value for instrument's sample terminator
     */
    protected byte[] initSampleTerminator() {
        return "\r\n".getBytes();
    }

    /**
     * Return initial value of DPA current limit.
     */
    protected int initCurrentLimit() {
        return 5000;
    }

    /**
     * Return initial value of instrument power policy.
     */
    protected PowerPolicy initInstrumentPowerPolicy() {
        return PowerPolicy.ALWAYS;
    }

    /**
     * Return initial value of instrument power policy.
     */
    protected PowerPolicy initCommunicationPowerPolicy() {
        return PowerPolicy.WHEN_SAMPLING;
    }

    /**
     * Return initial value of instrument startup time in millisec.
     */
    protected int initInstrumentStartDelay() {
        return 2000;
    }

    /**
     * Return initial value for maximum number of bytes in a instrument data
     * sample.
     */
    protected int initMaxSampleBytes() {
        return 1000;
    }

    protected void requestSample() {
        try {

	    // Wake up asleep and
	    // Verify connection...
	    _log4j.debug("requestSample(): looking for prompt...");
	    getPrompt(3);

            // Request sample...
	    // if in autonomous mode, it won't respond while sampling,
	    // so retries may be necessary.
            _log4j.debug("requestSample(): sending sample request...");
            _toDevice.write(_requestSample);
            _toDevice.flush();
            _log4j.debug("sent " + new String(_requestSample)
			 + " looking for " + new String(_requestSampleEcho));
            // Skip echoed command...
            StreamUtils.skipUntil(_fromDevice, _requestSampleEcho,
				  _attributes.promptTimeoutMsec);
    
        }
        catch (Exception e) {
            _log4j.error("requestSample() caught Exception", e);
        }
        return;
    }

    protected boolean getPrompt(int retries){
	_log4j.debug("getPrompt("+retries+")-");
	boolean gotPrompt = false;
        for (int i = 0; i < retries; i++) {
            try {
                getPrompt();
                gotPrompt = true;
                break;
            }
            catch (Exception e) {
            }
        }
	return gotPrompt;
    }

    protected void getPrompt() throws TimeoutException, IOException,
				      NullPointerException, Exception {
        _log4j.debug("getPrompt-");
	//        _fromDevice.flush();
        _toDevice.write(_getPrompt);
	_toDevice.flush();
        StreamUtils.skipUntil(_fromDevice, getPromptString(),
			      _attributes.promptTimeoutMsec);

        _log4j.debug("getPrompt done");
    }


    /** Method to send a command and wait for the prompt.  May throw exception if
     *  it doesn't find a prompt.
     */
    public void sendCommand(String cmd)
	throws TimeoutException, NullPointerException, IOException, Exception 
    {
        _log4j.debug("Sending cmd: " + cmd);
        _toDevice.write(cmd.getBytes());
	_toDevice.flush();
	// Look for response
	StreamUtils.skipUntil(_fromDevice, getPromptString(), 10000);
    }

    /** The base Seabird service has no commands that require confirmation.
     *  This method is a placeholder for those subclasses that require it.
     *  The default implementation is to just call sendCommand.
     */
    public void sendCommandConfirm(String cmd)
	throws TimeoutException, NullPointerException, IOException, Exception 
    {
	sendCommand(cmd);
    }

    /**
     * Stop autonomous instrument sampling
     */
    protected void stopAutonomousLogging() throws IOException, Exception,
					 TimeoutException {

	_log4j.debug("stop sampling-");

	// Verify connection...
	getPrompt(3);
	
	// Halt Sampling...
	// OK to do this while not sampling
	_toDevice.write(_haltSample);
	_toDevice.flush();
	StreamUtils.skipUntil(_fromDevice, getPromptString(), 1000);
	_log4j.debug("stop sampling done");

    }

    /**
     * Start autonomous sampling, per current Sample Mode
     * note: only conditionally allowed:
       - _safeMode=true
       - autonomous=true
     */
    protected void startAutonomousLogging() throws IOException, Exception,
					   TimeoutException {

        if ( _safeMode==true || _attributes.autonomous==true) {
	    _log4j.debug("start autoSample-");
	    // Verify connection...
	    getPrompt(3);
	    
	    // Autonomous sampling
	    // only allow if sample mode going to safeMode or operating in
	    // autonomous logging mode
            _toDevice.write(_startNow);
            _toDevice.flush();
	    _log4j.debug("start autoSample done");
        }else{
	    _log4j.debug("start autoSample skipped");
	}
    }

    /**
     * Set sample interval for autonomous logging
     * NOTE: may stop, but does NOT resume logging or quit session
     */
    protected void setAutonomousSampleInterval(long periodSec) throws IOException, Exception,
					   TimeoutException {

	_log4j.debug("set autoSample interval-");

	if ( (periodSec < MIN_AUTO_SAMPLE_INTERVAL_SEC) ||
	     (periodSec > MAX_AUTO_SAMPLE_INTERVAL_SEC) ){
	    throw new Exception("Invalid Autonomous Sample Interval ("+periodSec+"); value must be between "+
				MIN_AUTO_SAMPLE_INTERVAL_SEC+" and "+MAX_AUTO_SAMPLE_INTERVAL_SEC+" sec");
	}
	// stop autonomous logging
	stopAutonomousLogging();

        // Verify connection...
        getPrompt(3);

	String interval=_setSampleInterval+periodSec+"\r\n";
	_toDevice.write(interval.getBytes());
	_toDevice.flush();

	_log4j.debug("set autoSample interval done");
	return;

    }

    /**
     * QuitSession, putting instrument to lowest sampling power state
     */
    protected void quitSession() throws IOException, Exception,
					TimeoutException {

	_log4j.debug("quit session-");

        // quit session
        _toDevice.write(_quitSession);
        _toDevice.flush();

	_log4j.debug("quit session done");
    }

    /**
     * Set Seabird's clock.
     * Note: may stop, but does not resume logging or quit session
     */
    public void setClock() {

	long currentTime=System.currentTimeMillis();

        Date date = new Date(currentTime);

        try {
	    // stop autonomous logging
	    stopAutonomousLogging();

            String cmd = "MMDDYY=" + _dateFormatter.format(date) + "\r";
            _log4j.debug("set date cmd: " + cmd);
            _toDevice.write(cmd.getBytes());
            _toDevice.flush();
            cmd = "HHMMSS=" + _timeFormatter.format(date) + "\r";
            _log4j.debug("OK, now set time cmd: " + cmd);
            _toDevice.write(cmd.getBytes());
            _toDevice.flush();

        }
        catch (Exception e) {
            _log4j.error("setClock() failed", e);
        }
    }

    /**
     * Set data output format
     * Note: may stop, but does not resume logging or quit session
     */
    public void setDataFormat() throws IOException, Exception {

	// stop autonomous logging
	stopAutonomousLogging();

        // Set data format
        _toDevice.write(getFormatForSummaryCmd());
        _toDevice.flush();	
    }

    /**
     * Self-test routine; not yet implemented.
     */
    public int test() {
        return Device.OK;
    }

    /**
     * override the default behavior
     * If logging autonomously, return the instrument to sleep
     * after sampling.
    */
    protected void postSample() {
	if(_attributes.autonomous==true)
	try{
	    quitSession();
	}catch(Exception e){
	    _log4j.error("postSample - quitSession failed - "+e);
	}
    }

    /**
     * Initialize the seabird.
     */
    protected void initializeInstrument() throws Exception {

	// reset driver safeMode flag
	_safeMode=false;
	
        setSampleTimeout(_attributes.sampleTimeoutMsec);

	if(_attributes.autonomous){
            // configure to take sample, DO NOT log to instrument flash
	    // could use SL command, but TS ensures fresh sample
	    // and is recognized in autonomous sampling mode.
            _requestSample = "TS\r".getBytes();
            _requestSampleEcho = "TS\r\n".getBytes();
	    if(_attributes.log==true){
		throw(new Exception("Polled logging (TSS) not supported in autonomous logging mode; set log=false or autonomous=false."));
	    }
	}else{
	    // Stop autonomous logging by instrument (If was in safeMode).
	    _log4j.info("initializeInstrument() - Getting SBE's attention (Stops any previous logging).");
	    if (_attributes.log==true) {
		// Take sample, log to instrument's FLASH
		_requestSample = "TSS\r".getBytes();
		_requestSampleEcho = "TSS\r\n".getBytes();
	    }
	    else {
		// Take sample, do NOT log to instrument's FLASH
		_requestSample = "TS\r".getBytes();
		_requestSampleEcho = "TS\r\n".getBytes();
	    }
	}
        _log4j.debug("initializeInstrument(): _requestSample cmd = " +
		     new String(_requestSample));

        // Turn on DPA power/comms
        managePowerWake();

        boolean gotPrompt = getPrompt(3);
        if (!gotPrompt) {
            _log4j.error("initializeInstrument() - couldn't get prompt");
        }

	stopAutonomousLogging();

	setDataFormat();

        // Set the Seabird clock to "now"
        setClock();

	// Set autonomous sample interval, if applicable
	if(_attributes.autonomous==true){
	    setAutonomousSampleInterval(_attributes.autoSampleIntervalSec);
	    startAutonomousLogging();
	}
    }

    /**
     * Return parameters to use on serial port.
     */
    public SerialPortParameters getSerialPortParameters()
	throws UnsupportedCommOperationException {

        return new SerialPortParameters(9600, SerialPort.DATABITS_8,
					SerialPort.PARITY_NONE,
					SerialPort.STOPBITS_1);
    }

    /**
     * Return specifier for default sampling schedule.
     */
    protected ScheduleSpecifier createDefaultSampleSchedule()
	throws ScheduleParseException {

        // Sample every 30 seconds by default
        return new ScheduleSpecifier(30000);
    }

    /**
     * Get device's notion of its state: a Seabird status packet.
     */
    protected byte[] getInstrumentStateMetadata() {
	

	// Try to get prompt
	boolean gotPrompt = getPrompt(3);

	if (!gotPrompt) {
	    // Couldn't get instrument prompt
	    return "Couldn't get prompt".getBytes();
	}

	try {

            // Get status/config info...
            _log4j.debug("Requesting Status Info...");
            _toDevice.write(_getStatusInfo);
            _toDevice.flush();

            // Skip command echo...
            StreamUtils.skipUntil(_fromDevice, _getStatusInfo,
				  _attributes.promptTimeoutMsec);

            byte[] statusBuf = new byte[_maxStatusBytes];

            int statusBytes =
		StreamUtils.readUntil(_fromDevice, statusBuf,
				      getPromptString(), getSampleTimeout());


            // Get cal info...
            _log4j.debug("Requesting Cal Info...");
            _toDevice.write(getCalibrationCmd());
            _toDevice.flush();

            // Skip command echo...
            StreamUtils
		.skipUntil(_fromDevice, getCalibrationCmd(),
			   _attributes.promptTimeoutMsec);

            byte[] calBuf = new byte[_maxCalBytes];
            int calBytes =
		StreamUtils.readUntil(_fromDevice, calBuf,
				      getPromptString(), getSampleTimeout());

            // copy the sample into the exactly-right-sized byte array
            byte[] stateBuf = new byte[statusBytes + calBytes];
            System.arraycopy(statusBuf, 0, stateBuf, 0, statusBytes);
            System.arraycopy(calBuf, 0, stateBuf, statusBytes, calBytes);

            return stateBuf;

        }
        catch (Exception e) {

            String err = "Caught Exception while reading status/calibration data: "
		+ e.getMessage();

            _log4j.error(err, e);
            return err.getBytes();
        }
    }


    /**
     * Return CTD packet parser.
     */
    public PacketParser getParser() {
        return new SeabirdPacketParser();
    }


    protected abstract byte[] getFormatForSummaryCmd();


    protected abstract byte[] getCalibrationCmd();

    public org.mbari.siam.distributed.DevicePacketParser getDevicePacketParser() throws NotSupportedException {
        if (devicePacketParser == null) {
            devicePacketParser = new DevicePacketParser();
        }
        return devicePacketParser;    //To change body of overridden methods use File | Settings | File Templates.
    }

    protected class Attributes extends InstrumentServiceAttributes {


        public Attributes(DeviceServiceIF service) {
            super(service);
            summaryVars = new String[]{DevicePacketParser.TEMPERATURE,
				       DevicePacketParser.CONDUCTIVITY,
				       DevicePacketParser.PRESSURE,
				       DevicePacketParser.VOLTAGE0,
				       DevicePacketParser.VOLTAGE1,
				       DevicePacketParser.VOLTAGE2,
				       DevicePacketParser.VOLTAGE3};
        }

        /**
	   Flag indicates if data is logged internally 
	   (but still polled; use TSS if true, TS if false)
         */
        public boolean log = true;

	/** Operate in autonomous sampling mode if true 
	    (log data even if not polled; use SL) */
	public boolean autonomous=false;

	/** Sample period to use in autonomous sampling mode (not safe mode)
	    Value must be 

	    MIN_AUTO_SAMPLE_INTERVAL_SEC <= n <= MAX_AUTO_SAMPLE_INTERVAL_SEC

	    Currently, these are set to 6 s and 21600 s respectively.
	 */
	public int autoSampleIntervalSec=600;

	/** Sample interval (sec) used in safe mode (logs autonomously) */
        public int safeSampleIntervalSec = 300; //Default to 5 minutes.

	/** Sample timeout (millisec) */
	public int sampleTimeoutMsec = 15000;

	/** Get-prompt timeout (millisec) */
	public int promptTimeoutMsec = 8000;

	/**
	 * Called when all attributes have been parsed. Throw
	 * InvalidPropertyException if any invalid attribute values found
	 */
	public void checkValues() throws InvalidPropertyException {
	    StringBuffer errBuf=new StringBuffer();
	    int errCount=0;
	    /* Validate attribute values 
	       If errors found, add a line to the buffer and bump 
	       the error count -- might as well find them all
	       at once.
	    */
	    if(autoSampleIntervalSec<MIN_AUTO_SAMPLE_INTERVAL_SEC ||
	       autoSampleIntervalSec>MAX_AUTO_SAMPLE_INTERVAL_SEC){
		errBuf.append("Invalid autoSampleIntervalSec: must be "+MIN_AUTO_SAMPLE_INTERVAL_SEC+"<= n <="+MAX_AUTO_SAMPLE_INTERVAL_SEC+"\n");
		errCount++;
	    }
	    if(autonomous==true && log==true){
		errBuf.append("Conflicting values for autonomous and log: set one of these false\n");
		errCount++;
	    }

	    /* If errors were found, prepend the error count
	       and throw an exception indicating all the attribute
	       errors
	     */
	    if(errCount>0){
		errBuf.insert(0,"Invalid attributes found ["+errCount+" errors]");
		throw new InvalidPropertyException(errBuf.toString());
	    }
	}
    }


    public class DevicePacketParser extends org.mbari.siam.distributed.DevicePacketParser {
	public static final int MAX_AUX_CHAN = 8;
        public static final String TEMPERATURE = "temperature";
        public static final String CONDUCTIVITY = "conductivity";
        public static final String PRESSURE = "Pressure";
        public static final String VOLTAGE0 = "Voltage-0";
        public static final String VOLTAGE1 = "Voltage-1";
        public static final String VOLTAGE2 = "Voltage-2";
        public static final String VOLTAGE3 = "Voltage-3";

        protected void parseFields(DevicePacket packet) throws NotSupportedException, Exception {
            if (packet instanceof SensorDataPacket) {

                /*
                 * We're parsing Temperature and Conductivity and ignoring the other fields for now.
                 *
                 * TODO this assumes OUTPUTFORMAT=3 (SBE-16) or FORMAT=1(SBE37); we may need to support other formats too.
                 */

                final SensorDataPacket p = (SensorDataPacket) packet;
                final String data = new String(p.dataBuffer());
                final StringTokenizer tokenizer = new StringTokenizer(data, ",");
                final Float temperature = new Float(tokenizer.nextToken());
                final Float conductivity = new Float(tokenizer.nextToken());
                final Float pressure = new Float(tokenizer.nextToken());

                addMeasurement(TEMPERATURE, "Temperature", "C [ITS-90]", temperature);
                addMeasurement(CONDUCTIVITY, "Conductivity", "S/m", conductivity);
                addMeasurement(PRESSURE, "Pressure", "decibars", pressure);

		for (int auxChan = 0; auxChan < MAX_AUX_CHAN; auxChan++) {
		    if (tokenizer.hasMoreTokens()) {
			try {
			    Float voltage = 
				new Float(tokenizer.nextToken());

			    addMeasurement("Voltage-" + auxChan,
					   "Voltage " + auxChan,
					   "volts", voltage);
			}
			catch (NumberFormatException e) {
			    // Not a number - no more channels
			    break;
			}
			
		    }
		    else {
			// No more channels
			break;
		    }
		}
                
            }

        }
    }

}
