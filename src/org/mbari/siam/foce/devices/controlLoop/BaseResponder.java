/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.controlLoop;

import java.io.IOException;
import java.rmi.RemoteException;
import java.text.NumberFormat;

import org.apache.log4j.Logger;

import org.mbari.siam.registry.InstrumentDataListener;
import org.mbari.siam.registry.InstrumentRegistry;
import org.mbari.siam.registry.RegistryEntry;

import org.mbari.siam.distributed.devices.ProcessStateIF;
import org.mbari.siam.distributed.devices.ProcessConfigIF;
import org.mbari.siam.distributed.devices.ControlResponseIF;
import org.mbari.siam.distributed.devices.ControlProcessIF;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.dataTurbine.Turbinator;

public abstract class BaseResponder implements ControlResponseIF{
	/** Log4j logger */
	protected static Logger _log4j = Logger.getLogger(BaseResponder.class);
	public static final String DELIMITERS=",";

	protected int _velocity_cmode;
	protected int _ph_cmode;
	protected int _ph_rmode;
	protected int _esw_valve_amode;
	protected int _esw_pump_amode;
	protected int _thruster_amode;
	
	protected float _setPoint;
	protected double _offset;
	protected float _processValue;
	protected float _previousProcessValue;
	protected float _error;
	protected float _previousError;
	protected double _correction, _rawCorrection;	
	
	protected PacketParser _packetParser;
	protected Turbinator _turbinator;
	protected String _dataTurbineHost;
	protected StringBuffer _packetBuffer;
	protected StringBuffer _cfgPacketBuffer;
	protected String _recordType;
	protected SensorDataPacket _dataPacket;
	protected SensorDataPacket _lastDataPacket;
	protected int _maxDataBytes=256;
	protected boolean _doTurbinate=false;
	protected String _recordDelimiter=DELIMITERS;
	
	protected ControlProcessIF _controlProcess;
	protected ControlLoopAttributes _attributes=null;

	public BaseResponder(ControlProcessIF controlProcess, ControlLoopAttributes attributes) 
	throws Exception{
		_controlProcess=controlProcess;
		_attributes=attributes;
		_recordType="$GENERIC";
	}

	/** update output (new process command) 
	 */
	abstract public float update() throws Exception;		
	
	/** Update inputs, needed prior to updating output values. 
	 For example, input signals and tuning parameters that
	 may have changed (picked up from _attributes):
	 
	 _processValue, _setPoint, _error, _offset	 
	 */
	abstract protected void updateInputs() throws Exception;	

	/** Calculate correction value (heart of the PID loop) 
	 updateInputs must be called before calling this method
	 */
	abstract public float getCorrection() throws Exception;	
	
	/** return a sensor data packet 
	 sub-classes should override
	 */
	abstract public SensorDataPacket getSamplePacket();
	
	
	/** initialize control loop */
	public synchronized void initialize(){
		_previousProcessValue=0.0f;
		_previousError=0.0f;
		_correction=0.0;
		_rawCorrection=0.0;
	}
	
	/** reset control loop */
	public void reset(){
		// base class does nothing
	}
	
	
	/** Return most recent sample data packet */
	public SensorDataPacket getLastSample(){
		return null;
	}
	
	/** Return a sample buffer with current process data.
	 sub-classes should call the base class to get the 
	 common data items.
	 */
	public StringBuffer getSampleBuffer()
	throws Exception{

		if(_packetBuffer==null){
			_packetBuffer=new StringBuffer();
		}
		
		_packetBuffer.setLength(0);
		
		 _packetBuffer.append(_recordType+_recordDelimiter);
		 _packetBuffer.append(_processValue+_recordDelimiter);
		 _packetBuffer.append(_offset+_recordDelimiter);
		 _packetBuffer.append(_setPoint+_recordDelimiter);
		 _packetBuffer.append(_error+_recordDelimiter);
		 _packetBuffer.append(_correction+_recordDelimiter);
		 _packetBuffer.append(_rawCorrection+_recordDelimiter);
		 
		/*
		 _packetBuffer.append(_recordType+_recordDelimiter);
		 _packetBuffer.append(_velocity_cmode+_recordDelimiter);
		 _packetBuffer.append(_ph_cmode+_recordDelimiter);
		 _packetBuffer.append(_esw_valve_amode+_recordDelimiter);
		 _packetBuffer.append(_esw_pump_amode+_recordDelimiter);
		 _packetBuffer.append(_thruster_amode+_recordDelimiter);
		 */
		/* ORIGINAL
		_packetBuffer.append(_recordType+_recordDelimiter);
		_packetBuffer.append(_processValue+_recordDelimiter);
		_packetBuffer.append(_velocity_cmode+_recordDelimiter);
		_packetBuffer.append(_ph_cmode+_recordDelimiter);
		_packetBuffer.append(_esw_valve_amode+_recordDelimiter);
		_packetBuffer.append(_esw_pump_amode+_recordDelimiter);
		_packetBuffer.append(_thruster_amode+_recordDelimiter);
		_packetBuffer.append(_offset+_recordDelimiter);
		_packetBuffer.append(_setPoint+_recordDelimiter);
		_packetBuffer.append(_error+_recordDelimiter);
		_packetBuffer.append(_correction+_recordDelimiter);
		_packetBuffer.append(_rawCorrection+_recordDelimiter);
		 */
		return _packetBuffer;
	}
	
	/** Return a sample buffer with current response configuration.
	sub-classes should call the base class to get the 
	common data items.
	*/
	public StringBuffer getConfigBuffer()
	throws Exception{
		
		if(_cfgPacketBuffer==null){
			_cfgPacketBuffer=new StringBuffer();
		}
		
		_cfgPacketBuffer.setLength(0);
		_cfgPacketBuffer.append(_recordType+"_CFG"+_recordDelimiter);
		_cfgPacketBuffer.append(_velocity_cmode+_recordDelimiter);
		_cfgPacketBuffer.append(_ph_cmode+_recordDelimiter);
		_cfgPacketBuffer.append(_esw_valve_amode+_recordDelimiter);
		_cfgPacketBuffer.append(_esw_pump_amode+_recordDelimiter);
		_cfgPacketBuffer.append(_thruster_amode+_recordDelimiter);
		return _cfgPacketBuffer;
	}
	
	public void showParsedData(PacketParser.Field[] fields){
		NumberFormat nf=NumberFormat.getInstance();
		nf.setMaximumFractionDigits(5);
		nf.setMinimumFractionDigits(3);
		nf.setMinimumIntegerDigits(1);
		nf.setGroupingUsed(false);
		if(_log4j.isDebugEnabled()){
			for(int i=0;i<fields.length;i++){
				Object fieldValue=null;
				//if(_log4j.isDebugEnabled()){
				//_log4j.debug("field["+i+"]:"+fields[i].getClass().getName());
				//}
				if(fields[i].getValue() instanceof Double){
					//if(_log4j.isDebugEnabled()){
					//_log4j.debug("field["+i+"] is Double");
					//}
					fieldValue=(Double)fields[i].getValue();
				}else if(fields[i].getValue() instanceof Float){
					//if(_log4j.isDebugEnabled()){
					//_log4j.debug("field["+i+"] is Float");
					//}
					fieldValue=(Float)fields[i].getValue();
				}else if(fields[i].getValue() instanceof Integer){
					//if(_log4j.isDebugEnabled()){
					//_log4j.debug("field["+i+"] is Integer");
					//}
					fieldValue=(Integer)fields[i].getValue();
				}else if(fields[i].getValue() instanceof Short){
					//if(_log4j.isDebugEnabled()){
					//_log4j.debug("field["+i+"] is Short");
					//}
					fieldValue=(Short)fields[i].getValue();
				}else if(fields[i].getValue() instanceof String){
					//if(_log4j.isDebugEnabled()){
					//_log4j.debug("field["+i+"] is String");
					//}
					fieldValue=(String)fields[i].getValue();
				}else{
					//if(_log4j.isDebugEnabled()){
					//_log4j.debug("field["+i+"] is "+fields[i].getValue().getClass().getName());			
					//}
				}
				
				if(fieldValue!=null){
					String fieldString;
					if(fieldValue instanceof Number){
						fieldString=nf.format(((Number)fieldValue).doubleValue());
					}else{
						fieldString=(String)fieldValue;
					}
					/*
					if(i>=(ResponseParser.VEL_CMODE_INDEX+1) && i<=(ResponseParser.THRU_AMODE_INDEX+1)){
						fieldString+=" ["+((FOCEProcess)_controlProcess).modeName(((Number)fieldValue).intValue())+"]";
					}
					 */
					_log4j.debug(fields[i].getName()+":"+fieldString);
				}
			}
		}
		// add one more new line
		if(_log4j.isDebugEnabled()){
			_log4j.debug("\n");
		}
	}
	
}