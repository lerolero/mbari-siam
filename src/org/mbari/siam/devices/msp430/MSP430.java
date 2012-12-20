// Copyright MBARI 2002
package org.mbari.siam.devices.msp430;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.rmi.RemoteException;
import java.text.ParseException;

import org.mbari.siam.core.DebugMessage;
import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.distributed.devices.Environmental;
import org.mbari.siam.utils.StopWatch;
import org.mbari.siam.utils.StreamUtils;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.DeviceServiceIF;

/**
 * Driver for MSP430 Environmental service
 * 
 * @author Tom O'Reilly modified by Bob Herlien to add commandAndReply method
 *         for SleepService
 */
public class MSP430 extends PolledInstrumentService implements Environmental {

    static private Logger _log4j = Logger.getLogger(Environmental.class);

    protected static MSP430 _instance = null;
	
    protected Attributes _attributes = new Attributes(this);

    private static final int _SAMPLE_TIME_OUT = 10000;

    private byte _tmpBuf[] = new byte[1024];

    public MSP430() throws RemoteException {
	_instance = this;

    }

    /** Maximum number of bytes in environmental metadata. */
    protected static final int ENV_METADATA_BYTES = 50;

    /** Specify prompt string. */
    protected byte[] initPromptString() {
	return "> ".getBytes();
    }

    /** Specify sample terminator. */
    protected byte[] initSampleTerminator() {
	return "\r\n".getBytes();
    }

    /** Specify maximum bytes in raw sample. */
    protected int initMaxSampleBytes() {
	return 256;
    }

    /** Specify current limit. */
    protected int initCurrentLimit() {
	return 500;
    }

    /** Return initial value of instrument power policy. */
    protected PowerPolicy initInstrumentPowerPolicy() {
	return PowerPolicy.ALWAYS;
    }

    /** Return initial value of communication power policy. */
    protected PowerPolicy initCommunicationPowerPolicy() {
	return PowerPolicy.ALWAYS;
    }

    /** Specify device startup delay (millisec) */
    protected int initInstrumentStartDelay() {
	return 500;
    }

    /** Get MSP430's attention (i.e. user prompt) */
    protected void getAttention() throws IOException, TimeoutException,
					 Exception {

	_fromDevice.flush();
	_toDevice.write("\n".getBytes());
	StreamUtils.skipUntil(_fromDevice, getPromptString(), 5000);
    }

    /** Request a data sample. */
    protected synchronized void requestSample() throws IOException {
	_fromDevice.flush();
	_toDevice.write("\n".getBytes());// CR here breaks the parsing

	try {
	    StreamUtils.readUntil(_fromDevice, _tmpBuf, getPromptString(),
				  15000);
	} catch (Exception e) {
	    _log4j.error("requestSample() caught exception", e);
	}

	// prompt is not consumed during flush unless a delay is inserted
	_fromDevice.flush();
	_toDevice.write("get data\n".getBytes());
    }

    /** Initialize the sensors. */
    protected synchronized void initializeInstrument()
	throws InitializeException, Exception {

	// Disable sampling diagnostics, since the MSP430 is
	// sampled during the diagnostic procedure!
	if (_attributes.diagnosticSampleInterval != 0) {
	    _log4j.warn("Disable sampling diagnostics for this service");
	    _attributes.diagnosticSampleInterval = 0;
	}
		
	if (_attributes.setTurns) {
	    resetTurnsCounter(_attributes.turnsSetpoint);
	}

	setSampleTimeout(_SAMPLE_TIME_OUT);

	// Turn on DPA power/comms
	managePowerWake();

	_fromDevice.flush();
	_toDevice.write("set echo off\n".getBytes());
	_fromDevice.flush();
	if(_attributes.enableGFScan==false){
		_toDevice.write("gfscan off\n".getBytes());
	}else{
		_toDevice.write("gfscan on\n".getBytes());	
	}
	_fromDevice.flush();
    }

    /** Return parameters to use on serial port. */
    public SerialPortParameters getSerialPortParameters()
	throws UnsupportedCommOperationException {

	return new SerialPortParameters(9600, SerialPort.DATABITS_8,
					SerialPort.PARITY_NONE, 
					SerialPort.STOPBITS_1);
    }

    /** Environmental sensor does not have an internal clock. */
    public void setClock(long t) {
	return;
    }

    /** Self-test not implemented. */
    public int test() {
	return Device.OK;
    }

    /** Return specifier for default sampling schedule. */
    protected ScheduleSpecifier createDefaultSampleSchedule()
	throws ScheduleParseException {

	// Sample every 30 seconds by default
	return new ScheduleSpecifier(30000);
    }


    /* Following code inserted to allow SleepManager to send commands */
    /* 06/02/2003, rah (Bob Herlien) */

    /**
     * Get the MSP430 object. Note that, unlike "real" singleton getInstance
     * methods, this doesn't actually instantiate the object. PortManager is
     * responsible for instantiating it and assigning it to the appropriate
     * serial port. However, it SHOULD only be instantiated once, so we can save
     * and return THE instance of this class
     */
    public static MSP430 getInstance() {
	return (_instance);
    }

    /** Send a command to the MSP430 and receive the reply */
    public synchronized String commandAndReply(String cmd, long timeout)
	throws TimeoutException, NullPointerException, IOException,
	       Exception {
	byte[] buf = getSampleBuf();
	String reply;

	DebugMessage.print("MSP430.commandAndReply: " + cmd);
	_toDevice.write("\n".getBytes());
	StopWatch.delay(100);
	// prompt is not consumed during flush unless a delay is inserted
	_fromDevice.flush();
	_toDevice.write(cmd.getBytes());
	if (cmd.indexOf('\n') < 0)
	    _toDevice.write("\n".getBytes());

	StreamUtils.readUntil(_fromDevice, buf, getPromptString(), timeout);
	reply = new String(buf).trim();

	return (reply);
    }

    /** commandAndReply using default sample timeout */
    public String commandAndReply(String cmd) throws TimeoutException,
						     NullPointerException, IOException, Exception {
	return (commandAndReply(cmd, getSampleTimeout()));
    }

    /** Return packet parser. */
    public PacketParser getParser() {
	return new MSP430PacketParser();
    }

    /** Reset compass turns counter to specified value. */
    public void resetTurnsCounter(int nTurns) {

	try {
	    _attributes.setTurns = true;
	    _attributes.turnsSetpoint = nTurns;
			
	    _fromDevice.flush();
	    _toDevice.write("\n".getBytes());// CR here breaks the parsing

	    StreamUtils.readUntil(_fromDevice, _tmpBuf, getPromptString(),
				  15000);

	    _fromDevice.flush();
	    _toDevice.write(("set turns " + nTurns + "\n").getBytes());

	    getMetadata("setTurns".getBytes(), SERVICE_ATTRIBUTES, 
			new String[] {"setTurns", "turnsSetpoint"}, 
			true);
						
	} catch (Exception e) {
	    _log4j.error("resetTurnsCounter() caught Exception", e);
	}
    }

    /** Get the latest data sample. */
    public Environmental.Data getDataValues(boolean logData)
	throws NoDataException {

	_log4j.debug("getDataValue()");

	SensorDataPacket packet = acquireSample(logData);
	_log4j.debug("env: " + new String(packet.dataBuffer()));
	PacketParser parser = new MSP430PacketParser();

	PacketParser.Field[] fields = null;
	long timestamp = 0;

	Data data = new Data();

	try {
	    data.timestamp = System.currentTimeMillis();
	    fields = parser.parseFields(packet);
	} catch (NotSupportedException e) {
	    throw new NoDataException(e.getMessage());
	} catch (ParseException e) {
	    throw new NoDataException(e.getMessage());
	}

	_log4j.debug("getDataValue() - go through fields");
	for (int i = 0; i < fields.length; i++) {

	    if (fields[i] == null)
		continue;

	    _log4j.debug("getDataValue() - process field " + i);

	    String fieldName = fields[i].getName();
	    Number value = (Number) fields[i].getValue();

	    if (fieldName.equals(MSP430PacketParser.TEMPERATURE_MNEM)) {
		data.temperature = value.floatValue();
	    } else if (fieldName.equals(MSP430PacketParser.HUMIDITY_MNEM)) {
		data.humidity = value.floatValue();
	    } else if (fieldName.equals(MSP430PacketParser.PRESSURE_MNEM)) {
		data.pressure = value.floatValue();
	    } else if (fieldName.equals(MSP430PacketParser.GRND_FAULT_LO_MNEM)) {
		data.groundFaultLow = value.floatValue();
	    } else if (fieldName.equals(MSP430PacketParser.GRND_FAULT_HI_MNEM)) {
		data.groundFaultHigh = value.floatValue();
	    } else if (fieldName.equals(MSP430PacketParser.HEADING_MNEM)) {
		data.heading = value.floatValue();
	    } else if (fieldName.equals(MSP430PacketParser.TURNS_MNEM)) {
		data.turnsCount = value.intValue();
	    }
	}
	_log4j.debug("getDataValue() - done");
	return data;
    }

    /**
     * Get device's notion of its state.
     */
    protected byte[] getInstrumentStateMetadata() {

	String state = "";
	try {
	    _log4j.debug("getAttention()");
	    getAttention();
	    _log4j.debug("get version");
	    _fromDevice.flush();
	    _toDevice.write("get version\n".getBytes());

	    int nBytes = StreamUtils.readUntil(_fromDevice, _tmpBuf,
					       getPromptString(), 5000);

	    state += new String(_tmpBuf, 0, nBytes) + "\n";

	    _log4j.debug("get levels");
	    _fromDevice.flush();
	    _toDevice.write("get levels\n".getBytes());

	    nBytes = StreamUtils.readUntil(_fromDevice, _tmpBuf,
					   getPromptString(), 5000);

	    state += new String(_tmpBuf, 0, nBytes) + "\n";

	    _log4j.debug("get amask");
	    _fromDevice.flush();
	    _toDevice.write("get amask\n".getBytes());

	    nBytes = StreamUtils.readUntil(_fromDevice, _tmpBuf,
					   getPromptString(), 5000);

	    state += new String(_tmpBuf, 0, nBytes) + "\n";

	    _log4j.debug("get alarm");
	    _fromDevice.flush();
	    _toDevice.write("get alarm\n".getBytes());

	    nBytes = StreamUtils.readUntil(_fromDevice, _tmpBuf,
					   getPromptString(), 5000);

	    state += new String(_tmpBuf, 0, nBytes) + "\n";

	    return state.getBytes();
	} catch (Exception e) {
	    String err = "Got exception reading MSP430 state";
	    _log4j.error(err, e);
	    return (state + "\n" + err).getBytes();
	}
    }
	
    /** 
     * Configurable MSP430 attributes.
     * @author oreilly
     *
     */	
    class Attributes extends InstrumentServiceAttributes {
		
	Attributes(DeviceServiceIF service) {
	    super(service);
	}
		
	/** Flag indicates if turns counter has been set by service */
	boolean setTurns = false;

	/** Flag indicates if gfscan is enabled */
	boolean enableGFScan = true;

	/** Turn counter setpoint (only valid if setTurns is true). */
	int turnsSetpoint = 0;
		
	void setAttributeCallback(String name) {
			
	    if (name.equals("turnsSetPoint")) {
		// Initial turns value has been specified 
		setTurns = true;
	    }
	}
    }
}
