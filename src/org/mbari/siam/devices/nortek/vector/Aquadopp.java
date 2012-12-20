package org.mbari.siam.devices.nortek.vector;

/** NOTE: This version of Aquadopp is extended to Vector. 
If Vector is extended from org.mbari.siam.devices.nortek.Aquadopp (as of 
April 2 2010), the resulting service is unable to acquire samples - 
acquire() times out. We do not yet understand why, hence we instead extend
this older version of Aquadopp to Vector, which results in a working service.
Eventually this Aquadopp should be eliminated when we fix the problem.
T.O'R. - 4/2/2010

/**
 * @Title Nortek High Resolution (2MHz) Open Water (6000m) Current Meter (3D) Instrument Driver
 * @author Karen A. Salamy
 * @version 1.0
 * @date 11/01/2005
 *
 * Copyright MBARI 2005
 */


/**
 * RS-232C Port Protocol.  The following RS-232 settings should be used:
 * 19200 Baud (optimal)
 * 8 Data bits
 * 1 Stop bit
 * None - No Parity
 * None - Flow Control
 *
 * Supply Voltage for RS-232 is +9 to +16V VDC!
 */

/**
 * ASCII FORMAT OUTPUT (HOW TO INTERPRET)
 *
 * The data format is largely the same as the result of ASCII conversion of binary data
 * (using the Aquadopp software). The differences are in the format of the error and
 * status codes, and the use of only a single space between data values. Here is an
 * example with two lines of data:
 *
 * 03 15 2003 16 30 00 0 48 -0.008 0.142 0.017 91.0 92.0 86.0 11.5 1464.8 254.5 -3.8 0.0 218.486 5.26
 * 03 15 2003 16 45 00 0 48 -0.009 0.128 0.007 91.0 90.0 85.0 11.5 1464.8 258.6 -3.8 0.0 218.396 5.25
 *
 * Aquadopp Current Meter ASCII Data Format
 * Position 	Value 			Units
 * 1-6 		Date & Time
 * 7-8 		Error/status code
 * 9-11 	Velocity 		m/s
 * 12-14 	Amplitude 		counts
 * 15 		Battery voltage 	VDC
 * 16 		Sound speed 		m/s
 * 17 		Heading 		degrees
 * 18-19 	Pitch and roll 		degrees
 * 20 		Pressure 		m
 * 21 		Temperature 		deg C
 */


import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import net.java.jddac.common.meas.Measurement;
import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.core.SerialInstrumentPort;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.core.StreamingInstrumentService;
import org.mbari.siam.distributed.jddac.SiamRecord;
import org.mbari.siam.utils.StreamUtils;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.*;
import org.mbari.siam.distributed.DevicePacketParser;
import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.Enumeration;
import net.java.jddac.common.meas.collection.Record;

/** Instrument service for Aquadopp current profiler. */
public class Aquadopp extends StreamingInstrumentService
        implements AquadoppIF, Safeable {
    
    /** log4j Logger */
    static private Logger _log4j = Logger.getLogger(Aquadopp.class);
    
    public final static String ASCII_CONFIG_TAG = "configSummary";
    public final static String BINARY_CONFIG_TAG = "binaryConfiguration";
    public final static int INSTRUMENT_CONFIGURATION = 0x0;
    public final static int HARDWARE_CONFIGURATION = 0x05;
    public final static int PROFILER_VELOCITY_DATA = 0x21;
    public final static int HR_PROFILER_DATA = 0x2a;
    public final static int SYNC_BYTE = 0xa5;
    
    /** Indicates state of instrument stream */
    boolean _nowStreaming = false;
    
    byte[] _byteBuffer = new byte[1024];
    
    /** Default Aquadopp serial data bits */
    static final int _DATA_BITS = SerialPort.DATABITS_8;
    
    /** Default Aquadopp serial parity bits */
    static final int _PARITY = SerialPort.PARITY_NONE;
    
    /** Default Aquadopp serial stop bits */
    static final int _STOP_BITS = SerialPort.STOPBITS_1;
    
    /** Default Aquadopp current limit for the optode (milliamps) */
    static final int _INSTRUMENT_CURRENT_LIMIT = 1000;
    
    /** Default Aquadopp start delay (ms) */
    static final int _INSTRUMENT_START_DELAY = 0;
    
    /** Aquadopp DataStructure */
    DataStructure _data = null;
    
    /** Aquadopp instrument configuration */
    InstrumentConfiguration _instrumentConfig = null;
    
    HardwareConfiguration _hardwareConfig = null;
    
    /** Aquadopp profiler velocity data */
    //    ProfilerVelocityData _profilerVelocity = null;
    
    /** Aquadopp profiler velocity data */
    HRProfilerData _hrProfilerData = null;
    
    /** Instrument acknowledges successful command with ack ack */
    static final byte[] ACK_ACK = {0x06, 0x06};
    
    /** Instrument acknowledges failed command with nak nak */
    static final byte[] NAK_NAK = {0x15, 0x15};
    
    /** COMMANDS */
    
    /** Get instrument mode. */
    static final byte[] GET_MODE = "II".getBytes();
    
    /** MC returns instrument to command Mode (takes commands) */
    static final byte[] SET_CMD_MODE = "MC".getBytes();
    
    /** MA sends a single ASCII measurement and goes to sleep
     * (assuming the instrument is not set for continuous output). */
    static final byte[] MEASURE_ASCII = "MA".getBytes();
    
    /** AD is the BINARY equivalent of MA, which causes a single
     * binary measurement to be sent. */
    static final byte[] ACQUIRE_DATA = "AD".getBytes();
    
    /** SD starts measurement with recorder, but with no output on
     * serial port. SAFEABLE MODE. */
    static final byte[] GO_SAFE = "SD".getBytes();
    
    /** ST starts streaming command to instrument's serial port only. */
    static final byte[] STREAM = "ST".getBytes();
    
    /** SR starts streaming command to instrument's log as well as
     * serial port. */
    static final byte[] STREAM_TO_LOG = "SR".getBytes();
    
    /** Get All command.  MetaData command.  Lists current settings.*/
    static final byte[] GET_ALL = "GA".getBytes();
    
    /** Get file allocation table command. */
    static final byte[] GET_FAT = "RF".getBytes();
    
    /** Power Down command. Puts instrument into sleep mode. */
    static final byte[] POWER_DOWN = "PD".getBytes();
    
    /** Get Identification String (returns ASCII format of
     * Instrument Serial Number (AQD-XXXX).*/
    static final byte[] IDENTIFY = "ID".getBytes();
    
    /** Erase recorder */
    static final byte[] ERASE_RECORDER =
    {'F', 'O', (byte )0x12, (byte )0xd4,  (byte )0x1e, (byte )0xef};
    
    /** Set instrument clock */
    static final byte[] SET_CLOCK = "SC".getBytes();
    
    /** Max constants - Just punched values in to try to use sendCommand(). */
    static final int MAX_RESPONSE_BYTES = 10000;  // Keep it large for now!!
    
    static final int MAX_SAMPLE_TRIES = 3;
    
    static final int MAX_COMMAND_TRIES = 5;
    
    /** Instrument modes */
    final static short FIRMWARE_UPGRADE_MODE = 0x0;
    final static short MEASUREMENT_MODE = 0x01;
    final static short COMMAND_MODE = 0x02;
    final static short DATA_RETRIEVAL_MODE = 0x04;
    final static short CONFIRMATION_MODE = 0x05;
    
    
    /** Timeouts: This needs to be longer than the longest likely
     * intercharacter delay. (Milliseconds) */
    static final int RESPONSE_TIME = 10000; //Keep it large.  Aquadopp averages over 5 sec. before responding.
    
    static final int SAMPLE_RESPONSE_TIME = 6000; // Just over the averaging time of 5 sec.
    
    
    /** Response */
    //static final String RESPONSE_PROMPT = "\006\006"; // Instrument "Ack, Ack" Acknowledgement of command.
    
    
    /** Others Static items */
    static final String RESPONSE_EOL = "\r\n"; //Response after data output to screen.
    
    static final String DATA_SYNC = "";
    
    /**
     * Parser used to convert SensorDataPackets into SiamRecords that can be
     * consumed by the SummaryBlock
     */
    private DevicePacketParser devicePacketParser;
    
    /** Configurable Aquadopp attributes */
    Attributes _attributes = new Attributes(this);
    
    public Aquadopp() throws RemoteException {
        
        // Aquadopp DataStructure
        _data = new DataStructure();
        
        // Aquadopp instrument configuration
        _instrumentConfig = new InstrumentConfiguration();
        
        _hardwareConfig = new HardwareConfiguration();
        
        // Aquadopp profiler velocity data
        _hrProfilerData = new HRProfilerData();
        
    }
    
    /** Sets Aquadopp power policy */
    protected PowerPolicy initInstrumentPowerPolicy() {
        return PowerPolicy.WHEN_SAMPLING;
    }
    
    /** Sets Aquadopp communications power policy */
    protected PowerPolicy initCommunicationPowerPolicy() {
        return PowerPolicy.WHEN_SAMPLING;
    }
    
    /** Set Aquadopp startup delay time. Set to 0 above. */
    protected int initInstrumentStartDelay() {
        //Aquadopp software: RS-232C input buffer is checked for
        // 100 ms after each sample.
        return _INSTRUMENT_START_DELAY;
    }
    
    /** Sets Aquadopp current limit. Set to 1000 above. */
    protected int initCurrentLimit() {
        return _INSTRUMENT_CURRENT_LIMIT;
    }
    
    
    /** Sets the Aquadopp sample terminator. The string is terminated by
     * CR (\r) and LF (\n).*/
    protected byte[] initSampleTerminator() {
        return "\r\n".getBytes();
    }
    
    /** Sets the Aquadopp command prompt. There is no command prompt. */
    protected byte[] initPromptString() {
        return "".getBytes();
    }
    
    /** Sets the Aquadopp maximum number of bytes in an instrument
     * data sample */
    protected int initMaxSampleBytes() {
        // ~499 bytes maximum per sample - in full RS-232C comprehensive output
        return 1024;  // !May need to be decreased to 512!
    }
    
    
    /** Initialize the Instrument
     *  This means to set all initial settings - This section for one
     * time only instrument settings */
    protected void initializeInstrument()
    throws InitializeException, Exception {
        
        if (_attributes.logEnabled && _attributes.burstMode) {
            throw new InitializeException("burst mode not compatible with internal logging");
        }
        
        try {
            // Get instrument mode
            getMode();
        } catch (Exception e) {
            _log4j.error("initializeInstrument() - trying to get mode: ", e);
        }
        
        // Stop autonomous logging by instrument (If was in safeMode).
        _log4j.info("initializeInstrument() - Getting Aquadopp's attention (Stops any previous logging).");
        getInstrumentAttention();
        
        _log4j.info("initializeInstrument() - Setting Aquadopp Measurement Interval.");
	try {
	    setMeasurementInterval(_attributes.measurementIntervalSec);
        }
	catch (Exception e) {
	    _log4j.error("Failed to set measurment interval");
	    annotate("initializeInstrument(): Failed to set measurement interval".getBytes());
	    throw new InitializeException("'MI=" + _attributes.measurementIntervalSec + 
					  "' failed; must be >= avgInterval");
	}

        try {
            setSampleTimeout((_attributes.measurementIntervalSec * 1000) + 3000);
        } catch (RangeException e) {
            _log4j.error("Failed call to setSampleTimeout", e);
        }
        
        // Set instrument clock
        setClock();
        
        _log4j.debug("Done with initializeInstrument().");
    }
    
    
    /** Get instrument mode */
    short getMode() throws Exception {
        _fromDevice.flush();
        
        _toDevice.write(GET_MODE);
        _toDevice.flush();
        int nBytes = StreamUtils.readUntil(_fromDevice, _byteBuffer,
                ACK_ACK, 5000);
        
        short mode = DataStructure.getShort(_byteBuffer[0], _byteBuffer[1]);
        _log4j.debug("initializeInstrument() - mode=" + mode);
        return mode;
    }
    
    
    /** Return parameters to use on serial port. */
    public SerialPortParameters getSerialPortParameters()
    throws UnsupportedCommOperationException {
        
        return new SerialPortParameters(_attributes.baud, _DATA_BITS, _PARITY,
                _STOP_BITS);
    }
    
    
    /** Stop streaming */
    protected void stopStreaming() throws Exception {
        getInstrumentAttention();
        _nowStreaming = false;
    }
    
    /** Return true if instrument is streaming. */
    protected boolean isStreaming() {
        return _nowStreaming;
    }
    
    /** Put instrument into streaming mode */
    protected void startStreaming() throws Exception {
        // Clear first sample. Place in method prepareToSample().
        _fromDevice.flush();
        
        // If not yet sampling, put her in streaming mode...
        // Get intrument's attention, and start sampling
        getMode();
        
        getInstrumentAttention();
        if (_attributes.logEnabled) {
            // Stream and log to internal instrument storage
            _log4j.debug("startStreaming() - start streaming with " + new String(STREAM_TO_LOG));
            _toDevice.write(STREAM_TO_LOG);
        } else {
            // Stream without on-instrument logging
            _log4j.debug("startStreaming() - start streaming with " + new String(STREAM));
            
            _toDevice.write(STREAM);
        }
        _toDevice.flush();
        
        // NOTE: verify ACK ACK from instrument (else there's a problem)
        try {
            StreamUtils.skipUntil(_fromDevice, ACK_ACK, 5000);
        } catch (Exception e) {
            String errMsg = "Instrument didn't acknowledge " +
                    new String(STREAM_TO_LOG) + " command";
            
            throw new Exception(errMsg);
        }
        _nowStreaming = true;
    }
    
    
    
    
    
    /** Get instrument's attention, ready to accept a command. Throw
     * IOException if unsuccessful. */
    void getInstrumentAttention() throws Exception {
        
        _fromDevice.flush();
        sendSoftBreak();
        _nowStreaming = false;
        
        // If instrument asks for confirmation, issue "MC"
        try {
            StreamUtils.skipUntil(_fromDevice,
                    "Confirm:".getBytes(), 5000);
            
            getMode();
            
            _toDevice.write(SET_CMD_MODE);
            _toDevice.flush();
        } catch (Exception e) {
            // Note that instrument may not be asking for confirmation -
            // no problem though.
            _log4j.debug("Note: getInstrumentAttention(): " + e.getMessage() +
                    " (while looking for \"Confirm\")");
            _toDevice.write(SET_CMD_MODE);
            _toDevice.flush();
            
        }
        
        Thread.sleep(1000);
        _fromDevice.flush();
    }
    
    
    /** Erase the recorder */
    public synchronized void eraseRecorder() throws Exception {
        
        // Get instrument's attention
        getInstrumentAttention();
        
        /** Erase recorder; must also pass parameter 0xd412 0xef1e */
        _toDevice.write(ERASE_RECORDER);
        _toDevice.flush();
    }
    
    
    /** Get Instrument State Metadata. TRUE STATE OF INSTRUMENT. */
    protected byte[] getInstrumentStateMetadata()
    throws Exception {
        _log4j.info("Retrieving Aquadopp state Metadata...");
        // ~789 - 855 bytes for MetaData call.
        byte[] bytes= new byte[2048];
        int nTotalBytes = 0;
        int nBytes = 0;
        
        getInstrumentAttention();
        _fromDevice.flush();
        
        // Get instrument serial number
        _toDevice.write(IDENTIFY);
        _toDevice.flush();
        nBytes = StreamUtils.readUntil(_fromDevice, _byteBuffer,
                ACK_ACK, 5000);
        
        _log4j.debug("getInstrumentStateMetadata() - serial #: " +
                new String(_byteBuffer, 0, nBytes));
        
        System.arraycopy(_byteBuffer, 0, bytes, nTotalBytes, nBytes);
        nTotalBytes += nBytes;
        
        // Outputs MetaData (in binary format)
        _toDevice.write(GET_ALL);
        _toDevice.flush();
        
        String hardwareConfig = "";
        String instrumentConfig = "";
        String fileAllocTable = "";
        
        // Expect three data structures, then ACK ACK
        for (int i = 0; i < 3; i++) {
            if ((nBytes =
                    DataStructure.read(_fromDevice,
                    _byteBuffer,
                    10000)) <= 0) {
                
                throw new Exception("Got " + nBytes + "from read()\n");
            } else {
                // Successfully read a data structure; copy to return
                // array at appropriate index
                _log4j.debug("read() returned " + nBytes + " bytes");
                System.arraycopy(_byteBuffer, 0,
                        bytes, nTotalBytes, nBytes);
                
                // Increment total bytes read so far
                nTotalBytes += nBytes;
                
                _log4j.debug("struct ID: " +
                        DataStructure.id(_byteBuffer));
                
                if (DataStructure.id(_byteBuffer) ==
                        INSTRUMENT_CONFIGURATION) {
                    
                    _instrumentConfig.setBytes(_byteBuffer);
                    
                    _attributes.summaryIntervalSec =
                            _instrumentConfig.measurementInterval();
                    
                    _attributes.avgIntervalSec =
                            _instrumentConfig.avgInterval();
                    
                    _attributes.nCells = _instrumentConfig.nCells();
                    _attributes.nBeams = _instrumentConfig.nBeams();
                    
                    instrumentConfig = _instrumentConfig.toString();
                    
                    _log4j.debug("instrumentConfig: " + instrumentConfig);
                    
                    _hrProfilerData.setParameters(_attributes.nCells,
                            _attributes.nBeams);
                } else if (DataStructure.id(_byteBuffer) ==
                        HARDWARE_CONFIGURATION) {
                    _hardwareConfig.setBytes(_byteBuffer);
                    hardwareConfig = _hardwareConfig.toString();
                    _log4j.debug("hardwareConfig: " + hardwareConfig);
                }
            }
        }
        // Clear out any remainder from reading data structures...
        _fromDevice.flush();
        
        // Read file allocation table
        _toDevice.write(GET_FAT);
        _toDevice.flush();
        
        nBytes = StreamUtils.readUntil(_fromDevice, _byteBuffer,
                ACK_ACK, 5000);
        
    /*
     * Generate a binary representation to store in the metadata packet
     */
        byte[] binaryConfig = new byte[_instrumentConfig._dataBytes.length +
                _hardwareConfig._dataBytes.length + _byteBuffer.length];
        System.arraycopy(_instrumentConfig._dataBytes, 0, binaryConfig, 0 , _instrumentConfig._dataBytes.length);
        System.arraycopy(_hardwareConfig._dataBytes, 0, binaryConfig, _instrumentConfig._dataBytes.length, _hardwareConfig._dataBytes.length);
        System.arraycopy(_byteBuffer, 0, binaryConfig, _instrumentConfig._dataBytes.length + _hardwareConfig._dataBytes.length, _byteBuffer.length);
        
        
        FileAllocationTable fat =
                new FileAllocationTable(_byteBuffer,
                _hardwareConfig.recorderSize());
        
        
        fileAllocTable = fat.toString();
        
        _log4j.debug("FAT:\n" + fileAllocTable);
        _log4j.debug("Instrument has " + fat.nFiles() + " files");
        annotate(("Recorder contains " + fat.nFiles() +
                " files; MAX IS 32").getBytes());
        
        System.arraycopy(_byteBuffer, 0, bytes, nTotalBytes, nBytes);
        nTotalBytes += nBytes;
        
        byte[] asciiSummary =
                ("\n<" + ASCII_CONFIG_TAG + ">" +
                "instrumentConfig: " + instrumentConfig + "\n" +
                "hardwareConfig: " + hardwareConfig + "\n" +
                "FAT:\n" + fileAllocTable + "\n" +
                "</" + ASCII_CONFIG_TAG + ">").getBytes();
        
        // Allocate an array of exactly the right size
        byte returnBytes[] = new byte[nTotalBytes+asciiSummary.length];
        System.arraycopy(bytes, 0, returnBytes, 0, nTotalBytes);
        // Append ascii summary
        System.arraycopy(asciiSummary, 0, returnBytes, nTotalBytes,
                asciiSummary.length);
        
        return returnBytes;
        //return binaryConfig;
    }
    
    
    /** Set instrument clock to current time */
    public void setClock() {
        
        // Get current time in "BCD" format
        SimpleDateFormat bcdFormatter =
                new SimpleDateFormat("mm ss dd HH yy MM");
        
        String dateString = bcdFormatter.format(new Date());
        
        _log4j.debug("setClock() - current time: " + dateString);
        
        byte[] clockData = new byte[6];
        
        StringTokenizer tokenizer = new StringTokenizer(dateString);
        for (int nToken = 0; nToken < 6; nToken++) {
            if (!tokenizer.hasMoreTokens()) {
                _log4j.error("Ran out of tokens: " + dateString);
                return;
            }
            String token = tokenizer.nextToken();
            try {
                clockData[nToken] = Byte.parseByte(token, 16);
            } catch (NumberFormatException e) {
                _log4j.error("setClock() - bad number: " + token);
                return;
            }
        }
        
        // Send 'set clock' command to instrument
        try {
            _toDevice.write(SET_CLOCK);
            _toDevice.write(clockData);
            _toDevice.flush();
            StreamUtils.skipUntil(_fromDevice, ACK_ACK, 5000);
        } catch (Exception e) {
            _log4j.error("setClock() - Caught error while setting instrument clock", e);
        }
    }
    
    
    /**
     * setClock() not implemented. Samples are locally timestamped.
     *
     * @param t
     */
    
    /** Set the Aquadopp's clock.
     * This does nothing in the Aquadopp driver */
    public void setClock(long t) throws NotSupportedException {
        throw new NotSupportedException("not supported by Aquadopp");
    }
    
    public DevicePacketParser getDevicePacketParser() throws NotSupportedException {
        // It may not always be used so we lazy load it.
        if (devicePacketParser == null) {
            if (_log4j.isDebugEnabled()) {
                _log4j.debug("Creating a DevicePacketParser");
            }
            devicePacketParser = 
		new org.mbari.siam.devices.nortek.vector.DevicePacketParser(_instrumentConfig);
        }
        return devicePacketParser;
    }
    

    public PacketParser getParser() throws NotSupportedException {
	return new Parser(getDevicePacketParser());
    }
    
    /** Self-test routine; This does nothing in the Aquadopp driver */
    public int test() {
        return Device.OK;
    }
    
    
    /** Enter mode for resource-restricted environement. */
    public synchronized void enterSafeMode() throws Exception {
        
        _log4j.info("enterSafeMode() - Setting Aquadopp to SAFE mode.");
        
        _log4j.info("enterSafeMode() - Getting Aquadopp's attention.");
        getInstrumentAttention();
        
        _log4j.info("enterSafeMode() - Changing Aquadopp Measurement interval for Safe mode (300s).");
	try {
	    setMeasurementInterval(_attributes.safeMeasurementIntervalSec);
        }
	catch (Exception e) {
	    _log4j.error("Failed to set measurement interval");
	    annotate("enterSafeMode(): Failed to set measurement interval".getBytes());
	}

        _log4j.info("enterSafeMode() - Instructing Aquadopp to start autonomous sampling NOW.");
        _toDevice.write(GO_SAFE);
        _toDevice.flush();
        // NOTE: verify ACK ACK from instrument (else there's a problem)
        try {
            StreamUtils.skipUntil(_fromDevice, ACK_ACK, 5000);
        } catch (Exception e){
            _log4j.error("enterSafeMode() - Failed to place Aquadopp into SAFE Mode.", e);
        }
        
    } //end of method
    
    
    
    
    /** PRIVATE METHODS * */
    
    /** Send Nortek "soft break". */
    private void sendSoftBreak() throws IOException {
        _toDevice.write("@@@@@@".getBytes());
        _toDevice.flush();
	try {
	    Thread.sleep(100);
	}
	catch (Exception e) {
	}

        _toDevice.write("K1W%!Q".getBytes());
        _toDevice.flush();
        _nowStreaming = false;
    }
    
    /** Send "hard break" (500 msec). */
    private void sendHardBreak() throws IOException {
        if (_instrumentPort instanceof SerialInstrumentPort) {
            _log4j.debug("Sending break.....");
            SerialInstrumentPort sip =
                    (SerialInstrumentPort) _instrumentPort;
            
            // Hold line high for 500 msec
            sip.sendBreak(500*250);
            _nowStreaming = false;
        }
    }
    
    
    /** Return specifier for default sampling schedule. Subclasses MUST
     * provide the default sample schedule. */
    protected ScheduleSpecifier createDefaultSampleSchedule()
    throws ScheduleParseException {
        
        // Sample every 30 seconds by default
        return new ScheduleSpecifier(30000);
    }
    
    
    /** Service attributes. */
    public class Attributes extends StreamingInstrumentService.Attributes {
        
        Attributes(StreamingInstrumentService service) {
            super(service);
            
            /*
             * The names of the variables that will be summaryized. These should match
             * the names that are put out by the {@link DevicePacketParser} if you want
             * any actual summaries to occur.
             */
            summaryVars =
                    new String[]{"amplitude-0", "velocity-0", "velocity-1",
                    "velocity-2"};
        }
        
        /** Instrument baud rate */
        int baud = 19200;
        
        /** Instrument's averaging interval, in seconds */
        int avgIntervalSec = 0;
        
        /** Number of cells */
        int nCells = 0;
        
        /** Number of beams */
        int nBeams = 0;
        
        /** Summary interval, in seconds */
        int summaryIntervalSec = 10;
        
        /** Nominal sample measurement interval, in seconds */
        short measurementIntervalSec = 1;
        
        /** SAFE sample measurement interval, in seconds */
        short safeMeasurementIntervalSec = 300;
        
        /** Internal instrument log enabled */
        boolean logEnabled = true;
        
        /**
         * Return true if specified attribute is "configurable", i.e. can be
         * initialized at startup by a property.
         *
         * @param attributeName
         * @return true if attribute may be initialized by a property, false otherwize
         */
        protected boolean isConfigurable(String attributeName) {
            if (attributeName.equals("avgIntervalSec")) {
                return false;
            } else if (attributeName.equals("nCells")) {
                return false;
            } else {
                return true;
            }
        }
    }
    
    
    /** Read a Nortek binary sample; overrides
     * BaseInstrumentService.readSample().
     */
    protected int readSample(byte[] sample) throws TimeoutException, IOException, Exception {
        
        _log4j.debug("readSample()");
        
        
        // Read data from instrument into data structure
        int nBytes =
                DataStructure.read(_fromDevice, sample,
                (_attributes.avgIntervalSec + 1)*1000);
        
        // If we get here, we've got a sample...
        _nowStreaming = true;
        
        if (DataStructure.id(sample) == HR_PROFILER_DATA) {
            _hrProfilerData.setBytes(sample);
            _log4j.debug("readSample(): got sample");
            // _log4j.debug("Data:\n" + _hrProfilerData);
        }
        
        return nBytes;
    }
    
    /** Send command to set the measurement interval (assumes we've got
     * instrument's attention already) */
    protected void setMeasurementInterval(short sec) throws Exception {

	_log4j.warn("Bypassing setMeasurementInterval() for TEST!!!");
	return;
	/* ***
        byte[] cmd = new byte[4];
        cmd[0] = 'M';
        cmd[1] = 'I';
        
        // Least-significant byte comes first
        cmd[2] = (byte )(sec & 0xFF);
        
        // Most significant byte comes second
        cmd[3] = (byte )(sec >> 8);
        
        // Write the command to the instrument
        _toDevice.write(cmd);
        _toDevice.flush();
        
        // NOTE: verify ACK ACK from instrument (else there's a problem)
        StreamUtils.skipUntil(_fromDevice, ACK_ACK, 5000);
	*** */
    }
    
    
    
    /** Make sure to terminate measurement mode - it is "bad" to disconnect
     * power while in measurement mode. */
    protected String shutdownInstrument()
    throws Exception {
        
        // Get out of measurement if necessary
        getInstrumentAttention();
        
        return "Aquadopp shut down";
    }



    public class Parser extends PacketParser implements Serializable {

	DevicePacketParser _jddacParser;
	java.util.Vector _vector = new java.util.Vector();

	public Parser(DevicePacketParser jddacParser) {
	    _jddacParser = jddacParser;
	}


	public Parser.Field[] parseFields(DevicePacket packet) 
	    throws NotSupportedException, ParseException {
	    
	    SiamRecord record = null; 
	    // First parse to a JDDAC record
	    try {
		record = _jddacParser.parse(packet);
	    }
	    catch (Exception e) {
		throw new NotSupportedException(e.getMessage());
	    }

	    // Convert JDDAC record to array of PacketParser.Fields
	    _log4j.debug("parseFields(): record=" + record);

	    // Clear out temporary vector
	    _vector.clear();

	    // Convert each element to a PacketParser.Field
	    for (Enumeration e = record.elements(); e.hasMoreElements();) {
		Object element = e.nextElement();
		_log4j.debug("parseFields() - convert to Field: " + element +
			     "(" + e.getClass().getName() + ")");
		if (element instanceof Measurement) {
		    _log4j.debug("Got a Measurement: " + element);
		    Measurement measurement = (Measurement )element;
		    // Convert Measurement to Field
		    Field field = new Field((String )measurement.get("name"),
					    measurement.get("value"),
					    (String )measurement.get("units"));
		    _vector.add(field);
		}
	    }

	    // Allocate array
	    PacketParser.Field[] fields = 
		new PacketParser.Field[_vector.size()];

	    // Copy fields from vector to array
	    for (int i = 0; i < _vector.size(); i++) {
		fields[i] =  (Field )_vector.elementAt(i);
	    }

	    return fields;
	}

    }
    
} // End of Aquadopp class
