/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

import java.util.EventObject;

/** Node Event super class from which all Node events are extended */

public abstract class NodeEvent extends EventObject {
    protected int _id;
    protected boolean _consumed;

    /** Event mask for selecting SCHEDULER events */
    public static final long SCHEDULER_EVENT_MASK=0x1L;

    /** Event mask for selecting SCHEDULER events */
    public static final long SERVICE_EVENT_MASK=0x2L;

    /** Constructs a  NodeEvent with the specified source and type */
    public NodeEvent(){
	super(null);
    }

    /** Constructs a  NodeEvent with the specified source and type */
    public NodeEvent(Object source){
	super(source);
    }

    /** Constructs a  NodeEvent with the specified source and type */
    public NodeEvent(Object source, int id){
	super(source);
	_id=id;
    }

    /** Returns the event type */
    public int getID(){
	return _id;
    }

    /** Returns a string representing the state of this event */
    public String paramString(){
	return "ParamString";
    }

    /** Returns a string representation this event object */
    public String toString(){
	return "NodeEvent";
    }

    /** Consume event */
    protected void consume(){}

    /** Returns current value of consumed member */
    protected boolean isConsumed(){
	return _consumed;
    }

    /** Called by the garbage collector on an object when garbage collection determines that there are no more references to the object. */
    protected void finalize(){}
} // end class NodeEvent





