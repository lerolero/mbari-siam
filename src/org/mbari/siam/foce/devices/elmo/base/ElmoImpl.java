//
//  ElmoImpl.java
//
//  Builds high level function around basic API methods provided by org.mbari.siam.foce.devices.Elmo
//  for example, initialization, setting multiple parameters, combining operations.
//  Contains methods and members particular to FOCE louver and thrusters.
//
//	Requires (has) an Elmo subclass (e.g. ElmoSolo) rather than extending Elmo
//  to support future Elmo variants.
//
//  Created by headley on 6/8/10.
//  Copyright 2010 MBARI. All rights reserved.
//
package org.mbari.siam.foce.devices.elmo.base;

import java.util.Vector;
import java.util.Iterator;
import java.text.ParseException;
import java.text.DecimalFormat;
import java.io.IOException;
import java.rmi.RemoteException;

import gnu.io.SerialPort;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.NoSuchPortException;
import gnu.io.UnsupportedCommOperationException;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.devices.ElmoIF;
import org.mbari.siam.distributed.devices.ElmoLouverIF;
import org.mbari.siam.distributed.devices.ElmoThrusterIF;

public class ElmoImpl implements ElmoIF,ElmoLouverIF, ElmoThrusterIF{
	
	/** Log4j logger */
	protected static Logger _log4j = Logger.getLogger(ElmoImpl.class);
	
	/** serial version, for Serializable interface */
	public final static long serialVersionUID=0L;

	/** Elmo implementation (ElmoSolo, etc.; Elmo is abstract) */
	protected Elmo _elmo=null;
	
	/** forward controller serial port */
    protected  SerialPort _motorSerialPort=null;

	/////////////////////////////////////////////
	//            Motor Configuration          //
	/////////////////////////////////////////////
	
	/** Motor counts per motor revolution (before gear rection) */
	protected int _countsPerRevolution=6;
	
	/** default gear reduction ratio */
	protected double _gearRatio=67.5;

	protected int _maxMotorCounts=(Elmo.MAX_VELOCITY_COUNTS);
	protected int _minMotorCounts=(-Elmo.MAX_VELOCITY_COUNTS);
	
	/////////////////////////////////////////////
	//          Louver Configuration           //
	/////////////////////////////////////////////

	/** Distance (in motor counts) between louver feedback hall sensors */
	public static final int HALL_DISTANCE_COUNTS    = 315;//400;//338;
	
	/** mask for bits indicating digitial input logical pin state */
	public static final int HALL_ACTIVE_MASK        = (Elmo.MASK_DIGITAL_INPUT_ACTIVE & 0x3C);
	
	/** mask for bits indicating digital input  physical pin state */
	public static final int HALL_PINSTATE_MASK      = (Elmo.MASK_DIGITAL_INPUT_PIN & 0x3C0000);
	
	/** first bit of pin state in digital input port (used for bit shifting) */
	public static final int HALL_PINSTATE_START_BIT = 18;

	public static final double LOUVER_DEGREES_LIMIT_MIN=0.0;
	public static final double LOUVER_DEGREES_LIMIT_MAX=5.0;
	public static final double LOUVER_PERCENT_LIMIT_MIN=0.0;
	public static final double LOUVER_PERCENT_LIMIT_MAX=100.0;
	public static final long LOUVER_COUNTS_LIMIT_MIN=0L;
	public static final long LOUVER_COUNTS_LIMIT_MAX=4770L;
	public static final int LOUVER_HALL_LIMIT_MIN=0x0;
	public static final int LOUVER_HALL_LIMIT_MAX=0xF;
	
	public static final double DOUBLE_LIMIT_ZERO=0.0;
	public static final int INT_LIMIT_ZERO=0;
	public static final long LONG_LIMIT_ZERO=0L;

	/** min louver position (degrees) */
	double _louverDegreesMin   = 0.0;
	/** max louver position (degrees) */
	double _louverDegreesMax   = 52.497; //50.0
	
	/** min louver position (percent of engineering units range) */
	double _louverPercentMin = 0.0;
	/** max louver position (percent of engineering units range) */
	double _louverPercentMax = 1.0;
	
	/** max hall position feedback output */
	int _louverHallPhyMax=0xF;
	/** min hall position feedback output */
	int _louverHallPhyMin=0x1;
	/** total range of louver hall feedback */
	int _louverHallPhyRange=Math.abs(_louverHallPhyMax-_louverHallPhyMin);
	
	/** min logical louver hall feedback position range */
	int _louverHallMin       = 0x0;
	/** max logical louver hall feedback position range */
	int _louverHallMax       = _louverHallPhyRange;
	/** invert louver slider hall position order */
	boolean _invertHallPosition     = true;
	
	/** min louver displacement (counts) */
	long _louverCountsMin    = 0L;
	/** max louver displacement (counts) */
	long _louverCountsMax    = 4770L;
	
	/** louver position (engineering units) */
	//double _louverPositionUnits=0.0;
	/** louver position (percent) */
	//double _louverPositionPercent=0.0;
	/** louver position command (percent) */
	double _louverCommandPercent=0.0;

	/** Slider displacement revolutions per unit
	 (in this case, lead screw threads per inch) 
	 */
	double _displacementRPU = 10.0;
	/** kinematic model lead-screw linkage length */
	double _sliderLinkageLength=10.0;
	/** kinematic model lead-screw offset length */
	double _sliderOffset=8.81;
	/** kinematic model max displacement (inches) */
	double _modelMaxDisplacement = 5.17;//5.23;
	/** kinematic model louver linkage length (inches) */
	double _louverLinkageLength  = 1.5;
	/** kinematic model slider pivot offset (inches) */
	double _sliderPivotOffset    = -0.44;
	/** min slider displacement travel (inches) */
	double _minDisplacementTravel = 0.0;
	/** max slider displacement travel (inches) */
	double _maxDisplacementTravel = 5.2;
	
	public static final int MAX_DIGITAL_INPUT_BIT=3;
	
	/** Elmo input port bit used for hall feedback bit 0 */
	int _DIGITAL_INPUT_HALL_BIT[] = {Elmo.DIGITAL_INPUT_BIT2,Elmo.DIGITAL_INPUT_BIT3,Elmo.DIGITAL_INPUT_BIT4,Elmo.DIGITAL_INPUT_BIT5};
	
	/** speed to use for homing (counts/sec) */
	int _homingSpeedCounts     = 75;
	/** timeout margin added to computed value for some homing/centering motions */
	long _motionTimeoutOffsetMsec = 200L;
	double _motionTimeoutScale = 1.5;
	long _motionDistanceOffsetCounts = 0L;
	double _motionDistanceScale = 1.25;
	
	/** constant indicates next highest (logical) hall sensor boundary */
	public static final boolean ABOVE = true;
	/** constant indicates next lowest (logical) hall sensor boundary */
	public static final boolean BELOW = false;
	
	/////////////////////////////////////////////
	//         Turn Sensor Configuration       //
	/////////////////////////////////////////////
	public final static int TS_UNKNOWN               = -1;
	public final static int TS_DISABLED              = -2;
	public final static int TS_PENDING               = -3;
	public final static int TS_TRIGGERED             = -4;
	public final static int TS_ERROR                 = -5;
	public final static int TS_READ_ERROR            = -6;
	public final static int TS_TIMEOUT               = -7;
	public final static int TS_RESET                 = -8;
	protected long _tsStartTimeMS                    = -1L;
	protected long _tsElapsedMS                      = -1L;
	protected long _tsTriggerCount                   = -1L;
	protected int _tsState                           = TS_DISABLED;
	protected boolean _turnSensorEnabled=false;
	protected boolean _turnSensorReset=false;
	long _turnSensorTimeoutMSec=10000L;
	int _turnSensorInputBit=3;
	int _turnSensorOutputBit=1;
	
	/////////////////////////////////////////////
	//         Output Format Configuration     //
	/////////////////////////////////////////////	
	private static int DEFAULT_PRECISION=3;
	private static int MIN_PRECISION=1;
	private static int MAX_PRECISION=10;
	private int _precision=DEFAULT_PRECISION;
	/** DecimalFormat for number output formatting */
	private DecimalFormat df=new DecimalFormat();	
	
	
	/////////////////////////////////////////////
	//         internal variables              //
	/////////////////////////////////////////////	
	private StringBuffer _louverMessage=new StringBuffer();
	private StringBuffer _thrusterMessage=new StringBuffer();
	
	
	/** No-arg Constructor */
	public ElmoImpl(){
	}
	
	/** Constructor */
	public ElmoImpl(Elmo elmo){
		super();
		_elmo=elmo;
	}
	
	/** get (Elmo) motor controller */
	public Elmo getMotor(){
		return _elmo;
	}
	/** get (Elmo) motor controller */
	public void setMotor(Elmo elmo){
		_elmo=elmo;
	}
	
	/////////////////////////////////////////////
	//            SerialPort Configuration     //
	/////////////////////////////////////////////
	
	/** Configure the serial ports used to communicate with 
	 the motor controllers.
	 */
    public SerialPort configurePort(String serialPortName){
		
		String fileSeparator=":";
		String os=System.getProperty("os.name","unix").trim().toLowerCase();
		if(os.indexOf("win")>=0)
			fileSeparator=";";
		
		String cmdLinePorts=null;
		if(serialPortName!=null)
			cmdLinePorts=serialPortName;
		
		// get parameters passed in from wrapper script
		String propertyPorts=System.getProperty("devices","/dev/ttyS8"+fileSeparator+"/dev/ttyS10");
		if(_log4j.isDebugEnabled()){
		_log4j.debug("propertyPorts: "+propertyPorts+" cmdLinePorts="+cmdLinePorts);
		}
		int baud=Integer.parseInt(System.getProperty("baud","19200"));
		
		String serialPorts=null;
		if(cmdLinePorts!=null){
			serialPorts=cmdLinePorts;
		}else{
			serialPorts=propertyPorts;
		}
		if(_log4j.isDebugEnabled()){
		_log4j.debug("using serialPorts: "+serialPorts);
		}
		// set RXTX serial ports environment
		System.setProperty("gnu.io.rxtx.SerialPorts",serialPorts);
		SerialPort motorSerialPort=null;
		try{
			if(serialPortName!=null){
				CommPortIdentifier serialPortId = 
				CommPortIdentifier.getPortIdentifier(serialPortName);
				
				motorSerialPort = 
				(SerialPort)serialPortId.open(getClass().getName(), 1000);
				
				motorSerialPort.setSerialPortParams(baud, 
												  motorSerialPort.getDataBits(), 
												  motorSerialPort.getStopBits(), 
												  motorSerialPort.getParity());
				if(_log4j.isDebugEnabled()){
				_log4j.debug("Serial port " + 
							 motorSerialPort.getName() + 
							 "["+baud+","+
							 (motorSerialPort.getParity()==0?"N":new Integer(motorSerialPort.getParity()).toString())+","+
							 motorSerialPort.getDataBits()+","+
							 motorSerialPort.getStopBits()+"]");
				}
			}
			return motorSerialPort;
		}catch(Exception e){
			_log4j.error("Error configuring serial port ["+serialPortName+"]:");
			e.printStackTrace();
		}
		return null;
    }
	
	/////////////////////////////////////////////
	//            Motor Configuration          //
	/////////////////////////////////////////////
	
	public void setMinMotorCounts(int value)
	throws IllegalArgumentException
	{
		int maxCounts=(Elmo.MAX_VELOCITY_COUNTS);
		int minCounts=(-Elmo.MAX_VELOCITY_COUNTS);
		if( (value < minCounts) ||
		   (value > maxCounts) ){
			throw new IllegalArgumentException("minMotorCounts must be in range " + minCounts+" to "+maxCounts+" counts/sec");
		}
		_minMotorCounts=value;
	}
	public int getMinMotorCounts()
	{
		return _minMotorCounts;
	}

	public void setMaxMotorCounts(int value)
	throws IllegalArgumentException
	{
		int maxCounts=(Elmo.MAX_VELOCITY_COUNTS);
		int minCounts=(-Elmo.MAX_VELOCITY_COUNTS);
		if( (value < minCounts) ||
		   (value > maxCounts) ){
			throw new IllegalArgumentException("maxMotorCounts must be in range " + minCounts+" to "+maxCounts+" counts/sec");
		}
		_maxMotorCounts=value;
	}
	public int getMaxMotorCounts()
	{
		return _maxMotorCounts;
	}
	
		
	public void setMotorAcceleration(int value)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{
		_elmo.setAcceleration(value);
	}
	public int getMotorAcceleration()
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{		
		return _elmo.getAcceleration();
	}
	
	public void setMotorDeceleration(int value)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{
		_elmo.setDeceleration(value);
	}
	public int getMotorDeceleration()
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{
		return _elmo.getDeceleration();
	}

	public void setMotorStopDeceleration(int value)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{
		_elmo.setStopDeceleration(value);
	}
	public int getMotorStopDeceleration()
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{
		return _elmo.getStopDeceleration();
	}
		
	public void setDigitalInputFunction(int pin, int function)
	throws IllegalArgumentException,TimeoutException, IOException{
		switch(pin){
			case Elmo.DIGITAL_INPUT_BIT0:
			case Elmo.DIGITAL_INPUT_BIT1:
			case Elmo.DIGITAL_INPUT_BIT2:
			case Elmo.DIGITAL_INPUT_BIT3:
			case Elmo.DIGITAL_INPUT_BIT4:
			case Elmo.DIGITAL_INPUT_BIT5:
				break;
			default:
				throw new IllegalArgumentException("invalid digital input pin ["+pin+"]");
		}
		switch(function){
			case Elmo.INPUT_LOGIC_GP_HI:
			case Elmo.INPUT_LOGIC_GP_LO:
			case Elmo.INPUT_LOGIC_HARDSTOP_HI:
			case Elmo.INPUT_LOGIC_HARDSTOP_LO:
				break;
			default:
				throw new IllegalArgumentException("invalid digital input function ["+function+"]");
		}
		
		_elmo.setInputLogic(pin, function);

	}
	public void setReferenceSpeed(int vl, int vh)
	throws TimeoutException, IOException, NullPointerException, Exception
	{
		_elmo.setReferenceSpeed(vl,vh);
	}
	public void setReferenceLimit(int ll, int hl)
	throws TimeoutException, IOException, NullPointerException, Exception
	{
		_elmo.setReferenceLimit(ll,hl);
	}
	
	
	/** Validates specified speed against configured limits 
	 @param counts - speed in counts
	 */
	public void validateSpeed(int counts)
	throws IllegalArgumentException
	{
		if(counts < _minMotorCounts || counts > _maxMotorCounts){
			throw new IllegalArgumentException("invalid argument ["+counts+"] range: ["+_minMotorCounts+"<= value <= "+_maxMotorCounts+"]");
		}
		return;
	}
	/** Validates specified speed against configured limits 
	 @param rpm - speed in rpm
	 */
	public void validateSpeed(double rpm)
	throws IllegalArgumentException
	{
		double minRPM=Elmo.counts2rpm(_minMotorCounts,_countsPerRevolution);
		double maxRPM=Elmo.counts2rpm(_maxMotorCounts,_countsPerRevolution);
		if(rpm < minRPM || rpm > maxRPM){
			throw new IllegalArgumentException("invalid argument ["+rpm+"] range: ["+minRPM+"<= value <= "+maxRPM+"]");
		}
		return;
	}
	
	public void validateRelative(long distanceCounts)
	throws Exception{
		/*
		 long currentPosition=readLogicalLouverPosition(_invertHallPosition)*HALL_DISTANCE_COUNTS;
		 long totalDistance=(distanceCounts+currentPosition);
		 if(_log4j.isDebugEnabled()){
		 _log4j.debug("validateRelative - d:"+distanceCounts+" p:"+currentPosition+" td:"+totalDistance);
		 }
		 if( totalDistance<_louverCountsMin || totalDistance>_louverCountsMax ){
		 throw new Exception("total distance exceeds available travel [d:"+distanceCounts+" p~"+currentPosition+" td:"+totalDistance+"]");
		 }
		 */
	}
	
	public void validateAbsolute(long position)
	throws Exception{
		/*
		 // whether or not position counter is correct, can't go beyond the ends
		 if(position<_louverCountsMin || position>_louverCountsMax){
		 throw new Exception("validateAbsolute - total distance exceeds available travel [p:"+position+"]");		
		 }
		 
		 // check nominal vs actual position counter
		 long currentPX=getPositionCounter();
		 long nominalPX=readLogicalLouverPosition(_invertHallPosition)*HALL_DISTANCE_COUNTS;
		 long diffPX=Math.abs(nominalPX-currentPX);
		 if(_log4j.isDebugEnabled()){
		 _log4j.debug("validateAbsolute - p:"+position+" cpx:"+currentPX+" npx:"+nominalPX+" dpx:"+diffPX);
		 }
		 
		 // if the distances are off by more than 1/4 slot
		 // call it off
		 if( diffPX>(HALL_DISTANCE_COUNTS*0.25) ){
		 throw new Exception("validateAbsolute - position error too large [dPX:"+diffPX+" px:"+currentPX+" nom:"+nominalPX+"]");
		 }
		 */
	}
	
	////////////////////////////////////////////////////
	//         ElmoIF Interface Implementations       //
	////////////////////////////////////////////////////
	
	/** Initialize motor controller
	 satisfies ElmoIF */
	public void initializeController()
	throws TimeoutException, IOException, Exception{
	}
	
	public void initializeController(int serialMode,
									 int countsPerRevolution,
									 double gearRatio,
									 int unitMode,
									 int acceleration,
									 int deceleration,
									 int stopDeceleration) 
	throws TimeoutException, IOException, Exception
	{
		if(_elmo==null){
			return;
		}
		
		// Initialize motor using ELMO flash defaults for VL,VH,LL,HL
		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("initializing controller");
		//}
		_elmo.initializeController(serialMode, unitMode);
		setCountsPerRevolution(countsPerRevolution);
		setGearRatio(gearRatio);
		/* COMMENTED OUT - these *should* be set correctly in Elmo Flash
		 // Initialize motor using SIAM service attribute defaults for VL,VH,LL,HL
		 _elmo.initializeController(_countsPerRevolution,
		 _motorVLCounts,
		 _motorVHCounts,
		 _motorLLCounts,
		 _motorHLCounts);
		 */
		//if(_log4j.isDebugEnabled()){
		//log4j.debug("setting acceleration from service attributes");
		//}
		_elmo.setAcceleration(acceleration);
		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("setting deceleration from service attributes");
		//}
		_elmo.setDeceleration(deceleration);
		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("setting stop acceleration from service attributes");
		//}
		_elmo.setStopDeceleration(stopDeceleration);
		
		
	}
	/** Convert motor speed (rpm)  to (counts/sec) 
		(before gear train)
	    (using countsPerRevolution)
	 */
	public int rpm2counts(double rpm) 
	throws Exception{
		//_log4j.info("cpr="+_countsPerRevolution+" rpm="+rpm+" counts="+Elmo.rpm2counts(rpm,_countsPerRevolution));
		return Elmo.rpm2counts(rpm,_countsPerRevolution);
	}
	
	/** Convert motor speed (counts/sec) to (rpm)
	  (before gear train) 
	  (using countsPerRevolution)
	 */
	public double counts2rpm(int counts) throws Exception{
		return Elmo.counts2rpm(counts,_countsPerRevolution);
	}
	
	/** Convert output shaft speed (rpm) to (counts/sec)
  	    (at output after gear train)
		(using gear ratio and counts per revolution) 
	 */
	public int orpm2counts(double rpm) throws Exception{
		if(_gearRatio<=0.0){
			throw new Exception("invalid gearRatio ["+_gearRatio+"<=0]");
		}
		if(_countsPerRevolution<=0.0){
			throw new Exception("invalid countsPerRevolution ["+_countsPerRevolution+"<=0]");
		}
		double motorRPM=rpm*_gearRatio;
		//_log4j.info("cpr="+_countsPerRevolution+" rpm="+rpm+" gr="+_gearRatio+" mrpm="+motorRPM+" counts="+Elmo.rpm2counts(motorRPM,_countsPerRevolution));
		return Elmo.rpm2counts(motorRPM,_countsPerRevolution);		
	}
	
	/** Convert output shaft speed (counts/sec) to (rpm)
	 (at output after gear train)
	 (using gear ratio and counts per revolution) 
	 */
	public double counts2orpm(int counts) throws Exception{
		if(_gearRatio<=0.0){
			throw new Exception("invalid gearRatio ["+_gearRatio+"<=0]");
		}
		if(_countsPerRevolution<=0.0){
			throw new Exception("invalid countsPerRevolution ["+_countsPerRevolution+"<=0]");
		}
		return Elmo.counts2rpm(counts,_countsPerRevolution)/_gearRatio;				
	}

	/** read an Elmo register value */
	public String readRegister(String register)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{
		return _elmo.readRegister(register);
	}
	
	public void setSerialMode(int mode) throws IllegalArgumentException{
		_elmo.setSerialMode(mode);
	}
	
	public void setCountsPerRevolution(int value)
	throws IllegalArgumentException
	{
		if(value<=0){
			throw new IllegalArgumentException("invalid argument ["+value+"] range: ["+0+"<= value]");
		}
		_countsPerRevolution=value;
	}
	public int getCountsPerRevolution(){
		return _countsPerRevolution;
	}
	
	/** Enable or disable the motor
	 fulfills ElmoIF interface
	 */
	public void setEnable(boolean value, long timeoutMsec)
	throws TimeoutException{
		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("calling _elmo.setEnable("+value+","+timeoutMsec+")");
		//}
		_elmo.setEnable(value,timeoutMsec);
	}
	
	/** Set the gear ratio between the motor and the output shaft.
	 For example, if the output shaft turns 67.5 times slower than
	 the motor, the gear ratio should be set to 67.5.
	 */
	public void  setGearRatio(double value)
	throws IllegalArgumentException
	{
		if(value<=0.0){
			throw new IllegalArgumentException("invalid argument ["+value+"] range: ["+0.0+"<= value]");
		}		
		_gearRatio=value;
	}
	/** Get the gear ratio between the motor and the output shaft.
	 For example, if the output shaft turns 67.5 times slower than
	 the motor, the gear ratio should be set to 67.5.
	 */
	public double getGearRatio(){
		return _gearRatio;
	}
	
	/** Set motor position counter for position modes
	 @param positionCounts new value of position counter (counts)
	 fulfills ElmoIF interface 
	 */
	public void setPositionCounter(long positionCounts) 
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{
		_elmo.setPositionCounter(positionCounts);
	}
	
	/** Get motor position counter for position modes 
	 fulfills ElmoIF interface 
	 @return position counter (counts)
	 */
	public long getPositionCounter()
	throws TimeoutException, IOException, NullPointerException, Exception
	{
		return _elmo.getPositionCounter();
	}
	
	
	/** get motor jogging (commanded) velocity in counts/sec. */
    public int getJoggingVelocity()
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception,RemoteException{
		return _elmo.getJoggingVelocity();
	}
	
	/** Get motor velocity (in counts/sec, from 
	 motor feedback sensors, i.e., before gear train)
	 The encoder velocity is very noisy and typically reads
	 0 at very low speeds (<15 counts/sec). Averages samples
	 readings of the encoder velocity.
	 
	 Takes only one sample if samples<=0.
	 
	 fulfills ElmoIF interface	
	 */
	
	public int getEncoderVelocity()
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{
		return getEncoderVelocity(1);
	}		

	public int getEncoderVelocity(int nSamples)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{
		if(nSamples<=0){
			throw new IllegalArgumentException("invalid value for nSamples <=0");
		}
		
		double sum=0.0;
		int Vcount=0;
		if(nSamples==1){
			sum=(double)_elmo.getEncoderVelocity();
			Vcount=(int)sum;
		}else{
			for(int i=0;i<nSamples;i++)
				sum+=(double)_elmo.getEncoderVelocity();
			Vcount=(int)(sum/nSamples);
		}
		
		//double Vrpm=Elmo.counts2rpm(Vcount,_countsPerRevolution);
		//return Vrpm;
		
		return Vcount;
	}
	
	/** return motor enabled (MO) status. */
    public boolean isEnabled()
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception{
		return _elmo.isEnabled();
	}
	
	/** Get motor status code.
	 fulfills ElmoIF interface
	 @return status 
	 @see Elmo#getStatusRegister()
	 */
	public int getStatusRegister()
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{
		return _elmo.getStatusRegister();
	}
	
	/** return detailed motor fault information..
	 fulfills ElmoIF interface
	 @return fault 
	 @see Elmo#getMotorFault()
	 */
	public int getFaultRegister()
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{
		return _elmo.getMotorFault();
	}
		
	/** Get difference (counts) between commanded 
	 and actual position if available 
	 @return position error (counts)
	 fulfills ElmoIF interface
	 */
	public long getPositionError()
	throws TimeoutException, IOException, NullPointerException, Exception
	{
		return _elmo.getPositionError();
	}
	
	/** Set motor velocity (in rpm)
	 Do not initiate motion
	 fulfills ElmoIF interface
	 */
	public void setJoggingVelocityRPM(double rpm)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{
		setJoggingVelocity(_elmo.rpm2counts(rpm,_countsPerRevolution));
		return;
	}
	
	/** Set motor velocity (in counts)
	 Do not initiate motion
	 fulfills ElmoIF interface
	 */
	public void setJoggingVelocity(int counts)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{			
		validateSpeed(counts);
		_elmo.setJoggingVelocity(counts);
		return;
	}
	
	/** Set motor velocity (in counts)
	 Do not initiate motion
	 fulfills ElmoIF interface
	 */
	public void setPTPSpeed(int counts)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{			
		validateSpeed(counts);
		_elmo.setPTPSpeed(counts);
		return;
	}
	
	/** Set motor velocity (in rpm)
	 Do not initiate motion
	 fulfills ElmoIF interface
	 */
	public void setPTPSpeedRPM(double rpm)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{
		setPTPSpeed(_elmo.rpm2counts(rpm,_countsPerRevolution));
		return;
	}

	/** show current configuration information
		[ a configuration class may be worth doing at some point - klh]
	 */
	public String showConfiguration()
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{
		
		df.applyPattern("0.0E0");
		df.setMinimumFractionDigits(_precision);
		df.setGroupingUsed(false);
		df.setPositivePrefix("+");

		// gear ratio
		// unitMode
		// serialMode
		// driverMode (open/closed)
		// counts/rev
		// AC,DC,SD
		// VL,VH,LL,HL
		// max/min velocity
		// louver geometry/hall feedback...
		// turn sensor...
		// digital input config
		
		StringBuffer sb=new StringBuffer();
		
		// separate values from formatting...
		double gr=_gearRatio;
		int um=_elmo.getUnitMode();
		int sm=_elmo.getSerialMode();
		int cpr=_countsPerRevolution;
		int ac=_elmo.getAcceleration();
		int dc=_elmo.getDeceleration();
		int sd=_elmo.getStopDeceleration();
		int vl=_elmo.getReferenceSpeedLo();
		int vh=_elmo.getReferenceSpeedHi();
		int ll=_elmo.getReferenceLimitLo();
		int lh=_elmo.getReferenceLimitHi();
		int vmax=_maxMotorCounts;
		int vmin=_minMotorCounts;
		double lumin=_louverDegreesMin;
		double lumax=_louverDegreesMax;
		long lcmin=_louverCountsMin;
		long lcmax=_louverCountsMax;
		int lhpmin=_louverHallPhyMin;
		int lhpmax=_louverHallPhyMax;
		boolean ih=_invertHallPosition;
		double drpu=_displacementRPU;
		double sll=_sliderLinkageLength;
		double slo=_sliderOffset;
		double mmd=_modelMaxDisplacement;
		double lll=_louverLinkageLength;
		double spo=_sliderPivotOffset;
		double dtmin=_minDisplacementTravel;
		double dtmax=_maxDisplacementTravel;
		int hsc=_homingSpeedCounts;
		double mts=_motionTimeoutScale;
		long mto=_motionTimeoutOffsetMsec;
		double mds=_motionDistanceScale;
		long mdo=_motionDistanceOffsetCounts;
		int dhib[]=_DIGITAL_INPUT_HALL_BIT;
		String engrUnits=" in";
		
		// format the output
		sb.append("\n");
		sb.append("gear ratio           :"+df.format(gr));
		sb.append("\n");
		sb.append("unit mode            : "+um);
		sb.append("\n");
		sb.append("serial mode          : "+(sm==Elmo.MODE_SERIAL_LOCAL?"Local":"RFC1722"));
		sb.append("\n");
		//sb.append("driver mode:"+mode);
		//sb.append("\n");
		sb.append("resolution           : "+cpr+" counts/revolution");
		sb.append("\n");
		sb.append("AC                   : "+ac+" counts/s^2");
		sb.append("\n");
		sb.append("DC                   : "+dc+" counts/s^2");
		sb.append("\n");
		sb.append("SD                   : "+sd+" counts/s^2");
		sb.append("\n");
		sb.append("VL,VH                : "+vl+","+vh+" counts/s");
		sb.append("\n");
		sb.append("LL,LH                : "+ll+","+lh+" counts/s");
		sb.append("\n");
		sb.append("vmin,vmax            : "+vmin+","+vmax+" counts/sec");
		sb.append("\n");
		sb.append("louver configuration...");
		sb.append("\n");
		sb.append("lead screw travel    : "+df.format(lumin)+","+df.format(lumax)+engrUnits);
		sb.append("\n");
		sb.append("motor displacement   : "+lcmin+","+lcmax+" counts");
		sb.append("\n");
		sb.append("phy hall range       : 0x"+Integer.toHexString(lhpmin)+",0x"+Integer.toHexString(lhpmax));
		sb.append("\n");
		sb.append("invertHallPosition   : "+ih);
		sb.append("\n");
		sb.append("displacementRPU      : "+df.format(drpu)+" rev/in");
		sb.append("\n");
		sb.append("sliderLinkageLength  : "+df.format(sll)+engrUnits);
		sb.append("\n");
		sb.append("sliderOffset         : "+df.format(slo)+engrUnits);
		sb.append("\n");
		sb.append("modelMaxDisplacement : "+df.format(mmd)+engrUnits);
		sb.append("\n");
		sb.append("louverLinkageLength  : "+df.format(lll)+engrUnits);
		sb.append("\n");
		sb.append("sliderPivotOffset    : "+df.format(spo)+engrUnits);
		sb.append("\n");
		sb.append("Displacement Travel  : "+df.format(dtmin)+","+df.format(dtmax)+engrUnits);
		sb.append("\n");
		sb.append("Homing speed         : "+hsc+" counts/s");
		sb.append("\n");
		sb.append("motion timeout scale : "+mts);
		sb.append("\n");
		sb.append("motion timeout offset: "+mto+" msec");
		sb.append("\n");
		sb.append("motion distance scale : "+mds);
		sb.append("\n");
		sb.append("motion distance offset: "+mdo+" counts");
		sb.append("\n");
		sb.append("hall input bits      : ");
		for(int i=0; i<dhib.length; i++){
			sb.append("b"+i+":"+dhib[i]);
			if( i<(dhib.length-1) ){
				sb.append(" ");
			}
		}
		sb.append("\n");		
		sb.append("turn sensor...");
		sb.append("\n");

		return sb.toString();
	}
	
	//////////////////////////
	//    Motion Commands   //
	//////////////////////////

	/** Start motion according to currently set parameters.
	 
	 fulfills ElmoIF interface
	 */
	public void beginMotion() 
	throws TimeoutException, IOException,NullPointerException,IllegalArgumentException,Exception
	{
		_elmo.beginMotion();
	}
		
	/** Command motor velocity (in counts/sec)
	 fulfills ElmoIF interface
	 */
	public void jog(int counts)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{
		validateSpeed(counts);
		_elmo.jog(counts);
		return;
	}
	
	/** Move motor relative to current position at specified velocity
	 @param distanceCounts distance to move (counts)
	 fulfills ElmoIF interface
	 */
	public void ptpRelative(long distanceCounts, int velocityCounts,boolean wait) 
	throws TimeoutException, IOException,NullPointerException,IllegalArgumentException,Exception
	{
		if(_log4j.isDebugEnabled()){
		_log4j.debug("ElmoImpl.moveRelative("+distanceCounts+","+velocityCounts+")");
		}
		setPTPSpeed(velocityCounts);
		ptpRelative(distanceCounts,wait);
	}
	
	/** Move motor relative to current position
	 @param distanceCounts distance to move (counts)
	 fulfills ElmoIF interface
	 */
	public void ptpRelative(long distanceCounts,boolean wait) 
	throws TimeoutException, IOException,NullPointerException,IllegalArgumentException,Exception
	{
		validateRelative(distanceCounts);
		_elmo.ptpRelative(distanceCounts,wait);
	}
	
	/** Move motor to absolute position at specified velocity
	 Motion may be subject to modulo position counting 
	 modes in effect.
	 
	 @param position to move to (counts)
	 fulfills ElmoIF interface
	 */
	public void ptpAbsolute(long position, int velocityCounts,boolean wait) 
	throws TimeoutException, IOException,NullPointerException,IllegalArgumentException,Exception
	{
		setPTPSpeed(velocityCounts);
		ptpAbsolute(position,wait);
	}
	
	/** Move motor to absolute position
	 Motion may be subject to modulo position counting 
	 modes in effect.
	 
	 @param position to move to (counts)
	 fulfills ElmoIF interface
	 */
	public void ptpAbsolute(long position,boolean wait) 
	throws TimeoutException, IOException,NullPointerException,IllegalArgumentException,Exception
	{
		validateAbsolute(position);
		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("eu.moveAbs("+position+")");
		//}
		_elmo.ptpAbsolute(position,wait);
	}
	
	
	/** delay for specified number of milliseconds 
		@param delayMsec delay duration in milliseconds
	 */
	public void delay(long delayMsec){
		_elmo.delay(delayMsec);
	}
	
	/////////////////////////////////////////////
	//          Louver Support                 //
	/////////////////////////////////////////////

	public double getLouverDegreesMax(){
		return _louverDegreesMax;
	}
	public void setLouverDegreesMax(double value)
	throws IllegalArgumentException
	{
		if(value<LOUVER_DEGREES_LIMIT_MIN || value>LOUVER_DEGREES_LIMIT_MAX){
			throw new IllegalArgumentException("invalid argument ["+value+"] range: ["+LOUVER_DEGREES_LIMIT_MIN+"<= value <="+LOUVER_DEGREES_LIMIT_MAX+"]");
		}		
		_louverDegreesMax=value;
	}
	
	public double getLouverDegreesMin(){
		return _louverDegreesMin;
	}
	public void setLouverDegreesMin(double value)
	throws IllegalArgumentException
	{
		if(value<LOUVER_DEGREES_LIMIT_MIN || value>LOUVER_DEGREES_LIMIT_MAX){
			throw new IllegalArgumentException("invalid argument ["+value+"] range: ["+LOUVER_DEGREES_LIMIT_MIN+"<= value <="+LOUVER_DEGREES_LIMIT_MAX+"]");
		}		
		
		_louverDegreesMin=value;
	}
	
	public long getLouverCountsMax(){
		return _louverCountsMax;
	}
	public void setLouverCountsMax(long value)
	throws IllegalArgumentException
	{
		if(value<LOUVER_COUNTS_LIMIT_MIN || value>LOUVER_COUNTS_LIMIT_MAX){
			throw new IllegalArgumentException("invalid argument ["+value+"] range: ["+LOUVER_COUNTS_LIMIT_MIN+"<= value <="+LOUVER_COUNTS_LIMIT_MAX+"]");
		}		
		_louverCountsMax=value;
	}
	
	public long getLouverCountsMin(){
		return _louverCountsMin;
	}
	public void setLouverCountsMin(long value)
	throws IllegalArgumentException
	{
		if(value<LOUVER_COUNTS_LIMIT_MIN || value>LOUVER_COUNTS_LIMIT_MAX){
			throw new IllegalArgumentException("invalid argument ["+value+"] range: ["+LOUVER_COUNTS_LIMIT_MIN+"<= value <="+LOUVER_COUNTS_LIMIT_MAX+"]");
		}		
		_louverCountsMin=value;
	}

	public double getLouverPercentMax(){
		return _louverPercentMax;
	}
	public void setLouverPercentMax(double value)
	throws IllegalArgumentException
	{
		if(value<LOUVER_PERCENT_LIMIT_MIN || value>LOUVER_PERCENT_LIMIT_MAX){
			throw new IllegalArgumentException("invalid argument ["+value+"] range: ["+LOUVER_PERCENT_LIMIT_MIN+"<= value <="+LOUVER_PERCENT_LIMIT_MAX+"]");
		}		
		_louverPercentMax=value;
	}
	
	public double getLouverPercentMin(){
		return _louverPercentMin;
	}
	public void setLouverPercentMin(double value)
	throws IllegalArgumentException
	{
		if(value<LOUVER_PERCENT_LIMIT_MIN || value>LOUVER_PERCENT_LIMIT_MAX){
			throw new IllegalArgumentException("invalid argument ["+value+"] range: ["+LOUVER_PERCENT_LIMIT_MIN+"<= value <="+LOUVER_PERCENT_LIMIT_MAX+"]");
		}		
		_louverPercentMin=value;
	}
	
	public int getLouverHallPhyMin(){
		return _louverHallPhyMin;
	}
	public void setLouverHallPhyMin(int value)
	throws IllegalArgumentException
	{
		if(value<LOUVER_HALL_LIMIT_MIN || value>LOUVER_HALL_LIMIT_MAX){
			throw new IllegalArgumentException("invalid argument ["+value+"] range: ["+LOUVER_HALL_LIMIT_MIN+"<= value <="+LOUVER_HALL_LIMIT_MAX+"]");
		}		
		if(value>_louverHallPhyMax){
			throw new IllegalArgumentException("invalid argument ["+value+"] range: [must be <= "+_louverHallPhyMax+"]");
		}		
		_louverHallPhyMin=value;
		_louverHallPhyRange=Math.abs(_louverHallPhyMax-_louverHallPhyMin);
	}

	public int getLouverHallPhyMax(){
		return _louverHallPhyMax;
	}
	public void setLouverHallPhyMax(int value)
	throws IllegalArgumentException
	{
		if(value<LOUVER_HALL_LIMIT_MIN || value>LOUVER_HALL_LIMIT_MAX){
			throw new IllegalArgumentException("invalid argument ["+value+"] range: ["+LOUVER_HALL_LIMIT_MIN+"<= value <="+LOUVER_HALL_LIMIT_MAX+"]");
		}		
		if(value<_louverHallPhyMin){
			throw new IllegalArgumentException("invalid argument ["+value+"] range: [must be >= "+_louverHallPhyMin+"]");
		}		
		_louverHallPhyMax=value;
		_louverHallPhyRange=Math.abs(_louverHallPhyMax-_louverHallPhyMin);
	}
	
	
	public int getLouverHallMax(){
		return _louverHallMax;
	}
	public void setLouverHallMax(int value)
	throws IllegalArgumentException
	{
		if(value<LOUVER_HALL_LIMIT_MIN || value>LOUVER_HALL_LIMIT_MAX){
			throw new IllegalArgumentException("invalid argument ["+value+"] range: ["+LOUVER_HALL_LIMIT_MIN+"<= value <="+LOUVER_HALL_LIMIT_MAX+"]");
		}		
		_louverHallMax=value;
	}

	public int getLouverHallMin(){
		return _louverHallMin;
	}
	public void setLouverHallMin(int value)
	throws IllegalArgumentException
	{
		if(value<LOUVER_HALL_LIMIT_MIN || value>LOUVER_HALL_LIMIT_MAX){
			throw new IllegalArgumentException("invalid argument ["+value+"] range: ["+LOUVER_HALL_LIMIT_MIN+"<= value <="+LOUVER_HALL_LIMIT_MAX+"]");
		}		
		_louverHallMin=value;
	}
	public double getDisplacementRPU(){
		return _displacementRPU;
	}
	public void setDisplacementRPU(double value)
	throws IllegalArgumentException
	{
		if(value<DOUBLE_LIMIT_ZERO){
			throw new IllegalArgumentException("invalid argument ["+value+"] range: ["+DOUBLE_LIMIT_ZERO+"< value ]");
		}		
		_displacementRPU=value;
	}
	public double getSliderLinkageLength(){
		return _sliderLinkageLength;
	}
	public void setSliderLinkageLength(double value)
	throws IllegalArgumentException
	{
		if(value<DOUBLE_LIMIT_ZERO){
			throw new IllegalArgumentException("invalid argument ["+value+"] range: ["+DOUBLE_LIMIT_ZERO+"< value ]");
		}		
		_sliderLinkageLength=value;
	}
	public double getSliderOffset(){
		return _sliderOffset;
	}
	public void setSliderOffset(double value)
	throws IllegalArgumentException
	{
		if(value<DOUBLE_LIMIT_ZERO){
			throw new IllegalArgumentException("invalid argument ["+value+"] range: ["+DOUBLE_LIMIT_ZERO+"< value ]");
		}		
		_sliderOffset=value;
	}
	public double getModelMaxDisplacement(){
		return _modelMaxDisplacement;
	}
	public void setModelMaxDisplacement(double value)
	throws IllegalArgumentException
	{
		if(value<DOUBLE_LIMIT_ZERO){
			throw new IllegalArgumentException("invalid argument ["+value+"] range: ["+DOUBLE_LIMIT_ZERO+"< value ]");
		}		
		_modelMaxDisplacement=value;
	}
	public double getLouverLinkageLength(){
		return _louverLinkageLength;
	}
	public void setLouverLinkageLength(double value)
	throws IllegalArgumentException
	{
		if(value<DOUBLE_LIMIT_ZERO){
			throw new IllegalArgumentException("invalid argument ["+value+"] range: ["+DOUBLE_LIMIT_ZERO+"< value ]");
		}		
		_louverLinkageLength=value;
	}
	public double getSliderPivotOffset(){
		return _sliderPivotOffset;
	}
	public void setSliderPivotOffset(double value)
	throws IllegalArgumentException
	{
		if(value<DOUBLE_LIMIT_ZERO){
			throw new IllegalArgumentException("invalid argument ["+value+"] range: ["+DOUBLE_LIMIT_ZERO+"< value ]");
		}		
		_sliderPivotOffset=value;
	}
	public double getMinDisplacementTravel(){
		return _minDisplacementTravel;
	}
	public void setMinDisplacementTravel(double value)
	throws IllegalArgumentException
	{
		if(value<DOUBLE_LIMIT_ZERO){
			throw new IllegalArgumentException("invalid argument ["+value+"] range: ["+DOUBLE_LIMIT_ZERO+"< value ]");
		}		
		_minDisplacementTravel=value;
	}
	public double getMaxDisplacementTravel(){
		return _maxDisplacementTravel;
	}
	public void setMaxDisplacementTravel(double value)
	throws IllegalArgumentException
	{
		if(value<DOUBLE_LIMIT_ZERO){
			throw new IllegalArgumentException("invalid argument ["+value+"] range: ["+DOUBLE_LIMIT_ZERO+"<= value ]");
		}		
		_maxDisplacementTravel=value;
	}
	public int getDIHall_Bit(int bit){
		return _DIGITAL_INPUT_HALL_BIT[bit];
	}
	public void setDIHallBit(int bit, int value)
	throws IllegalArgumentException
	{
		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("bit:"+bit+" value:"+value);
		//}
		if(bit<INT_LIMIT_ZERO || bit>MAX_DIGITAL_INPUT_BIT){
			throw new IllegalArgumentException("invalid argument ["+bit+"] range: ["+INT_LIMIT_ZERO+"<= value <= "+MAX_DIGITAL_INPUT_BIT+"]");
		}
		switch(value){
			case Elmo.DIGITAL_INPUT_BIT0:
			case Elmo.DIGITAL_INPUT_BIT1:
			case Elmo.DIGITAL_INPUT_BIT2:
			case Elmo.DIGITAL_INPUT_BIT3:
			case Elmo.DIGITAL_INPUT_BIT4:
			case Elmo.DIGITAL_INPUT_BIT5:
				_DIGITAL_INPUT_HALL_BIT[bit]=value;
				break;
			default:
				throw new IllegalArgumentException("invalid argument ["+value+"] range: ["+Elmo.DIGITAL_INPUT_BIT0+"<= value <= "+Elmo.DIGITAL_INPUT_BIT5+"]");
		}
	}
	public int getHomingSpeedCounts(){
		return _homingSpeedCounts;
	}
	public void setHomingSpeedCounts(int value)
	throws IllegalArgumentException
	{
		if(value<LONG_LIMIT_ZERO){
			throw new IllegalArgumentException("invalid argument ["+value+"] range: ["+LONG_LIMIT_ZERO+"<= value ]");
		}		
		_homingSpeedCounts=value;
	}
	public long getMotionTimeoutOffsetMsec(){
		return _motionTimeoutOffsetMsec;
	}
	public void setMotionTimeoutOffsetMsec(long value)
	throws IllegalArgumentException
	{
		if(value<LONG_LIMIT_ZERO){
			throw new IllegalArgumentException("invalid argument ["+value+"] range: [value >= "+LONG_LIMIT_ZERO+"]");
		}		
		_motionTimeoutOffsetMsec=value;
	}
	public double getMotionTimeoutScale(){
		return _motionTimeoutScale;
	}
	public void setMotionTimeoutScale(double value)
	throws IllegalArgumentException
	{
		if(value<0.0){
			throw new IllegalArgumentException("invalid argument ["+value+"] range: [value >= "+LONG_LIMIT_ZERO+"]");
		}		
		_motionTimeoutScale=value;
	}
	
	public long getMotionDistanceOffsetMsec(){
		return _motionDistanceOffsetCounts;
	}
	public void setMotionDistanceOffsetMsec(long value)
	throws IllegalArgumentException
	{
		if(value<LONG_LIMIT_ZERO){
			throw new IllegalArgumentException("invalid argument ["+value+"] range: [value >= "+LONG_LIMIT_ZERO+"]");
		}		
		_motionDistanceOffsetCounts=value;
	}
	public double getMotionDistanceScale(){
		return _motionDistanceScale;
	}
	public void setMotionDistanceScale(double value)
	throws IllegalArgumentException
	{
		if(value<0.0){
			throw new IllegalArgumentException("invalid argument ["+value+"] range: [value >= "+LONG_LIMIT_ZERO+"]");
		}		
		_motionDistanceScale=value;
	}
	
	/** Get number of motor counts per unit of travel
	 (varies with lead screw gear ratio, counts per revolution
	 and displacement revolution per units)
	 */
	public double countsPerUnitTravel(){
		if(_log4j.isDebugEnabled()){
		_log4j.debug("countsPerUnitTravel - countsPerRevolution:"+_countsPerRevolution+" _gearRatio:"+_gearRatio+" _displacementRPU:"+_displacementRPU);
		}
		return _countsPerRevolution*_gearRatio*_displacementRPU;
	}
	
	/** Validate an louver position value expressed as a percentage of fully open */
	public boolean validatePercent(double value)
	throws IllegalArgumentException
	{
		if(value<_louverPercentMin || value>_louverPercentMax){
			throw new IllegalArgumentException("validatePercent: invalid value ["+value+"] - must be "+_louverPercentMin+" <= N <= "+_louverPercentMax);
		}
		return true;
	}
	
	/** Validate an louver position value expressed in engineering units */
	public boolean validateDegrees(double value)
	throws IllegalArgumentException
	{
		if(value<_louverDegreesMin || value>_louverDegreesMax){
			throw new IllegalArgumentException("validatePercent: invalid value ["+value+"] - must be "+_louverDegreesMin+" <= N <= "+_louverDegreesMax);
		}
		return true;
	}
	
	/** validate a louver position expressed in motor counts */
	public boolean validateCounts(long value)
	throws IllegalArgumentException
	{
		if( (value < _louverCountsMin ) || (value > _louverCountsMin)){
			throw new IllegalArgumentException("validateCounts: invalid value ["+value+"] - must be "+_louverCountsMin+"<= N <="+_louverCountsMax);
		}
		return true;
	}
	
	/** validate a louver position expressed as one of the Hall sensor feedback values 
	 */
	public boolean validatePosition(int value)
	throws IllegalArgumentException
	{
		if( (value < _louverHallMin) || (value > _louverHallMax) ){
			throw new IllegalArgumentException("validatePosition: value must be "+_louverHallMin+"<= N <="+_louverHallMax);
		}
		return true;
	}
	
	/** Convert louver position value (percent) to (engineering) units value
		This routine does not validate, so may return values that are not physically valid
		for invalid input
	 */
	public double percent2degrees(double percentValue)
	throws IllegalArgumentException
	{
		double degreesValue=(_louverDegreesMin+percentValue*(_louverDegreesMax-_louverDegreesMin));
		if(_log4j.isDebugEnabled()){
		_log4j.debug("percent2Units - louverUnitsMin:"+_louverDegreesMin+" louverUnitsMax:"+_louverDegreesMax+" units:"+degreesValue);
		}		
		return degreesValue;
	}
	
	/** Convert louver position value (engineering units) to percent value
	    This routine does not validate, so may return values that are not physically valid
		for invalid input
	 */
	public double degrees2percent(double degreesValue)
	throws IllegalArgumentException
	{
		double percentValue=(degreesValue-_louverDegreesMin)/(_louverDegreesMax-_louverDegreesMin);
		return percentValue;
	}
	
	/** read and return louver position, expressed as one of the Hall sensor feedback values */
	//protected int readLogicalLouverPosition(boolean invertScale)
	protected int readPhysicalLouverPosition()
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException,Exception
	{
		int phyPosition = (_elmo.getInputPort() & HALL_PINSTATE_MASK) >> HALL_PINSTATE_START_BIT;
		return phyPosition;
	}
	
	protected int readLogicalLouverPosition(boolean invertScale)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException,Exception
	{
		int phyPosition = readPhysicalLouverPosition();
		int position = 0;
		
		if(invertScale==true){
			position=_louverHallPhyMax-phyPosition;
		}else{
			position=phyPosition-_louverHallPhyMin;
		}
		return position;
		
	}
	
	
	/** compute displacement (motor counts) for given louver position (degrees).
	 This is an implementation of the kinematic position equations for the
	 louvers (an offset crank-slider mechanism).
	 
	 Louver angle T1 is related to lead screw displacement D by:
	 D = a SIN(T1) - b SIN( ACOS( -(a cos(T1)+c)/b ) )
	 
	 where
	 
	 a = louver linkage length ( 1.5" )
	 b = lead screw linkage length ( 10" )
	 c = lead screw offset ( 8.81" )
	 
	 This expression references displacement relative to the louver pivot point,
	 and displacement increases with decreasing angle; it does not take into 
	 account the offset (-0.44 in) of the slider pivot or the length of the 
	 louver pivot arm (+1.5 in).
	 
	 The coordinate system is transformed such that displacement D' is in 
	 the range 0-maxDisplacementTravel (5.3 in) and D`(0)=0.
	 
	 D' = K - D + A + Z
	 
	 where
	 
	 K = Max positive travel (6.23 in)
	 A = offsets for louver pivot arm ( +1.5 in)
	 Z = offsets for louver pivot arm and slider ( -0.44 in)
	 
	 */
	public long getDisplacementCounts(double louverPositionDegrees){
		
		// convert louver angle to input (louver pivot) angle in 
		// kinematic model coordinates
		double T1=Math.toRadians(90.0-louverPositionDegrees);
		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("louverPositionDegrees:"+louverPositionDegrees+" T1:"+T1);
		//}
		
		// compute linkage angle as a function of input angle
		double cosT2 = -(_louverLinkageLength*Math.cos(T1)+_sliderOffset)/_sliderLinkageLength;
		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("cosT2:"+cosT2);
		//}
		
		double T2=Math.acos(cosT2);
		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("T2:"+T2);
		//}
		
		// compute slider (lead screw) displacement in 
		// kinematic model coordinates
		double a_sinT1=_louverLinkageLength*Math.sin(T1);
		double b_sinT2=_sliderLinkageLength*Math.sin(T2);
		
		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("a_sinT1:"+a_sinT1+" b_sinT2:"+b_sinT2);
		//}
		
		// compute displacement in engineering units,
		// referenced to kinematic model coordinate system
		// (louver axis pivot is on X axis)
		double dUnits = a_sinT1+b_sinT2;
		
		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("dUnits:"+dUnits);
		//}
		
		// transform coordinates 
		dUnits = _modelMaxDisplacement - dUnits + _louverLinkageLength + _sliderPivotOffset;
		
		//if(_log4j.isDebugEnabled()){
		// _log4j.debug("dUnits:"+dUnits+" _modelMaxDisplacement:"+_modelMaxDisplacement+" _louverLinkageLength:"+_louverLinkageLength+" _sliderPivotOffset:"+_sliderPivotOffset);
		//_log4j.debug("countsPerUnitTravel:"+countsPerUnitTravel());
		//}
		
		// convert to counts
		long dCounts = (long)(dUnits*countsPerUnitTravel());
		
		return (dCounts);
		
	}
		
	/////////////////////////////////
	//        Louver Control       //
	/////////////////////////////////
	/** If invertHallPosition is true, the order of the feedback readings is reversed,
	 ranging from 15-0 instead of 0-15. This is used when the feedback board may be 
	 mounted in either direction along the lead screw axis while maintaining the same 
	 logical feedback sense (0 is closed, 15 is open)
	 */
	public boolean getInvertHallPosition(){
		return _invertHallPosition;
	}
	/** If invertHallPosition is true, the order of the feedback readings is reversed,
		ranging from 15-0 instead of 0-15. This is used when the feedback board may be 
	    mounted in either direction along the lead screw axis while maintaining the same 
		logical feedback sense (0 is closed, 15 is open)
	 */
	public void setInvertHallPosition(boolean value){
		_invertHallPosition=value;
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
		
		int seekDirection=(positive==true?1:-1);
		
		// stop the motor
		setEnable(false,Elmo.TM_CMD_MSEC);
		
		// read louver position feedback (start position)
		int initialPosition=readLogicalLouverPosition(_invertHallPosition);
		if(_log4j.isDebugEnabled()){
		_log4j.debug("findboundary - initialPos:0x"+Integer.toHexString(initialPosition));
		}
		
		if(initialPosition==_louverHallMax && positive==true){
			throw new IllegalArgumentException("Can not seek above max feedback position ["+Integer.toHexString(_louverHallMax)+"]");
		}
		if(initialPosition==_louverHallMin && positive==false){
			throw new IllegalArgumentException("Can not seek below min feedback position ["+Integer.toHexString(_louverHallMin)+"]");
		}
		// set up input logic to stop at next transition
		// (if LSB is 0, stop when it changes to 1, etc.)
		//if(initialPosition%2 == 0){
		if( ( (readPhysicalLouverPosition() & 0x1)) == 1){
			_elmo.setInputLogic(_DIGITAL_INPUT_HALL_BIT[0],Elmo.INPUT_LOGIC_HARDSTOP_LO);
		}else{
			_elmo.setInputLogic(_DIGITAL_INPUT_HALL_BIT[0],Elmo.INPUT_LOGIC_HARDSTOP_HI);
		}
		if(_log4j.isDebugEnabled()){
		_log4j.debug("findboundary - feedback mode set (LSB ignored):\n"+getLouverStatusMessage());
		}
		
		// set up relative motion to next 
		// feedback position (1 sensor distance),
		// which will stop when it hits the transition...
		
		// compute motion distance
		// (dist=motionDistanceScale*(Hall sensor distance) + motionDistanceOffset to allow for position errors)
		// distance uses both a scale and offset for flexibility (can use one, both or neither)
		int distTics=(int)(_motionDistanceScale*seekDirection*HALL_DISTANCE_COUNTS+_motionDistanceOffsetCounts);
		
		// compute motion timeout
		// timeout uses both a scale and offset for flexibility (can use one, both or neither)
		// (time=motionTimeoutScale*(dist/velocity) + motionTimeoutOffset (ms))
		//long motionTimeoutMsec = (long)(1000L*(_motionTimeoutScale*(double)Math.abs(distTics)/(double)speedCounts)+_motionTimeoutOffsetMsec);
		//boolean stopOK=false;
		
		// start motion, mark time
		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("findboundary - dist:"+distTics+" speedCounts:"+speedCounts+" timeout:("+_motionTimeoutScale+","+_motionTimeoutOffsetMsec+")");
		//}
		try{
			ptpRelative(distTics,speedCounts,true);
			setEnable(false,Elmo.TM_CMD_MSEC);
		}catch(Exception e){
			if(_log4j.isDebugEnabled()){
			_log4j.debug("findboundary - exception while seeking boundary:");
			}
			e.printStackTrace();
			setEnable(false,Elmo.TM_CMD_MSEC);		
		}

		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("restoring GP input:\n"+getLouverStatusMessage());
		//}
		_elmo.setInputLogic(_DIGITAL_INPUT_HALL_BIT[0],Elmo.INPUT_LOGIC_GP_HI);
		
		// should now be at original position+/-1
		// (hopefully at the center)
		int position=readLogicalLouverPosition(_invertHallPosition);
		if( position!=initialPosition+seekDirection){
			// oh dear...
			setEnable(false,Elmo.TM_CMD_MSEC);
			throw new Exception("findboundary - stop at wrong position:\nipos:"+initialPosition+" cpos:"+position+" seekd:"+seekDirection+"\n"+getLouverStatusMessage());			
		}
		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("findboundary - OK:\n"+getLouverStatusMessage());
		//}
		long boundaryLocation=getPositionCounter();
		
		return boundaryLocation;
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
		// stop the motor
		setEnable(false,Elmo.TM_CMD_MSEC);
		
		// read louver position feedback (start position)
		int initialPosition=readLogicalLouverPosition(_invertHallPosition);
		long initialPX=getPositionCounter();
		long centerPosition=initialPX;
		long upperBoundary=initialPX;
		long lowerBoundary=initialPX;
		
		if(initialPosition==_louverHallMax){
			if(_log4j.isDebugEnabled()){
			_log4j.debug("at max hall stop - seeking lower boundary");
			}
			// If original position is 0xF, we should seek the lower
			// transition boundary...
			lowerBoundary=findBoundary(BELOW,speedCounts);
			
			// move half slot to center of position
			ptpRelative(HALL_DISTANCE_COUNTS/2,true);
			
		}else if(initialPosition==_louverHallMin){
			if(_log4j.isDebugEnabled()){
			_log4j.debug("at min hall stop...");
			}
			// Assuming it is physically possible to go beyond 0xF...
			// if position is 0x0, could be either end 
			// since Halls read 0x0 above 0xF...
			
			// make sure we're at real zero
			try{
				if(_log4j.isDebugEnabled()){
				_log4j.debug("at min hall stop - seeking upper boundary");
				}
				// the happy path...
				upperBoundary=findBoundary(ABOVE,speedCounts);
				// move half slot to center of position
				ptpRelative(-HALL_DISTANCE_COUNTS/2,true);
			}catch (Exception e) {
				_log4j.warn("couldn't find upper boundary; maybe at false zero above 0xF:");
				e.printStackTrace();
			}
			if(_log4j.isDebugEnabled()){
			//_log4j.debug("checking for false zero...");
			}
			
			// maybe just at false zero...
			lowerBoundary=findBoundary(BELOW,speedCounts);
			
			if(readLogicalLouverPosition(_invertHallPosition)==_louverHallMax){
				if(_log4j.isDebugEnabled()){
				//_log4j.debug("found false zero...moving to next lower");
				}
				// found the upper boundary (between 0xF and the false zero)
				// now find the lower boundary (between 0xE and 0xF)
				lowerBoundary=findBoundary(BELOW,speedCounts);
				
				if(_log4j.isDebugEnabled()){
				//_log4j.debug("moving to center or max position...");
				}
				// move half slot to center of position
				ptpRelative(HALL_DISTANCE_COUNTS/2,true);
				
			}else{
				// Let's see where we are and continue
				// if there's a way
				int wtf=readLogicalLouverPosition(_invertHallPosition);
				if(wtf>=_louverHallMax || wtf==_louverHallMin){
					// give up...
					throw new Exception("totally confused, giving up: "+getLouverStatusMessage());
				}
			}
			
		}else{
			if(_log4j.isDebugEnabled()){
			_log4j.debug("at position "+initialPosition+"...seeking upper boundary");
			}			
			// otherwise, party on...
			// find upper boundary
			upperBoundary=findBoundary(ABOVE,speedCounts);
			if(_log4j.isDebugEnabled()){
			//_log4j.debug("upper boundary at position "+readLogicalLouverPosition(_invertHallPosition)+"...seeking lower boundary");
			}
			
			// find lower boundary
			// do it twice: first time finds the upper boundary again
			lowerBoundary=findBoundary(BELOW,speedCounts);
			if(_log4j.isDebugEnabled()){
			//_log4j.debug("lower boundary at position "+readLogicalLouverPosition(_invertHallPosition)+"...seeking lower boundary again");
			}
			
			lowerBoundary=findBoundary(BELOW,speedCounts);
			
			long width=((upperBoundary-lowerBoundary));
			long cdist=width/2;
			long center=lowerBoundary+cdist;
			if(_log4j.isDebugEnabled()){
			//_log4j.debug("Pi: "+initialPosition+" Pc:"+readLogicalLouverPosition(_invertHallPosition)+" Bhi:"+upperBoundary+" Blo:"+lowerBoundary+" w:"+width+" c:"+center+" d:"+cdist);
			}
			
			// validate not off by more than 10%
			if(width > 1.1*HALL_DISTANCE_COUNTS || width<0.9*HALL_DISTANCE_COUNTS){
				// something wrong
				throw new Exception("invalid width found [ub:"+upperBoundary+" lb:"+lowerBoundary+" w:"+width+"]");
			}
			
			// move half slot to center of position
			if(_log4j.isDebugEnabled()){
			//_log4j.debug("moving to center of slot - d:"+cdist+"\n"+getLouverStatusMessage());
			}
			ptpRelative(cdist,true);
			
			if(_log4j.isDebugEnabled()){
			//_log4j.debug("should be at center of slot:\n"+getLouverStatusMessage());
			}
				
		}
		
		// verify that we are still at the same position
		// (hopefully at the center)
		int position=readLogicalLouverPosition(_invertHallPosition);
		
		if(position!=initialPosition){
			setEnable(false,Elmo.TM_CMD_MSEC);
			throw new Exception("centering error - initial!=final : ipos:"+initialPosition+" fpos:"+position+"\n"+getLouverStatusMessage());			
		}
		
		//long newPX=HALL_DISTANCE_COUNTS*position;
		if(_log4j.isDebugEnabled()){
		//_log4j.debug("setting PX="+newPX+" PXi="+initialPX);
		}
		// set position counter to nominal position for this location
		//setPositionCounter(newPX);
		
		// return the original position
		// (for what it's worth)
		return initialPX;
	}
	/** Home to one of the louver's 16 Hall effect position
	 feedback switches. The basic strategy is:
	 - find the center of the current (nearest) feedback position
	 - travel to the desired position (now a multiple of the sensor spacing)
	 - reset the position counter
	 
	 This minimizes unnecessary opening/closing of the louver. 
	 This version uses the current value of _homingSpeedCounts
	 */
	public void home(int position)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException,Exception
	{
		home(position,false,0L,_homingSpeedCounts,_homingSpeedCounts);
	}
	public void home(int position, int velocityCounts)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException,Exception
	{
		home(position,false,0L,velocityCounts,velocityCounts);
	}
	/** Home to one of the louver's 16 Hall effect position
	 feedback switches. The basic strategy is:
	 - find the center of the current (nearest) feedback position
	 - travel to the desired position (now a multiple of the sensor spacing)
	 - reset the position counter
	 
	 This minimizes unnecessary opening/closing of the louver. 
	 */
	public void home(int position, boolean setCounter, long counterValue, int vLo, int vHi)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException,Exception
	{
		if(vLo<=0 || vHi<=0){
			throw new Exception("home - invalid velocity ["+vLo+","+vHi+"] must be > 0 ");
		}
		
		setEnable(false,Elmo.TM_CMD_MSEC);
		_elmo.setInputLogic(_DIGITAL_INPUT_HALL_BIT[0],Elmo.INPUT_LOGIC_GP_HI);
		
		// read/save input logic modes?
		int initialPosition=readLogicalLouverPosition(_invertHallPosition);

		// move nominal distance at specified vHi
		int distance;
		/*
		if(position >= initialPosition){
			distance=(position-initialPosition)*HALL_DISTANCE_COUNTS;
		}else{
			distance=-(initialPosition-position-1)*HALL_DISTANCE_COUNTS;		
		}*/
		
		// aim for one stop before the stop we want
		// (unless we're already there)
		if(position == initialPosition){
			distance=0;
		}else if(position > initialPosition){
			distance=(position-initialPosition-1)*HALL_DISTANCE_COUNTS;
		}else{
			distance=(position-initialPosition)*HALL_DISTANCE_COUNTS;		
		}
		
		_log4j.info("home - coarse seek: pos:"+position+" ipos:"+initialPosition+" dist:"+distance+"\n"+getLouverStatusMessage());
		if(distance!=0){
			ptpRelative(distance,vHi,true);
		}
		initialPosition=readLogicalLouverPosition(_invertHallPosition);
		
		//see where we are and home to final stop
		// at vLo
		int bcount=0;
		boolean seek=ABOVE;

		// calculate initial distance (number of boundaries)
		// and direction
		if(position >= initialPosition){
			bcount=position-initialPosition+1;
			seek=ABOVE;
		}else {
			bcount=initialPosition-position;
			seek=BELOW;
		}
		
		// initialize current position
		int currentPosition=initialPosition;

		// count boundaries at low speed
		// (end at transition just below the 
		// target position)
		for(int i=0;i<bcount;i++){
			findBoundary(seek,vLo);
			currentPosition=readLogicalLouverPosition(_invertHallPosition);
			if(currentPosition==initialPosition){
				break;
			}
		}
		// at low speed, find the final boundary
		// (target position defined to be transition
		// approached from the next logical stop below)
		while(currentPosition>position){
			findBoundary(!seek,vLo);
			currentPosition=readLogicalLouverPosition(_invertHallPosition);
		}
		if(setCounter==true){
			setPositionCounter(counterValue);
		}
		// set state variable (commanded motor position)
		_louverCommandPercent=getLouverPositionPercent();

		if(_log4j.isDebugEnabled()){
		//_log4j.debug("home - should be home:\n"+getLouverStatusMessage());
		}
		// restore input logic modes
	}
	
	/** Set an interpolated louver position between 
	 _louverDegreesMin and _louverDegreesMax.
	 Position is expressed as a percentage of full
	 range, 1.0 being open and 0.0 being closed.
	 */
	public void setLouverPercent(double positionPercent)
	throws IllegalArgumentException, Exception
	{
		// no need to validate here, since
		// it is done by setLouverDegrees()
		//validatePercent(positionPercent);		
		if(_log4j.isDebugEnabled()){
		_log4j.debug("setLouverPercent - positionUnits:"+positionPercent);
		}
		setLouverDegrees(percent2degrees(positionPercent));
		
	}
	
	/** Set an interpolated louver position between 
	 _louverDegreesMin and _louverDegreesMax.
	 Position is expressed in engineering units.
	 */
	public void setLouverDegrees(double positionDegrees)
	throws IllegalArgumentException, Exception
	{
		// very important to validate here
		// (setLouverPercent depends on this validation)
		validateDegrees(positionDegrees);

		// get counts value for given output position
		long motorPosition=getDisplacementCounts(positionDegrees);

		// set state variable (commanded motor position)
		_louverCommandPercent=degrees2percent(positionDegrees);

		if(_log4j.isDebugEnabled()){
		_log4j.debug("setLouverDegrees - positionUnits:"+positionDegrees+" motorPosition:"+motorPosition);
		}
		// could move relative too...
		// moveRelative(motorPosition-getPositionCounter()+getPositionError());
		// moveAbsolute(motorPosition+getPositionError());
		ptpAbsolute(motorPosition,true);

	}
	
	/** Get louver position (in engineering units)
	 
	 This derives from the kinematic position equations for the
	 louvers (an offset crank-slider mechanism).
	 
	 Louver angle T1 is related to lead screw displacement D by:
	 D = a SIN(T1) - b SIN( ACOS( -(a cos(T1)+c)/b ) )
	 
	 where
	 
	 a = louver linkage length ( 1.5" )
	 b = lead screw linkage length ( 10" )
	 c = lead screw offset ( 8.81" )
	 
	We solve this for T1, which may be represented a quadratic in
	Cos(T1) (or Sin(T1) depending on implementation of arccos and arcsin
	 and the geometry assumptions)
	 
	 @return louver position (degrees)
	 */
	public double getLouverPositionDegrees()
	throws TimeoutException, IOException, NullPointerException, Exception{
		return (getLouverPositionDegrees(getPositionCounter()));
	}
				
	public double getLouverPositionDegrees(long positionCounts)
	throws TimeoutException, IOException, NullPointerException, Exception{
		
		// set geometry parameters
		double a=_louverLinkageLength;
		double b=_sliderLinkageLength;
		double c=_sliderOffset;
		
		// get displacement (logical reference frame, in engineering units (inches))
		double logicalDisplacement=positionCounts/countsPerUnitTravel();
		
		// convert to reference frame used by kinematic model
		double d=_modelMaxDisplacement + _louverLinkageLength + _sliderPivotOffset - logicalDisplacement;
		
		// for convenience and readability,
		// these constants represent major algebraic terms
		double M=((a*a-b*b+c*c+d*d)/(2*a*d));
		double N=(d*d)/(c*c+d*d);
		
		// these substitutions are the 
		// the coefficients of the quadratic equation
		// rationalized so the x^2 term coefficient A=1
		double B=(2*c*N*M)/d;
		double C=N*(M*M-1);
		
		// solve for sin of angle in terms of displacement
		// (quadratic formula, A=1)
		double sinT1=(0.5*(-B+Math.sqrt(B*B-4*C)));
		
		// solve for angle (take arcsin)
		double T1=Math.asin(sinT1);

		// return angle in degrees
	    return Math.toDegrees(T1);
	}
	
	/** Get louver position (as a percent of full travel)
	 @return louver position (percent, 0.0-1.0)
	 */
	public double getLouverPositionPercent()
	throws TimeoutException, IOException, NullPointerException, Exception{
		return getLouverPositionPercent(getPositionCounter());
	}
	
	/** Get louver position (as a percent of full travel)
	 @return louver position (percent, 0.0-1.0)
	 */
	public double getLouverPositionPercent(long positionCounts)
	throws TimeoutException, IOException, NullPointerException, Exception{
		return degrees2percent(getLouverPositionDegrees(positionCounts));
	}
	
	/** return a message indicating the state of several motor registers */
	public String getLouverStatusMessage()
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException,Exception
	{
		
		df.applyPattern("0.0E0");
		df.setMinimumFractionDigits(_precision);
		df.setGroupingUsed(false);
		df.setPositivePrefix(" ");
		// data format:
		// commandedLouverPosition,motorPositonCounter,PX,IP,PE,SR,MF
		//StringBuffer sb=new StringBuffer();
		_louverMessage.setLength(0);
		_louverMessage.append("lpos:"+df.format(_louverCommandPercent));
		_louverMessage.append(",");
		_louverMessage.append("PX:"+Long.toString(getPositionCounter()));
		_louverMessage.append(",");
		int ip=(_elmo.getInputPort() & Elmo.MASK_DIGITAL_INPUT_PORT);
		_louverMessage.append("IP:0x"+Integer.toHexString(ip));
		_louverMessage.append(",");
		_louverMessage.append("FBt:0x"+Integer.toHexString(readPhysicalLouverPosition()));
		_louverMessage.append(",");
		_louverMessage.append("FBi:0x"+Integer.toHexString(readLogicalLouverPosition(_invertHallPosition)));
		_louverMessage.append(",");
		_louverMessage.append("PE:"+Long.toString(getPositionError()));
		_louverMessage.append(",");
		_louverMessage.append("SR:0x"+Long.toHexString(getStatusRegister()));
		_louverMessage.append(",");
		_louverMessage.append("MF:0x"+Long.toHexString(getFaultRegister()));
		_louverMessage.append(",");
		_louverMessage.append("UM:"+_elmo.getUnitMode());
		_louverMessage.append("\n");
		_louverMessage.append("PA:"+_elmo.cmdWriteReadInt(Elmo.CMD_ABSOLUTE_POSITION,Elmo.BUF32,Elmo.TM_CMD_MSEC));
		_louverMessage.append(",");
		_louverMessage.append("PR:"+_elmo.cmdWriteReadInt(Elmo.CMD_RELATIVE_POSITION,Elmo.BUF32,Elmo.TM_CMD_MSEC));
		_louverMessage.append(",");
		_louverMessage.append("JV:"+getJoggingVelocity());
		_louverMessage.append(",");
		_louverMessage.append("MO:"+_elmo.isEnabled());
		_louverMessage.append("\n");
		for(int i=0;i<6;i++){
			_louverMessage.append("IL["+(i+1)+"]:"+_elmo.getInputLogic(i));		
			if(i<5){
				_louverMessage.append(" ");
			}
		}
		return _louverMessage.toString();
	}
	
	/** return a (terse) message indicating the state of several motor registers */
	public String getLouverSampleMessage()
	throws TimeoutException,IOException, Exception 
	{
		//return getLouverSampleMessage();
		df.applyPattern("0.0E0");
		df.setMinimumFractionDigits(_precision);
		df.setGroupingUsed(false);
		df.setPositivePrefix(" ");
		// data format:
		// commandedLouverPosition,motorPositionPercent,motorPositonCounter,PX,IP,PE,SR,MF,IL[1:6]
		//StringBuffer sb=new StringBuffer();
		_louverMessage.setLength(0);
		//louver commanded position(engineering units)
		_louverMessage.append(df.format(_louverCommandPercent));
		_louverMessage.append(",");
		//louver actual position (percent)
		_louverMessage.append(df.format(getLouverPositionPercent()));
		_louverMessage.append(",");
		//PX
		_louverMessage.append(Long.toString(getPositionCounter()));
		_louverMessage.append(",");
		int ip=(readPhysicalLouverPosition());
		//IP
		_louverMessage.append(Integer.toHexString(ip ));
		_louverMessage.append(",");
		//FB
		_louverMessage.append(Integer.toHexString(readLogicalLouverPosition(_invertHallPosition)));
		_louverMessage.append(",");
		//PE
		_louverMessage.append(Long.toString(getPositionError()));
		_louverMessage.append(",");
		//SR
		_louverMessage.append(Long.toHexString(getStatusRegister()));
		_louverMessage.append(",");
		//MF
		_louverMessage.append(Long.toHexString(getFaultRegister()));
		_louverMessage.append(",");
		//UM
		_louverMessage.append(_elmo.getUnitMode());
		_louverMessage.append(",");
		//IL[1:6]
		for(int i=0;i<6;i++){
			_louverMessage.append(_elmo.getInputLogic(i));
			if(i<5){
				_louverMessage.append(",");
			}
		}
		return _louverMessage.toString();
	}
	
	///////////////////////////////////////
	//         Turn Sensor Support       //
	///////////////////////////////////////
	
	public void initializeTurnSensor(){
		if(_log4j.isDebugEnabled()){
		//_log4j.debug("disarming turn sensor trigger");
		}
		disarmTurnsSensorTrigger();	
		setTurnSensorState(ElmoImpl.TS_DISABLED,-1L,-1L);
	}	
	public long getTurnSensorTimeoutMSec(){
		return _turnSensorTimeoutMSec;
	}
	public void setTurnSensorTimeoutMSec(long value)
	throws IllegalArgumentException
	{
		if(value<LONG_LIMIT_ZERO){
			throw new IllegalArgumentException("invalid argument ["+value+"] range: ["+LONG_LIMIT_ZERO+"<= value ]");
		}		
		_turnSensorTimeoutMSec=value;
	}
	
	public int getTurnSensorInputBit(){
		return _turnSensorInputBit;
	}
	public void setTurnSensorInputBit(int value)
	throws IllegalArgumentException
	{
		if(value<INT_LIMIT_ZERO){
			throw new IllegalArgumentException("invalid argument ["+value+"] range: ["+INT_LIMIT_ZERO+"<= value ]");
		}		
		_turnSensorInputBit=value;
	}
	
	public int getTurnSensorOutputBit(){
		return _turnSensorOutputBit;
	}
	public void setTurnSensorOutputBit(int value)
	throws IllegalArgumentException
	{
		if(value<INT_LIMIT_ZERO){
			throw new IllegalArgumentException("invalid argument ["+value+"] range: ["+INT_LIMIT_ZERO+"<= value ]");
		}		
		_turnSensorOutputBit=value;
	}
	/*
	 Configure the digital I/O to trigger the Homing function
	 and configure the Homing function to set a digital 
	 output.
	 */
	public boolean armTurnsSensorTrigger(){
		try{
			// IL[5]=7 
			// configure high-speed digital input 
			// as General Purpose (active HIGH)
			//_elmo.cmdWriteReadX(Elmo.CMD_ENABLE_MAIN_HOME_SEQUENCE_HI);
			_elmo.cmdWriteReadX(Elmo.CMD_GENERAL_PURPOSE_HI);
			
			// HM[4]=1 // after homing, set digital output OP=HM[6]
			_elmo.cmdWriteReadX(Elmo.mkCmd(Elmo.CMD_HOMING_BEHAVIOR,Elmo.HOMING_BEHAVIOR_SET_OUTPUT));
			
			// HM[6]=1 // output value after homing
			_elmo.cmdWriteReadX(Elmo.mkCmd(Elmo.CMD_HOMING_OUTPUT_VALUE,1));
			
			// HM[3]=17 // trigger from programmed function for Digital Input 5 (i.e. IL[5])
			// which is set as an active HIGH general purpose input
			//_elmo.cmdWriteReadX(Elmo.CMD_HOMING_EVENT_DEFINITION+"="+Elmo.HOMING_EVENT_MAIN_HOME_SWITCH);
			_elmo.cmdWriteReadX(Elmo.mkCmd(Elmo.CMD_HOMING_EVENT_DEFINITION,Elmo.HOMING_EVENT_DIN5));
			
			// HM[5]=2 // do not set position counter (PX) when homing
			_elmo.cmdWriteReadX(Elmo.mkCmd(Elmo.CMD_HOMING_EVENT_PX,Elmo.HOMING_EVENT_NONE));
			
			// configure digital output 1 GPO active HI
			_elmo.cmdWriteReadX(Elmo.mkIndexCmd("OL",1,1));	    
			
			// OP=0 // clear digital outputs
			_elmo.cmdWriteReadX(Elmo.mkCmd(Elmo.CMD_DIGITIAL_OUTPUT,0));
			
			// HM[1]=1
			// arm Main Home sequence 
			_elmo.cmdWriteReadX(Elmo.CMD_ARM_HOMING_PROCESS);
			
			// note the start time
			//_turnSensorStartMillis=System.currentTimeMillis();
			_tsStartTimeMS=System.currentTimeMillis();
			//setTurnSensorState(ElmoImpl.TS_PENDING,0L,-1L);
			
			return true;
		}catch(IOException ie){
			ie.printStackTrace();
		}catch(NullPointerException ne){
			ne.printStackTrace();
		}catch(IllegalArgumentException iae){
			iae.printStackTrace();
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return false;
	}
	
	/** Disarm the turn sensor homing function */
	public boolean disarmTurnsSensorTrigger(){
		try{
			// IL[5]=5
			// configure high speed digital input
			// to ignore switch
			_elmo.cmdWriteReadX(Elmo.CMD_SET_DIO5_IGNORE_HI);
			
			// HM[1]=0
			// disarm Main Home sequence
			_elmo.cmdWriteReadX(Elmo.CMD_DISARM_HOMING_PROCESS);
			
			// HM[4]=2 // after homing, do nothing
			_elmo.cmdWriteReadX(Elmo.mkCmd(Elmo.CMD_HOMING_BEHAVIOR,Elmo.HOMING_BEHAVIOR_NONE));
			
			// HM[3]=0 // trigger immediate mode
			//_elmo.cmdWriteReadX(Elmo.CMD_HOMING_EVENT_DEFINITION+"="+Elmo.HOMING_EVENT_IMMEDIATE);
			
			//setTurnSensorState(ElmoImpl.TS_DISABLED,-1L,-1L);
			
			return true;
		}catch(IOException ie){
			ie.printStackTrace();
		}catch(NullPointerException ne){
			ne.printStackTrace();
		}catch(IllegalArgumentException iae){
			iae.printStackTrace();
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return false;
	}
	
	/** Compute the time for one rotation at the 
	 current speed
	 */
	public long getTurnSensorTimeoutMillis(double velocityRPM,double gearRatio){
		
		// get 1.5 rotation times in millisec at current speed 
		// divided down by the shaft gear ratio
		// ((1000 msec/s)*(60 s/min)*(1.5*min/rev)*(1/gearRatio))
		double rotationTimeMillis=(1000.0*60.0*1.5/velocityRPM)*(1/gearRatio);
		return (long)rotationTimeMillis;
	}
	
	
	/**
	 return DISABLED, TRIGGERED, PENDING, READ_ERROR, RESET
	 */
	public int readTurnSensor(){
		try{
			
			if(_turnSensorEnabled==false){
				if(_log4j.isDebugEnabled()){
				_log4j.debug(">>> turn sensor DISABLED");
				}
				return ElmoImpl.TS_DISABLED;
			}else if(_turnSensorReset==true){
				if(_log4j.isDebugEnabled()){
				_log4j.debug(">>> turn sensor RESET");
				}
				return ElmoImpl.TS_RESET;
			}
			
			// motor is waiting...
			// should be turning, timed out, or still waiting...
			// read the sensor...
			if(_log4j.isDebugEnabled()){
			_log4j.debug(">>> reading turn sensor flag");
			}
			int test = _elmo.cmdWriteReadInt("OB[1]","OB[1]".length(),1000L);
			if(_log4j.isDebugEnabled()){
			_log4j.debug(">>> turn sensor flag returned "+test);
			}
			if( test!=0){
				if(_log4j.isDebugEnabled()){
				_log4j.debug(">>> turn sensor TRIGGERED");
				}
				// if bit set, the sensor is turning...
				return ElmoImpl.TS_TRIGGERED;
			}
			
			if(_log4j.isDebugEnabled()){
			_log4j.debug(">>> turn sensor PENDING");
			}
			return ElmoImpl.TS_PENDING;
			
		}catch(NullPointerException ne){
			ne.printStackTrace();
		}catch(IllegalArgumentException iae){
			iae.printStackTrace();
		}catch(Exception e){
			e.printStackTrace();
		}
		if(_log4j.isDebugEnabled()){
		_log4j.debug(">>> turns sensor read ERROR");
		}
		
		return ElmoImpl.TS_READ_ERROR;
	}
	
	protected void setTurnSensorState(int state,long triggerCount, long elapsedTime){
		_tsElapsedMS=elapsedTime;
		_tsTriggerCount=triggerCount;
		_tsState=state;
		return;
	}
	
	/** Read turn sensor and update sensor state.
	 The turn sensor consists of a magnet/hall effect pair 
	 wired to one of the Elmo digital input pins. Elmo
	 homing functions (HM[N]) are used to configured to 
	 detect a transition on that pin and set an output
	 (OP[N]) bit when triggered.
	 
	 @return ElmoImpl.TS_DISABLED if disabled
	 @return ElmoImpl.TS_TIMEOUT if time since last trigger > _turnSensorTimeoutMSec
	 @return ElmoImpl.TS_UNKNOWN if in an undefined state
	 @return ElmoImpl.TS_PENDING if waiting for a trigger
	 @return ElmoImpl.TS_TRIGGERED if triggered
	 @return ElmoImpl.TS_READ_ERROR if an error occurred while reading the sensor
	 */
	protected int updateTurnSensorState(){
		
		// check for change to/from disabled/enabled state
		if(_turnSensorEnabled==true && _tsState==ElmoImpl.TS_DISABLED){
			// enable turn sensor
			armTurnsSensorTrigger();
			setTurnSensorState(ElmoImpl.TS_PENDING,0L,-1L);
			return _tsState;
		}else if(_turnSensorEnabled==false && _tsState!=ElmoImpl.TS_DISABLED){
			// disable turn sensor
			disarmTurnsSensorTrigger();
			setTurnSensorState(ElmoImpl.TS_DISABLED,-1L,-1L);
			return _tsState;
		}
		
		// read turn sensor and
		// get elapsed time since last trigger
		int turnSensorStatus=readTurnSensor();
		_tsElapsedMS=System.currentTimeMillis()-_tsStartTimeMS;
		
		try{
			switch(turnSensorStatus){
				case ElmoImpl.TS_DISABLED:
				case ElmoImpl.TS_TIMEOUT:
					break;
				case ElmoImpl.TS_UNKNOWN:
					// disable turn sensor
					disarmTurnsSensorTrigger();
					setTurnSensorState(ElmoImpl.TS_DISABLED,-1L,-1L);						
					break;
				case ElmoImpl.TS_PENDING:
					if(_tsElapsedMS >= _turnSensorTimeoutMSec){
						// could reset here...for now, require reset to clear timeout
						setTurnSensorState(ElmoImpl.TS_TIMEOUT,_tsTriggerCount,_tsElapsedMS);
					}						
					break;
				case ElmoImpl.TS_TRIGGERED:
					armTurnsSensorTrigger();
					setTurnSensorState(ElmoImpl.TS_PENDING,++_tsTriggerCount,0L);						
					break;
				case ElmoImpl.TS_READ_ERROR:          
					setTurnSensorState(ElmoImpl.TS_READ_ERROR,_tsTriggerCount,_tsElapsedMS);
					break;
				case ElmoImpl.TS_RESET:
					setTurnSensorState(ElmoImpl.TS_PENDING,0L,-1L);
					_turnSensorReset=false;
					break;
				default:
					// should not get here
					setTurnSensorState(ElmoImpl.TS_UNKNOWN,_tsTriggerCount,_tsElapsedMS);
					_log4j.error("Elmo error: entering invalid state");
					break;
			}
		}catch(NullPointerException ne){
			ne.printStackTrace();
		}catch(IllegalArgumentException iae){
			iae.printStackTrace();
		}catch(Exception e){
			e.printStackTrace();
		}
		return _tsState;
	}
	
	/** return thruster sample string */
	public String getThrusterSampleMessage()
	throws TimeoutException,IOException, Exception 
	{
		//StringBuffer sb=new StringBuffer();
		//return getLouverSampleMessage();
		/*
		df.applyPattern("0.0E0");
		df.setMinimumFractionDigits(_precision);
		df.setGroupingUsed(false);
		df.setPositivePrefix(" ");
		
		// average encoder velocity
		//sb.append(df.format(getEncoderVelocity()));
		 */
		_thrusterMessage.setLength(0);
		_thrusterMessage.append(getEncoderVelocity());
		_thrusterMessage.append(",");
		// commanded jogging velocity
		_thrusterMessage.append(getJoggingVelocity());
		_thrusterMessage.append(",");
		// motor status register (SR)
		_thrusterMessage.append(Integer.toHexString(getStatusRegister()));
		_thrusterMessage.append(",");
		// motor fault register (MF)
		_thrusterMessage.append(Integer.toHexString(getFaultRegister()));
		_thrusterMessage.append(",");
		// sensor turn status (external feedback via Elmo digital I/O
		int tsstate=updateTurnSensorState();
		_thrusterMessage.append(tsstate);
		_thrusterMessage.append(",");
		_thrusterMessage.append(getTSTriggerCount());
		_thrusterMessage.append(",");
		_thrusterMessage.append(getTSElapsedMsec());
		return _thrusterMessage.toString();		
	}
	
	
	////////////////////////////////////////
	//         Turn Sensor Control       //
	////////////////////////////////////////
	
	/** Enable or disable the turns sensor */
	public void setTurnsSensorEnable(boolean value){
		_turnSensorEnabled=value;
	}
	public long getTSTriggerCount(){
		return _tsTriggerCount;
	}
	public long getTSElapsedMsec(){
		return _tsElapsedMS;
	}
	public int getTSState(){
		return _tsState;
	}
	public static String getTSStateMnemonic(int stateValue){
		switch(stateValue){
			case ElmoImpl.TS_UNKNOWN:
				return "UNKNOWN";
			case ElmoImpl.TS_DISABLED:
				return "DISABLED";
			case ElmoImpl.TS_PENDING:
				return "PENDING";
			case ElmoImpl.TS_TRIGGERED:
				return "TRIGGERED";
			case ElmoImpl.TS_ERROR:
				return "ERROR";
			case ElmoImpl.TS_READ_ERROR:
				return "READ_ERROR";
			case ElmoImpl.TS_TIMEOUT:
				return "TIMEOUT";
			case ElmoImpl.TS_RESET:
				return "RESET";
			default:
				break;
		}
		return "UNDEFINED";		
	}
	public String getTSStateName(){
		return getTSStateMnemonic(_tsState);
	}
	
		
}