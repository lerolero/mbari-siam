/**
 * Copyright 2004 MBARI. MBARI Proprietary Information. All rights reserved.
 */
package org.mbari.siam.devices.serialadc;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.rmi.RemoteException;
import java.util.StringTokenizer;

import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.core.EventManager;
import org.mbari.siam.core.PowerEvent;

import org.mbari.siam.utils.StopWatch;
import org.mbari.siam.utils.StreamUtils;

import org.mbari.siam.distributed.devices.Power;

import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.TimeoutException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

public class SerialAdc extends PolledInstrumentService implements Instrument {

    // CVS revision 
    private static String _versionID = "$Revision: 1.2 $";

    // log4j Logger
    static private Logger _log4j2 = Logger.getLogger(SerialAdc.class);

    /** Default SerialAdc serial baud rate */
    static final int _BAUD_RATE = 38400;

    /** Default SerialAdc serial data bits */
    static final int _DATA_BITS = SerialPort.DATABITS_8;

    /** Default SerialAdc serial stop bits */
    static final int _STOP_BITS = SerialPort.STOPBITS_1;

    /** Default SerialAdc parity checking */
    static final int _PARITY = SerialPort.PARITY_NONE;

    /** DATA COUNT string length */
    static final int _DATA_COUNT_LINE_LEN = 18;

    /** DATA COUNT number places length */
    static final int _DATA_COUNT_NUMBER_LEN = 7;

    /** Period is senconds which the log time sync will be performed */
    static final int _LOG_TIME_SYNC_PERIOD = 3600; //in seconds

    /** SerialAdc sample timeout in milliseconds */
    static final int _SAMPLE_TIMEOUT = 20000;

    /** SerialAdc instuemnt start up delay in milliseconds */
    static final int _INSTRUMENT_START_DELAY = 10000;

    /** SerialAdc instuemnt shutdown delay in milliseconds */
    static final int _INSTRUMENT_SHUTDOWN_DELAY = 5000;

    /** Last time in secondes the log time sync was performed */
    long _lastLogTimeSync = 0;


    // Configurable SerialADC attributes
    Attributes _attributes = new Attributes(this);

    /** Indicates that the power system is in trouble and the system
	needs to be put into safe mode and powered down.
    */
    static final int _POWER_STATUS_CRITICAL=2;
    
    /** Indicates that the power system is not at nominal levels
	but it is not necessary to shut down
    */
    static final int _POWER_STATUS_WARNING=1;
    
    /** Indicates that the power system is OK
     */
    static final int _POWER_STATUS_OK=0;

    /** Indicates that the power system status cannot be determined
     */
    static final int _POWER_STATUS_UNKNOWN=3;
    

    public SerialAdc() throws RemoteException {
    }

    /** Sets SerialAdc power policy */
    protected PowerPolicy initInstrumentPowerPolicy() {
	return PowerPolicy.ALWAYS;
    }

    /** Sets SerialAdc communications power policy */
    protected PowerPolicy initCommunicationPowerPolicy() {
	return PowerPolicy.ALWAYS;
    }

    /** Set SerialAdc startup delay time */
    protected int initInstrumentStartDelay() {
	//rather long but the P2 has to search for used files now
	return _INSTRUMENT_START_DELAY;
    }

    /** Sets SerialAdc current limit */
    protected int initCurrentLimit() {
	return 500;
    }

    /** Sets the SerialAdc sample terminator */
    protected byte[] initSampleTerminator() {
	return "RDY\r\n".getBytes();
    }

    /** Sets the SerialAdc command prompt */
    protected byte[] initPromptString() {
	return "RDY\r\n".getBytes();
    }

    /**
     * Sets the SerialAdc maximum number of bytes in a instrument data sample
     */
    protected int initMaxSampleBytes() {
	return 4096;
    }

    /** Initialize the serial ADC board. */
    protected void initializeInstrument() {
	try {
	    setSampleTimeout(_SAMPLE_TIMEOUT);
	} catch (RangeException e) {
	    _log4j2.error(e);
	}

	// Turn on DPA power/comms
	managePowerWake();

	// grab get instrument attention
	try {
	    // Make sure instrument's onboard app is running
	    // (might be just sitting in picodos)
	    _toDevice.write("APP\r".getBytes());

	    getInstrumentAttention(3);

	    if(this instanceof Power){
		Power power=(Power)this;
		int enableBatteries=_attributes.enableBackupBatteries?Power.BIN_BACKUP_EN:Power.BIN_BACKUP_DI;
		int enableCapacitors=_attributes.enableBackupCapacitors?Power.BIN_BACKUP_EN:Power.BIN_BACKUP_DI;
		byte[] retval = power.binBackups(enableBatteries,enableCapacitors);
		if(retval!=null)
		    _log4j2.debug("Power backup initialization returned: "+new String(retval));
		return;
	    }

	    
	} catch (Exception e) {
	    _log4j2.error(e);
	}

	_log4j2.debug("done with initializeInstrument()");
    }

    /** Return parameters to use on serial port. */
    public SerialPortParameters getSerialPortParameters()
	throws UnsupportedCommOperationException {

	return new SerialPortParameters(_BAUD_RATE, _DATA_BITS, _PARITY,
					_STOP_BITS);
    }

    /**
     * Signal the SerialAdc that you are ready to fetch the data. This method
     * must be called before request sample
     */
    protected void prepareToSample() throws Exception {
	byte[] cap_cmd = "DATA CAPTURE ".getBytes();
	byte[] cap_params = _attributes.captureParams.getBytes();

	//get instruments attention
	getInstrumentAttention(3);

	//log time sync if enabled
	if (_attributes.logTimeSync) {
	    //get the current time since epoch in seconds
	    long time_seconds = System.currentTimeMillis() / 1000;

	    if ((_lastLogTimeSync + _LOG_TIME_SYNC_PERIOD) < time_seconds) {
		_lastLogTimeSync = time_seconds;

		//write the time to the P2
		String log_time_sync = "LOG TIME " + time_seconds + "\r";
		_toDevice.write(log_time_sync.getBytes());

		//read out RDY response
		StreamUtils.skipUntil(_fromDevice, getSampleTerminator(),
				      getSampleTimeout());

		_log4j2.debug("time sync'd P2 with: " + log_time_sync);
	    }
	}

	//capture the data
	_toDevice.write(cap_cmd);
	_toDevice.write(cap_params);

	//read out RDY response
	StreamUtils.skipUntil(_fromDevice, getSampleTerminator(),
			      getSampleTimeout());
    }

    /** Read captured data sample from the SerialAdc. */
    protected int readSample(byte[] sample) throws TimeoutException, Exception {
	int bytes_read = StreamUtils.readUntil(_fromDevice, sample,
					       getSampleTerminator(), getSampleTimeout());

	// if you got less than _DATA_COUNT_LINE_LEN bytes something is wrong
	if (bytes_read < _DATA_COUNT_LINE_LEN){
	    _log4j2.debug("bytesRead<DataCountLineLen ("+bytes_read+","+_DATA_COUNT_LINE_LEN+")");
	    return -1;
	}

	if (_attributes.dataCountCheck) {
	    //get DATA COUNT string
	    String data_count_str = new String(sample,
					       (bytes_read - _DATA_COUNT_NUMBER_LEN),
					       _DATA_COUNT_NUMBER_LEN);

	    data_count_str = data_count_str.trim();

	    _log4j2.debug("data_count_str = " + data_count_str);

	    // initialize sample CRC in case parse fails
	    int data_count = 0;

	    //get the data count of the fetched packet
	    try {
		data_count = Integer.parseInt(data_count_str);
	    } catch (NumberFormatException e) {
		_log4j2.warn("failed to parse data_count_str: " + e);
		return -1;
	    }

	    if (data_count != (bytes_read - _DATA_COUNT_LINE_LEN)) {
		_log4j2.warn("got DATA COUNT " + data_count + " expected "
			    + (bytes_read - _DATA_COUNT_LINE_LEN));
		return -1;
	    }
	}

	return bytes_read;
    }

    /** Return SerialAdc metadata. */
    protected byte[] getInstrumentStateMetadata() {
	//allocate storage for meta data
	byte[] vers_buff = new byte[128];
	byte[] config_buff = new byte[4096];
	byte[] calib_buff = new byte[2048];
	String error_str = "";

	int vers_bytes = 0;
	int config_bytes = 0;
	int calib_bytes = 0;

	String class_name = this.getClass().getName();

	try {
	    getInstrumentAttention(3);
	    //get version
	    _toDevice.write("VERS ALL\r".getBytes());

	    vers_bytes = StreamUtils.readUntil(_fromDevice, vers_buff,
					       getSampleTerminator(), getSampleTimeout());
	    //get configuration info
	    _toDevice.write("GET CONFIG\r".getBytes());

	    config_bytes = StreamUtils.readUntil(_fromDevice, config_buff,
						 getSampleTerminator(), getSampleTimeout());
	    //get calibration info
	    _toDevice.write("GET CALIBRATION\r".getBytes());

	    calib_bytes = StreamUtils.readUntil(_fromDevice, calib_buff,
						getSampleTerminator(), getSampleTimeout());
	} catch (Exception e) {
	    // Couldn't get metadata...
	    _log4j2.warn("Couldn't get " + class_name + " instrument state");
	    error_str = "Couldn't get " + class_name + " instrument state\n";
	}

	String meta_data;
	//grab the metadata or the error string
	if (error_str.length() > 1) {
	    meta_data = error_str;
	} else {
	    meta_data = "VERSION:\r\n" + new String(vers_buff, 0, vers_bytes)
		+ "CONFIGURATION:\r\n"
		+ new String(config_buff, 0, config_bytes)
		+ "CALIBRATION:\r\n"
		+ new String(calib_buff, 0, calib_bytes);
	}

	return meta_data.getBytes();
    }

    /** Set the sensor's clock. This does nothing in the SerialAdc driver */
    public void setClock(long time) {
    }

    /** Self-test routine; This does nothing in the SerialAdc driver */
    public int test() {
	return Device.OK;
    }

    /** Request captured data from the SerialAdc */
    protected void requestSample() throws IOException {
	byte[] cap_cmd = "DATA FETCH ".getBytes();
	byte[] cap_params = _attributes.captureParams.getBytes();

	//flush the input
	_fromDevice.flush();
	//fetch the data
	_toDevice.write(cap_cmd);
	_toDevice.write(cap_params);
    }

    /** Try to gracefully exit P2 app then, stop the service. */
    protected String shutdownInstrument() throws Exception {
	// exit the P2 application to make sure logging is stopped 
	// correctly
	try {
	    getInstrumentAttention(3);
	} catch (Exception e) {
	    _log4j2.warn("failed to get P2 attention on shutdown: " + e);
	}

	// May take several attempts to shutdown P2 board
	int i;
	for (i = 0; i < 3; i++) {
	    try {
		_toDevice.write("EXIT\r".getBytes());
		StreamUtils.skipUntil(_fromDevice, "C:\\>".getBytes(),
				      _INSTRUMENT_SHUTDOWN_DELAY);
		break;
	    } catch (Exception e) {
		// Timed out, or some other problem
		_log4j2.warn("failed to 'EXIT' P2 app on shutdown: " + e);

		// wait a bit before you try again
		StopWatch.delay(100);
	    }
	}

	//send and error if things just didn't work out
	if (i > 2)
	    _log4j2.error("SerialAdc failed to shutdown correctly");

	return "OK";
    }

    void getInstrumentAttention(int tries) throws Exception {
	//always make sure the echo mode is off
	_toDevice.write("ECHO OFF\r".getBytes());

	// May take several attempts to get SerialAdc prompt
	for (int i = 0; i < tries; i++) {
	    _fromDevice.flush();
	    _toDevice.write("\r".getBytes());

	    try {
		StreamUtils.skipUntil(_fromDevice, getPromptString(),
				      getSampleTimeout());
	    } catch (Exception e) {
		// Timed out, or some other problem
		_log4j2.warn(e);

		if (i == tries - 1) {
		    // No more tries left
		    e = new Exception("getInstrumentAttention(): "
				      + "exceeded max tries");
		    _log4j2.error(e);
		    throw e;
		}

		StopWatch.delay(100);
	    }
	}
    }

    /** Return specifier for default sampling schedule. */
    protected ScheduleSpecifier createDefaultSampleSchedule()
	throws ScheduleParseException {
	// Sample every 60 seconds by default
	return new ScheduleSpecifier(60000);
    }

    /***/
    protected class ADCChannel{
	double _avg=Double.NaN;
	double _min=Double.NaN;
	double _max=Double.NaN;
	double _dev=Double.NaN;
	int _samples=(-1);
	int _errors=(-1);
	int _channel=(-1);

	ADCChannel(){
	}

	ADCChannel(String line){
	    parse(line);
	}

	public boolean parse(String line){
	    StringTokenizer st=new StringTokenizer(line," ");
	    
	    String t=st.nextToken().trim();

	    if(t.indexOf("ADC")==0){
		_channel=Integer.parseInt(t.substring(3));
		try{
		    while(st.hasMoreTokens()){
			t=st.nextToken().trim();
			if(t.indexOf("A")==0){
			    _avg=Double.parseDouble(t.substring(1));
			}else
			if(t.indexOf("N")==0){
			    _samples=Integer.parseInt(t.substring(1));
			}else
		        if(t.indexOf("L")==0){
			    _min=Double.parseDouble(t.substring(1));
			}else
			if(t.indexOf("H")==0){
			    _max=Double.parseDouble(t.substring(1));
			}else
		        if(t.indexOf("D")==0){
			    _dev=Double.parseDouble(t.substring(1));
		        }else
			if(t.indexOf("E")==0){
			    _errors=Integer.parseInt(t.substring(1));
			}
		    }
		}catch(NumberFormatException e){
		    _log4j2.error("Invalid number format parsing "+t+" in "+line);
		    return false;
		}
	    }else{
		_log4j2.error("Unknown channel type "+t+" in "+line);
		return false;
	    }
	    return true;
	}

	public double getAverage(){return _avg;}
	public double getMinimum(){return _min;}
	public double getMaximum(){return _max;}
	public double getDeviation(){return _dev;}
	public int getSamples(){return _samples;}
	public int getErrors(){return _errors;}
	public int getChannel(){return _channel;}
	public String toString(){
	    String s="A"+_avg+
		"L"+_min+
		"H"+_max+
		"N"+_samples+
		"D"+_dev+
		"E"+_errors+
		"C"+_channel;
	    return s;
	}
    }

    /** Determine power status based on configurable service 
	attributes (powerOkLow and powerWarningLow), indicating
	the minimum values for each condition.
     */
    protected int getPowerStatus(ADCChannel adcChannel){
	

	double powerStatus=adcChannel.getAverage();
	int samples=adcChannel.getSamples();

	_log4j2.debug("getPowerStatus raw status: "+powerStatus+
		      " powerOkLow="+_attributes.powerOkLow+
		      " powerWarningLow="+_attributes.powerWarningLow+
		      " statusMinSamples="+samples);

	// do any filtering here
	if(samples < _attributes.statusMinSamples)
	    return _POWER_STATUS_UNKNOWN;

	if(powerStatus >= _attributes.powerOkLow)
	    return _POWER_STATUS_OK;

	if(powerStatus > _attributes.powerWarningLow)
	    return _POWER_STATUS_WARNING;

	return _POWER_STATUS_CRITICAL;
    }

    /** Get specified ADC channel line from sample buffer */
    public String getChannel(String channelName){
	BufferedReader br=new BufferedReader(new InputStreamReader(new ByteArrayInputStream(getSampleBuf())));
	String line="";
	_log4j2.debug("getChannel: looking for "+channelName);
	try{
	    while( (line=br.readLine())!=null ){
		line=line.trim();
		if(line.indexOf(channelName)>=0){
		    _log4j2.debug("getChannel: found "+channelName+" in "+line);
		    return line;
		}
	    }
	}catch(IOException e){
	    e.printStackTrace();
	    _log4j2.error(e);
	}
	_log4j2.debug("getChannel: could not find "+channelName+" in:\n"+getSampleBuf());
	return null;
    }

    /**
     * Called after sample has been acquired, processed and logged. By default 
     this method does nothing, an may be overridden in the subclass.
    */
    protected void postSample() {
	super.postSample();
	
	// Here, we'll check for power failure and notify the node service
	// if a fault has occurred that will require the node to be shutdown

	// look in the sample buffer just received for the power status
	// info
	String line=getChannel(_attributes.powerStatusChannel);
	if(line==null){
	    _log4j2.error("Could not find PowerStatusChannel record "+_attributes.powerStatusChannel);
	    return;
	}

	// parse the power status line (done by the ADCChannel constructor)
	ADCChannel adcChannel=new ADCChannel(line);
	int powerStatus=getPowerStatus(adcChannel);

	if(powerStatus==_POWER_STATUS_CRITICAL){
	    
	    _log4j2.warn("POWER_STATUS_CRITICAL condition detected! Posting event now.");

	    // power critical -- generate an event and post it to the 
	    // EventManager. The NodeService listens for this event and
	    // will initiate safe shutdown of the node.
	    PowerEvent event = 
		new PowerEvent(this, PowerEvent.POWER_FAILURE_DETECTED,adcChannel.toString());

	    // Post event to the EventManager queue
	    EventManager.getInstance().postEvent(event);


	    _log4j2.info("service started");
	    
	}else{
	    String s="";
	    if(powerStatus==_POWER_STATUS_WARNING){
		s="_POWER_STATUS_WARNING";
		String msg="POWER_STATUS_WARNING condition detected: "+adcChannel.toString();
		annotate(msg.getBytes());
	    }
	    if(powerStatus==_POWER_STATUS_OK)
		s="_POWER_STATUS_OK";
	    if(powerStatus==_POWER_STATUS_UNKNOWN)
		s="_POWER_STATUS_UNKNOWN";

	    _log4j2.debug("postSample(): getPowerStatus returned "+s);
	}
    }


    /** Configurable SerialADC attributes */
    class Attributes extends InstrumentServiceAttributes {

	/** _dataCountCheck can be enabled and disabled via property */
	boolean dataCountCheck = false;

	/** _logTimeSync can be enabled and disabled via property */
	boolean logTimeSync = false;

	/** Default DATA CAPTURE and DATA FETCH parameters */
	String captureParams = "ALL\r";

	/** String to parse for to find powerStatusFlag */
	String powerStatusChannel="ADC99";

	/** Power OK lower threshold (should be > powerWarningLow) */
	double powerOkLow=0;
	      
	/** Power WARNING lower threshold (should be > 0) */
	double powerWarningLow=0;

	/** Minimum samples for to validate power status */
	int statusMinSamples=20;

	/** Set state of backup batteries at service start  */
	boolean enableBackupBatteries = false;

	/** Set state of backup caps at service start  */
	boolean enableBackupCapacitors = true;

	/** Constructor, with required InstrumentService argument */
	Attributes(DeviceServiceIF service) {
	    super(service);
	}

	/**
	 * Throw InvalidPropertyException if any invalid attribute values found
	 */
	public void checkValues() throws InvalidPropertyException {

	    String prop = captureParams;

	    /** Parse capture parameters */
	    //make sure only ADC GF ENV WS or ADC exist in the string
	    StringTokenizer st = new StringTokenizer(prop.trim());

	    /* check all the tokens to see what they be */
	    boolean bad_param = false;

	    while (st.hasMoreTokens() && (!bad_param)) {
		String token = st.nextToken();

		if ((token.compareToIgnoreCase("ADC") != 0)
		    && (token.compareToIgnoreCase("GF") != 0)
		    && (token.compareToIgnoreCase("WS") != 0)
		    && (token.compareToIgnoreCase("ENV") != 0)
		    && (token.compareToIgnoreCase("BP") != 0)
		    && (token.compareToIgnoreCase("ALL") != 0)) {
		    bad_param = true;
		}
	    }

	    //if you got a bad param use the default
	    if (bad_param) {
		_log4j2.warn("Bad parameter in captureParams entry '" + prop
			    + "', using default 'ALL'");

		captureParams = "ALL\r";

	    } else {
		_log4j2.debug("Setting captureParams to '" + prop
			     + "' found in properties");

		// all the parameters were valid, so use the string
		captureParams = prop.trim() + "\r";
	    }
	}
    }
}

