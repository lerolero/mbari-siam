package org.mbari.siam.devices.nortek.vector;

/**
 * @Title Nortek Vector Current Meter Instrument Driver
 * @author Bob Herlien
 * @version 1.0
 * @date 08 May 2008
 *
 * Copyright MBARI 2009
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

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.lang.NumberFormatException;
import java.text.ParseException;

import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.core.SerialInstrumentPort;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.core.StreamingInstrumentService;
import org.mbari.siam.distributed.measurement.Averager;
import org.mbari.siam.utils.StopWatch;
import org.mbari.siam.utils.PrintfFormat;
import org.mbari.siam.utils.StreamUtils;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.Safeable;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.Velocity;
import org.mbari.siam.distributed.DevicePacketParser;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.InvalidDataException;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.NotSupportedException;


/** Instrument service for Aquadopp current profiler. */
public class Vector extends Aquadopp
    implements AquadoppIF, Safeable
{
    /** log4j Logger */
    static private Logger _log4j = Logger.getLogger(Vector.class);
    
    public final static int VECTOR_VELOCITY_DATA = 0x10;
    public final static int VECTOR_SYSTEM_DATA = 0x11;
    
    /** Define system data and averaged data record types */
    static final long RECORDTYPE_SYSTEM = RECORDTYPE_DEFAULT + 1;
    static final long RECORDTYPE_AVG_VEL = RECORDTYPE_SYSTEM + 1;

    /** Set instrument clock */
    static final byte[] CONFIGURE_CMD = "CC".getBytes();
    
    /** Default Vector current limit for the optode (milliamps) */
    static final int _INSTRUMENT_CURRENT_LIMIT = 3000;
    
    /** Default Vector start delay (ms) */
    static final int _INSTRUMENT_START_DELAY = 0;
    
    /** Vector velocity data */
    public VectorVelocityData _velocityData = null;
    
    /** Vector system data */
    public VectorSystemData _systemData = null;

    /** Following are for calculating average velocities */
    protected long _avgStartTime = 0;
    protected double _velXaccum = 0.0, _velYaccum = 0.0, _velZaccum = 0.0;
    protected int  _avgSamples = 0;
    protected SensorDataPacket _avgDataPkt = new SensorDataPacket();
    protected PrintfFormat _avgFmt = new PrintfFormat("%.3f");


    /** COMMANDS */
    
    /** Max constants - Just punched values in to try to use sendCommand(). */
    static final int MAX_RESPONSE_BYTES = 5000;  // Keep it large for now!!
    
    static final int MAX_SAMPLE_TRIES = 3;
    
    static final int MAX_COMMAND_TRIES = 5;
    
    
    /** Timeouts: This needs to be longer than the longest likely
     * intercharacter delay. (Milliseconds) */
    static final int RESPONSE_TIME = 10000; //Keep it large.  Aquadopp averages over 5 sec. before responding.
    
    static final int SAMPLE_RESPONSE_TIME = 1000;
    
    /**
     * Parser used to convert SensorDataPackets into SiamRecords that can be
     * consumed by the SummaryBlock
     */
    private DevicePacketParser devicePacketParser;
    
    /** Configurable Aquadopp attributes */
    public VectorAttributes _attributes = new VectorAttributes(this);
    
    public Vector() throws RemoteException
    {
	super();

	_log4j.debug("Vector constructor!");
        
        // Vector velocity data
        _velocityData = new VectorVelocityData();
        
        // Vector system data
        _systemData = new VectorSystemData();
        
    }
    
    /** Sets Vector power policy */
    protected PowerPolicy initInstrumentPowerPolicy() {
        return PowerPolicy.ALWAYS;
    }
    
    /** Sets Vector communications power policy */
    protected PowerPolicy initCommunicationPowerPolicy() {
        return PowerPolicy.ALWAYS;
    }
    
    
    /** Sets the Aquadopp maximum number of bytes in an instrument
     * data sample */
    protected int initMaxSampleBytes() {
        return 256; 
    }
    
    
    /** Initialize the Instrument
     *  This means to set all initial settings - This section for one
     * time only instrument settings */
    protected void initializeInstrument() throws InitializeException, Exception
    {
	_log4j.debug("Allocating new _avgDataPkt, ID = " + getId());
	_avgDataPkt = new SensorDataPacket(getId(), 128);

        if (_attributes.logEnabled && _attributes.burstMode) {
            throw new InitializeException("burst mode not compatible with internal logging");
        }

	/* If in listenOnly mode, can't configure instrument */
	if (_attributes.listenOnly)
	{
	    _nowStreaming = true;
	    return;
	}

	_toDevice.setInterByteMsec(0);
        
        try {
            // Get instrument mode
            getMode();
        } catch (Exception e) {
            _log4j.error("initializeInstrument() - trying to get mode: ", e);
        }
        
        // Stop autonomous logging by instrument (If was in safeMode).
        _log4j.info("initializeInstrument() - Getting Aquadopp's attention (Stops any previous logging).");
        getInstrumentAttention();
        
        try {
            setSampleTimeout(2000);
        } catch (RangeException e) {
            _log4j.error("Failed call to setSampleTimeout", e);
        }

	// Send configuration file
	if (_attributes.configFile != null)
	{
	    _log4j.debug("About to send configFile:  " + _attributes.configFile);
	    StopWatch.delay(200);

	    try {
		sendConfiguration(_attributes.configFile);
	    } catch (Exception e) {
		_log4j.warn("Exception sending config file " + _attributes.configFile +
			    " to Vector: " + e);
	    }
	}

	_log4j.debug("Sent configFile:  " + _attributes.configFile);

        // Set instrument clock
        setClock();

	clearAverages(System.currentTimeMillis());
        
        _log4j.debug("Done with initializeInstrument().");
    }
    
    
    /** Get Instrument State Metadata. TRUE STATE OF INSTRUMENT. */
    protected byte[] getInstrumentStateMetadata()
	throws Exception
    {
	if (_attributes.listenOnly)
	    return("Instrument in listen-only mode.  Can't retrieve state metadata\n".getBytes());

	byte[] rtn = super.getInstrumentStateMetadata();

	_attributes.coordSystem = _instrumentConfig.coordSystem();

	/* Per Sven at Nortek, fequency = 512/hAvgInterval, rah	*/
	_attributes.sampleFrequency = _instrumentConfig.sampleFrequency();

	_log4j.debug("coordSystem = " + _instrumentConfig.coordSystem() +
		     "; sampleFrequency = " + _instrumentConfig.sampleFrequency());
        return(rtn);
    }
    
    protected void sendConfiguration(String configFile) throws Exception
    {
	FileInputStream in = new FileInputStream(configFile);
	int bytesRead;
	byte[] buffer = new byte[1024];

	_toDevice.write(CONFIGURE_CMD);

	while ((bytesRead = in.read(buffer)) > 0)
	    _toDevice.write(buffer, 0, bytesRead);

	in.close();

	try {
            StreamUtils.skipUntil(_fromDevice, ACK_ACK, 5000);
        } catch (Exception e) {
            throw new IOException("Instrument didn't acknowledge CC command");
        }
    }
    
    /** Return specifier for default sampling schedule. Subclasses MUST
     * provide the default sample schedule. */
    protected ScheduleSpecifier createDefaultSampleSchedule()
    throws ScheduleParseException {
        
        // Sample every 60 seconds by default
        return new ScheduleSpecifier(60000);
    }
    
    
    /** Service attributes. */
    public class VectorAttributes extends Aquadopp.Attributes {
        
        public VectorAttributes(StreamingInstrumentService service) {
            super(service);
            
            /*
             * The names of the variables that will be summaryized. These should match
             * the names that are put out by the {@link DevicePacketParser} if you want
             * any actual summaries to occur.
             */
            summaryVars =
                    new String[]{"velocity-X", "velocity-Y", "velocity-Z"};
        }
	/** Registry Name	*/
	public String registryName = "Velocity";
        
        /** Coordinate System */
	public String coordSystem = "XYZ";

        /** Sample Frequency	*/
	public int sampleFrequency = 2;

	/** Configuration file to send to instrument. 
	 * Leave as null if you don't want to download a configuration.
	 * File type should be of type .pcf.  See "CC" command in System Integrator Manual.
	 */
	public String configFile = null;

	/** True to listen (snoop) only.  Used when instrument is configured for external
	 *  control, and our TxD doesn't go to the instrument
	*/
	public boolean listenOnly = false;

	/** Set limitSamplesPerSession to false by default */
	public boolean limitSamplesPerSession = false;

	/** Seconds to average over in order to generate avgVelocity{XYZ}
	 * Set to zero to disable averaging.
	 */
	public int avgVelocitySecs = 20;

        /**
         * Return true if specified attribute is "configurable", i.e. can be
         * initialized at startup by a property.
         *
         * @param attributeName
         * @return true if configurable, false otherwise
         */
        protected boolean isConfigurable(String attributeName) {
            if (attributeName.equals("avgIntervalSec")) {
                return false;
            } else if (attributeName.equals("sampleFrequency")) {
                return false;
            } else if (attributeName.equals("coordSystem")) {
                return false;
            } else {
                return true;
            }
        }
    }
    
    
    /** Helper function for readSample to actually get the data from the Vector
     */
    protected int readVectorSample(byte[] sample) throws TimeoutException, IOException, Exception
    {
	return(DataStructure.read(_fromDevice, sample,
				  SAMPLE_RESPONSE_TIME));
    }
        
    /** Read a Nortek binary sample; overrides
     * BaseInstrumentService.readSample().
     */
    protected int readSample(byte[] sample) throws TimeoutException, IOException, Exception {
        
        _log4j.debug("readSample()");
        
        // Read data from instrument into data structure
        int nBytes = readVectorSample(sample);
        
        // If we get here, we've got a sample...
        _nowStreaming = true;
        
        switch (DataStructure.id(sample))
	{
	  case VECTOR_SYSTEM_DATA:
            _systemData.setBytes(sample);
	    setRecordType(RECORDTYPE_SYSTEM);
            _log4j.debug("readSample(): got system data");
	      break;

	  case VECTOR_VELOCITY_DATA:
            _velocityData.setBytes(sample);
	    setRecordType(RECORDTYPE_DEFAULT);
            _log4j.debug("readSample(): got velocity data");
	      break;

        }

        return nBytes;
    }


    protected void clearAverages(long now)
    {
	_velXaccum = 0.0;
	_velYaccum = 0.0;
	_velZaccum = 0.0;
	_avgSamples = 0;
	_avgStartTime = now;
    }

    /** Override acquire() in order to calculate averages */
    protected synchronized SensorDataPacket acquire(boolean logSample)
	throws NoDataException
    {
	SensorDataPacket packet = super.acquire(logSample);
	byte[] sample = packet.dataBuffer();
	long curTime = System.currentTimeMillis();
	
	if ((_attributes.avgVelocitySecs > 0) && 
	    (DataStructure.id(sample) == VECTOR_VELOCITY_DATA))
	{
	    try {
		double mult = _systemData.velocityMult();
		_velXaccum += (mult * _velocityData.velocityX());
		_velYaccum += (mult * _velocityData.velocityY());
		_velZaccum += (mult * _velocityData.velocityZ());
		_avgSamples += 1;
	    } catch (NullPointerException e) {
		return(packet);
	    }

	    if (((curTime - _avgStartTime) > (1000*_attributes.avgVelocitySecs)) &&
		(_avgSamples > 0))
	    {
		String avgStr = _avgFmt.sprintf(_velXaccum/_avgSamples) + " " + 
		    _avgFmt.sprintf(_velYaccum/_avgSamples) + " " + 
		    _avgFmt.sprintf(_velZaccum/_avgSamples) + " " + 
		    _avgSamples;
		
		_avgDataPkt.setDataBuffer(avgStr.getBytes());
		_avgDataPkt.setSystemTime(curTime);
		_avgDataPkt.setRecordType(RECORDTYPE_AVG_VEL);
		clearAverages(curTime);

		callDataListeners(_avgDataPkt);

		if (logSample) {
		    logPacket(_avgDataPkt);
		}

	    }
	}

	return(packet);
    }


    /** Make sure to terminate measurement mode - it is "bad" to disconnect
     * power while in measurement mode. */
    protected String shutdownInstrument() throws Exception
    {
        // Get out of measurement if necessary
        getInstrumentAttention();
        
        return "Vector shut down";
    }

    /** Return a PacketParser. */
    public PacketParser getParser() throws NotSupportedException{
	return new VectorPacketParser(_attributes.registryName,_systemData.velocityMult());
    }

    /** Parse the Vector data into a Velocity struct	*/
    public Object parseDataPacket(SensorDataPacket pkt) throws InvalidDataException
    {
	try{
	    PacketParser parser=getParser();
	    // return PacketParser.Field[]
	    return parser.parseFields(pkt);
	}catch(NotSupportedException e){
	    _log4j.error(e.toString());
	}catch(ParseException p){
	    throw new InvalidDataException("ParseException caught: "+p.toString());
	}
	return null;

	/*
	  double x, y, z, mult;

	  try {
	  mult = _systemData.velocityMult();

	  x = mult * _velocityData.velocityX();
	  y = mult * _velocityData.velocityY();
	  z = mult * _velocityData.velocityZ();
	  } catch (NullPointerException e) {
	  throw new InvalidDataException();
	  }

	  return(new Velocity(x,y,z));
	*/
    }


    /** Method overridden in order to implement 'listenOnly' */
    void getInstrumentAttention() throws Exception
    {
	if (!_attributes.listenOnly)
	    super.getInstrumentAttention();
    }

    /** Stop streaming.  Overridden to implement 'listenOnly' */
    protected void stopStreaming() throws Exception
    {
	if (!_attributes.listenOnly)
	    super.stopStreaming();
    }

    /** Put instrument into streaming mode.  Overriddent to implement 'listenOnly' */
    protected void startStreaming() throws Exception
    {
	if (!_attributes.listenOnly)
	    super.startStreaming();
    }

} // End of Vector class
