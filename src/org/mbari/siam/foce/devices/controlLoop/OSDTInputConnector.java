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
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.devices.ControlInputIF;

/** Connects a FilterInput to a SIAM registry data stream */

public class OSDTInputConnector extends InputConnector implements ControlInputIF{
	
	public static final long DEFAULT_UPDATE_PERIOD=10000L;

	/** Log4j logger */
	protected static Logger _log4j = Logger.getLogger(OSDTInputConnector.class);

	
	/** Service attributes (configuration values)
	 These may change on SetProperties.
	 */
	String _server=null;
	String _source=null;
	String _channel=null;
	OSDTConnectorWorker _worker=null;
	Sink _sink;
	long _workerPeriodMsec;
	
	public OSDTInputConnector(String server, String source, String channel, int dataType, FilterInput input)
	throws InvalidPropertyException{
		this(server, source, channel, dataType, input, DEFAULT_ID, DEFAULT_UPDATE_TIMEOUT, DEFAULT_UPDATE_PERIOD);
	}
	
	public OSDTInputConnector(String server, String source, String channel, int dataType, FilterInput input, long updateTimeoutMsec, long workerPeriodMsec)
	throws InvalidPropertyException{
		this(server, source, channel, dataType, input, DEFAULT_ID,updateTimeoutMsec, workerPeriodMsec);
	}	
	
	/** Constructor */
	public OSDTInputConnector(String server, String source, String channel, int dataType, FilterInput input, int id, long updateTimeoutMsec, long workerPeriodMsec)
	throws InvalidPropertyException{
		super(input,dataType,id,updateTimeoutMsec);
		_server=server;
		_source=source;
		_channel=channel;
		_sink=new Sink();
		_workerPeriodMsec=workerPeriodMsec;
	}
	
	public void setWorkerPeriod(long periodMsec){
		_workerPeriodMsec=periodMsec;
		if(_worker!=null){
			_worker.setUpdatePeriod(periodMsec);
		}
	}
	public  String channel(){
		return _channel;
	}
	
	//////////////////////////
	// ControlInputIF methods
	// (others implemented in base class)
	//////////////////////////
	/** initialize control input (ControlInputIF)*/
	public void initialize() 
	throws RemoteException{
		if(_worker==null){
			_worker=new OSDTConnectorWorker(this, _workerPeriodMsec);
		}
		setState(STATE_INITIALIZED);
	}

	/** get control input value (ControlInputIF)*/
	public void connect() 
	throws Exception{
		setState(STATE_CONNECTING);
		clearStatus(STATUS_CONNECT_ERROR);
		try{
			if(_log4j.isDebugEnabled()){
			_log4j.debug("OSDTConnectorWorker starting ["+_server+",\""+_source+"/"+_channel+"\"]");
			}
			if(!_worker.isRunning()){
				_worker.start();
			}else{
				if(_log4j.isDebugEnabled()){
				_log4j.debug("OSDTConnectorWorker ["+_server+","+_source+"/"+_channel+"] already running");
				}
			}
		}catch (Exception e) {
			e.printStackTrace();
			setState(STATE_INITIALIZED);
			setStatus(STATUS_CONNECT_ERROR);
			throw e;
		}
		setState(STATE_WAITING);
	}
	
	public void disconnect() 
	throws Exception{
		setState(STATE_DISCONNECTING);
		clearStatus(STATUS_DISCONNECT_ERROR);
		try{
			if(_log4j.isDebugEnabled()){
			_log4j.debug("connector terminating worker ["+_server+","+_source+"/"+_channel+"]");
			}
			_worker.terminate();
			_sink.CloseRBNBConnection();		
		}catch (Exception e) {
			e.printStackTrace();
			setState(STATE_INITIALIZED);
			setStatus(STATUS_DISCONNECT_ERROR);
			throw e;
		}
		setState(STATE_INITIALIZED);
	}
	
	

}