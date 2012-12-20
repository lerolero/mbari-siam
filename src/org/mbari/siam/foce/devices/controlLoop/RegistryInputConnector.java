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
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.devices.ControlInputIF;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.core.DeviceService;

/** Connects a FilterInput to a SIAM registry data stream */

public class RegistryInputConnector extends InputConnector implements InstrumentDataListener, ControlInputIF{
	
	/** Log4j logger */
	protected static Logger _log4j = Logger.getLogger(RegistryInputConnector.class);
	
	/** Service attributes (configuration values)
	 These may change on SetProperties.
	 */
	String _registryKey;
	String _fieldKey;
		
	public RegistryInputConnector(String registryKey, String fieldKey,int dataType, FilterInput input, long updateTimeoutMsec)
	throws InvalidPropertyException{
		
		this(registryKey, fieldKey, dataType, input, DEFAULT_ID,DEFAULT_UPDATE_TIMEOUT);
	
	}	
	
	/** Constructor */
	public RegistryInputConnector(String registryKey, String fieldKey,int dataType, FilterInput input,int id, long updateTimeoutMsec)
	throws InvalidPropertyException{
		super(input,dataType,id,updateTimeoutMsec);
		_registryKey=registryKey;
		_fieldKey=fieldKey;
	}
	
	
	//////////////////////////
	// ControlInputIF methods
	// (others implemented in base class)
	//////////////////////////
	/** initialize control input (ControlInputIF)*/
	public void initialize() 
	throws RemoteException{
	}
	
	/** get control input value (ControlInputIF)*/
	public void connect() 
	throws Exception, RemoteException{
		registerCallbacks();	
	}
	public void disconnect() 
	throws Exception, RemoteException{
	}
	
	
	/** Callback for InstrumentDataListener interface, called when the services
	 register with the InstrumentRegistry
	 */
	public void serviceRegisteredCallback(RegistryEntry entry){
		_log4j.info("serviceRegisteredCallback for ControlLoop ["+entry.registryName()+"]");
	}
	
	/** dataCallback for all services with which we are registered as data listeners. 
	 When data packets arrive, this method extracts
	 the relevant data and prepares it.
	 Some of the data is dropped into filters, and other data is used directly
	 to set the value of various control loop parameters.
	*/     
	public void dataCallback(DevicePacket sensorData, PacketParser.Field[] fields){
		
		setState(ControlInputIF.STATE_UPDATING);
		
		PacketParser.Field fieldRegName;
		String regName;
		boolean errors=false;
		try{
			fieldRegName=PacketParser.getField(fields,"registryName");
			regName=(String)fieldRegName.getValue();
			if(regName!=null){
				if(regName.equals(_registryKey)){
						PacketParser.Field parserField=PacketParser.getField(fields,_fieldKey);
						switch (_dataType) {
							case FIELD_TYPE_DOUBLE:
								Double doubleFieldValue=(Double)parserField.getValue();
								_input.put(doubleFieldValue.doubleValue());
								break;
							case FIELD_TYPE_LONG:
								Long longFieldValue=(Long)parserField.getValue();
								_input.put(longFieldValue.doubleValue());
								break;
							case FIELD_TYPE_INT:
								Integer intFieldValue=(Integer)parserField.getValue();
								_input.put(intFieldValue.doubleValue());
								break;
							default:
								_log4j.error("Invalid field type: ["+_dataType+"]");
								errors=true;
								break;
						}
				}else{
					_log4j.error("Unrecognized registryName field: ["+regName+"]");
					errors=true;
				}
			}else{
				_log4j.error("registryName field is null");
				errors=true;
			}
		}catch(NoDataException e){
			_log4j.error("Missing registryName field");
			errors=true;
		}
		if(errors){
			setStatus(ControlInputIF.STATUS_UPDATE_ERROR);
		}else{
			clearStatus(ControlInputIF.STATUS_UPDATE_ERROR);
			setLastUpdateTime(System.currentTimeMillis());
		}
		setState(ControlInputIF.STATE_WAITING);
		return;
	}

	public Instrument findService(){
		if(_registry==null){
			_registry=InstrumentRegistry.getInstance();
		}
		
		RegistryEntry registryEntry=_registry.findEntry(_registryKey);
		if(registryEntry!=null){
			DeviceService service=registryEntry.getService();
			if(service!=null){
				if(service instanceof Instrument){
					Instrument instrumentService=(Instrument)registryEntry.getService();
					return instrumentService;
				}else{
					_log4j.error("service does not implement Instrument");
				}
			}else{
				_log4j.error("service is null");			
			}
		}else{
			_log4j.error("registry entry is null - call registerCallbacks() to register");			
		}
		return null;
	}
			
	/** Attempts to register services. Services already registered
	 are not registered again. Services that are not found should 
	 be put on a list and registered when they become available.
	 Logs messages when services are	not registered.
	 */
	protected void registerCallbacks(){
		if(_registry==null){
			_registry=InstrumentRegistry.getInstance();
		}
		
		RegistryEntry registryEntry=_registry.findEntry(_registryKey);
		if(registryEntry==null){
			if (_registry.registerDataCallback(this, _registryKey) == null){
				_log4j.warn("service "+_registryKey+" entry not found");
			}
		}
	}		
}
