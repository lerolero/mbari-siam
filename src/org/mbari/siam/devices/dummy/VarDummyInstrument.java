/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.dummy;

import java.util.Random;
import java.io.IOException;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.Summarizer;
import org.mbari.siam.distributed.SummaryPacket;
import org.mbari.siam.distributed.Safeable;

/**
 * Extension of DummyInstrument to allow variable length of data
 */
public class VarDummyInstrument 
    extends DummyInstrument
    implements Instrument, Safeable {

    /** Log4j logger */
    protected static Logger _log4j = 
	Logger.getLogger(VarDummyInstrument.class);

    private static final int SUMMARY_INTERVAL = 3;
    private int _nSamples = 0;
    Random _random = new Random();
    VarDummyAttributes _attributes = new VarDummyAttributes(this);

    public VarDummyInstrument() throws RemoteException {
	super();
    }

    /** Request a data sample */
    protected void requestSample() throws Exception {
	_log4j.debug("requestSample() - snooze()");
	if (_attributes.requestSampleSleepSec > 0)
	    snooze(_attributes.requestSampleSleepSec);
	_log4j.debug("requestSample() - done with snooze()");
    }

    /** Get a dummy SensorDataPacket. */
    protected synchronized SensorDataPacket acquire(boolean logSample)
	throws NoDataException {

	_log4j.debug("acquire(" + logSample + ")");
	setStatusSampling();

	try {
	    if (_attributes.acquireSleepSec > 0)
		snooze(_attributes.acquireSleepSec);
	}
	catch (InterruptedException e) {
	    _log4j.warn("Caught InterruptedException");
	    throw new NoDataException("snooze() interrupted");
	}

	long time = System.currentTimeMillis();
	SensorDataPacket dataPacket = new SensorDataPacket(getId(), _attributes.dataLength);
	_random.nextBytes(dataPacket.dataBuffer());
	dataPacket.setSystemTime(time);

	if ((_attributes.summaryInterval > 0) && ((_nSamples % _attributes.summaryInterval) == 0))
	{
	    SummaryPacket summary = new SummaryPacket(getId());
	    byte[] summaryData = new byte[_attributes.summaryLength];
	    _random.nextBytes(summaryData);
	    summary.setData(time, summaryData);
	    logPacket(summary);
	}

	if (logSample) {
	    logPacket(dataPacket);
	}

	_nSamples++;

	setStatusOk();

	return dataPacket;
    }

    /**
     * Attributes for Variable-length DummyInstrument service
     * @author bobh
     *
     */
    class VarDummyAttributes extends InstrumentServiceAttributes
    {
	VarDummyAttributes(DeviceServiceIF service)
	{
	    super(service);
	}

	/** Length of data to generate	*/
	int  dataLength = 100;

	/** Throw initialize exception from initializeInstrument() */ 
	boolean throwInitializeException = false;

	/** Number of seconds to sleep in requestSample() */
	int requestSampleSleepSec = 0;
		
	/** Number of seconds to sleep in acquire() */
	int acquireSleepSec = 0;

	/** Interval for generating summary packets.
	    < 0 turns off summary packets.  Turn off by default */
	int summaryInterval = -1;
		
	/** Length of summary packet data to generate	*/
	int summaryLength = 300;
    }

} // end of class
