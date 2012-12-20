/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.controlLoop;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.Serializable;
import java.io.IOException;

import org.apache.log4j.Logger;


import org.mbari.siam.distributed.devices.ControlInputIF;
import org.mbari.siam.distributed.devices.ControlOutputIF;

public abstract class ControlOutputImpl implements ControlOutputIF{
	int _id;
	String _name=null;
	int _state;
	int _status;
	StringBuffer _statusBuffer;

	public abstract Object getDevice() throws RemoteException;	
	public abstract void setOutputValue(double value) throws Exception, RemoteException;
	public abstract void setOutputValue(int value) throws Exception, RemoteException;
	public abstract void setOutputValue(boolean value) throws Exception, RemoteException;
 
	public void setOutputID(int id){
		_id=id;
	}
	public int getOutputID(){
		return _id;
	}
    public void setName(String name) throws RemoteException{
		_name=name;
	}
	public String name() throws RemoteException{
		return _name;
	}
	protected void setState(int state){
		_state=state;
	}		
	protected void setStatus(int statusMask){
		_status = (_status|statusMask);
	}
	protected void clearStatus(int statusMask){
		_status = (_status & (~statusMask));
	}
	public int getState() throws RemoteException{
		return _state;
	}	
	public int getStatus() throws RemoteException{
		return _status;
	}
	
	public String stateString(){
		return stateString(_state);
	}
	
	public String stateString(int state){
		switch (state) {
			case STATE_READY:
				return "READY";
			case STATE_UNINITIALIZED:
				return "UNINITIALIZED";
			case STATE_UPDATING:
				return "UPDATING";
			default:
				break;
		}
		return null;
	}
	
	public String statusString(){
		return statusString(_status);
	}
	public String statusString(int status){
		
		if(status==STATUS_OK){
			return "OK";
		}
		_statusBuffer.setLength(0);
		if((status & STATUS_TIMEOUT_EXPIRED)>0){
			_statusBuffer.append("TIMEOUT_EXPIRED");
		}
		if((status & STATUS_NOT_CONNECTED)>0){
			if(_statusBuffer.length()>0){
				_statusBuffer.append("+");
			}
			_statusBuffer.append("NOT_CONNECTED");
		}
		if((status & STATUS_CONNECT_ERROR)>0){
			if(_statusBuffer.length()>0){
				_statusBuffer.append("+");
			}
			_statusBuffer.append("STATUS_CONNECT_ERROR");
		}
		if((status & STATUS_UPDATE_ERROR)>0){
			if(_statusBuffer.length()>0){
				_statusBuffer.append("+");
			}
			_statusBuffer.append("STATUS_UPDATE_ERROR");
		}
		if((status & STATUS_DISCONNECT_ERROR)>0){
			if(_statusBuffer.length()>0){
				_statusBuffer.append("+");
			}
			_statusBuffer.append("STATUS_DISCONNECT_ERROR");
		}
		if((status & STATUS_ERROR)>0){
			if(_statusBuffer.length()>0){
				_statusBuffer.append("+");
			}
			_statusBuffer.append("STATUS_ERROR");
		}
		if((status & STATUS_BAD_COMMAND)>0){
			if(_statusBuffer.length()>0){
				_statusBuffer.append("+");
			}
			_statusBuffer.append("STATUS_BAD_COMMAND");
		}
		return _statusBuffer.toString();
	}
	public OutputState getOutputState()
	throws RemoteException{
		return new OutputState(this);
	}
	
}
