/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.controlLoop;

import java.util.*;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.rmi.RemoteException;

import com.rbnb.sapi.*;

import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.ServiceAttributes;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.foce.devices.controlLoop.*;
import org.mbari.siam.distributed.devices.*;
import org.mbari.siam.utils.RangeValidator;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

/* Control Loop Worker thread
 */
/*
 $Id: $
 $Name: $
 $Revision: $
 */

public class OSDTConnectorWorker extends ConnectorWorker  {
	
    static protected Logger _log4j = Logger.getLogger(OSDTConnectorWorker.class);  
	
	ChannelMap _requestMap=new ChannelMap();
	String _channelDescription=null;
	Sink _sink;
	OSDTInputConnector _osdtConnector;

	public OSDTConnectorWorker(OSDTInputConnector connector, long updatePeriodMillisec){
		super(connector, updatePeriodMillisec);
		_osdtConnector=connector;
		_channelDescription=_osdtConnector._source+"/"+_osdtConnector._channel;
		_sink=_osdtConnector._sink;
	}
	
	public void initialize() throws Exception{
		super.initialize();
		try{
			String sinkName=(_osdtConnector._source+"-sink");
			if(_log4j.isDebugEnabled()){
			_log4j.debug("opening connection to "+_osdtConnector._server+" as "+sinkName);
			}
			_sink.OpenRBNBConnection(_osdtConnector._server,sinkName);
			if(_log4j.isDebugEnabled()){
			_log4j.debug("verifying connection "+_osdtConnector._server+" - "+sinkName);			
			}
			while(_sink.VerifyConnection()==false){
				delay(1000L);
			}
			if(_log4j.isDebugEnabled()){
			_log4j.debug("constructing request for "+_channelDescription);
			}
			_requestMap=new ChannelMap();
			_requestMap.Add(_channelDescription);
			if(_log4j.isDebugEnabled()){
			_log4j.debug("start monitoring "+sinkName);
			}
			_sink.Monitor(_requestMap,1);
		}catch(SAPIException e){
			e.printStackTrace();
			throw e;
		}
	}
	
	public void shutdown(){
		if(_log4j.isDebugEnabled()){
		_log4j.debug("Shutting down Sink connection ["+_channelDescription+"]");
		}
		_sink.CloseRBNBConnection();
	}
	
	public void doWorkerAction(){
		boolean errors=false;
		try{
			_osdtConnector.setState(ControlInputIF.STATE_UPDATING);
	
			//if(_sink.VerifyConnection()){ // for some reason, doing this check causes Fetch not to return any values			
			if(_log4j.isDebugEnabled()){
			_log4j.debug("fetching frame ["+_channelDescription+"]");
			}
			ChannelMap frame=_sink.Fetch(5000L);
			
			if(frame == null){
				_log4j.error("no data received");
				errors=true;
				// maybe should set state/status and return...
			}else{
				if(_log4j.isDebugEnabled()){
				_log4j.debug("received "+frame.NumberOfChannels()+" channels from "+_channelDescription);
				}
				if(frame.NumberOfChannels()>0){
					for(int i=0;i<frame.NumberOfChannels();i++){
						double[] times=frame.GetTimes(i);
						if(_log4j.isDebugEnabled()){
						_log4j.debug(" channel["+i+"] ["+frame.GetName(i)+"] has "+times.length+" points:");
						}
						for(int  j=0;j<times.length;j++){
							double channelValue=Double.NaN;
							switch(frame.GetType(i)){
								case ChannelMap.TYPE_FLOAT64:
									if(_log4j.isDebugEnabled()){
									_log4j.debug("OSDTConWorker F64: "+frame.GetName(i)+":"+times[j]+","+frame.GetDataAsFloat64(i)[j]+" ");
									}
									channelValue=(double)frame.GetDataAsFloat64(i)[j];
									//_osdtConnector.getFilterInput().put(frame.GetDataAsFloat64(i)[j]);
									break;
								case ChannelMap.TYPE_FLOAT32:
									if(_log4j.isDebugEnabled()){
									_log4j.debug("OSDTConWorker F32: "+frame.GetName(i)+":"+times[j]+frame.GetDataAsFloat32(i)[j]+" ");
									}
									channelValue=(double)frame.GetDataAsFloat32(i)[j];
									//_osdtConnector.getFilterInput().put(frame.GetDataAsFloat32(i)[j]);
									break;
								case ChannelMap.TYPE_INT16:
									if(_log4j.isDebugEnabled()){
									_log4j.debug("OSDTConWorker I16: "+frame.GetName(i)+":"+times[j]+frame.GetDataAsInt16(i)[j]+" ");
									}
									channelValue=(double)frame.GetDataAsInt16(i)[j];
									//_osdtConnector.getFilterInput().put(frame.GetDataAsInt16(i)[j]);
									break;
								case ChannelMap.TYPE_INT32:
									if(_log4j.isDebugEnabled()){
									_log4j.debug("OSDTConWorker I32: "+frame.GetName(i)+":"+times[j]+frame.GetDataAsInt32(i)[j]+" ");
									}
									channelValue=(double)frame.GetDataAsInt32(i)[j];
									//_osdtConnector.getFilterInput().put(frame.GetDataAsInt32(i)[j]);
									break;
								case ChannelMap.TYPE_INT64:
									if(_log4j.isDebugEnabled()){
									_log4j.debug("OSDTConWorker I64: "+frame.GetName(i)+":"+times[j]+frame.GetDataAsInt64(i)[j]+" ");
									}
									channelValue=(double)frame.GetDataAsInt64(i)[j];
									//_osdtConnector.getFilterInput().put(frame.GetDataAsInt64(i)[j]);
									break;
								case ChannelMap.TYPE_INT8:
									if(_log4j.isDebugEnabled()){
									_log4j.debug("OSDTConWorker I8: "+frame.GetName(i)+":"+times[j]+frame.GetDataAsInt8(i)[j]+" ");
									}
									channelValue=(double)frame.GetDataAsInt8(i)[j];
									//_osdtConnector.getFilterInput().put(frame.GetDataAsInt8(i)[j]);
									break;
									//case ChannelMap.TYPE_STRING:
									//	System.out.println(times[j]+frame.GetDataAsString(i)[j]+" ");
									//	_osdtConnector.getFilterInput().put(frame.GetDataAsString(i)[j]);
									//	break;
								default:
									_log4j.error("Type not supported:"+frame.GetType(i));
									errors=true;
									break;
							}
							if(errors==false){
								// get the range validator from the input connector
								RangeValidator validator=_osdtConnector.getValidator();
								if(validator!=null){
									// if it's non-null, use it to validate the input
									double test=_osdtConnector.getValidator().validate(channelValue);
									if(Double.isNaN(test)){
										// if the input is invalid, flag an error
										_log4j.error("invalid value ["+channelValue+"] from OSDT data stream (via range validator)");
										errors=true;
									}else{
										// if the input is valid, drop it into the processing chain
										_osdtConnector.getFilterInput().put(channelValue);
									}
								}else{
									// if there is no validator, drop it into the processing chain
									_osdtConnector.getFilterInput().put(channelValue);
								}
									
							}
						}
					}
					// set the last update time for this connector
					_osdtConnector.setLastUpdateTime(System.currentTimeMillis());
				}
			}
		}catch (Exception e) {
			_log4j.error(e);
			errors=true;
		}		
		// update status
		if(errors){
			_osdtConnector.setStatus(ControlInputIF.STATUS_UPDATE_ERROR);
		}else{
			_osdtConnector.clearStatus(ControlInputIF.STATUS_UPDATE_ERROR);
		}
		// update state
		_osdtConnector.setState(ControlInputIF.STATE_WAITING);
		return;
	}
}