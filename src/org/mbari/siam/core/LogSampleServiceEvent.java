/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

import org.mbari.siam.distributed.DevicePacket;


/** LogSample Service Event */

public class LogSampleServiceEvent extends NodeEvent {

    public static final int SAMPLE_LOGGED = 1;

    /** ID of service that initiated event */
    public int _serviceID = -1;

    /** DevicePacket that is being published */
    protected DevicePacket _logSample = null;

    /** Constructs a LogSampleServiceEvent with the specified source, type, serviceID, and DevicePacket */
    public LogSampleServiceEvent(Object source, int id, int serviceID, DevicePacket logSample)
    {
	super(source);
	_id=id;
	_serviceID = serviceID;
	_logSample = logSample;
    }

    /** getServiceID() */
    public int getServiceID(){
	return _serviceID;
    }
    
    /** Return the sample that was logged */
    public DevicePacket getLogSample() {
	return _logSample;
    }

    /** Save the sample that was logged */
    public void setLogSample(DevicePacket logSample) {
	_logSample = logSample;
    }

} // end class LogSampleServiceEvent


