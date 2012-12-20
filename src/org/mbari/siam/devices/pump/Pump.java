/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.pump;

import java.rmi.RemoteException;
import java.rmi.Naming;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.AttributeChecker;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.PropertyException;
import org.mbari.siam.distributed.PowerPort;
import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.core.InstrumentService;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.core.NodeProperties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

/**
   Fake instrument for testing purposes.
*/
public class Pump
    extends InstrumentService implements Instrument {

    /** log4j logger */
    static Logger _log4j = Logger.getLogger(Pump.class);

    // Configurable MetSys attributes
    PumpAttributes _attributes = new PumpAttributes(this);

    public Pump() 
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
	return 1500;
    }

    /** Return initial value of instrument power policy. */
    protected PowerPolicy initInstrumentPowerPolicy() {
	return PowerPolicy.WHEN_SAMPLING;
    }

    /** Return initial value of communication power policy. */
    protected PowerPolicy initCommunicationPowerPolicy() {
	return PowerPolicy.ALWAYS;
    }

    /** Request a data sample from the compass. */
    protected void requestSample() throws IOException {
	_log4j.debug("Pump.requestSample()");
    }

    /** Get attention of the instrument. */
    protected void getAttention(int maxTries) 
	throws Exception {
	return;
    }

    /** Return metadata. */
    protected byte[] getInstrumentMetadata() {

	return "Pump METADATA GOES HERE".getBytes();
    }

    /** Get a dummy SensorDataPacket. */
    public SensorDataPacket getData() 
	throws NoDataException {

	SensorDataPacket packet = new SensorDataPacket(getId(), 100);
	packet.setSystemTime(System.currentTimeMillis());
	packet.setDataBuffer("This is dummy pump data".getBytes());
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
	// Sample every 5 seconds by default
       	return new ScheduleSpecifier();
    }

    /** Return parameters to use on serial port. */
    public SerialPortParameters getSerialPortParameters() 
	throws UnsupportedCommOperationException {

	return new SerialPortParameters(9600, 
					SerialPort.DATABITS_8,
					SerialPort.PARITY_NONE,
					SerialPort.STOPBITS_1);
    }


    /** Initialize attributes; register this service with RMI. */
    protected void initializeInstrument() 
	throws InitializeException, Exception {

	// Register with RMI so that PumpUser can find me
	// If _registryName is null, I am not available for use by others
	if (_attributes.rmiPumpServiceName == null)
	    return;

	_log4j.debug("Pump.setProperties(): binding name");

	try{
	    Naming.bind(_attributes.rmiPumpServiceName, this);
	}
	catch(Exception se) {
	    _log4j.error("Pump.setProperties(): "+se);
	}
    }
}


