/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.*;

import java.rmi.RemoteException;

/**
   StreamingInstrumentService represents an instrument that "streams" data 
   asynchronously to its serial port. 
*/
public abstract class StreamingInstrumentService 
    extends BaseInstrumentService 
    implements Instrument, DeviceServiceIF, ScheduleOwner {
	
    /** Log4j logger */
    protected static Logger _log4j = 
	Logger.getLogger(StreamingInstrumentService.class);
	
    protected int _subsampleInterval = 1;
	
    // Keep track of how many times attributes are loaded
    private int _nAttributeLoads = 0;

    // Boolean to remember shutdown
    protected boolean _shutdown = false;

    // Boolean to remember suspend
    protected boolean _suspend = false;

    // Generic streaming instrument attributes
    Attributes _streamingAttributes = new Attributes(this);
	
    /** Create new service. */
    public StreamingInstrumentService() 
	throws RemoteException {
	super();
	_shutdown = false;
	_suspend = false;
    }
	
	
    /**
     * Do instrument- and service-specific initialization. By default, this
     * method does nothing, and may be overridden.
     */
    protected void initializeInstrument()
	throws InitializeException, Exception {
	super.initializeInstrument();
	_shutdown = false;
	_suspend = false;
    }

    /** Run the task; collect specified number of samples from stream */
    protected void acquireFromStream() {
	synchronized(this) {
	    _log4j.debug("acquireFromStream()");
		
	    if (_streamingAttributes.burstMode || !isStreaming()) {
		try {
		    startStreaming();
		}
		catch (Exception e) {
		    setStatusError();
		    _errorCache.add("tried startStreaming(): " + e.getMessage(),
				    System.currentTimeMillis());
				
		    _log4j.error("acquireFromStream(), tried startStreaming(): " 
				 + e.getMessage());
		    return;
		}
	    }
		
	    try {
		// About to access instrument device - keep track of this thread
		startDeviceAccess();
	    }
	    catch (Exception e) {
		_log4j.error(e.getMessage());
	    }

	    // turn on power
	    managePowerWake();
	}
	// Read and block on serial port, waiting for data
	int nSamples = 0;
	while (!doneAcquiring(nSamples)) {
	    nSamples++;
	    // Don't necessarily save every sample. In some cases, might
	    // only save samples from a particular time-window, or might
	    // want to subsample. 
	    if (!saveThisSample(nSamples)) {
		// Just read the sample from the instrument, and proceed 
		// to next one without creating a packet
		try {
		    readSample(getSampleBuf());
		}
		catch (Exception e) {
		    _log4j.error("Call to readSample() failed. Reason: " + 
				 e.getMessage());
					
		    _log4j.debug("Stack Trace for readSample() failure", e);
		}
		continue;
	    }

	    SensorDataPacket packet = null;
	    try {
		packet = acquire(true);
	    }
	    catch (NoDataException e) {
		_log4j.error(e.getMessage());
		_errorCache.add("tried acquire(): " + e.getMessage(),
				System.currentTimeMillis());
	    }
			
	    /*
	     * Attempt to process a packet in the InstrumentServiceBlock
	     */
	    try {
		if (packet != null && getInstrumentServiceBlock() != null) {
		    _log4j.debug("Adding packet to InstrumentServiceBlock");
		    getInstrumentServiceBlock().processDevicePacket(packet);
		}
	    }
	    catch (Exception e) {
		_log4j.error("An error occured when adding a packet to the InstrumentServiceBlock", e);
	    }

	    // Yield to other threads...
	    Thread.yield();
	}
	synchronized(this) {
	    if (_streamingAttributes.burstMode) {
		// Stop streaming
		try {
		    stopStreaming();
		}
		catch (Exception e) {
		    String errMsg = "ERR:stopStreaming() failed: " + e;
		    _log4j.error(errMsg);
		    annotate(errMsg.getBytes());
		}
	    }

	    // turn off power
	    managePowerSleep();

	    // Denote not sampling any more
	    endDeviceAccess();
	}
    }
	
    /** Return true if done acquiring. */
    protected boolean doneAcquiring(int nSamples) {
		
	if (_streamingAttributes.limitSamplesPerSession && 
	    nSamples >= _streamingAttributes.samplesPerSession) {
	    return true;
	}
	else {
	    return(_shutdown || _suspend);
	}
    }
    
    /** Stop the service */
    public byte[] shutdown() {
	_shutdown = true;
	_log4j.debug("shutdown() - cancel timer");
	ScheduleTask task = getDefaultSampleSchedule();
	if (task != null) {
	    task.cancelTimer();
	}
	else {
	    _log4j.warn("shutdown() - default sample schedule is null");
	}
	_log4j.debug("call super.shutdown()");
	return(super.shutdown());
    }

    public void suspend() {
	_log4j.debug("StreamingInstrumentService.suspend()");
	_suspend = true;
	super.suspend();
	setStatusSuspend();
    }

    public void resume() {
	_log4j.debug("StreamingInstrumentService.resume()");
	_suspend = false;
	super.resume();
	getDefaultSampleSchedule().resetTimer();
    }

    /** Acquire a sample. */
    protected synchronized SensorDataPacket acquire(boolean logSample)
	throws NoDataException {

	// Check for SUSPENDED or SAFE state
	int state = getStatus();
	_log4j.debug("acquire() - state=" + state);
	if (state == Device.SUSPEND) {
	    throw new NoDataException("service is suspended");
	}
	else if (state == Device.SAFE) {
	    throw new NoDataException("service is suspended");
	}
		
	try {
	    // Read raw sample from instrument
	    _log4j.debug(_serviceName + ": calling readSample()\n");
	    setStatusSampling();
			
	    int nBytes = readSample(getSampleBuf());
	    if (nBytes <= 0) {
		setStatusError();
		throw new NoDataException("No bytes read");
	    }
			
	    validateSample(getSampleBuf(), nBytes);
			
	    // Process the sample, generate TimeStampedData object
	    // (By default, base class' processSample() just
	    // puts raw data in output object)
	    _log4j.debug(_serviceName + 
			 ": calling processSample() \n");
	    SensorDataPacket dataPacket = 
		processSample(getSampleBuf(), nBytes);
			
	    // Allow sub-class to deal w/instrument after sampling
	    // complete
	    postSample();
			
	    if (logSample) {
		_errorCache.flush();
		logPacket(dataPacket);
	    }
			
	    setStatusOk();
			
	    // Don't really need to send every packet
	    // to the console, except for debugging.
	    // Some services implement printData 
	    // using System.x.print methods, rather
	    // than log4j.
	    //printData(dataPacket.dataBuffer());
			
	    return dataPacket;
			
	} 
	catch (Exception e) {
	    setStatusError();
	    _log4j.error("acquire(): " + e.getMessage() + ":port " + 
			 _instrumentPort.getTerseStatus());
				
	    throw new NoDataException(e.getMessage() + ":port " + 
				      _instrumentPort.getTerseStatus());
	} 
    }
	
	
    /** Execute the scheduled task, as part of ScheduleOwner implementation. */
    public final void doScheduledTask(ScheduleTask task) {
	acquireFromStream();
    }
	
    /** Return true if current sample should be saved/logged, else return
	false. */
    private boolean saveThisSample(int nSample) {
	if ((nSample % _subsampleInterval) == 0) {
	    return true;
	}
	else {
	    return false;
	}
    }
	
	
    /** Set the ServiceAttributes object for this service. */
    public final void setAttributes(ServiceAttributes attributes) {
	super.setAttributes(attributes);
	if (_nAttributeLoads > 0 && attributes instanceof Attributes) {
	    // Only set if an StreamingAttributes object is coming in
	    _streamingAttributes = (Attributes) attributes;
	}
	else {
	    //			_log4j.error("Subclass attributes are not instance of StreamingInstrumentService.Attributes.  Class is " + attributes.getClass().getName());
	}
	_nAttributeLoads++;
    }
	
	
    /** Put instrument into streaming mode. */
    abstract protected void startStreaming() throws Exception;
	
    /** Take instrument out of streaming mode. */
    abstract protected void stopStreaming() throws Exception;
	
    /** Return true if device currently in streaming mode, else 
	return false. */
    abstract protected boolean isStreaming();
	
    /** StreamingInstrumentService attributes. */
    public class Attributes extends InstrumentServiceAttributes {
		
	public Attributes(StreamingInstrumentService service) {
	    super(service);
	}
		
	/** Limits samples per streaming session */
	public boolean limitSamplesPerSession = true;
		
	/** Samples per scheduled streaming session */
	public int samplesPerSession = 10;
		
	/** Instrument should stream only while service is sampling. */
	public boolean burstMode = true;
    }        
	
}

