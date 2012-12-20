/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.pumpUser;

import java.rmi.RemoteException;
import java.rmi.Naming;
import java.io.InputStream;
import java.io.IOException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.AttributeChecker;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.PowerPort;
import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.core.InstrumentService;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.devices.pump.Pump;
import org.mbari.siam.devices.pump.PumpAttributes;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

/**
   Fake Pump User instrument for testing purposes.
*/
public class PumpUser
    extends InstrumentService implements Instrument {

    /** log4j logger */
    static Logger _log4j = Logger.getLogger(PumpUser.class);

    /** Pump Service */
    public Device _pumpService=null;

    // Configurable PumpUser attributes
    PumpAttributes _attributes = new PumpAttributes(this);

    public PumpUser() 
	throws RemoteException {
    }

    /** Specify compass device startup delay (millisec) */
    protected int initInstrumentStartDelay() {
    	return 500;
    }
    
    /** Specify compass prompt string. */
    protected byte[] initPromptString() {
	return ">".getBytes();
    }

    /** Specify sample terminator. */
    protected byte[] initSampleTerminator() {
	return "\r\n".getBytes();
    }

    /** Specify maximum bytes in raw compass sample. */
    protected int initMaxSampleBytes() {
	return 32;
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

    /** Request a data sample  */
    protected void requestSample() throws IOException {
	_log4j.debug("PumpUser.RequestSample");
    }

    /** Get attention of the instrument. */
    protected void getAttention(int maxTries) 
	throws Exception {
	return;
    }

    /** Return metadata. */
    protected byte[] getInstrumentMetadata() {

	return "PumpUser METADATA GOES HERE".getBytes();
    }

    /** Get a dummy SensorDataPacket. */
    public SensorDataPacket getData() 
	throws NoDataException {
	if(getPump()==null){
	    _log4j.info("No Pump available...forging ahead");
	}
	else
	try{
	    _log4j.debug("PumpUser.getData()");
	    _log4j.debug("PumpUser.getData(): turning pump ON");
	    _pumpService.powerOn();
	    _log4j.debug("PumpUser.getData(): waiting 5 sec...");
	    Thread.sleep(5000);
	    _log4j.debug("PumpUser.getData(): turning pump OFF");
	    _pumpService.powerOff();
	}catch(InterruptedException e){
	}
	catch(RemoteException r){
	    _log4j.error("PumpUser.getData: remote exception"+r);
	}
	SensorDataPacket packet = new SensorDataPacket(getId(), 100);
	packet.setSystemTime(System.currentTimeMillis());
	packet.setDataBuffer("This is pump user data".getBytes());
	return packet;
    }

    /** No internal clock. */
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
	// Sample every 20 seconds by default
	return new ScheduleSpecifier(20000);
    }

    /** Return parameters to use on serial port. */
    public SerialPortParameters getSerialPortParameters() 
	throws UnsupportedCommOperationException {

	return new SerialPortParameters(9600, 
					SerialPort.DATABITS_8,
					SerialPort.PARITY_NONE,
					SerialPort.STOPBITS_1);
    }


    /** Get Pump Service */
    protected Device getPump(){

	if(_pumpService != null ){
	    _log4j.debug("Already have pump service");
	    return _pumpService;
	}

	_log4j.debug("getPump(): lookup pump service ("+
			   _attributes.rmiPumpServiceName+")");

	if(_attributes.rmiPumpServiceName==null){
	    _log4j.error("getPump(): No Registry Name; setting _pumpService=null");
	    _pumpService=null;
	}
	else {
	    try{
		_pumpService = 
		    (Device)Naming.lookup("rmi://localhost/" +
					  _attributes.rmiPumpServiceName);
	    }
	    catch(Exception e){
		_log4j.error("getPump() caught exception: "+e);
		_pumpService=null;
	    }
	}
	return _pumpService;
    }

}

