/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

/** Comm Event */

public class CommEvent extends NodeEvent {

    // Constants
    /** Range of CommEvent IDs */
    public static final int COMM_FIRST = 1;
    public static final int COMM_LAST = 1;

    /** CommEvent IDs */
    //public static final int SOME_COMM_EVENT=1;

    // Fields

    // Constructors
    /** Constructs a  CommEvent with the specified source and type */
    public CommEvent(Object source){
	super(source);
    }

    /** Constructs a  CommEvent with the specified source and type */
    public CommEvent(Object source, int id ){
	super(source);
	_id=id;
    }

    // Methods

    
} // end class CommEvent


