/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.controlLoop;

import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import org.mbari.siam.distributed.devices.ControlOutputIF;
import org.mbari.siam.utils.RangeValidator;

/** Encapsulate ControlOutputIF state (immutable) */

public class OutputState{
	protected Logger _log4j = Logger.getLogger(OutputState.class);
	public final static String RECORD_HEADER="$OSTATE";
	public final static String DELIMITERS=",";

	protected String _name;
	protected int _id;
	protected String _state;
	protected String _status;
	private StringBuffer _sbuf;
	protected String _recordDelimiter=DELIMITERS;
	
	public OutputState(String name,
					  int id,
					  String state,
					  String status){
		_name=name;
		_id=id;
		_state=state;
		_status=status;
		_sbuf=new StringBuffer();
	}
	
	public OutputState(ControlOutputIF output)
	throws RemoteException{
		super();
		if(output==null){
			return;
		}
		_name=output.name();
		_id=output.getOutputID();
		_state=output.stateString();
		_status=output.statusString();
		_sbuf=new StringBuffer();
	}
	
	public String name(){return _name;}
	public int id(){return _id;}
	public String state(){return _state;}
	public String status(){return _status;}
	public String toString(){
		_sbuf.setLength(0);
		_sbuf.append(RECORD_HEADER+_recordDelimiter);
		_sbuf.append(_name+_recordDelimiter);
		_sbuf.append(_state+_recordDelimiter);
		_sbuf.append(_status);
		return _sbuf.toString();
	}
}
