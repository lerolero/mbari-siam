/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

/** Scheduler Event */

public abstract class SchedulerEvent extends NodeEvent {

    // Constants
    /** Range of SchedulerEvent IDs */
    public static final int SCHEDULER_FIRST = 1;
    public static final int SCHEDULER_LAST = 1;

    /** SchedulerEvent IDs */
    //public static final int SOME_SCHEDULER_EVENT=1;

    // Field

    // Constructors
    /** Constructs a  SchedulerEvent with the specified source and type */
    public SchedulerEvent(Object source){
	super(source);
    }

    /** Constructs a  SchedulerEvent with the specified source and type */
    public SchedulerEvent(Object source, int id ){
	super(source,id);
    }

    // Methods

} // end class SchedulerEvent




