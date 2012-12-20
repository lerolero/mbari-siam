package org.mbari.siam.devices.seabird.streamingSBE19;

/**
 * @Title SIAM Driver for Seabird SBE19plus in Profile (Streaming) mode
 * @author Bob Herlien
 * @version 1.0
 * @date 09/24/2009
 *
 * Copyright MBARI 2009
 */


import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.core.SerialInstrumentPort;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.core.StreamingInstrumentService;
import org.mbari.siam.distributed.measurement.Averager;
import org.mbari.siam.utils.StopWatch;
import org.mbari.siam.utils.StreamUtils;
import org.mbari.siam.devices.seabird.base.SeabirdPacketParser;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.*;
import org.mbari.siam.distributed.DevicePacketParser;
import org.mbari.siam.devices.seabird.eventDetector.TurbidityEventDetector;

import java.io.IOException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

/** Instrument service for Seabird SBE19plus in profile mode (MP command) */
public class StreamingSBE19 extends StreamingInstrumentService implements Instrument
{
    /** log4j Logger */
    static private Logger _log4j = Logger.getLogger(StreamingSBE19.class);
    
    /** Indicates state of instrument stream */
    boolean _nowStreaming = false;

    /** Copy of _attributes, but with correct type (SBE19Attributes).
     * Gets set to point at _attributes via constructor.
     */
    SBE19Attributes _sbeAttributes = new SBE19Attributes(this);

    protected TurbidityEventDetector _eventDetector;

    /** Scratch buffer for sending commands */
    protected byte[] cmdbuf = new byte[1024];

    protected byte[] _getPrompt = "\r".getBytes();

    SimpleDateFormat _dateFormatter = new SimpleDateFormat("MMddyy");
    SimpleDateFormat _timeFormatter = new SimpleDateFormat("HHmmss");

    protected byte[] _getStatusInfo = "DS\r".getBytes();

    protected byte[] _getCalCmd = "DCAL\r".getBytes();

    protected int _maxStatusBytes = 2000;

    protected int _maxCalBytes = 2000;

    protected static final int MAX_SAMPLE_BYTES = 4096;

    protected DevicePacketParser _devicePacketParser;

    /** Number of fields in a SBE record (temperature, conductivity, pressure,
	volt1, volt2, volt3, volt4) */
    protected final int N_FIELDS = 7;

    /** Widths of temperature, conductivity, pressure, volt1, volt2, volt3, and
	volt4 fields */
    protected final int[] FIELD_WIDTHS = {8, 9, 9, 7, 7, 7, 7};

    /** Number of SBE records read so far. */
    private long _nRecords = 0;

    /**
     * Parser used to convert SensorDataPackets into SiamRecords that can be
     * consumed by the SummaryBlock
     */
    
    public StreamingSBE19() throws RemoteException
    {
	super();
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
        return "\n".getBytes();
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
        return(MAX_SAMPLE_BYTES);
    }


    /** Initialize the Instrument
     *  This means to set all initial settings - This section for one
     * time only instrument settings */
    protected void initializeInstrument() throws InitializeException, Exception
    {
	super.initializeInstrument();

        setSampleTimeout(_sbeAttributes.sampleTimeoutMsec);

        // Turn on DPA power/comms
        managePowerWake();

        boolean gotPrompt = getPrompt(3);
        if (!gotPrompt) {
            _log4j.error("initializeInstrument() - couldn't get prompt");
        }

	sendCommand("STOP\r");
	sendCommandConfirm("MP\r");
	sendCommand("IgnoreSwitch=Y\r");
	sendCommand("AutoRun=N\r");
        setClock();
	sendCommand("Navg=" + (4*(_sbeAttributes.secondsPerSample)) + "\r");
	StopWatch.delay(2000);

	if (_sbeAttributes.detectEvents)
	{
	    try {
		TurbidityEventDetector.Parameters eventParams = 
		    new TurbidityEventDetector.Parameters(_sbeAttributes.staWidthSec,
							  _sbeAttributes.ltaWidthSec,
							  _sbeAttributes.attenuationTriggerRatio,
							  _sbeAttributes.attenuationDetriggerRatio,
							  _sbeAttributes.maxTriggerSec);

		_eventDetector = 
		    new TurbidityEventDetector(this, eventParams,
					       _sbeAttributes.eventDetectorStatusIntervalSec);

		if (_sbeAttributes.useShoreMessaging) {
		    _eventDetector.enableShoreMessaging();
		}
		else {
		    _eventDetector.disableShoreMessaging();
		}

		_eventDetector.initialize(_sbeAttributes.modemHost,
					  _sbeAttributes.secondsPerSample *
					  _sbeAttributes.samplesPerPacket);

		_eventDetector.dispatchMessage("Created TurbidityEventDetector");
	    } catch (Exception e) {
		_log4j.error("Exception creating TurbidityEventDetector: "+e);
		annotate(("ERROR - event detector constructor failed: " + e.toString()).getBytes());
	    }

	    // Ensure that output format shows comma-separated engineering units
	    // (since this service will parse the data)
	    sendCommand("outputformat=3\r");
	    
	    // Ensure that output includes analog sensor voltages
	    sendCommandConfirm("volt0=y\r");
	    sendCommandConfirm("volt1=y\r");
	    sendCommandConfirm("volt2=y\r");
	    sendCommandConfirm("volt3=y\r");

	}
	else {
	    _log4j.info("Event detection disabled by attribute");
	    annotate("Event detection disabled by attribute".getBytes());
	}
    }
    

    /** Try to get instrument prompt */
    protected void getPrompt() throws TimeoutException, IOException,
				      NullPointerException, Exception
    {
        _log4j.debug("getPrompt-");
	_fromDevice.flush();
        _toDevice.write(_getPrompt);
	_toDevice.flush();
        StreamUtils.skipUntil(_fromDevice, getPromptString(),
			      _sbeAttributes.promptTimeoutMsec);

        _log4j.debug("getPrompt done");
    }


    /** Try to get prompt.  Try <i>retries</i> times */
    protected boolean getPrompt(int retries)
    {
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


    /** Method to send a command and wait for the prompt.  May throw exception if
     *  it doesn't find a prompt.
     */
    public void sendCommand(String cmd, long timeout)
	throws TimeoutException, NullPointerException, IOException, Exception 
    {
        _log4j.debug("Sending cmd: " + cmd);
        _toDevice.write(cmd.getBytes());
	_toDevice.flush();
	// Look for response
	StreamUtils.skipUntil(_fromDevice, getPromptString(), timeout);
    }


    /** Method to send a command and wait for the prompt.  Default timeout of 5 seconds.
     *  May throw exception if it doesn't find a prompt.
     */
    public void sendCommand(String cmd)
	throws TimeoutException, NullPointerException, IOException, Exception 
    {
	sendCommand(cmd, 5000);
    }

    /** Method to send a command for which the SBE-19 requires confirmation.  Unfortunately, there appears to
     * be two different modes of confirmation, depending on the firmware in the SBE-19.  You either need to
     * send the command a second time, or you need to confirm with a 'Y'.  This method tries to parse the
     * prompt and do the right thing.
     */
    public void sendCommandConfirm(String cmd)
	throws TimeoutException, NullPointerException, IOException, Exception 
    {
        _log4j.debug("Sending cmd: " + cmd);
        _toDevice.write(cmd.getBytes());
	_toDevice.flush();
	try {
	    Thread.sleep(1000);
	} catch (Exception e) {
	}

	int nbytes = _fromDevice.available();
	if (nbytes > cmdbuf.length)
	    nbytes = cmdbuf.length;
	_fromDevice.read(cmdbuf, 0, nbytes);
	String response = new String(cmdbuf, 0, nbytes);
	_fromDevice.flush();
	_log4j.debug("Response: " + response);

	if (response.indexOf("Y/N") >= 0)
	{
	    _log4j.debug("Writing \"Y\"");
	    _toDevice.write("Y\r".getBytes());
	    _toDevice.flush();
	    StreamUtils.skipUntil(_fromDevice, getPromptString(), 5000);
	}
	else if (response.indexOf("repeat") >= 0)
	{
	    _log4j.debug("Repeating: " + cmd);
	    _toDevice.write(cmd.getBytes());
	    _toDevice.flush();
	    StreamUtils.skipUntil(_fromDevice, getPromptString(), 5000);
	}
	else
	{
	    _log4j.warn("Did not receive expected confirmation prompt from command: " + cmd);
	    getPrompt(3);
	}
	
    }


    /** Return parameters to use on serial port. */
    public SerialPortParameters getSerialPortParameters()
    throws UnsupportedCommOperationException {
        
        return new SerialPortParameters(_sbeAttributes.baud, SerialPort.DATABITS_8,
					SerialPort.PARITY_NONE, SerialPort.STOPBITS_1);
    }
    
    
    /** Stop streaming */
    protected void stopStreaming() throws Exception
    {
	getPrompt(3);

	for (int i=0; i<3; i++)	{
	    try {
		sendCommand("STOP\r", 3000);
		_nowStreaming = false;
		return;
	    } catch (Exception e) {
		_log4j.debug("Exception in stopStreaming: " + e);
	    }
	}
    }
    
    /** Return true if instrument is streaming. */
    protected boolean isStreaming()
    {
        return _nowStreaming;
    }
    
    /** Put instrument into streaming mode */
    protected void startStreaming() throws Exception
    {
	if (!_nowStreaming)
	{
	    getPrompt(3);
	    sendCommand("STARTNOW\r");
	    _nowStreaming = true;
	}
    }
    
    
    /**
     * Set Seabird's clock.
     * Note: may stop, but does not resume logging or quit session
     */
    public void setClock()
    {
	long currentTime=System.currentTimeMillis();

        Date date = new Date(currentTime);

        try {
            sendCommand("MMDDYY=" + _dateFormatter.format(date) + "\r");
            sendCommand("HHMMSS=" + _timeFormatter.format(date) + "\r");
        }
        catch (Exception e) {
            _log4j.error("setClock() failed", e);
        }
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
    protected byte[] getInstrumentStateMetadata()
    {
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
				  _sbeAttributes.promptTimeoutMsec);

            byte[] statusBuf = new byte[_maxStatusBytes];

            int statusBytes =
		StreamUtils.readUntil(_fromDevice, statusBuf,
				      getPromptString(), 15000);


            // Get cal info...
            _log4j.debug("Requesting Cal Info...");
            _toDevice.write(_getCalCmd);
            _toDevice.flush();

            // Skip command echo...
            StreamUtils
		.skipUntil(_fromDevice, _getCalCmd,
			   _sbeAttributes.promptTimeoutMsec);

            byte[] calBuf = new byte[_maxCalBytes];
            int calBytes =
		StreamUtils.readUntil(_fromDevice, calBuf,
				      getPromptString(), 15000);

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

    public DevicePacketParser getDevicePacketParser() 
	throws NotSupportedException {
        // It may not always be used so we lazy load it.
        if (_devicePacketParser == null) {
	    _devicePacketParser = new SBE19DevicePacketParser();
        }
        return _devicePacketParser;
    }
    
    
    /** Self-test routine; This does nothing in the Aquadopp driver */
    public int test() {
        return Device.OK;
    }
    

    /** Internal routine to determine whether an individual data point is valid.
     * Note this differs from validateSample(), in that it validates a single
     * data point within a sample, and not the entire sample.
     */
    protected boolean validateRecord(byte[] dataPoint, int nBytes)
    {
	// Note that implementing this by counting commas saves us from creating 
	// a new String and new StringTokenizer for every data point in the 
	// sample.
	int nFields = 0;
	int fieldWidth = 0;
	for (int i = 0; i < nBytes; i++) {

	    if (dataPoint[i] == ',') {

		if (fieldWidth != FIELD_WIDTHS[nFields]) {
		    return false;
		}

		nFields++;
		// Reset field width counter
		fieldWidth = 0;
	    }
	    else if ((dataPoint[i] == '\r') || (dataPoint[i] == '\n')) {

		if (fieldWidth != FIELD_WIDTHS[nFields]) {
		    return false;
		}

		nFields++;
		break;
	    }
	    else {
		fieldWidth++;
	    }
	}

	return(nFields == N_FIELDS);
    }


    /** Read an instrument sample */
    protected int readSample(byte[] sample) throws TimeoutException, IOException, Exception
    {
        _log4j.debug("readSample()");

	int totBytes = 0;
	long sleepTime = (1000L * _sbeAttributes.secondsPerSample) - 500L;

	for (int i = 0; i < _sbeAttributes.samplesPerPacket; )
	{
	    // If no data available yet, delay slightly less than one sample time.
	    // This is to offload the CPU. Otherwise we spend all our time
	    // in readUntil() chewing up the CPU
	    if (_fromDevice.available() <= 0) {
		try {
		    Thread.sleep(sleepTime);
		} catch (Exception e) {
		}
	    }
	    int nbytes = StreamUtils.readUntil(_fromDevice, cmdbuf,
					       getSampleTerminator(), getSampleTimeout());

	    if (validateRecord(cmdbuf, nbytes)) {
		System.arraycopy(cmdbuf, 0, sample, totBytes, nbytes);
		totBytes += nbytes;
		sample[totBytes++] = '\n';
		i++;
	    }
	    else {
		annotate(("readSample() got incomplete data point: " + 
			  new String(cmdbuf, 0, nbytes)).getBytes());
	    }
	}

	try {
	    Thread.sleep(100);
	} catch (Exception e) {
	}
 
	while(_fromDevice.available() > 0)
	    sample[totBytes++] = (byte)_fromDevice.read();

	return(totBytes);
    }
    
    
    protected float getTransmisChannel(String sample)
	throws NumberFormatException
    {
	    StringTokenizer tokenizer =  new StringTokenizer(sample, ", ");
	    String token = null;
				
	    for (int nToken = 0; tokenizer.hasMoreTokens(); nToken++)
	    {
		try {
		    token = tokenizer.nextToken();

		    if (nToken == _sbeAttributes.transmisChannel+3)
			return(Float.parseFloat(token));
		}
		catch (NumberFormatException e) {
		    String errMsg = "Invalid voltage: " + token + 
			" : token #" + nToken + " of record: " + sample;

		    _log4j.error(errMsg);

		    throw new NumberFormatException(errMsg);
		}
	    }
	    return((float)0.0);
    }


    /** Process latest acquired sample. Note that sample includes multiple SBE 
     records (specified by samplesPerPacket attribute) */
    protected SensorDataPacket processSample(byte[] sample, int nBytes) 
	throws Exception
    {

	SensorDataPacket packet = super.processSample(sample, nBytes);

	if (_sbeAttributes.detectEvents == false) {
	    return packet;
	}

	if (!_eventDetector.enabled())
	{
	    _log4j.warn("event detection is disabled");
	    return(packet);
	}

	// GEt transmissometer value from FIRST RECORD ONLY, pass to event detector
	// (ignore later samples in the packet)
	_eventDetector.processSample(getTransmisChannel(new String(packet.dataBuffer())),
				     packet.systemTime(),
				     _sbeAttributes.transmittSlope,
				     _sbeAttributes.transmittIntcpt,
				     _sbeAttributes.secondsPerSample *
				     _sbeAttributes.samplesPerPacket);


	return packet;
    }


    /** Make sure to terminate measurement mode - it is "bad" to disconnect
     * power while in measurement mode. */
    protected synchronized String shutdownInstrument() throws Exception
    {
	stopStreaming();
	Thread.sleep(2000);
        return("SBE-19+ shut down");
    }
    

    /** Service attributes. */
    public class SBE19Attributes extends StreamingInstrumentService.Attributes {
        
        SBE19Attributes(StreamingInstrumentService service)
	{
            super(service);
            
            /*
             * The names of the variables that will be summaryized. These should match
             * the names that are put out by the {@link SBE19DevicePacketParser} if you want
             * any actual summaries to occur.
             */
            summaryVars = new String[]{SBE19DevicePacketParser.TEMPERATURE, 
				       SBE19DevicePacketParser.CONDUCTIVITY, 
				       SBE19DevicePacketParser.PRESSURE, 
				       SBE19DevicePacketParser.VOLTAGE0, 
				       SBE19DevicePacketParser.VOLTAGE2};
        }
        
        /** Instrument baud rate */
        int baud = 9600;
        
        /** In profiling mode, instrument takes raw samples at 4 Hz. This parameter
	 * tells it how often we want a sample, and thus indirectly sets NAVG in the CTD.
	 * For example, for the default secondsPerSample=2, it will set NAVG to 8.
	 */
        int secondsPerSample = 2;

	/** Seabird samples per SensorDataPacket */
	int samplesPerPacket = 1;

        /** Summary interval, in seconds */
        int summaryIntervalSec = 10;

	/** Sample timeout (millisec) */
	public int sampleTimeoutMsec = 5000;

	/** Get-prompt timeout (millisec) */
	public int promptTimeoutMsec = 8000;

	/** Enable/disable event detection */
	public boolean detectEvents = true;

	/** Enable/disable use of shore messaging service */
	public boolean useShoreMessaging = false;

	/** Transmissometer channel, ZERO-based */
	public int transmisChannel = 0;

	/** Linear calibration %transmittance slope */
	public float transmittSlope = 20.f;

	/** Linear calibration %transmittance intercept */
	public float transmittIntcpt = 0.f;

	/** Width (in seconds) of short-term average (STA) */
	public int staWidthSec = 21600;

	/** Width (in seconds) of long-term average (LTA) */
	public int ltaWidthSec = 144000;

	/** Attenuation STA/LTA event trigger ratio */
	public float attenuationTriggerRatio = 1.5f;

	/** Attenuation STA/LTA event detrigger ratio */
	public float attenuationDetriggerRatio = 1.05f;

	/** Maximum consecutive seconds to acquire in triggered state
	 before resetting event detector */
	public int maxTriggerSec = 1800000;

	/** Shore messaging service host name */
	public String modemHost = "surface";

	/** Periodically log status of event detector */
	public int eventDetectorStatusIntervalSec = 3600;

        public void checkValues() throws InvalidPropertyException
	{
	    if ((secondsPerSample < 1) || (secondsPerSample > (32767/4)))
		throw new InvalidPropertyException("Invalid secondsPerSample");
	    
	    if (secondsPerSample > 3) {
		sampleTimeoutMsec = 1000 * (secondsPerSample + 2);
		try {
		    setSampleTimeout(sampleTimeoutMsec);
		} catch (Exception e ) {
		}
	    }
	}
    }
    
    
    public class SBE19DevicePacketParser extends org.mbari.siam.distributed.DevicePacketParser
    {
	public static final int MAX_AUX_CHAN = 8;
        public static final String TEMPERATURE = "temperature";
        public static final String CONDUCTIVITY = "conductivity";
        public static final String PRESSURE = "Pressure";
        public static final String VOLTAGE0 = "Voltage-0";
        public static final String VOLTAGE1 = "Voltage-1";
        public static final String VOLTAGE2 = "Voltage-2";
        public static final String VOLTAGE3 = "Voltage-3";

        protected void parseFields(DevicePacket packet) throws NotSupportedException, Exception {
            if (packet instanceof SensorDataPacket)
	    {
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

} /* class StreamingSBE19 */
