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

public class VEL_PID_Responder extends VEL_Responder{
	/** Log4j logger */
	protected static Logger _log4j = Logger.getLogger(VEL_PID_Responder.class);

	float _Kp;
	float _Ki;
	float _Kd;
	float _scaleFactor;

	float _maxError;
	double _errorSum;
	double _maxErrorSum;

	float _max_ki;
	float p_term, d_term;// int16
	double i_term;//int32
	
		
	public VEL_PID_Responder(FOCEProcess controlProcess, ControlLoopAttributes attributes) 
	throws Exception{
		super(controlProcess,attributes);
	}	
	
	/** initialize control loop */
	public synchronized void initialize(){
		super.initialize();

		_max_ki=Long.MAX_VALUE/2.0f;
		_Ki=_Kp=_Ki=0.0f;
		_scaleFactor=1.0f;
		_errorSum=0.0f;
	}
	
	/** Calculate correction value (heart of the PID loop) 
	 
		The following variables must be set (e.g. by update())
		prior to calling getCorrection():
	    _processValue, _previousProcessValue, _error, 
	    _maxError,_maxErrorSum, 
	    _scaleFactor, _Kp, _Ki, _Kd
	 */
	public float getCorrection() throws Exception{
		double temp;//int32
		
		_maxError    = (Float.MAX_VALUE/(_Kp + 1));
		_maxErrorSum = (_max_ki/(_Ki + 1));

		// Calculate Pterm and limit _error overflow
		if (_error > _maxError){
			p_term = Short.MAX_VALUE;
		}
		else if (_error < -_maxError){
			p_term = Short.MIN_VALUE;
		}
		else{
			p_term = _Kp * _error;
		}
		
		// Calculate Iterm and limit integral runaway
		temp = _errorSum + _error;
		if(temp > _maxErrorSum){
			i_term = _max_ki ;
			_errorSum = _maxErrorSum;
		}
		else if(temp < -_maxErrorSum){
			i_term = -_max_ki ;
			_errorSum = -_maxErrorSum;
		}
		else{
			_errorSum = temp;
			i_term = _Ki * _errorSum;
		}
		
		// Calculate Dterm
		d_term = _Kd * (_previousProcessValue - _processValue);
		
		_previousProcessValue = _processValue;
		
		_rawCorrection = (p_term + i_term + d_term)/_scaleFactor;
		
		
		return((float)_rawCorrection);
	}
		
	/** Return a sample buffer with current response configuration.
	 sub-classes should call the base class to get the 
	 common data items.
	 */
	public StringBuffer getConfigBuffer()
	throws Exception{
		_cfgPacketBuffer=super.getConfigBuffer();
		
		// add response-specific fields
		_cfgPacketBuffer.append(_Kp+_recordDelimiter);
		_cfgPacketBuffer.append(_Ki+_recordDelimiter);
		_cfgPacketBuffer.append(_Kd+_recordDelimiter);
		_cfgPacketBuffer.append(_scaleFactor+_recordDelimiter);
		_cfgPacketBuffer.append(p_term+_recordDelimiter);
		_cfgPacketBuffer.append(i_term+_recordDelimiter);
		_cfgPacketBuffer.append(d_term+_recordDelimiter);
		return _cfgPacketBuffer;
	}		
}