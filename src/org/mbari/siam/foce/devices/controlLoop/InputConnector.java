/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.controlLoop;

import java.io.IOException;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import com.rbnb.sapi.*;

import org.mbari.siam.registry.InstrumentDataListener;
import org.mbari.siam.registry.InstrumentRegistry;
import org.mbari.siam.registry.RegistryEntry;
import org.mbari.siam.utils.Filter;
import org.mbari.siam.utils.FilterInput;
import org.mbari.siam.utils.BoxcarFilter;
import org.mbari.siam.utils.RangeValidator;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.devices.ControlInputIF;

/** Connects a FilterInput to a data stream */

public abstract class InputConnector implements ControlInputIF{

	public static final int DEFAULT_ID=0;
	public static final long DEFAULT_UPDATE_TIMEOUT=-1L;
	
	/** Log4j logger */
	protected static Logger _log4j = Logger.getLogger(InputConnector.class);
	
	FilterInput _input;
	RangeValidator _validator;
	int _dataType;
	Instrument _service;
	int _id;
	int _state;
	int _status;

	StringBuffer _statusBuffer=new StringBuffer();
	
	/** Service Registry instance */
	InstrumentRegistry _registry=null;
	
	long _last_update_msec=0L;
	long _update_timeout_msec=DEFAULT_UPDATE_TIMEOUT;

	public InputConnector(){}
	
	public InputConnector(FilterInput input,int dataType)
	throws InvalidPropertyException{
		this(input,dataType,DEFAULT_ID,DEFAULT_UPDATE_TIMEOUT);
	}
	public InputConnector(FilterInput input,int dataType, int id, long updateTimeoutMsec)
	throws InvalidPropertyException{
		_input=input;
		_id=id;
		switch (dataType) {
			case FIELD_TYPE_DOUBLE:
			case FIELD_TYPE_LONG:
			case FIELD_TYPE_INT:
				_dataType=dataType;
				break;
			default:
				throw new InvalidPropertyException("Invalid data type: ["+dataType+"]");
		}
		setUpdateTimeout(updateTimeoutMsec);
		setLastUpdateTime(System.currentTimeMillis());
		clearStatus(STATUS_MASK_ALL);
		setState(STATE_INSTANTIATED);
	}
	
	public int dataType(){
		return _dataType;
	}
	
	public String typeName(){
		switch (_dataType) {
			case ControlInputIF.FIELD_TYPE_DOUBLE:
				return "DOUBLE";
			case ControlInputIF.FIELD_TYPE_FLOAT:
				return "FLOAT";
			case ControlInputIF.FIELD_TYPE_LONG:
				return "LONG";
			case ControlInputIF.FIELD_TYPE_INT:
				return "INT";
			case ControlInputIF.FIELD_TYPE_SHORT:
				return "SHORT";
			case ControlInputIF.FIELD_TYPE_BYTE:
				return "BYTE";
			case ControlInputIF.FIELD_TYPE_BOOLEAN:
				return "BOOLEAN";
			default:
				break;
		}
		return "Invalid dataType";
	}
	
	public int id(){
		return _id;
	}
	
	protected void setState(int state){
		_state=state;
	}
	protected void setStatus(int statusMask){
		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("_status:"+_status+" mask:"+statusMask);
		//}
		_status = (_status|statusMask);
		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("_status:"+_status);
		//}
	}
	protected void clearStatus(int statusMask){
		_status = (_status & (~statusMask));
	}
	
	public String stateString(){
		return stateString(_state);
	}
	
	public String stateString(int state){
			switch (state) {
				case STATE_INSTANTIATED:
					return "INSTANTIATED";
				case STATE_INITIALIZED:
					return "INITIALIZED";
				case STATE_CONNECTING:
					return "CONNECTING";
				case STATE_WAITING:
					return "WAITING";
				case STATE_UPDATING:
					return "UPDATING";
				case STATE_DISCONNECTING:
					return "DISCONNECTING";
				default:
					break;
			}
		return null;
	}
	
	public String statusString(){
		return statusString(_status);
	}
	public String statusString(int status){
		try{
		getStatus();
		}catch(Exception e) {
			
		}

		if(status==0){
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
		return _statusBuffer.toString();
	}
	
	//////////////////////////
	// ControlInputIF methods
	//////////////////////////
	/** initialize control input (ControlInputIF)*/
	public abstract void initialize() throws RemoteException;
	
	/** get control input value (ControlInputIF)*/
	public abstract void connect() throws Exception, RemoteException;
	/** disconnect from data source */
	public abstract void disconnect() throws Exception, RemoteException;

	/** return connector state */
	public int getState() throws RemoteException{
		return _state;
	}	
	
	/** return connector status */
	public int getStatus() throws RemoteException{
		if(updateTimeoutExpired()==true){
			setStatus(STATUS_TIMEOUT_EXPIRED);
		}else{
			clearStatus(STATUS_TIMEOUT_EXPIRED);
		}
		return _status;
	}
	
	public FilterInput getFilterInput(){
		return _input;
	}
	
	public void setFilterInput(FilterInput input){
		 _input=input;
	}
	
	public void setInputID(int id){
		_id=id;
	}
	public int getInputID(){
		return _id;
	}
	public Number getInputValue(){
		if(_input!=null){
			switch (_dataType) {
				case ControlInputIF.FIELD_TYPE_DOUBLE:
					return new Double(_input.doubleValue());
				case ControlInputIF.FIELD_TYPE_INT:
					return new Integer(_input.intValue());
				case ControlInputIF.FIELD_TYPE_LONG:
					return new Long(_input.longValue());
				default:
					break;
			}
		}
		return null;		
	}
		
	public void setLastUpdateTime(long time_msec){
		_last_update_msec=time_msec;
	}
	
	public long timeSinceLastUpdate(){
		return (System.currentTimeMillis()-_last_update_msec);
	}
	public void setUpdateTimeout(long timeoutMsec){
		_update_timeout_msec=timeoutMsec;		
	}
	public long getUpdateTimeout(){
		return _update_timeout_msec;		
	}
	public boolean updateTimeoutExpired(){
		if(_update_timeout_msec<=0L){
			return false;
		}
		return (timeSinceLastUpdate()>_update_timeout_msec);
	}
	
	public Instrument getService(){
		return _service;
	}
	
	public void setService(Instrument service){
		_service=service;
	}
	
	public void setValidator(RangeValidator validator){
		_validator=validator;
	}
	public RangeValidator getValidator(){
		return _validator;
	}
	public InputState getInputState()
	throws RemoteException{
		return new InputState(this);
	}
}