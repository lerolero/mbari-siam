/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.controlLoop;

import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import org.mbari.siam.distributed.devices.ControlInputIF;
import org.mbari.siam.utils.RangeValidator;

/** Encapsulate ControlInputIF state (immutable) */

public class InputState{
	
	protected Logger _log4j = Logger.getLogger(InputState.class);
	public final static String RECORD_HEADER="$ISTATE";
	public final static String DELIMITERS=",";
	protected String _name;
	protected int _id;
	protected String _state;
	protected String _status;
	protected long _utmo;
	protected long _tslu;
	protected boolean _toex;
	protected long _sampleCount=-1;
	protected double _validRatio=-1.0;
	private StringBuffer _sbuf;
	protected String _recordDelimiter=DELIMITERS;
	
	public InputState(String name,
					  int id,
					  String state,
					  String status,
					  long utmo,
					  long tslu,
					  boolean toex,
					  long sampleCount,
					  double validRatio){
		_name=name;
		_id=id;
		_state=state;
		_status=status;
		_utmo=utmo;
		_tslu=tslu;
		_toex=toex;
		_sampleCount=sampleCount;
		_validRatio=validRatio;
		_sbuf=new StringBuffer();
	}
	
	public InputState(ControlInputIF input)
	throws RemoteException{
		super();
		if(input==null){
			return;
		}
		_name=input.getFilterInput().name();
		_id=input.getInputID();
		_state=input.stateString();
		_status=input.statusString();
		_utmo=input.getUpdateTimeout();
		_tslu=input.timeSinceLastUpdate();
		_toex=input.updateTimeoutExpired();
		RangeValidator validator=input.getValidator();
		_sampleCount=-1;
		_validRatio=-1.0;
		if(validator!=null){
			_sampleCount=validator.getSampleCount();
			_validRatio=validator.getValidRatio();
		}
		_sbuf=new StringBuffer();

	}
	
	public String name(){return _name;}
	public int id(){return _id;}
	public String state(){return _state;}
	public String status(){return _status;}
	public long utmo(){return _utmo;}
	public long tslu(){return _tslu;}
	public boolean toex(){return _toex;}
	public long sampleCount(){return _sampleCount;}
	public double validRatio(){return _validRatio;}
	public String toString(){
		_sbuf.setLength(0);
		_sbuf.append(RECORD_HEADER+_recordDelimiter);
		_sbuf.append(_name+_recordDelimiter);
		_sbuf.append(_state+_recordDelimiter);
		_sbuf.append(_status+_recordDelimiter);
		_sbuf.append(_utmo+_recordDelimiter);
		_sbuf.append(_tslu+_recordDelimiter);
		_sbuf.append(_toex+_recordDelimiter);
		_sbuf.append(_sampleCount+_recordDelimiter);
		_sbuf.append(_validRatio);
		return _sbuf.toString();
	}
}
