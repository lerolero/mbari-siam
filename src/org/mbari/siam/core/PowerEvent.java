/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

/** Power Event */

public class PowerEvent extends NodeEvent {

    // Constants
    /** Range of PowerEvent IDs */
    public static final int POWER_FIRST = 1;
    public static final int POWER_LAST = 1;

    /** PowerEvent IDs */
    public static final int POWER_FAILURE_DETECTED=1;

    /** Power Event Constants */
    public static final int POWER_DO_SAFEMODE=1;
    public static final int POWER_DO_HALT=2;

    // Fields
    public int _type=0;
    public String _reading=null;

    // Constructors
    /** Constructs a  PowerEvent with the specified source and type */
    public PowerEvent(Object source){
	super(source);
    }

    /** Constructs a  PowerEvent with the specified source and type */
    public PowerEvent(Object source, int id , String reading){
	super(source);
	_id=id;
	_reading=reading;
    }

    /** Constructs a  PowerEvent with the specified source and type */
    public PowerEvent(Object source, int id, int type ){
	super(source,id);
	_type=type;
    }

    // Methods
    public void setType(int type){
	_type=type;
    }
    public int getType(){
	return _type;
    }
    public String getReading(){
	return _reading;
    }
    
} // end class PowerEvent


