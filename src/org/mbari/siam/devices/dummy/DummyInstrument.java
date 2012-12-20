/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.dummy;

import java.util.Vector;
import java.util.Iterator;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.core.SerialPortParameters;

import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.Summarizer;
import org.mbari.siam.distributed.SummaryPacket;
import org.mbari.siam.distributed.Safeable;

/**
 * Fake instrument for testing purposes.
 */
public class DummyInstrument 
    extends PolledInstrumentService 
    implements Instrument, Safeable {

    /** Log4j logger */
    protected static Logger _log4j = 
	Logger.getLogger(DummyInstrument.class);

    private static final int SUMMARY_INTERVAL = 3;

    private int _nSamples = 0;
	
    boolean _summarize = true;

    //    Object _semaphore = new Object();

    protected Attributes _attributes = new Attributes(this);

    private Vector _samplingThreads = new Vector();

    public DummyInstrument() throws RemoteException {
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

    /** Request a data sample */
    protected void requestSample() throws Exception {
	System.err.println("DummyInstrument - requestSample()!");
	_log4j.debug("requestSample()");
	_log4j.debug("requestSample() - snooze()");
	snooze(20);
	_log4j.debug("requestSample() - done with snooze()");
    }

    /** Get attention of the instrument. */
    protected void getAttention(int maxTries) throws Exception {
	return;
    }

    /** Return metadata. */
    protected byte[] getInstrumentMetadata() {

	return "DummyInstrument METADATA GOES HERE".getBytes();
    }

    protected void initializeInstrument() 
	throws InitializeException, Exception {
	if (_attributes.throwInitializeException) {
	    throw new InitializeException("DUMMY EXCEPTION TEST");
	}
	_log4j.debug("initializeInstrument() - done");
    }

    /** Get a dummy SensorDataPacket. */
    protected synchronized SensorDataPacket acquire(boolean logSample)
	throws NoDataException {

	_log4j.debug("acquire(" + logSample + ")");
	setStatusSampling();

	try {
	    snooze(30);
	}
	catch (InterruptedException e) {
	    _log4j.warn("Caught InterruptedException");
	    throw new NoDataException("snooze() interrupted");
	}

	long time = System.currentTimeMillis();
	SensorDataPacket dataPacket = new SensorDataPacket(getId(), 100);
	dataPacket.setSystemTime(time);
	dataPacket.setDataBuffer("This is dummy instrument data - VERSION 2!"
				 .getBytes());


	if ((_nSamples % SUMMARY_INTERVAL) == 0) {
	    SummaryPacket summary = new SummaryPacket(getId());
	    summary.setSystemTime(System.currentTimeMillis());
	    summary.setData(time, "This is a SUMMARY".getBytes());
	    logPacket(summary);
	}

	if (logSample) {
	    logPacket(dataPacket);
	}

	_nSamples++;

	setStatusOk();

	return dataPacket;
    }

    /** No internal clock. */
    public void setClock() throws NotSupportedException {
	throw new NotSupportedException("Dummy.setClock() not supported");
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

    /** Return parameters to use on serial port. */
    public SerialPortParameters getSerialPortParameters()
	throws UnsupportedCommOperationException {

	return new SerialPortParameters(9600, SerialPort.DATABITS_8,
					SerialPort.PARITY_NONE, 
					SerialPort.STOPBITS_1);
    }

    /**
     * Extend InstrumentServiceAttributes, as a test.
     * @author oreilly
     *
     * TODO To change the template for this generated type comment go to
     * Window - Preferences - Java - Code Style - Code Templates
     */
    protected class Attributes extends InstrumentServiceAttributes {
	protected Attributes(DeviceServiceIF service) {
	    super(service);
	}
		
	byte bvalue = 1;
	short svalue = 2;
	int ivalue = 3;
	long longValue = 4;
	double dvalue = 5.;
	String strValue = "six";

	/** Throw initialize exception from initializeInstrument() */ 
	boolean throwInitializeException = false;

	// Number of seconds to sleep while "sampling"
	int sampleSleepSec = 30;
		
	/** Return software version of extensions. */
	protected String getExtendedVersion() {
	    return "My extended version";
	}
    }

/**
 * Implementation of Seabird 16plus Safe Mode operation.
 */
    public synchronized void enterSafeMode() throws Exception {
    _log4j.info("enterSafeMode() - Instructing Dummy instrument to begin auto-sampling NOW.");

    } //end of method


} // end of class
