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
import org.mbari.siam.distributed.devices.ValveIF;
import org.mbari.siam.distributed.devices.Valve2WayIF;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.dataTurbine.Turbinator;

import org.mbari.siam.foce.devices.controlLoop.FOCEProcess;

public abstract class PH_Responder extends BaseResponder implements ProcessParameterIF{
	/** Log4j logger */
	protected static Logger _log4j = Logger.getLogger(PH_Responder.class);
	
	public static final String RECORD_HEADER="$PH_PID";
	
	protected static final int FWD_VALVE_INDEX=0;
	protected static final int AFT_VALVE_INDEX=1;
	protected static final int NUMBER_OF_VALVES=2;
	
	double _lastPumpCommand;
	double _lastPumpDelta;
	int _lastFwdValveCommand;
	int _lastAftValveCommand;
	double _vxInt, _vyInt,_vmagInt,_vhdgInt;
	double _vxExt, _vyExt,_vmagExt,_vhdgExt;
	double _phExternal,_phGradient;
	NumberFormat _decFormat;
	FOCEProcess foceProcess;
	
	public PH_Responder(FOCEProcess controlProcess, ControlLoopAttributes attributes) 
	throws Exception{
		super((ControlProcessIF)controlProcess, attributes);
		_log4j.debug("PH_Responder: called super");
		foceProcess=controlProcess;
		_log4j.debug("PH_Responder: setting record header");
		_recordType=RECORD_HEADER;
		_log4j.debug("PH_Responder: setting number format");
		_decFormat=NumberFormat.getInstance();
		_decFormat.setMaximumFractionDigits(5);
		_decFormat.setMinimumFractionDigits(3);
		_decFormat.setMinimumIntegerDigits(1);
		_decFormat.setGroupingUsed(false);
		_log4j.debug("PH_Responder: initializing");
		
		initialize();
	}
	
	/** initialize control loop */
	public synchronized void initialize(){
		super.initialize();
		_log4j.debug("PH_Responder.initialize: called super.initialize");
		_doTurbinate=_attributes.ph_pid_enable_turbinator;	
		_log4j.debug("PH_Responder.initialize: creating PH_ResponseParser");
		_packetParser=new PH_ResponseParser(_attributes.registryName+" PID_PH",_recordDelimiter);
		_log4j.debug("PH_Responder.initialize: setting up buffers and packets");
		_packetBuffer=new StringBuffer();
		_dataPacket=new SensorDataPacket(_attributes.isiID,_maxDataBytes);
		_dataPacket.setRecordType(1L);
		
		if(_doTurbinate){
			initTurbinator();
		}
		_log4j.debug("PH_Responder.initialize: done");
	}
	
	protected void initTurbinator(){
		
		// set up a data turbine source 
		// using SIAM Turbinator and SensorDataPacket (required by Turbinator)
		try{
			if(_packetParser==null){
				_packetParser=new PH_ResponseParser(_attributes.registryName+" PID_PH",_recordDelimiter);
			}
			if(_turbinator!=null){
				_turbinator.close();
			}
			_turbinator=new Turbinator(_packetParser,
									   "PID_PH",
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
	
	/** Apply physical/operational constraints to the raw pH signal correction from the PID loop.
	 (The actuation method(s) may also apply additional contraints)
	 */
	protected float applyConstraints(double correction) throws Exception{
		double limitedValue=correction;
		
		// check for overflow vs max/min float
		if(Math.abs(correction) > Float.MAX_VALUE){
			_log4j.warn("pH correction overflow - set to upper limit ["+correction+" / "+Float.MAX_VALUE+"]");
			if(correction<0){
				limitedValue = -Float.MAX_VALUE;
			}else{
				limitedValue = Float.MAX_VALUE;
			}
		}
		else if(Math.abs(correction) < Float.MIN_VALUE){
			if(_log4j.isDebugEnabled()){
				_log4j.debug("pH correction overflow - set to lower limit ["+correction+" / "+Float.MIN_VALUE+"]");
			}
			if(correction<0){
				limitedValue = -Float.MIN_VALUE;
			}else{
				limitedValue = Float.MIN_VALUE;
			}
		}
		
		// check vs configured limits
		double maxCorrection=_attributes.ph_max_correction;
		if(Math.abs(correction) > maxCorrection) {
			if(correction<0){
				limitedValue = -maxCorrection;
			}else{
				limitedValue = maxCorrection;
			}
			
		}
		return (float)limitedValue;
	}
	
	/** get any signals needed */
	protected synchronized void updateInputs()
	throws Exception{
		try{
			// use temporary variables so that if any fail,
			// they won't leave the loop in a bad state
			
			// get process pH value (current output)
			float internalPH=_controlProcess.getSignal(SIG_PH_INT_FILT).floatValue();
			// get setpoint (desired value, taking into accound constant or offset mode)
			float phSetpoint=_controlProcess.getParameter(PARAM_PH_SETPOINT).floatValue();
			// get external pH value
			double phExternal=_controlProcess.getSignal(SIG_PH_EXT_FILT).doubleValue();
			// get gradient signal (difference between forward and aft sensors)
			double phGradient=_controlProcess.getSignal(SIG_PH_GRAD).doubleValue();
			// get error signal (difference between output and setpoint)
			float sigErr=_controlProcess.getSignal(SIG_PH_ERR).floatValue();
			// get current configured offset value 
			double offset=_controlProcess.getParameter(PARAM_PH_OFFSET).floatValue();//_attributes.ph_offset;
			
			// transfer temp variables to loop variables
			_processValue=internalPH;
			_setPoint=phSetpoint;
			_phExternal=phExternal;
			_phGradient=phGradient;
			_error=sigErr;
			_offset=offset;
			
			/*
			 // get current configured PID parameters
			 _Kp=_attributes.ph_pid_Kp;
			 _Ki=_attributes.ph_pid_Ki;
			 _Kd=_attributes.ph_pid_Kd;
			 _scaleFactor=_attributes.ph_pid_scale_factor;
			 _max_ki=_attributes.ph_pid_max_ki;
			 */
			_ph_cmode=_attributes.ph_control_mode;
			_ph_rmode=_attributes.ph_response_mode;
			_velocity_cmode=_attributes.velocity_control_mode;
			_esw_valve_amode=_attributes.esw_valve_actuation;
			_esw_pump_amode=_attributes.esw_pump_actuation;
			_thruster_amode=_attributes.esw_valve_actuation;
			_doTurbinate=_attributes.ph_pid_enable_turbinator;				
			
			// do these last so they don't break the loop if they fail
			_vxInt=_controlProcess.getSignal(SIG_VH2O_INT_X_FILT).doubleValue();
			_vyInt=_controlProcess.getSignal(SIG_VH2O_INT_Y_FILT).doubleValue();
			_vxExt=_controlProcess.getSignal(SIG_VH2O_EXT_X_FILT).doubleValue();
			_vyExt=_controlProcess.getSignal(SIG_VH2O_EXT_Y_FILT).doubleValue();
			_vmagInt=_controlProcess.getSignal(SIG_VH2O_INT_MAG_FILT).doubleValue();
			_vhdgInt=_controlProcess.getSignal(SIG_VH2O_INT_DIR_FILT).doubleValue();
			_vmagExt=_controlProcess.getSignal(SIG_VH2O_EXT_MAG_FILT).doubleValue();
			_vhdgExt=_controlProcess.getSignal(SIG_VH2O_EXT_DIR_FILT).doubleValue();
			
			
		}catch(Exception e){
			_log4j.error("update - error getting signals");
			e.printStackTrace();
			throw e;
		}
	}
	
	/** convert the pH signal correction to pump motor input 
		phTarget is either an offset or setpoint, depending on 
		the value of mode (CONTROL_MODE_CONSTANT, CONTROL_MODE_OFFSET).
	 */
	protected double pumpCommand(double phCorrection){
		double dph=0.0;//Math.abs(phTarget)-phCorrection;
		double phTarget=0.0;
		/*

		switch (mode) {
			case CONTROL_MODE_CONSTANT:
				dph=phCorrection;
				break;
			case CONTROL_MODE_OFFSET:
				dph=Math.abs(phTarget)-phCorrection;
			default:
				break;
		}
		 */
		double pumpRate=foceProcess.dph2rate(phCorrection);
		double pumpCommand=foceProcess.volume2rpm(pumpRate); //foceProcess.volume2rpm(pumpRate);
		
		// apply constraints...
		if( (pumpCommand <= 0) ){
			// don't run pump in reverse
			if(_log4j.isDebugEnabled()){
				_log4j.debug("ESW pump command <= 0 ["+_decFormat.format(pumpCommand)+"] - using 0");
			}
			pumpCommand=0;
		}/*else if( (pumpCommand < (0.5*_attributes.esw_pump_min_rpm)) ){
			if(_log4j.isDebugEnabled()){
				_log4j.debug("ESW pump command < min/2 ["+_decFormat.format(pumpCommand)+"] - using 0");
			}
			pumpCommand=0;
		}*/else if(pumpCommand<_attributes.esw_pump_min_rpm){
			/*
			if(_log4j.isDebugEnabled()){
				_log4j.debug("ESW pump set < min ["+_decFormat.format(pumpCommand)+"<"+_decFormat.format(_attributes.esw_pump_min_rpm)+"] - using min");
			}
			pumpCommand=_attributes.esw_pump_min_rpm;
			 */
			if(_log4j.isDebugEnabled()){
				_log4j.debug("ESW pump set < min ["+_decFormat.format(pumpCommand)+"<"+_decFormat.format(_attributes.esw_pump_min_rpm)+"] - using 0");
			}
			pumpCommand=0;
		}
		
		if(pumpCommand>_attributes.esw_pump_max_rpm){
			if(_log4j.isDebugEnabled()){
				_log4j.debug("ESW pump set > max ["+_decFormat.format(pumpCommand)+"<"+_decFormat.format(_attributes.esw_pump_max_rpm)+"] - using max");
			}
			pumpCommand=_attributes.esw_pump_max_rpm;
		}
		
		double maxChange=Math.abs(_attributes.ph_eswpump_max_change);
		if(Math.abs(_lastPumpCommand-pumpCommand) > maxChange){
			if(_log4j.isDebugEnabled()){
				_log4j.debug("limiting ESW pump change ["+_decFormat.format(maxChange)+"<"+_decFormat.format(pumpCommand)+"]");
			}
			pumpCommand=((_lastPumpCommand-pumpCommand)<0?_lastPumpCommand+maxChange:_lastPumpCommand-maxChange);
		}
		
		if(_log4j.isDebugEnabled()){
			_log4j.debug(" phTarget:"+_decFormat.format(phTarget)+
						 " phCorrection (pH units):"+_decFormat.format(phCorrection)+
						 " dph:"+_decFormat.format(dph)+
						 " pumpRate (l/min):"+_decFormat.format(pumpRate)+
						 " command (rpm):"+_decFormat.format(pumpCommand));
		}
		return pumpCommand;
	}
	
	
	/** Get command updates to CO2 delivery valves */
	protected int[] valveCommands() throws Exception{
		
		ControlOutputIF fwdValveOutput=(ControlOutputIF)(_controlProcess.getOutput(OUTPUT_FWD_ESW_VALVE));
		ControlOutputIF aftValveOutput=(ControlOutputIF)(_controlProcess.getOutput(OUTPUT_AFT_ESW_VALVE));
		
		ValveIF fwdValve=null;
		ValveIF aftValve=null;
		if(fwdValveOutput!=null && aftValveOutput!=null){
			fwdValve=(ValveIF)fwdValveOutput.getDevice();
			aftValve=(ValveIF)aftValveOutput.getDevice();
		}
		
		int fwdOpen=1;
		int fwdClose=0;
		int aftOpen=1;
		int aftClose=0;
		if(fwdValve!=null){
			fwdOpen=fwdValve.getFunctionMap(Valve2WayIF.OPEN_FUNC);
			fwdClose=fwdValve.getFunctionMap(Valve2WayIF.CLOSE_FUNC);
		}
		if(aftValve!=null){
			aftOpen=fwdValve.getFunctionMap(Valve2WayIF.OPEN_FUNC);
			aftClose=fwdValve.getFunctionMap(Valve2WayIF.CLOSE_FUNC);
		}
		
		
		int[] commands=new int[NUMBER_OF_VALVES];
		
		commands[FWD_VALVE_INDEX]=fwdClose;
		commands[AFT_VALVE_INDEX]=aftClose;
		
		if(_log4j.isDebugEnabled()){
			_log4j.debug("getting direction");
		}
		double currentDirection=foceProcess.getSignal(SIG_VH2O_INT_DIR_FILT).doubleValue();
		if(_log4j.isDebugEnabled()){
			_log4j.debug("getting direction is ["+_decFormat.format(currentDirection)+"]");
		}
		
		if(currentDirection<=0){
			commands[FWD_VALVE_INDEX]=fwdClose;
			commands[AFT_VALVE_INDEX]=aftOpen;
		}else{
			commands[FWD_VALVE_INDEX]=fwdOpen;
			commands[AFT_VALVE_INDEX]=aftClose;
		}
		return commands;
	}
	
	/** Send command updates to inputs (CO2 delivery pump) */
	protected void updateValveOutputs(int[] valveCommands) throws Exception{
		if(_log4j.isDebugEnabled()){
			_log4j.debug("updateOutputs - getting valve outputs ");
		}
		ControlOutputIF fwdValveOutput=_controlProcess.getOutput(OUTPUT_FWD_ESW_VALVE);
		ControlOutputIF aftValveOutput=_controlProcess.getOutput(OUTPUT_AFT_ESW_VALVE);
		
		// send to fwd valve...
		fwdValveOutput.setOutputValue(valveCommands[FWD_VALVE_INDEX]);
		_lastFwdValveCommand=valveCommands[FWD_VALVE_INDEX];
		// send to aft valve...
		aftValveOutput.setOutputValue(valveCommands[AFT_VALVE_INDEX]);
		_lastAftValveCommand=valveCommands[AFT_VALVE_INDEX];
	}
	
	/** Send command updates to inputs (CO2 delivery pump) */
	protected void updatePumpOutputs(double pumpCommand) throws Exception{
		
		// check valve outputs before actuating pump...
		// stop pump if valves closed...
		
		if(_log4j.isDebugEnabled()){
			_log4j.debug("updateOutputs - getting pump outputs ");
		}
		ControlOutputIF motorOutput=_controlProcess.getOutput(OUTPUT_ESW_PUMP_VELOCITY);
		
		if(_log4j.isDebugEnabled()){
			_log4j.debug("updateOutputs - setting pump outputs ["+pumpCommand+" (rpm)]");
		}
		motorOutput.setOutputValue(pumpCommand);
		_lastPumpCommand=pumpCommand;
	}
	
	
	/** update output: where the rubber is told to meet the road (update control loop) 
	 */
	public synchronized float update() throws Exception{
		
		updateInputs();
		
		boolean correctPump=false;
		boolean correctValves=false;
		
		// implents the control-mode selection matrix 
		// to decide what outputs are subject to
		// closed loop control
		if(_ph_cmode==CONTROL_MODE_MANUAL){
			if(_velocity_cmode==CONTROL_MODE_MANUAL){
				correctPump=false;
				correctValves=false;
			}else if(_velocity_cmode==CONTROL_MODE_CONSTANT){
				correctPump=false;
				correctValves=false;				
			}else if(_velocity_cmode==CONTROL_MODE_DEADBAND){
				correctPump=false;
				correctValves=true;
			}else if(_velocity_cmode==CONTROL_MODE_OFFSET){
				correctPump=false;
				correctValves=true;
			}else{
				throw new Exception("Invalid velocity control mode ["+_velocity_cmode+"] in pH PID");
			}
		}else if(_ph_cmode==CONTROL_MODE_CONSTANT || 
				 _ph_cmode==CONTROL_MODE_OFFSET){
			if(_velocity_cmode==CONTROL_MODE_MANUAL){
				correctPump=true;
				correctValves=false;
			}else if(_velocity_cmode==CONTROL_MODE_CONSTANT){
				correctPump=true;
				correctValves=false;
			}else if(_velocity_cmode==CONTROL_MODE_DEADBAND){
				correctPump=true;
				correctValves=true;
			}else if(_velocity_cmode==CONTROL_MODE_OFFSET){
				correctPump=true;
				correctValves=true;
			}else{
				throw new Exception("Invalid velocity control mode ["+_velocity_cmode+"] in pH PID");
			}
		}else{
			throw new Exception("Invalid pH control mode ["+_ph_cmode+"] in pH PID");
		}
		
		float phCorrection=0.0f;
		double pumpCommand=0.0;
		int[] valveCommands=null;
		
		if(_log4j.isDebugEnabled()){
			_log4j.debug(" vcmode: "+foceProcess.modeName(_velocity_cmode)+
						 " pcmode:"+foceProcess.modeName(_ph_cmode)+
						 " prmode:"+foceProcess.modeName(_ph_rmode)+
						 " doPump:"+correctPump+
						 " doValves:"+correctValves+
						 " vamode:"+foceProcess.modeName(_esw_valve_amode)+
						 " pamode:"+foceProcess.modeName(_esw_pump_amode));
		}
		if(correctPump==true){
			// calculate pump corrections if indicated
			phCorrection = getCorrection();
			_correction=applyConstraints(phCorrection);
			/*
			double phTarget=0.0;
			
			switch (_ph_cmode) {
				case CONTROL_MODE_CONSTANT:
					phTarget=(double)(_processValue-_setPoint);
					break;
				case CONTROL_MODE_OFFSET:
					phTarget=(double)(_offset);
					break;
				default:
					break;
			}
			 */
			pumpCommand = pumpCommand(_correction);			
		}else{
			// or get the current value
			Number eswPumpSpeed=_controlProcess.getSignal(SIG_ESW_PUMP_VEL);
			if(eswPumpSpeed!=null){
				pumpCommand=eswPumpSpeed.doubleValue();
			}else{
				if(_log4j.isDebugEnabled()){
					_log4j.error("ESW pump velocity signal is null - using last valid value ["+_lastPumpCommand+"]");
				}
				pumpCommand=_lastPumpCommand;
			}
			_correction=0.0;
			_rawCorrection=0.0;
		}
		
		
		if(correctValves==true){
			// calculate valve corrections if indicated
			valveCommands=valveCommands();			
		}else{
			// or get the current value
			valveCommands=new int[NUMBER_OF_VALVES];
			Number fwdValveState=_controlProcess.getSignal(SIG_ESW_FWD_VALVE_STATE);
			Number aftValveState=_controlProcess.getSignal(SIG_ESW_AFT_VALVE_STATE);
			if(fwdValveState!=null && aftValveState!=null){
				valveCommands[FWD_VALVE_INDEX]=fwdValveState.intValue();
				valveCommands[AFT_VALVE_INDEX]=aftValveState.intValue();
			}else{
				if(_log4j.isDebugEnabled()){
					_log4j.error("can't read valve state (valve correction disabled): valve input is null - using calculated value");
				}
				valveCommands=valveCommands();			
				if(_log4j.isDebugEnabled()){
					_log4j.error("fwd valve:"+valveCommands[FWD_VALVE_INDEX]+" aft valve:"+valveCommands[AFT_VALVE_INDEX]);
				}
			}
		}
		
		// send commands to valves (before pump)
		if(_esw_valve_amode==ACTUATION_ENABLED){
			try{
				// update valve outputs...
				updateValveOutputs(valveCommands);
			}catch (Exception e) {
				e.printStackTrace();
				// set _lastXcommand for debugging only
				// since updateOutput may fail if no device is present
				_lastFwdValveCommand=valveCommands[FWD_VALVE_INDEX];
				_lastAftValveCommand=valveCommands[AFT_VALVE_INDEX];
				// disable pump actuation?
			}			
		}else{
			// if no actuation, set lastXcommand to current value
			// (set above)
			_lastFwdValveCommand=valveCommands[FWD_VALVE_INDEX];
			_lastAftValveCommand=valveCommands[AFT_VALVE_INDEX];
		}
		
		// send commands to pump
		if(_esw_pump_amode==ACTUATION_ENABLED){
			try{
				// update ESW pump velocity
				updatePumpOutputs(pumpCommand);
			}catch (Exception e) {
				e.printStackTrace();
				// set _lastXcommand for debugging only
				// since updateOutput may fail if no device is present
				_lastPumpCommand=pumpCommand;
			}
		}else{
			// if no actuation, set lastXcommand to current value
			// (set above)
			_lastPumpCommand=pumpCommand;
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
				_log4j.info("exception while writing to turbinator in PH_Responder:");
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
		
		// add the parameters for this PID (and parser)
		
		_packetBuffer.append(_lastPumpCommand+_recordDelimiter);
		_packetBuffer.append(_lastFwdValveCommand+_recordDelimiter);
		_packetBuffer.append(_lastAftValveCommand);		 
		
		/* ORIGINAL
		 _packetBuffer.append(_phExternal+_recordDelimiter);
		 _packetBuffer.append(_lastPumpCommand+_recordDelimiter);
		 _packetBuffer.append(_lastFwdValveCommand+_recordDelimiter);
		 _packetBuffer.append(_lastAftValveCommand+_recordDelimiter);
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