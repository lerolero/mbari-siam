/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.mdTest;

import java.rmi.RemoteException;
import java.io.IOException;
import java.lang.Math;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.core.DebugMessage;
import org.mbari.siam.distributed.PowerPort;
import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.core.InstrumentService;
import org.mbari.siam.core.SerialPortParameters;

/**
   Fake instrument for testing purposes.
*/
public class mdTest 
    extends InstrumentService implements Instrument {

    // log4j Logger
    static private Logger _log4j = Logger.getLogger(mdTest.class);

    private static int sampleNo=1;
    public static final long RECORDTYPE_ALPHA=1;
    public static final long RECORDTYPE_BETA=2;

    public mdTest() 
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


    /** Request a data sample from the compass. */
    protected void requestSample() throws IOException {
	_log4j.debug("mdTest.requestSample()");
    }

    /** Get attention of the instrument. */
    protected void getAttention(int maxTries) 
	throws Exception {
	return;
    }

    /** Return metadata. */
    protected byte[] getInstrumentMetadata() {

	return ("mdTest system time is "+System.currentTimeMillis()).getBytes();
    }


    /** Get a dummy SensorDataPacket. */
    public synchronized SensorDataPacket acquireSample(boolean logSample) 
	throws NoDataException {

	_log4j.debug("mdTest.acquireSample: entry, logSample="+logSample);
	_log4j.debug("mdTest.acquireSample: getting packet");
	SensorDataPacket packet = new SensorDataPacket(getId(), 100);
	_log4j.debug("mdTest.acquireSample: setting time");
	packet.setSystemTime(System.currentTimeMillis());

	
	_log4j.debug("mdTest.acquireSample: filling out data packet");
	if(((sampleNo++)%3)!=0){
	    packet.setDataBuffer(("AA"+sampleNo+",AB"+Math.random()+",AC"+Math.random()).getBytes());
	    setRecordType(RECORDTYPE_ALPHA);
	}else{
	    packet.setDataBuffer(("BA"+sampleNo+",BB"+Math.random()).getBytes());
	    setRecordType(RECORDTYPE_BETA);
	}

	_log4j.debug("mdTest.acquireSample: logging sample "+packet+"\n_recordType="+_recordType);
	if (logSample) {
	    logPacket(packet);
	}
	_log4j.debug("mdTest.acquireSample: exit");

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

    /*
    public static void main(String args[]){
	try{
	mdTest foo = new mdTest();
	}catch(RemoteException e){}
    }
    */
}
