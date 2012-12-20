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
import org.mbari.siam.utils.Filter;
import org.mbari.siam.utils.BoxcarFilter;
import org.mbari.siam.utils.FilterInput;

public class PH_LIN_Responder extends PH_Responder{
	/** Log4j logger */
	protected static Logger _log4j = Logger.getLogger(PH_LIN_Responder.class);
	
	// response-specific members here
	BoxcarFilter _bcFilter;
	FilterInput _filterInput;
	int _lin_filter_depth;
	double _lin_slope;
	double _lin_offset;
	double _lin_error_limit;
	
	public PH_LIN_Responder(FOCEProcess controlProcess, ControlLoopAttributes attributes) 
	throws Exception{
		super(controlProcess,attributes);
	}

	/** initialize control loop */
	public synchronized void initialize(){
		super.initialize();
		// init response-specific variables here
		_lin_slope=_attributes.ph_lin_slope;
		_lin_offset=_attributes.ph_lin_offset;
		_lin_error_limit=_attributes.ph_lin_error_limit;
		_lin_filter_depth=_attributes.ph_lin_filter_depth;
		_filterInput=new FilterInput("lin_bci");
		_bcFilter=new BoxcarFilter("lin_bcf",Filter.DEFAULT_ID,_lin_filter_depth);
		try{
			_bcFilter.addInput(_filterInput);
		}catch (Exception e) {
			_log4j.error("PH_LIN_Responder failed to add input in initialize");
		}
	}
	
	/** get any signals needed */
	protected synchronized void updateInputs()
	throws Exception{
		
		super.updateInputs();
		
		try{
			// get current configured PID parameters
			_lin_slope=_attributes.ph_lin_slope;
			_lin_offset=_attributes.ph_lin_offset;
			_lin_error_limit=_attributes.ph_lin_error_limit;
			
			if(_lin_filter_depth!=_attributes.ph_lin_filter_depth){				
				_bcFilter.setDepth(_lin_filter_depth);
				_lin_filter_depth=_attributes.ph_lin_filter_depth;
			}
		}catch(Exception e){
			_log4j.error("update - error getting signals");
			e.printStackTrace();
			throw e;
		}
	}
	
	/** Calculate correction value (heart of the control loop) 
		Makes linear incremental adjustments (e.g. to ESW pump)
		based on filtered process values.
	 
		The following variables must be set (e.g. by update())
		prior to calling getCorrection():
	    _processValue, _previousProcessValue, _error, 
	    _maxError,_maxErrorSum, 
	 */
	public float getCorrection() throws Exception{
		// drop the current process value into the filter
		// (process value is updated by update() method)
		_filterInput.put((double)_error);
		
		// set previous process value from current process value
		_previousProcessValue = _processValue;
		
		// An offset term is a function of desired pH offset or setpoint
		// and internal water velocity
		double offsetTerm=0.0;
		switch (_ph_cmode) {
			case CONTROL_MODE_CONSTANT:
				offsetTerm=(double)(_phExternal-_setPoint);
				break;
			case CONTROL_MODE_OFFSET:
				offsetTerm=(double)(_offset);
				break;
			default:
				break;
		}
		
		// An error term is a linear function of the 
		// filtered error signal.
		// The error term is applied to correct small
		// perturbations when the error is small.
		double errorTerm=0.0;
		if(Math.abs(_bcFilter.floatValue())<_lin_error_limit){
			errorTerm=_lin_slope*_bcFilter.floatValue()+_lin_offset;
		}
		
		// The correction value is returned as an
		// absolute pH offset relative to the background pH
		_rawCorrection = errorTerm+offsetTerm;
		
		return((float)_rawCorrection);
	}
	
	/** Return a sample buffer with current response configuration.
	 sub-classes should call the base class to get the 
	 common data items.
	 */
	public StringBuffer getConfigBuffer()
	throws Exception{
		
		// base class fills in common fields
		_cfgPacketBuffer=super.getConfigBuffer();
		
		// append response-specific fields 	
		_cfgPacketBuffer.append(_lin_filter_depth+_recordDelimiter);
		_cfgPacketBuffer.append(_lin_slope+_recordDelimiter);
		_cfgPacketBuffer.append(_lin_offset+_recordDelimiter);
		return _cfgPacketBuffer;
	}
	
}