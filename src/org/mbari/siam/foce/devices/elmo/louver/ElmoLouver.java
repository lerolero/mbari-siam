/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.elmo.louver;

import java.util.Vector;
import java.util.Iterator;
import java.text.ParseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.DecimalFormat;

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
import org.mbari.siam.distributed.devices.ElmoLouverIF;
import org.mbari.siam.foce.devices.elmo.base.*;

import org.mbari.siam.foce.deployed.IOMapper;

public class ElmoLouver extends ElmoService implements ElmoLouverIF
{
		
	/** Log4j logger */
	protected static Logger _log4j = Logger.getLogger(ElmoLouver.class);
		
	public ElmoLouver()throws RemoteException{
		super();
		_attributes = new ElmoLouverAttributes(this);
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
		
		// though thrusters power on in initialize instrument
		// louvers do not because the power will be manually turned on before the service is started
		
		this.initializeController();
		// home to position (Hall stop)
		// if homePosition attribute >= 0 
		if(((ElmoLouverAttributes)_attributes).homePosition>=0){
			home(((ElmoLouverAttributes)_attributes).homePosition,
				 true,
				 ((ElmoLouverAttributes)_attributes).homePositionCounter,
				 ((ElmoLouverAttributes)_attributes).homingSpeedLoCounts,
				 ((ElmoLouverAttributes)_attributes).homingSpeedHiCounts);
		}
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
		
		 // note use of _attributes (base class) vs ((ElmoLouverAttributes)_attributes) (subclass)
		this.initializeController(  ((ElmoLouverAttributes)_attributes).serialMode,
								    ((ElmoLouverAttributes)_attributes).countsPerRevolution,
								    ((ElmoLouverAttributes)_attributes).gearRatio,
									((ElmoLouverAttributes)_attributes).unitMode,
									((ElmoLouverAttributes)_attributes).motorAcceleration,
									((ElmoLouverAttributes)_attributes).motorDeceleration,
									((ElmoLouverAttributes)_attributes).motorStopDeceleration);
		
		//_elmo.setDisplacementRPU(((ElmoLouverAttributes)_attributes).displacementRPU);		
		_elmo.setDIHallBit(Elmo.DIGITAL_INPUT_BIT0,((ElmoLouverAttributes)_attributes).DIGITAL_INPUT_HALL_BIT0);		
		_elmo.setDIHallBit(Elmo.DIGITAL_INPUT_BIT1,((ElmoLouverAttributes)_attributes).DIGITAL_INPUT_HALL_BIT1);		
		_elmo.setDIHallBit(Elmo.DIGITAL_INPUT_BIT2,((ElmoLouverAttributes)_attributes).DIGITAL_INPUT_HALL_BIT2);		
		_elmo.setDIHallBit(Elmo.DIGITAL_INPUT_BIT3,((ElmoLouverAttributes)_attributes).DIGITAL_INPUT_HALL_BIT3);	
		
		_elmo.setDigitalInputFunction(((ElmoLouverAttributes)_attributes).DIGITAL_INPUT_HALL_BIT0,Elmo.INPUT_LOGIC_GP_HI);
		_elmo.setDigitalInputFunction(((ElmoLouverAttributes)_attributes).DIGITAL_INPUT_HALL_BIT1,Elmo.INPUT_LOGIC_GP_HI);
		_elmo.setDigitalInputFunction(((ElmoLouverAttributes)_attributes).DIGITAL_INPUT_HALL_BIT2,Elmo.INPUT_LOGIC_GP_HI);
		_elmo.setDigitalInputFunction(((ElmoLouverAttributes)_attributes).DIGITAL_INPUT_HALL_BIT3,Elmo.INPUT_LOGIC_GP_HI);
		
		_elmo.setHomingSpeedCounts(((ElmoLouverAttributes)_attributes).homingSpeedLoCounts);	
		_elmo.setMotionTimeoutOffsetMsec(((ElmoLouverAttributes)_attributes).motionTimeoutOffsetMsec);	
		_elmo.setMotionTimeoutScale(((ElmoLouverAttributes)_attributes).motionTimeoutScale);	

		/* override flash defaults
		 VL[2]=-340  VH[2]=340
		 LL[2]=-1080 HL[2]=1080
		 */
		_elmo.setReferenceSpeed(-3400,3400);
		_elmo.setReferenceLimit(-10800,10800);
	}
		
	/////////////////////////////////
	//        Louver Control       //
	/////////////////////////////////
	/** If invertHallPosition is true, the order of the feedback readings is reversed,
	 ranging from 15-0 instead of 0-15. This is used when the feedback board may be 
	 mounted in either direction along the lead screw axis while maintaining the same 
	 logical feedback sense (0 is closed, 15 is open)
	 */
	public void setInvertHallPosition(boolean value){
		_elmo.setInvertHallPosition(value);
	}
	/** If invertHallPosition is true, the order of the feedback readings is reversed,
	 ranging from 15-0 instead of 0-15. This is used when the feedback board may be 
	 mounted in either direction along the lead screw axis while maintaining the same 
	 logical feedback sense (0 is closed, 15 is open)
	 */
	public boolean getInvertHallPosition() {
		return _elmo.getInvertHallPosition();
	}
	
	/** Find and stop at the nearest Hall sensor feedback transition location. 
	 
	 Throws IllegalArgumentException if current position is above upper 
	 or below lower hall feedback limit (0x0, 0xF),
	 
	 @param positive If positive is true, find the transition by moving in the positive direction, 
	 otherwise move in the negative direction.
	 @param speedCounts operate at speedCounts counts/sec
	 @return position counter value at transition
	 */
	public long findBoundary(boolean positive,int speedCounts)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException,Exception
	{
		return _elmo.findBoundary(positive,speedCounts);
	}
	
	/** Find the center of the current Hall effect feedback position. 
	 The strategy is to travel to the nearest transition boundaries, 
	 then move halfway between them. The strategy is slightly modified 
	 for the two endpoints, where transitions are somewhat different.
	 
	 The upper transition is also a false 0, so that has to be dealt 
	 with if it's possible to get there.
	 
	 Notes: 
	 - Resets position counter (but returns original value)
	 - Disables the motor
	 
	 */
	public long center(int speedCounts)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException,Exception
	{
		return _elmo.center(speedCounts);
	}
	
	/** Home to one of the louver's 16 Hall effect position
	 feedback switches. The basic strategy is:
	 - find the center of the current (nearest) feedback position
	 - travel to the desired position (now a multiple of the sensor spacing)
	 - reset the position counter
	 
	 This minimizes unnecessary opening/closing of the louver. 
	 */
	public void home(int position, int velocityCounts)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException,Exception
	{
		_elmo.home(position,velocityCounts);
	}
	public void home(int position, boolean setPx, long counterValue, int vLo, int vHi)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException,Exception
	{
		_elmo.home(position,setPx,counterValue,vLo,vHi);
	}
	
	
	/** Set an interpolated louver position between 
	 _louverUnitsMin and _louverUnitsMax.
	 Position is expressed as a percentage of full
	 range, 1.0 being open and 0.0 being closed.
	 */
	public void setLouverPercent(double positionPercent)
	throws IllegalArgumentException, Exception
	{
		_elmo.setLouverPercent(positionPercent);
	}
	
	/** Set an interpolated louver position between 
	 _louverUnitsMin and _louverUnitsMax.
	 Position is expressed in engineering units.
	 */
	public void setLouverDegrees(double positionDegrees)
	throws IllegalArgumentException, Exception
	{
		_elmo.setLouverDegrees(positionDegrees);
	}
	
	/** Get louver position (in engineering units)
	 @return louver position (degrees)
	 */
	public double getLouverPositionDegrees()
	throws TimeoutException, IOException, NullPointerException, Exception{
		return _elmo.getLouverPositionDegrees();
	}
	
	/** Get louver position (as a percent of full travel)
	 @return louver position (percent, 0.0-1.0)
	 */
	public double getLouverPositionPercent()
	throws TimeoutException, IOException, NullPointerException, Exception{
		return _elmo.getLouverPositionPercent();
	}
	
	/** return a message indicating the state of several motor registers */
	public String getLouverStatusMessage()
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException,Exception
	{
		return _elmo.getLouverStatusMessage();
	}
	/** return a message indicating the state of several motor registers */
	public String getLouverSampleMessage()
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException,Exception
	{
		return _elmo.getLouverSampleMessage();
	}
	
	
	/**
	 * Override base class readSample (defined in BaseInstrumentService)
	 *
	 * @param sample output buffer
	 */
	protected int readSample(byte[] sample) 
	throws TimeoutException,IOException, Exception 
	{
		
		byte[] smsg = _elmo.getLouverSampleMessage().getBytes();
		
		int len=(smsg.length<=sample.length?smsg.length:sample.length);
		for(int i=0;i<len;i++){
			sample[i]=smsg[i];
		}
		
		return len;
	}
		
	/** Return a PacketParser. */
	public PacketParser getParser() throws NotSupportedException{
		return new ElmoLouverPacketParser(_attributes.registryName);
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
	
	public class ElmoLouverAttributes extends ElmoService.Attributes {
		
		/** Home to specified position ( <n<) on initialize instrument. 
			Set <0 to disable homing during initialization
		 */
		int homePosition=0;
		
		/** initialization value for position counter (PX)
			when homing enabled (homePosition>=0)
		 */
		long homePositionCounter=0L;
		/** slow speed to use for homing (counts/sec)
			slow speed is used at the end of homing
			sequence, for fine positioning
		 */
		int homingSpeedLoCounts     = 50;
		/** fast speed to use for homing (counts/sec)
		 fast speed is used at the beginning of homing
		 sequence, for coarse positioning
		 */
		int homingSpeedHiCounts     = 100;
		
		
		/** min louver position (engineering units) */
		double louverDegreesMin   = 0.0;
		/** max louver position (engineering units) */
		double louverDegreesMax   = 52.497;

		/** min louver position (percent of engineering units range) */
		double louverPercentMin = 0.0;
		/** max louver position (percent of engineering units range) */
		double louverPercentMax = 1.0;
		
		/** max hall position feedback output */
		int louverHallPhyMax=0xF;
		/** min hall position feedback output */
		int louverHallPhyMin=0x1;
		
		/** min louver displacement (counts) */
		long louverCountsMin    = 0L;
		/** max louver displacement (counts) */
		long louverCountsMax    = 4770L;

		/** invert louver slider hall position order */
		boolean invertHallPosition     = true;

		/** Elmo input port bit used for hall feedback bit 0 */
		int DIGITAL_INPUT_HALL_BIT0 = 2;
		/** Elmo input port bit used for hall feedback bit 1 */
		int DIGITAL_INPUT_HALL_BIT1 = 3;
		/** Elmo input port bit used for hall feedback bit 2 */
		int DIGITAL_INPUT_HALL_BIT2 = 4;
		/** Elmo input port bit used for hall feedback bit 3 */
		int DIGITAL_INPUT_HALL_BIT3 = 5;
		
		/** timeout margin added to computed value for some homing/centering motions */
		double motionTimeoutScale=1.5;
		long motionTimeoutOffsetMsec=200L;
						
		/**
		 * Constructor, with required InstrumentService argument
		 */
		public ElmoLouverAttributes(DeviceServiceIF service) {
			super(service);
			
			// override base attribute default initialization
			serialMode=Elmo.MODE_SERIAL_RFC1722;
			unitMode=Elmo.MODE_SINGLE_FEEDBACK_POSITION;
			motorAcceleration=1000;
			motorDeceleration=1000;
			motorStopDeceleration=10000;
			gearRatio=15.0;
		}
		
		/**
		 * Throw InvalidPropertyException if any invalid attribute values found
		 */
		public void checkValues() 
		throws InvalidPropertyException 
		{
			super.checkValues();
			if(_elmo!=null && _elmo.getMotor()!=null){
				
				try{
					_elmo.validateSpeed(homingSpeedLoCounts);
				}catch (IllegalArgumentException iae) {
					iae.printStackTrace();
					throw new InvalidPropertyException("Invalid homingSpeedLoCounts ["+homingSpeedLoCounts+"]");
				}
				try{
					_elmo.validateSpeed(homingSpeedHiCounts);
				}catch (IllegalArgumentException iae) {
					iae.printStackTrace();
					throw new InvalidPropertyException("Invalid homingSpeedHiCounts ["+homingSpeedHiCounts+"]");
				}
				
				
				if(_elmo.getInvertHallPosition()!=invertHallPosition){
					try{
						_elmo.setInvertHallPosition(invertHallPosition);
					}catch (IllegalArgumentException iae) {
						iae.printStackTrace();
						throw new InvalidPropertyException("Invalid invertHallPosition ["+invertHallPosition+"]");
					}
				}
				if(_elmo.getMotionTimeoutOffsetMsec()!=motionTimeoutOffsetMsec){
					try{
						_elmo.setMotionTimeoutOffsetMsec(motionTimeoutOffsetMsec);
					}catch (IllegalArgumentException iae) {
						iae.printStackTrace();
						throw new InvalidPropertyException("Invalid motionTimeoutOffsetMsec ["+motionTimeoutOffsetMsec+"]");
					}
				}
				if(_elmo.getMotionTimeoutScale()!=motionTimeoutScale){
					try{
						_elmo.setMotionTimeoutScale(motionTimeoutScale);
					}catch (IllegalArgumentException iae) {
						iae.printStackTrace();
						throw new InvalidPropertyException("Invalid motionTimeoutScale ["+motionTimeoutScale+"]");
					}
				}
				if(_elmo.getLouverHallPhyMin()!=louverHallPhyMin){
					try{
						_elmo.setLouverHallPhyMin(louverHallPhyMin);
					}catch(IllegalArgumentException iae){
						iae.printStackTrace();
						throw new InvalidPropertyException("Invalid louverHallPhyMin ["+louverHallPhyMin+"]");					
					}
				}
				if(_elmo.getLouverHallPhyMax()!=louverHallPhyMax){
					try{
						_elmo.setLouverHallPhyMax(louverHallPhyMax);
					}catch(IllegalArgumentException iae){
						iae.printStackTrace();
						throw new InvalidPropertyException("Invalid louverHallPhyMax ["+louverHallPhyMax+"]");					
					}
				}
				if(_elmo.getLouverDegreesMin()!=louverDegreesMin){
					try{
						_elmo.setLouverDegreesMin(louverDegreesMin);
					}catch(IllegalArgumentException iae){
						iae.printStackTrace();
						throw new InvalidPropertyException("Invalid louverUnitsMin ["+louverDegreesMin+"]");					
					}
				}
				if(_elmo.getLouverDegreesMax()!=louverDegreesMax){
					try{
						_elmo.setLouverDegreesMax(louverDegreesMax);
					}catch(IllegalArgumentException iae){
						iae.printStackTrace();
						throw new InvalidPropertyException("Invalid louverUnitsMax ["+louverDegreesMax+"]");					
					}
				}
				if(_elmo.getLouverPercentMin()!=louverPercentMin){
					try{
						_elmo.setLouverPercentMin(louverPercentMin);
					}catch(IllegalArgumentException iae){
						iae.printStackTrace();
						throw new InvalidPropertyException("Invalid louverPercentMin ["+louverPercentMin+"]");					
					}
				}
				if(_elmo.getLouverPercentMax()!=louverPercentMax){
					try{
						_elmo.setLouverPercentMax(louverPercentMax);
					}catch(IllegalArgumentException iae){
						iae.printStackTrace();
						throw new InvalidPropertyException("Invalid louverPercentMax ["+louverPercentMax+"]");					
					}
				}
				if(_elmo.getLouverCountsMin()!=louverCountsMin){
					try{
						_elmo.setLouverCountsMin(louverCountsMin);
					}catch(IllegalArgumentException iae){
						iae.printStackTrace();
						throw new InvalidPropertyException("Invalid louverCountsMin ["+louverCountsMin+"]");					
					}
				}
				if(_elmo.getLouverCountsMax()!=louverCountsMax){
					try{
						_elmo.setLouverCountsMax(louverCountsMax);
					}catch(IllegalArgumentException iae){
						iae.printStackTrace();
						throw new InvalidPropertyException("Invalid louverCountsMax ["+louverCountsMax+"]");					
					}
				}
				
				try{
					_elmo.validatePosition(homePosition);
				}catch(IllegalArgumentException iae){
					iae.printStackTrace();
					throw new InvalidPropertyException("Invalid homePosition ["+homePosition+"]");					
				}
				try{
					_elmo.validateCounts(homePositionCounter);
				}catch(IllegalArgumentException iae){
					iae.printStackTrace();
					throw new InvalidPropertyException("Invalid homePosition ["+homePositionCounter+"]");					
				}
			}
		}
		
	}	
	
}
