/****************************************************************************/
/* Copyright 2002 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.core;

import java.io.IOException;
import java.rmi.RemoteException;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.InvalidDataException;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.ServiceAttributes;
import org.mbari.siam.core.Scheduler.ScheduleKey;

/**
PolledInstrumentService represents an instrument that is synchronously polled
for its data.
 */
public abstract class PolledInstrumentService 
    extends BaseInstrumentService 
    implements Instrument, DeviceServiceIF, ScheduleOwner {

    /** Log4j logger */
    private static Logger _log4j = 
	Logger.getLogger(PolledInstrumentService.class);

    /** Constructor. */
    public PolledInstrumentService() throws RemoteException {
	super();
    }


    /** Execute the scheduled task, as part of ScheduleOwner implementation. */
    public final void doScheduledTask(ScheduleTask task) {
	try {
	    acquireSample(true);
	} catch (NoDataException e) {
	    _log4j.error("doScheduledTask: caught NoDataException: " + e, e);
	}
    }


    /**
     * Poll instrument for sample, acquire and process it, and put data 
     * into output queue. Call sequence is requestSample(), readSample(), and
     * processSample(). If these are successful, output is put into outbound
     * message queue.
     * 
     */
    protected synchronized SensorDataPacket acquire(boolean logSample)
	throws NoDataException {

	// Check for SUSPENDED or SAFE state
	int state = getStatus();
	if (state == Device.SUSPEND) {
	    throw new NoDataException("service is suspended");
	}
	else if (state == Device.SAFE) {
	    throw new NoDataException("service in safe mode");
	}

	assertSamplingState();

	String name = new String(getName());

	//prepare device for sampling
	try {
	    prepareToSample();
	} catch (Exception e) {
	    setStatusError();
	    managePowerSleep();
	    _log4j.error(name + e.getMessage());
	    _errorCache.add("prepareToSample() exception: " + e.getMessage(),
			    System.currentTimeMillis());

	    throw new NoDataException(e.getMessage());
	}

	int tries;
	StringBuffer errMsg = new StringBuffer("");
	boolean interrupted = false;

	for (tries = 0; 
	     tries < _instrumentAttributes.maxSampleTries;
	     tries++) {

	    if (Thread.interrupted() || getStatus() == Device.SHUTDOWN ||
		getStatus() == Device.SAFE) {
		_log4j.warn("acquire(): thread interrupted?");
		interrupted = true;
		break;
	    }

	    try {
		_log4j.debug(name + ": calling requestSample()\n");
		// Send sample request to instrument
		requestSample();

		// Read raw sample from instrument
		int nBytes;

		_log4j.debug(name + ": calling readSample()\n");
		if ((nBytes = readSample(getSampleBuf())) <= 0) {
		    _log4j.warn(name + ": readSample() returned 0 bytes");
		    errMsg.append("readSample returned 0 bytes;");
		    incRetryCount();
		    continue;
		}

		_log4j.debug("read " + nBytes + " sample bytes");

		validateSample(getSampleBuf(), nBytes);

		// Process the sample, generate TimeStampedData object
		// (By default, base class' processSample() just
		// puts raw data in output object)
		_log4j.debug(name + ": calling processSample() \n");
		SensorDataPacket dataPacket = 
		    processSample(getSampleBuf(), nBytes);
                
                

		// Allow sub-class to deal w/instrument after sampling
		// complete
		postSample();

		// turn off power
		managePowerSleep();

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
	    catch (InterruptedException e) {
		// We're done; break 
		_log4j.warn("acquire(): got InterruptedException");
		interrupted = true;
		break;
	    }

	    catch (Exception e) {
		incRetryCount();
		_log4j.warn(name + ".acquire(): " + 
			    e.getClass().getName() + ": " + e.getMessage(), e);

		errMsg.append(":" + e.getMessage());
	    }

	    // other Exceptions fly past into the caller
	    Thread.yield();
	}

	if (interrupted) {
	    // If interrupted, just throw an exception here; let the 
	    // interrupting thread (shutdown or safe, most likely) do
	    // the instrument and power cleanup.
	    throw new NoDataException("acquire interrupted");
	}

	// Failed to acquire and process sample.
	setStatusError();

	// Allow sub-class to deal w/ instrument after sampling complete
	postSample();

	// turn off power
	managePowerSleep();

	// Throw NoDataException
	_log4j.error(name + ": Retry limit exceeded (retries=" + 
		      tries + ")");

	errMsg.append(":retry limit exceeded (" + tries + ")");
	errMsg.append(":port " + _instrumentPort.getTerseStatus());
	errMsg.insert(0, "ERR: ");
	_errorCache.add(errMsg.toString(), System.currentTimeMillis());

	throw new NoDataException(new String(errMsg));
    }

    /** Set the ServiceAttributes object for this service. */
    public final void setAttributes(ServiceAttributes attributes) {
	super.setAttributes(attributes);
    }

    /** Request a data sample from instrument. */
    protected abstract void requestSample() throws TimeoutException, Exception;

    /** Return specifier for default sampling schedule. Subclasses MUST
     provide the default sample schedule. */
    protected abstract ScheduleSpecifier createDefaultSampleSchedule()
	throws ScheduleParseException;


}
