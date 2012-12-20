/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.controlLoop;

import java.io.IOException;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;


import org.mbari.siam.registry.InstrumentDataListener;
import org.mbari.siam.registry.InstrumentRegistry;
import org.mbari.siam.registry.RegistryEntry;
import org.mbari.siam.utils.Filter;
import org.mbari.siam.utils.FilterInput;
import org.mbari.siam.utils.BoxcarFilter;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.devices.ControlInputIF;

/** Connects a FilterInput to a SIAM registry data stream */

public class RawInputConnector extends InputConnector{
	/** Log4j logger */
	protected static Logger _log4j = Logger.getLogger(RawInputConnector.class);

	public static final int FIELD_TYPE_DOUBLE=0;
	public static final int FIELD_TYPE_LONG=1;
	public static final int FIELD_TYPE_INT=2;
	
	/** Service attributes (configuration values)
	 These may change on SetProperties.
	 */
	String _registryKey;
	String _fieldKey;
	FilterInput _input;
	int _fieldType;
	int _id;
	Instrument _service=null;
	
	public RawInputConnector(String registryKey, String fieldKey,int fieldType, FilterInput input)
	throws InvalidPropertyException{
		
		this(registryKey, fieldKey, fieldType, input, 0);
	
	}	
	
	/** Constructor */
	public RawInputConnector(String registryKey, String fieldKey,int fieldType, FilterInput input,int id)
	throws InvalidPropertyException{
		_registryKey=registryKey;
		_fieldKey=fieldKey;
		_input=input;
		switch (fieldType) {
			case FIELD_TYPE_DOUBLE:
			case FIELD_TYPE_LONG:
			case FIELD_TYPE_INT:
				break;
			default:
				throw new InvalidPropertyException("Invalid field type: ["+fieldType+"]");
		}
		_fieldType=fieldType;
		_id=id;
	}
	
	public void setInputID(int id){
		_id=id;
	}
	public int getInputID(){
		return _id;
	}
	public Number getInputValue(){
		if(_input!=null){
			switch (_fieldType) {
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
	
	/** initialize control input (ControlInputIF)*/
	public void initialize() 
	throws RemoteException{
	}
	
	/** get control input value (ControlInputIF)*/
	public void connect() 
	throws RemoteException{
	}
	/** get control input value (ControlInputIF)*/
	public void disconnect() 
	throws RemoteException{
	}
	public Instrument getService(){
		return _service;
	}
	public void setService(Instrument service){
		_service=service;
	}
	public FilterInput getFilterInput() {
		return _input;
	}

}