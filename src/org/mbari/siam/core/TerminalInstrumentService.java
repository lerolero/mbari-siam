/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.rmi.RemoteException;

import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

/**
   The "instrument" is just a terminal emulator (e.g. HyperTerm, minicom)
*/
public class TerminalInstrumentService 
    extends InstrumentService implements Instrument {

    /** log4j logger */
    static Logger _log4j = Logger.getLogger(TerminalInstrumentService.class);

    public TerminalInstrumentService() 
	throws RemoteException {
	_log4j.debug("TerminalInstrumentService ctr");

	try {
	    setSampleTimeout(10000);
	}
	catch (RangeException e) {
	}
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
	return "\r".getBytes();
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
	_log4j.debug("TerminalInstrumentService.requestSample()");
	_toDevice.write("SEND_SAMPLE".getBytes());
    }

    /** Get attention of the instrument. */
    protected void getAttention(int maxTries) 
	throws Exception {
	return;
    }

    /** Return metadata. */
    protected byte[] getInstrumentMetadata() {

	return "TerminalInstrumentService METADATA GOES HERE".getBytes();
    }


    /** No internal clock. */
    public void setClock(long t) {
	return;
    }

    /** Self-test not implemented. */
    public int test() {
	return Device.OK;
    }

    /** Initialize */
    protected void initializeInstrument() 
	throws InitializeException, Exception {
    	
	setSampleTimeout(10000);
    	
	// Get instrument attention; puts compass into polled mode.
	getAttention(5);
    }

    /** Return specifier for default sampling schedule. */
    protected ScheduleSpecifier createDefaultSampleSchedule() 
	throws ScheduleParseException {

	// Sample once per minute
	return new ScheduleSpecifier(60);
    }

    /** Cache service properties on the node, such that current property
     values will be restored next time service is created on this node. */
    public void cacheProperties(byte[] note) throws Exception {
	throw new Exception("Not implemented");
	    
    }

    /** Clear properties cache. */
    public void clearPropertiesCache(byte[] note) 
	throws RemoteException, Exception {
	throw new Exception("Not implemented");
    }


    /** Return serial port parameters to use on port. */
    public SerialPortParameters getSerialPortParameters() 
	throws UnsupportedCommOperationException {

	return new SerialPortParameters(38400,
					SerialPort.DATABITS_8,
					SerialPort.PARITY_NONE,
					SerialPort.STOPBITS_1);
    }
}
