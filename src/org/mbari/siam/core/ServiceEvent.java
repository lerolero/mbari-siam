/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

/** Service Event */

public class ServiceEvent extends NodeEvent {

    // Constants
    /** Range of ServiceEvent IDs */
    public static final int SERVICE_FIRST = 1;
    public static final int SERVICE_LAST = 1;

    /** ServiceEvent IDs */
    public static final int STATE_CHANGE=1;

    /** State Change Constants */
    public static final int INSTALLED = 1;
    public static final int REMOVED = 2;
    public static final int REQUEST_COMPLETE = 3;
    // public static final int SAMPLE_LOGGED = 4;  // Deprecated 7 May 2008 rah

    // Fields
    /** Indicates which state change has occurred */
    public int _stateChange = 0;

    /** ID of service that initiated event */
    public int _serviceID = -1;

    // Constructors
    /** Constructs a  ServiceEvent with the specified source and type */
    public ServiceEvent(Object source){
	super(source);
    }

    /** Constructs a  ServiceEvent with the specified source and type */
    public ServiceEvent(Object source, int id, int stateChange, int serviceID){
	super(source);
	_id=id;
	_stateChange=stateChange;
	_serviceID = serviceID;
    }

    // Methods
    /** Return the type of state change */
    public int getStateChange(){
	return _stateChange;
    }

    /** getServiceID() */
    public int getServiceID(){
	return _serviceID;
    }

    
} // end class ServiceEvent


