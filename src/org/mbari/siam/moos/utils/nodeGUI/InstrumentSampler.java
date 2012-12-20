/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.moos.utils.nodeGUI;

import java.util.Vector;
import java.io.IOException;
import org.apache.log4j.Logger;

import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.NoDataException;


/** This class is responsible for getting data from one or more
    specified instrument services.
*/
public class InstrumentSampler implements Runnable {
    static protected Logger _logger = 
	Logger.getLogger(InstrumentSampler.class);
	
    Vector _instruments = new Vector();

    Instrument _instrument = null;
    int _sampleIntervalMsec = 0;
    Application _application;
    boolean _run = true;

    /** Constructor. Put specified instrument into list of instruments to be
     sampled. */
    public InstrumentSampler(Instrument instrument, 
			     int sampleIntervalMsec, 
			     Application application) {

	_logger.info("InstrumentSampler constructor");
	addInstrument(instrument);
	setInterval(sampleIntervalMsec);
	_application = application;
	_logger.info("InstrumentSampler constructor complete.");
    }
	
    /** Add an instrument to be sampled. */
    void addInstrument(Instrument instrument) {
	_instruments.add(instrument);
    }
	
    /** Set sampling interval. */
    synchronized public void setInterval(int msec) {
	_sampleIntervalMsec = msec;
    }
	

    /** Terminate sampling. */
    public void terminate() {
	_run = false;
    }

    /** Acquire samples from specified instruments at specified interval. */
    public void run() {
		
	_logger.info("InstrumentSampler.run()");
    
	while (_run) {
			
	    // Sleep until next sampling
	    try {
		Thread.sleep(_sampleIntervalMsec);
	    }
	    catch (InterruptedException e) {
	    }

	    try {
		_logger.debug("acquire sample");
		_application.sampleStartCallback();

		// Get sample from each specified instrument
		for (int i = 0; i < _instruments.size(); i++) {

		    Instrument instrument = 
			(Instrument )_instruments.elementAt(i);

		    if (instrument == null) {
			_logger.info("Instrument service " + i + " not found");
			continue;
		    }

		    try {
			DevicePacket packet = instrument.acquireSample(false);
			_application.processSample(packet);
		    }
		    catch (NoDataException e) {
			_logger.info("No data from instrument #" + i);
		    }
		}

		_application.sampleEndCallback();
	    }
	    catch (IOException e) {
		// Connection problem
		_application.sampleErrorCallback(e);
	    }
	}
    }
}
