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

import org.mbari.siam.distributed.devices.ControlOutputIF;
import org.mbari.siam.distributed.devices.ControlResponseIF;
import org.mbari.siam.distributed.devices.ControlProcessIF;
import org.mbari.siam.distributed.devices.ProcessParameterIF;
import org.mbari.siam.distributed.devices.ElmoIF;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.dataTurbine.Turbinator;

import org.mbari.siam.foce.devices.controlLoop.FOCEProcess;

public abstract class VEL_Responder extends BaseResponder implements ProcessParameterIF{
	/** Log4j logger */
	protected static Logger _log4j = Logger.getLogger(VEL_Responder.class);
	
	public static final String RECORD_HEADER="$PID_VELOCITY";

	protected static final int FWD_THRUSTER_INDEX  =0;
	protected static final int AFT_THRUSTER_INDEX  =1;
	protected static final int NUMBER_OF_THRUSTER  =2;

	double _lastFwdThrusterCommand;
	double _lastAftThrusterCommand;
	double _lastDelta;
	double _vxInt, _vyInt,_vmagInt,_vhdgInt;
	double _vxExt, _vyExt,_vmagExt,_vhdgExt;
		
	FOCEProcess _foceProcess;

	public VEL_Responder(FOCEProcess controlProcess, ControlLoopAttributes attributes) 
	throws Exception{
		super((ControlProcessIF)controlProcess, attributes);
		_foceProcess=controlProcess;
		_recordType=RECORD_HEADER;
		initialize();
	}
	
	/** initialize control loop */
	public synchronized void initialize(){
		super.initialize();
		_doTurbinate=_attributes.velocity_pid_enable_turbinator;	
		_packetParser=new VEL_ResponseParser(_attributes.registryName+" PID_VELOCITY",_recordDelimiter);
		_packetBuffer=new StringBuffer();
		_dataPacket=new SensorDataPacket(_attributes.isiID,_maxDataBytes);
		_dataPacket.setRecordType(1L);
		if(_doTurbinate){
			initTurbinator();
		}
	}

	protected void initTurbinator(){
		// set up a data turbine source 
		// using SIAM Turbinator and SensorDataPacket (required by Turbinator)
		try{
			if(_packetParser==null){
				_packetParser=new VEL_ResponseParser(_attributes.registryName+" PID_VELOCITY",_recordDelimiter);
			}
			if(_turbinator!=null){
				_turbinator.close();
			}
			_turbinator=new Turbinator(_packetParser,
									   "PID_VELOCITY",
									   _attributes.rbnbServer,
									   _attributes.locationName,
									   new String(_attributes.serviceName),
									   _attributes.advertiseService);
			
			if(_packetBuffer==null){
				_packetBuffer=new StringBuffer();
			}
			if(_dataPacket==null){
				_dataPacket=new SensorDataPacket(_attributes.isiID,_maxDataBytes);
			}
			_dataPacket.setRecordType(1L);
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/** get any signals needed. typically, this is where the 
		process value and setpoint are updated.
	 */
	protected synchronized void updateInputs()
	throws Exception{
		try{
			// use temporary variables so that if any fail,
			// they won't leave the loop in a bad state
			
			// get process value (current output)
			float processValue=_controlProcess.getSignal(SIG_VH2O_INT_X_FILT).floatValue();
			// get setpoint (desired value, taking into accound constant or offset mode)
			float setpoint=_controlProcess.getParameter(PARAM_VELOCITY_SETPOINT).floatValue();
			// get current configured offset value 
			double offset=_attributes.velocity_offset;
			// get error signal (difference between output and setpoint)
			float sigErr=_controlProcess.getSignal(SIG_VH2O_INT_ERR).floatValue();
			// get velocity signals
			double vxInt=_controlProcess.getSignal(SIG_VH2O_INT_X_FILT).doubleValue();
			double vyInt=_controlProcess.getSignal(SIG_VH2O_INT_Y_FILT).doubleValue();
			double vxExt=_controlProcess.getSignal(SIG_VH2O_EXT_X_FILT).doubleValue();
			double vyExt=_controlProcess.getSignal(SIG_VH2O_EXT_Y_FILT).doubleValue();
			double vmagInt=_controlProcess.getSignal(SIG_VH2O_INT_MAG_FILT).doubleValue();
			double vhdgInt=_controlProcess.getSignal(SIG_VH2O_INT_DIR_FILT).doubleValue();
			double vmagExt=_controlProcess.getSignal(SIG_VH2O_EXT_MAG_FILT).doubleValue();
			double vhdgExt=_controlProcess.getSignal(SIG_VH2O_EXT_DIR_FILT).doubleValue();
			
			// transfer temp variables to loop variables
			_processValue=processValue;
			_setPoint=setpoint;
			_offset=offset;
			_error=sigErr;
			_vxInt=vxInt;
			_vyInt=vyInt;
			_vxExt=vxExt;
			_vyExt=vyExt;
			_vmagInt=vmagInt;
			_vhdgInt=vhdgInt;
			_vmagExt=vmagExt;
			_vhdgExt=vhdgExt;
			
			// get current configured PID gains
			_ph_cmode=_attributes.ph_control_mode;
			_velocity_cmode=_attributes.velocity_control_mode;
			_esw_valve_amode=_attributes.esw_valve_actuation;
			_esw_pump_amode=_attributes.esw_pump_actuation;
			_thruster_amode=_attributes.esw_valve_actuation;
			_doTurbinate=_attributes.velocity_pid_enable_turbinator;				
			
		}catch(Exception e){
			_log4j.error("update - error getting signals");
			e.printStackTrace();
			throw e;
		}
	}

	/** Get command updates to CO2 delivery valves */
	protected double[] thrusterCommands(double correction) throws Exception{
		
		double[] commands={0.0,0.0};
		double currentDirection=_foceProcess.getSignal(SIG_VH2O_INT_DIR_FILT).doubleValue();

		//double thrusterSpeed=_foceProcess.vel2rpm(_vxInt+correction);
		double thrusterSpeed=_lastFwdThrusterCommand+_foceProcess.vel2rpm(correction);

		double maxChange=Math.abs(_attributes.velocity_thruster_max_change);
		if(Math.abs(_lastFwdThrusterCommand-thrusterSpeed) > maxChange){
			thrusterSpeed=((_lastFwdThrusterCommand-thrusterSpeed)<0?_lastFwdThrusterCommand+maxChange:_lastFwdThrusterCommand-maxChange);
		}
		
		if( Math.abs(thrusterSpeed) < Math.abs(_attributes.velocity_min_rpm)){
			thrusterSpeed=0.0;
		}else if(Math.abs(thrusterSpeed) > Math.abs(_attributes.velocity_max_rpm)){
			thrusterSpeed=( thrusterSpeed<0 ? (-_attributes.velocity_max_rpm) : (_attributes.velocity_max_rpm));
		}

		commands[FWD_THRUSTER_INDEX]= thrusterSpeed;
		commands[AFT_THRUSTER_INDEX]= -thrusterSpeed;

		return commands;
	}
	
	/** Send command updates to inputs (CO2 delivery pump) */
	protected void updateThrusterOutputs(double[] thrusterCommands) throws Exception{
		if(_log4j.isDebugEnabled()){
		_log4j.debug("updateOutputs - getting thruster outputs ");
		}
		ControlOutputIF fwdThrusterOutput=_controlProcess.getOutput(OUTPUT_FWD_THRUSTER_VELOCITY);
		ControlOutputIF aftThrusterOutput=_controlProcess.getOutput(OUTPUT_AFT_THRUSTER_VELOCITY);
		
		if(_log4j.isDebugEnabled()){
		_log4j.debug("updateOutputs - setting thruster outputs...");
		}
		
		// send to fwd valve...
		fwdThrusterOutput.setOutputValue(thrusterCommands[FWD_THRUSTER_INDEX]);
		_lastFwdThrusterCommand=thrusterCommands[FWD_THRUSTER_INDEX];
		
		// send to aft valve...
		aftThrusterOutput.setOutputValue(thrusterCommands[AFT_THRUSTER_INDEX]);
		_lastAftThrusterCommand=thrusterCommands[AFT_THRUSTER_INDEX];
	}
	
	/** Apply physical/operational constraints to the raw signal correction from the PID loop */
	protected float applyConstraints(double correction) throws Exception{
				
		double limitedValue=correction;
		
		// check for overflow vs max/min float
		// (should never get here, but it should know if we do)
		if(Math.abs(correction) > Float.MAX_VALUE){
			_log4j.warn("correction overflow - set to upper limit ["+correction+" / "+Float.MAX_VALUE+"]");
			if(correction<0){
				limitedValue = -Float.MAX_VALUE;
			}else{
				limitedValue = Float.MAX_VALUE;
			}
		}
		else if(Math.abs(correction) < Float.MIN_VALUE){
			if(_log4j.isDebugEnabled()){
			_log4j.debug("correction overflow - set to lower limit ["+correction+" / "+Float.MIN_VALUE+"]");
			}
			if(correction<0){
				limitedValue = -Float.MIN_VALUE;
			}else{
				limitedValue = Float.MIN_VALUE;
			}
		}
		
		// check vs configured limits...
		
		// check max allowed correction
		double maxCorrection=_attributes.velocity_max_correction;
		if(Math.abs(correction) > maxCorrection) {
			if(correction<0){
				limitedValue = -maxCorrection;
			}else{
				limitedValue = maxCorrection;
			}
		}
		// return value with limits applied
		return (float)limitedValue;
	}
		
	protected float getDeadBandCorrection() throws Exception{
		float correction=0.0f;
		
		// if velocity is in deadband
		if( (_vxInt >= _attributes.velocity_deadband_lo) &&
		   (_vxInt <= _attributes.velocity_deadband_hi)){
			
			if(_vhdgInt >= 0.0){
				// if direction is positive, set to upper deadband
				correction = (float)(Math.abs(_attributes.velocity_deadband_hi)-Math.abs(_vxInt));
			}else{
				// if direction is negative, set to lower deadband
				correction = (float)-(Math.abs(_attributes.velocity_deadband_lo)-Math.abs(_vxInt));			
			}
		}else{
			// if outside of deadband, set correction s.t. velocity=0
			// (since a correction=0 will not change the velocity)
			correction=(float)(-_vxInt);
		}
		
		return correction;
	}
	
	/** update output: where the rubber is told to meet the road (update control loop) 
	 */
	public synchronized float update() throws Exception{
	
		updateInputs();
		boolean correctThrusters=false;
		boolean isDeadband=false;
		
		if(_log4j.isDebugEnabled()){
		_log4j.debug(_controlProcess.toString());
		}
		
		// implents the control-mode selection matrix 
		// to decide what outputs are subject to
		// closed loop control
		// (does not depend on ph control mode)
		//if(_ph_cmode==CONTROL_MODE_MANUAL){
			if(_velocity_cmode==CONTROL_MODE_MANUAL){
				correctThrusters=false;
			}else if(_velocity_cmode==CONTROL_MODE_CONSTANT){
				correctThrusters=true;
			}else if(_velocity_cmode==CONTROL_MODE_DEADBAND){
				correctThrusters=true;
				isDeadband=true;
			}else if(_velocity_cmode==CONTROL_MODE_OFFSET){
				correctThrusters=true;
			}else{
				throw new Exception("Invalid velocity control mode ["+_velocity_cmode+"] in velocity PID");
			}
		
		//}else if(_ph_cmode==CONTROL_MODE_CONSTANT ||
		//		 _ph_cmode==CONTROL_MODE_OFFSET){
		 /*
			if(_velocity_cmode==CONTROL_MODE_MANUAL){
				correctThrusters=false;
			}else if(_velocity_cmode==CONTROL_MODE_CONSTANT){
				correctThrusters=true;
			}else if(_velocity_cmode==CONTROL_MODE_DEADBAND){
				correctThrusters=true;
				isDeadband=true;
			}else if(_velocity_cmode==CONTROL_MODE_OFFSET){
				correctThrusters=true;
			}else{
				throw new Exception("Invalid velocity control mode ["+_velocity_cmode+"] in velocity PID");
			}
		 */
		//}else{
		//	throw new Exception("Invalid pH control mode ["+_ph_cmode+"] in pH PID");
		//}
		
		float thrusterCorrection=0.0f;
		double[] thrusterCommands=null;
		
		if(correctThrusters==true){
			// calculate thruster corrections if indicated
			if(isDeadband==true){
				// get the correction for deadband mode
				thrusterCorrection = getDeadBandCorrection();			
				_correction=applyConstraints(thrusterCorrection);				
				thrusterCommands=thrusterCommands(_correction);
			}else{
				// get correction for PID modes
				thrusterCorrection = getCorrection();
				_correction=applyConstraints(thrusterCorrection);				
				thrusterCommands=thrusterCommands(_correction);
			}
		}else{
			// or get the current value
			thrusterCommands=new double[2];
			Number fwdThrusterVel=_controlProcess.getSignal(SIG_FWD_THRUSTER_VEL);
			Number aftThrusterVel=_controlProcess.getSignal(SIG_AFT_THRUSTER_VEL);
			if(fwdThrusterVel!=null){
				thrusterCommands[FWD_THRUSTER_INDEX]=fwdThrusterVel.doubleValue();
			}else{
				if(_log4j.isDebugEnabled()){
				_log4j.error("*** fwd thruster velocity signal is null - using last valid value ["+_lastFwdThrusterCommand+"]");
				}
				fwdThrusterVel=new Double(_lastFwdThrusterCommand);
			}
			if(aftThrusterVel!=null){
				thrusterCommands[AFT_THRUSTER_INDEX]=aftThrusterVel.doubleValue();
			}else{
				if(_log4j.isDebugEnabled()){
				_log4j.error("*** aft thruster velocity signal is null - using last valid value ["+_lastAftThrusterCommand+"]");
				}
				aftThrusterVel=new Double(_lastAftThrusterCommand);
			}
			_correction=0.0;
			_rawCorrection=0.0;
		}
		
		// send commands to thrusters
		if(_thruster_amode==ACTUATION_ENABLED){
			try{
				// update thruster outputs and 
				// (contains control-mode-appropriate logic,
				// so won't actuate )
				updateThrusterOutputs(thrusterCommands);
			}catch (Exception e) {
				e.printStackTrace();
				// set _lastXcommand for debugging only
				// since updateOutput may fail if no device is present
				_lastFwdThrusterCommand=thrusterCommands[FWD_THRUSTER_INDEX];
				_lastAftThrusterCommand=thrusterCommands[AFT_THRUSTER_INDEX];
				// disable pump actuation?
			}			
		}else{
			// if no actuation, set lastXcommand to current value
			// (set above)
			_lastFwdThrusterCommand=thrusterCommands[FWD_THRUSTER_INDEX];
			_lastAftThrusterCommand=thrusterCommands[AFT_THRUSTER_INDEX];			
		}
		
		// prepare a data packet
		_dataPacket=getSamplePacket();
		PacketParser.Field[] parsedFields=_packetParser.parseFields(_dataPacket);

		// only shows data if log4j debug is enabled
		showParsedData(parsedFields);
		
		if(_doTurbinate){
			try{
				if(_turbinator==null){
					initTurbinator();
				}
				_turbinator.write((DevicePacket)_dataPacket,parsedFields);
			}catch (Exception e) {
				_log4j.info("exception while writing to turbinator in VEL_Responder:");
				e.printStackTrace();
			}
		}
		
		return (float)_correction;
	}		
	
	/** return a sensor data packet 
	 sub-classes should override
	 */
	public SensorDataPacket getSamplePacket(){
		try{
			_packetBuffer=getSampleBuffer();
			if(_dataPacket==null){
				_dataPacket=new SensorDataPacket(_attributes.isiID,_maxDataBytes);
				_dataPacket.setRecordType(1L);
			}
			_dataPacket.setDataBuffer(_packetBuffer.toString().getBytes());
			_dataPacket.setSystemTime(System.currentTimeMillis());
			_lastDataPacket=_dataPacket;
			return _dataPacket;
		}catch (Exception e) {
			if(_log4j.isDebugEnabled()){
				_log4j.debug("Exception in getSamplePacket:"+e);
				e.printStackTrace();
			}
			return null;
		}
	}
	
	/** Return a sample buffer with current process data.
	 sub-classes should call the base class to get the 
	 common data items.
	 */
	public synchronized StringBuffer getSampleBuffer()
	throws Exception{
		
		// creates packet buffer if it doesn't exist
		// initializes it and fills in common
		// packet fields
		super.getSampleBuffer();
		
		 _packetBuffer.append(_lastFwdThrusterCommand+_recordDelimiter);
		 _packetBuffer.append(_lastAftThrusterCommand+_recordDelimiter);
		/* 
		_packetBuffer.append(_lastFwdThrusterCommand+_recordDelimiter);
		_packetBuffer.append(_lastAftThrusterCommand+_recordDelimiter);
		_packetBuffer.append(_vxInt+_recordDelimiter);
		_packetBuffer.append(_vyInt+_recordDelimiter);
		_packetBuffer.append(_vxExt+_recordDelimiter);
		_packetBuffer.append(_vyExt+_recordDelimiter);
		_packetBuffer.append(_vmagInt+_recordDelimiter);
		_packetBuffer.append(_vhdgInt+_recordDelimiter);
		_packetBuffer.append(_vmagExt+_recordDelimiter);
		 _packetBuffer.append(_vhdgExt);
		 */
		return _packetBuffer;
	}
}