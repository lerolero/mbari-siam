/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.elmo.thruster;

import java.util.Vector;
import java.util.Iterator;
import java.text.ParseException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.DecimalFormat;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import org.apache.log4j.Logger;

import org.mbari.siam.registry.InstrumentDataListener;
import org.mbari.siam.registry.InstrumentRegistry;
import org.mbari.siam.registry.RegistryEntry;

import org.mbari.siam.core.BaseInstrumentService;
import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.core.NodeProperties;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.core.InstrumentPort;
import org.mbari.siam.core.SerialInstrumentPort;
import org.mbari.siam.core.AnalogInstrumentPort;
import org.mbari.siam.core.ServiceSandBox;

import org.mbari.siam.utils.StreamUtils;
import org.mbari.siam.utils.PrintfFormat;
import org.mbari.siam.utils.StopWatch;

import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.Parent;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.Velocity;
import org.mbari.siam.distributed.CommsMode;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.Summarizer;
import org.mbari.siam.distributed.SummaryPacket;
import org.mbari.siam.distributed.Safeable;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.PropertyException;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.InvalidDataException;
import org.mbari.siam.distributed.devices.ElmoIF;
import org.mbari.siam.distributed.devices.ElmoThrusterIF;
import org.mbari.siam.foce.devices.elmo.base.*;

import org.mbari.siam.foce.deployed.IOMapper;

public class ElmoThruster extends ElmoService implements ElmoThrusterIF
{
		
	/** Log4j logger */
	protected static Logger _log4j = Logger.getLogger(ElmoThruster.class);
		
	public ElmoThruster()throws RemoteException{
		super();
		_attributes = new ElmoThrusterAttributes(this);
	}
	
	/** required by PolledInstrumentService */
	protected ScheduleSpecifier createDefaultSampleSchedule() 
	throws ScheduleParseException
	{
		// Sample every 30 seconds by default
		return new ScheduleSpecifier(86400000);
	}
	
	/** Override default initializeInstrument.
	 */
	protected void initializeInstrument() throws InitializeException, Exception
	{
		super.initializeInstrument();
		
		//power on (controlled differently from other SIAM services on FOCE)
		switchElmoPower(true);

		// initialize controller
		this.initializeController();
		// home to position (Hall stop)?
	}		
	
	
	///////////////////////////////////////////////
	//        ElmoIF Implementations       //
	///////////////////////////////////////////////
	
	/** Initialize motor controller
	 */
	
	public void initializeController() 
	throws TimeoutException, IOException,Exception,RemoteException
	{
		super.initializeController();
		this.initializeController(((ElmoThrusterAttributes)_attributes).serialMode,
								  ((ElmoThrusterAttributes)_attributes).countsPerRevolution,
								  ((ElmoThrusterAttributes)_attributes).gearRatio,
								  ((ElmoThrusterAttributes)_attributes).unitMode,
								  ((ElmoThrusterAttributes)_attributes).motorAcceleration,
								  ((ElmoThrusterAttributes)_attributes).motorDeceleration,
								  ((ElmoThrusterAttributes)_attributes).motorStopDeceleration);

		// set speed limits for this application
		_elmo.setMinMotorCounts(((ElmoThrusterAttributes)_attributes).minMotorCounts);
		_elmo.setMaxMotorCounts(((ElmoThrusterAttributes)_attributes).maxMotorCounts);
		_elmo.setReferenceSpeed(-450,450);
		_elmo.setReferenceLimit(-1500,1500);

	}
	
	////////////////////////////////////////
	//         Turn Sensor Control       //
	////////////////////////////////////////
	
	/*
	 Configure the digital I/O to trigger the Homing function
	 and configure the Homing function to set a digital 
	 output.
	 */
	public void setTurnsSensorEnable(boolean value) {
		_elmo.setTurnsSensorEnable(value);
	}
	
	/** Return number of times the turns sensor has been triggered since the last reset */	
	public long getTSTriggerCount(){
		return _elmo.getTSTriggerCount();
	}
	
	/** Return the elapsed time since last turns sensor trigger (msec) */	
	public long getTSElapsedMsec(){
		return _elmo.getTSElapsedMsec();
	}
	/** Return the turns sensor state */	
	public int getTSState(){
		return _elmo.getTSState();
	}
	/** return a mnemonic for turns sensor state */
	public String getTSStateName(){
		return _elmo.getTSStateName();
	}
	/** return thruster sample string */
	public String getThrusterSampleMessage()
	throws TimeoutException,IOException, Exception{
		return _elmo.getThrusterSampleMessage();	
	} 
	
	
	/**
	 * Override base class readSample (defined in BaseInstrumentService)
	 *
	 * @param sample output buffer
	 */
	protected int readSample(byte[] sample) 
	throws TimeoutException,IOException, Exception 
	{
		
		byte[] smsg = getThrusterSampleMessage().getBytes();
		
		int len=(smsg.length<=sample.length?smsg.length:sample.length);
		for(int i=0;i<len;i++){
			sample[i]=smsg[i];
		}
		
		if(((ElmoThrusterAttributes)_attributes).undervoltageRecovery==true){
			try{
				int faultReg=getFaultRegister();
				if(faultReg==Elmo.MF_UNDER_VOLTAGE){

					// set start speed
					int startSpeed=orpm2counts((double)((ElmoThrusterAttributes)_attributes).uvr_start_orpm);
					// velocity increment (counts)
					int increment=orpm2counts((double)((ElmoThrusterAttributes)_attributes).uvr_step_inc_orpm);
					// time per increment
					long stepTimeMsec=((ElmoThrusterAttributes)_attributes).uvr_step_millisec;
					
					// don't let others interfere
					// as we restart the motor
					synchronized (this) {
						// get last velocity command
						int lastVelocity=getJoggingVelocity();
						
						// if we weren't going too fast
						// just re-send the last command
						if(Math.abs(lastVelocity) <= Math.abs(((ElmoThrusterAttributes)_attributes).uvr_threshold_orpm) ){
							startSpeed=lastVelocity;
						}
						
						// try to bring it back up to speed
						stepStart(startSpeed,lastVelocity,increment,stepTimeMsec);						
					}
				}
			}catch (Exception e) {
				// print it and let it go
				_log4j.warn("Exception while recovering from undervoltage:"+e);
			}
		}
		
		return len;
	}
	
	/** Brings up motor in step-wise fashion.
		For recovering from under-voltage condition.
		Speeds are in counts.
	 */
	protected void stepStart(int startSpeed, int endSpeed, int increment, long stepTime)
	throws Exception{
		
		// initialize last command
		int lastCommand=startSpeed;
		
		// step through speeds
		for(int i=startSpeed;i<=endSpeed;i+=increment){
			
			validateSpeed(i);
			
			// command the next speed
			jog(i);
			// wait the prescribed time
			delay(stepTime);
			// record the last command sent
			lastCommand=i;
		}
		// command the desired end speed
		// if we haven't already
		if(lastCommand!=endSpeed){
			jog(endSpeed);
		}
	}
	
	
	/** Return a PacketParser. */
	public PacketParser getParser() throws NotSupportedException{
		return new ElmoPacketParser(_attributes.registryName);
	}
	
	/** Parse a SensorDataPacket into a double[] array (used by infrastructure)  */
	public Object parseDataPacket(SensorDataPacket pkt) throws InvalidDataException
	{
		try{
			PacketParser parser=getParser();
			// return PacketParser.Field[]
			return parser.parseFields(pkt);
		}catch(NotSupportedException e){
			_log4j.error(e.toString());
		}catch(ParseException p){
			throw new InvalidDataException("ParseException caught: "+p.toString());
		}
		return null;
	}
	
	/** Perform any instrument-specific shutdown actions and optionally
	 return a human-readable message (e.g. noting caveats, status, etc);
	 the returned message is purely for human operator.
	 (override BaseInstrumentService)
	 */
	protected String shutdownInstrument()
	throws Exception {
		
		if(_log4j.isDebugEnabled()){
		_log4j.debug("Elmo shutdownInstrument: calling super()");
		}
		String retval=super.shutdownInstrument();
		if(_log4j.isDebugEnabled()){
		_log4j.debug("Elmo shutdownInstrument: getting IOMapper");
		}
		retval+=" "+switchElmoPower(false);
		return retval;
	}
	
	/** Enable or disable power to Elmos
	 Power is switched by memory mapped IO ports on
	 an A/D card (not a standard power port) using
	 foceio.
	 
	 @param state true for on, false for off
	 
	 @return String OK on success, null or error message otherwise
	 
	 */
	protected String switchElmoPower(boolean state){
		
		try{
			if(_attributes.powerPolicy==PowerPolicy.NEVER){
				if(_log4j.isDebugEnabled()){
				_log4j.debug("Power Policy Never - not switching power");
				}
				return "OK";
			}
			// get an IOMapper instance
			IOMapper iom=IOMapper.getInstance();
			// use it to toggle the power bit for this Elmo
			if(_log4j.isDebugEnabled()){
			_log4j.debug("switchElmoPower: calling switchElmoBit ["+((ElmoThrusterAttributes)_attributes).powerBit+"]");
			}
			return iom.transact("switchElmoBit "+((ElmoThrusterAttributes)_attributes).powerBit+" "+(state?1:0)+"\n");
		}catch(IOException e){
			if(_log4j.isDebugEnabled()){
			_log4j.debug("switchElmoPower: caught exception:");
			}
			e.printStackTrace();
		}
		return null;
	}
	
	public class ElmoThrusterAttributes extends ElmoService.Attributes {
		/** The Elmos are not powered via the typical 
		 PowerPort model (via a FOCERelayBoard). 
		 They are wired through digital IO ports on
		 the DMM32 card and controlled via foceio.
		 
		 powerBit is used to assign the
		 digital IO bit; currently only the top two
		 bits are used (6 and 7, i.e., 0x80 and 0x40).
		 
		 This is used to turn off the bit
		 at shutdown only. It should be set in the
		 properties file. By default, the mask is
		 invalid and will cause an invalid property
		 exception if unset.
		 */
		int powerBit=-1;

		/** default number of encoder samples to read
		 when measuring velocity. Will default to 1
		 if set <=0.
		 */
		int encoderSamples=1;
		
		/** number of ms before turn sensor times out if not triggered */
		long turnSensorTimeoutMS=10000L;
		/** digital I/O bit used for turns sensor (hall switch) input */
		int turnSensorInputBit=Elmo.DIGITAL_INPUT_BIT3;
		/** digital I/O bit used for turns sensor output (sets this bit when turnSensorInputBit is triggered) */
		int turnSensorOutputBit=Elmo.DIGITAL_INPUT_BIT1;

		/** Enable checking and recovery for undervoltage (disabled by default) */
		boolean undervoltageRecovery=false;
		/** Under-voltage recover: min speed (output RPM) to just re-issue command (ramp speed if above) */
		int uvr_threshold_orpm=30;
		/** Under-voltage recover: step-start recovery start speed (output RPM) */
		int uvr_start_orpm=30;
		/** Under-voltage recover: step-start recovery speed increment (output RPM) */
		int uvr_step_inc_orpm=3;
		/** Under-voltage recover: step-start recovery time increment */
		long uvr_step_millisec=15000;
		
		/**
		 * Constructor, with required InstrumentService argument
		 */
		public ElmoThrusterAttributes(DeviceServiceIF service) {
			super(service);
			
			// override base attribute default initialization
			unitMode=Elmo.MODE_SPEED;
			motorAcceleration=100;
			motorDeceleration=100;
			motorStopDeceleration=1000;
			gearRatio=67.5;
			minMotorCounts=-405;
			maxMotorCounts=405;
		}
		
		/**
		 * Called when specified attribute has been found. Throw
		 * InvalidPropertyException if specified attribute has invalid value.
		 * Note that the ServiceAttributes base class automatically validates
		 * the value type before setAttributeCallback() is invoked; so this
		 * method needs only to validate the value.
		 * 
		 * @param attributeName
		 *            name of parsed attribute
		 */
		protected void setAttributeCallback(String attributeName, String valueString)
		throws InvalidPropertyException {
			if(attributeName.equals("uvr_threshold")){
				validateInt(valueString,0,30,true,true);
			}else if(attributeName.equals("uvr_start")){
				validateInt(valueString,0,30,true,true);
			}else if(attributeName.equals("uvr_step_inc")){
				validateInt(valueString,1,30,true,true);
			}else if(attributeName.equals("uvr_step_millisec")){
				validateLong(valueString,1,10000,true,true);
			}
		}
		
		/** validate whether numeric attribute (as String) value falls inside/outside specified range. 
		 @param valueString number to validate (String)
		 @param min range minimum
		 @param max range maximum
		 @param inside if true, compare to inside of range; otherwise compare to outside
		 @param includeEnds include endpoints (min/max) in validation
		 
		 @throws InvalidPropertyException if number is part of the specified range
		 
		 */
		public void validateInt(String valueString, int min, int max, boolean inside, boolean includeEnds) 
		throws InvalidPropertyException{
			int value;
			try{
				value=Integer.parseInt(valueString);
			}catch (Exception e) {
				throw new InvalidPropertyException("parse error for "+valueString);
			}
			if(inside){
				if(includeEnds){
					if((value<min) || (value>max)){
						throw new InvalidPropertyException("range error for "+valueString);
					}		
				}else{
					if((value<=min) || (value>=max)){
						throw new InvalidPropertyException("range error for "+valueString);
					}						
				}
			}else{
				if(includeEnds){
					if((value>min) || (value<max)){
						throw new InvalidPropertyException("range error for "+valueString);
					}		
				}else{
					if((value>=min) || (value<=max)){
						throw new InvalidPropertyException("range error for "+valueString);
					}						
				}
			}
		}
		/** validate whether numeric attribute (as String) value falls inside/outside specified range. 
		 @param valueString number to validate (String)
		 @param min range minimum
		 @param max range maximum
		 @param inside if true, compare to inside of range; otherwise compare to outside
		 @param includeEnds include endpoints (min/max) in validation
		 
		 @throws InvalidPropertyException if number is part of the specified range
		 
		 */
		public void validateLong(String valueString, long min, long max, boolean inside, boolean includeEnds) 
		throws InvalidPropertyException{
			long value;
			try{
				value=Long.parseLong(valueString);
			}catch (Exception e) {
				throw new InvalidPropertyException("parse error for "+valueString);
			}
			if(inside){
				if(includeEnds){
					if((value<min) || (value>max)){
						throw new InvalidPropertyException("range error for "+valueString);
					}		
				}else{
					if((value<=min) || (value>=max)){
						throw new InvalidPropertyException("range error for "+valueString);
					}						
				}
			}else{
				if(includeEnds){
					if((value>min) || (value<max)){
						throw new InvalidPropertyException("range error for "+valueString);
					}		
				}else{
					if((value>=min) || (value<=max)){
						throw new InvalidPropertyException("range error for "+valueString);
					}						
				}
				
			}
		}
		
		/**
		 * Throw InvalidPropertyException if any invalid attribute values found
		 */
		public void checkValues() 
		throws InvalidPropertyException 
		{
			super.checkValues();
			
			if(powerBit<0 || powerBit>7){
				throw new InvalidPropertyException("Invalid powerBit: "+powerBit+" valid values are 0-7");		
			}
			
			if(!(mode.equalsIgnoreCase(OPEN_LOOP) || mode.equalsIgnoreCase(CLOSED_LOOP)) ){
				throw new InvalidPropertyException("Invalid mode: "+mode+" valid values are "+OPEN_LOOP+"||"+CLOSED_LOOP);
			}
		}
	}	
	
}
