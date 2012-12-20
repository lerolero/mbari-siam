/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.elmo.base;

import java.util.Vector;
import java.util.Iterator;
import java.util.StringTokenizer;
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

import org.mbari.siam.foce.deployed.IOMapper;

public class ElmoService extends PolledInstrumentService 
	implements Instrument, InstrumentDataListener, ElmoIF
{
		
	/** Log4j logger */
	protected static Logger _log4j = Logger.getLogger(ElmoService.class);

	/** Service attributes */
	public Attributes _attributes;
	
	/** serial version, for Serializable interface */
	public final static long serialVersionUID=0L;
	protected int DFL_MAX_SAMPLE_BYTES=128;
	protected int DFL_CURRENT_LIMIT_MILLIAMPS=5000;

	public static final String OPEN_LOOP="openLoop";
	public static final String CLOSED_LOOP="closedLoop";

	public static final int DEFAULT_COUNTS_PER_REVOLUTION = 6;

	/** Elmo implementation */
	protected ElmoImpl _elmo=null;
	
	/////////////////////////////////////////////
	//         Output Format Configuration     //
	/////////////////////////////////////////////	
	protected static int DEFAULT_PRECISION=5;
	protected static int MIN_PRECISION=1;
	protected static int MAX_PRECISION=10;
	protected int _precision=DEFAULT_PRECISION;
	/** DecimalFormat for number output formatting */
	protected DecimalFormat df=new DecimalFormat();	

	//protected CSpline cals=new CSpline();

	public ElmoService()throws RemoteException{
		_attributes=new Attributes(this);
	}
	
	/** required by PolledInstrumentService */
	protected ScheduleSpecifier createDefaultSampleSchedule() 
	throws ScheduleParseException
	{
		// Sample every 30 seconds by default
		return new ScheduleSpecifier(3600000);
	}
	
	/**required by BaseInstrumentService */
	protected int initMaxSampleBytes() {
		return DFL_MAX_SAMPLE_BYTES;
	}
	/**required by BaseInstrumentService */
	protected byte[] initPromptString() {
		return "N/A".getBytes();
	}
	/**required by BaseInstrumentService */
	protected byte[] initSampleTerminator() {
		return "N/A".getBytes();
	}
	/**required by BaseInstrumentService */
	protected int initCurrentLimit() {
		return DFL_CURRENT_LIMIT_MILLIAMPS;
	}
	/** Return initial value of instrument power policy. */
	protected PowerPolicy initInstrumentPowerPolicy() {
		return PowerPolicy.ALWAYS;
	}
	/** Return initial value of communication power policy. */
	protected PowerPolicy initCommunicationPowerPolicy() {
		return PowerPolicy.ALWAYS;
	}
	/**required by BaseInstrumentService */
	protected int initInstrumentStartDelay() {
		return 0;
	}
	
	/**required by DeviceService */
	public SerialPortParameters getSerialPortParameters()
	throws UnsupportedCommOperationException
	{
		return new SerialPortParameters(19200, SerialPort.DATABITS_8,
										SerialPort.PARITY_NONE, SerialPort.STOPBITS_1);
	}
	/** Self-test not implemented. */
	public int test() {
		return -1;
	}
	
	/** Override default initializeInstrument.
	 */
	protected void initializeInstrument() throws InitializeException, Exception
	{
		//super.initializeInstrument();
		// initialize motor utils
		SerialInstrumentPort sp = (SerialInstrumentPort)(this._instrumentPort);
		ElmoSolo solo=new ElmoSolo(sp.getSerialPort());
		_elmo= new ElmoImpl(solo);
		// home to position (Hall stop)?
	}		
	
	/**required by BaseInstrumentService */
	protected void requestSample() throws TimeoutException, Exception {
		return;
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
		//_log4j.debug("Elmo shutdownInstrument: getting IOMapper");
		}
		//retval+=" "+switchElmoPower(false);
		return retval;
	}
	
	/**
	 Get device-specific metadata (e.g. configuration, operating mode).
     */
    protected byte[] getInstrumentStateMetadata() {
		StringBuffer metadata=new StringBuffer();
		String test;
		/*		 
		 SN VR
		 UM
		 
		 KI[1] KP[1] KI[2] KP[2]
		 AC DC SD
		 VL[2] VH[2] LL[2] HL[2] 
		 VL[3] VH[3] XM[1] XM[2]
		 
		 MO VX VY VE DV[2]
		 PR PA PX PE
		 IL IF
		 
		 CA
		 MC PL[1] PL[2] CL[1] CL[2] CL[3]
		 
		 CD OP IP
		 SR MF MS
		 
		 HM[1] HM[2] HM[3] HM[4] HM[5] HM[6]
		 IL[5]

		 SN;VR;UM;PM;RM;SF;TS;KI[1];KP[1];KI[2];KP[2];AC;DC;SD;VL[2];VH[2];LL[2];HL[2];VL[3];VH[3];XM[1];XM[2];MO;VX;VY;VE;DV[1];DV[2];DV[3];DV[4];DV[5];DV[6];DV[7];PR;PA;PX;PE;IL;IF;CA;MC;PL[1];PL[2];CL[1];CL[2];CL[3];CD;OP;IP;SR;MF;MS;HM[1];HM[2];HM[3];HM[4];HM[5];HM[6];IL[5];XP[0];XP[1];XP[2];XP[4];XP[5];XP[6];XP[7];WS[33];WS[34];BV;
		 
		 EO;MC;PL[1];PL[2];CL[1];CL[2];CL[3];LL[2];HL[2];HL[3];VL[2];VH[2];AC;DC;SD;UM;LC;PM;RM;SF;TS;KI[2];KP[2];KP[3];HX=1;SR;MF;HX=0;WS[33];WS[34];CD;DV[1];DV[2];DV[3];DV[4];DV[5];DV[6];DV[7];EC;MS;XP[0];XP[1];XP[2];XP[4];XP[5];XP[6];XP[7];BV;
		 */
		
		String cmds="SN[1];SN[2];SN[3];SN[4];VR;EO;UM;PM;RM;SF;TS;KI[1];KP[1];KI[2];KP[2];AC;DC;SD;VL[2];VH[2];LL[2];HL[2];VL[3];VH[3];XM[1];XM[2];MO;VX;VY;VE;DV[1];DV[2];DV[3];DV[4];DV[5];DV[6];DV[7];PR;PA;PX;PE;IL[1];IL[2];IL[3];IL[4];IL[5];IL[6];MC;PL[1];PL[2];CL[1];CL[2];CL[3];CD;OP;IP;SR;MF;MS;HM[1];HM[2];HM[3];HM[4];HM[5];HM[6];IL[5];XP[0];XP[1];XP[2];XP[4];XP[5];XP[6];XP[7];WS[33];WS[34];BV;";
		StringTokenizer st=new StringTokenizer(cmds,";");
		
		while(st.hasMoreTokens()){
			String cmd=st.nextToken();
			test=null;
			try{
				test=_elmo.readRegister(cmd);
				test=test.substring(0,test.indexOf(";"));
				if(cmd.equalsIgnoreCase("SR") || 
				   cmd.equalsIgnoreCase("MF")){
					test="0x"+Integer.toHexString(Integer.parseInt(test));
				}
				if(cmd.equalsIgnoreCase("CD")){
					byte[] tbytes=test.getBytes();
					for(int i=0;i<tbytes.length;i++){
						if(tbytes[i]<0x20){
							tbytes[i]=0x20;
						}
					}
					test=new String(tbytes);
				}else{
					test=test.trim();
				}
			}catch (Exception e) {
				test="N/A [test:"+test+", err:"+e.getMessage()+"]";	
			}
			metadata.append(cmd+":"+test.trim()+"\n");
		}
		
		return metadata.toString().getBytes();
	}
	
	/////////////////////////////////////////////////////////
	//        InstrumentDataListener Implementations       //
	/////////////////////////////////////////////////////////

	/** dataCallback from the sensors.
	 Fulfills InstrumentDataListener interface
	 */
	public void dataCallback(DevicePacket sensorData, PacketParser.Field[] fields)
	{
		_log4j.warn("dataCallback does nothing");
	}
	
	/** Callback for InstrumentDataListener interface, called when 
	 service is registered with the InstrumentRegistry
	 Fulfills InstrumentDataListener interface
	 */
	public void serviceRegisteredCallback(RegistryEntry entry)
	{
		_log4j.info("serviceRegisteredCallback does nothing");
	}
	
	/** Validate velocity value.
		By default, does nothing and relies on the
		configured ElmoImpl limits.
		Subclasses may override this method to provide
		special range validatation logic.
		For example, a service may wish to impose constraints 
		 like a range that includes 0 rpm and 50-100 rpm, but 
		no speeds between 0-50.
	 */
	public void validateSpeed(int counts)
	throws IllegalArgumentException{
		// no action by default
		return;
	}
	
	///////////////////////////////////////////////
	//        ElmoIF Implementations       //
	///////////////////////////////////////////////
	
	/////////////////////////////////
	//        Motor Control        //
	/////////////////////////////////
	
	/** Initialize motor controller
	 */
	public void initializeController() 
	throws TimeoutException, IOException,Exception
	{
		_elmo.initializeController();
	}
	
	public void initializeController(int serialMode,
									 int countsPerRevolution,
									 double gearRatio,
									 int mode,
									 int acceleration,
									 int deceleration,
									 int stopDeceleration) 
	throws TimeoutException, IOException, Exception
	{
		_elmo.initializeController(_attributes.serialMode,
								   _attributes.countsPerRevolution,
								   _attributes.gearRatio,
								   _attributes.unitMode,
								   _attributes.motorAcceleration,
								   _attributes.motorDeceleration,
								   _attributes.motorStopDeceleration);		
	}
	
	/** Convert motor speed (rpm)  to (counts/sec) 
	 (before gear train)
	 (using countsPerRevolution)
	 */
	public int rpm2counts(double rpm) 
	throws Exception{
		return _elmo.rpm2counts(rpm);
	}
	
	/** Convert motor speed (counts/sec) to (rpm)
	 (before gear train) 
	 (using countsPerRevolution)
	 */
	public double counts2rpm(int counts) throws Exception{
		return _elmo.counts2rpm(counts);
	}
	
	/** Convert output shaft speed (rpm) to (counts/sec)
	 (at output after gear train)
	 (using gear ratio and counts per revolution) 
	 */
	public int orpm2counts(double rpm) throws Exception{
		return _elmo.orpm2counts(rpm);		
	}
	
	/** Convert output shaft speed (counts/sec) to (rpm)
	 (at output after gear train)
	 (using gear ratio and counts per revolution) 
	 */
	public double counts2orpm(int counts) throws Exception{
		return _elmo.counts2orpm(counts);
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
		_attributes.countsPerRevolution=value;
	}
	public int getCountsPerRevolution(){
		return _attributes.countsPerRevolution;
	}
	
	
	/** start motion using current settings n
	 */
    public void setEnable(boolean value, long timeoutMsec)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{
		_elmo.setEnable(value,timeoutMsec);
	}
	
	/** Set the gear ratio between the motor and the output shaft.
	 For example, if the output shaft turns 67.5 times slower than
	 the motor, the gear ratio should be set to 67.5.
	 */
	public void  setGearRatio(double value)
	throws IllegalArgumentException{
		_elmo.setGearRatio(value);
	}
	
	/** Get the gear ratio between the motor and the output shaft.
	 For example, if the output shaft turns 67.5 times slower than
	 the motor, the gear ratio should be set to 67.5.
	 */
	public double getGearRatio() {
		return _elmo.getGearRatio();
	}
	
	/** Set motor position counter for position modes
	 @param positionCounts new value of position counter (counts)
	 fulfills ElmoIF interface 
	 */
	public void setPositionCounter(long positionCounts) 
	throws TimeoutException, IOException,NullPointerException,IllegalArgumentException,Exception
	{
		_elmo.setPositionCounter(positionCounts);
	}
	
	/** Get motor position counter for position modes 
	 @return position counter (counts)
	 */
	public long getPositionCounter()
	throws TimeoutException, IOException, NullPointerException, Exception{
		return _elmo.getPositionCounter();
	}
	
	/** get motor jogging (commanded) velocity in counts/sec. */
    public int getJoggingVelocity()
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{
		return _elmo.getJoggingVelocity();
	}
	
	/** get motor feedback velocity in counts/sec. */
    public int getEncoderVelocity()
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{
		return _elmo.getEncoderVelocity();
	}
    
	/** get (average of nSamples) motor feedback velocity in counts/sec. */
    public int getEncoderVelocity(int nSamples)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{
		return _elmo.getEncoderVelocity(nSamples);
	}
	
	/** return motor enabled (MO) status. */
    public boolean isEnabled()
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception{
		return _elmo.isEnabled();
	}
	
    /** return motor status. */
    public int getStatusRegister()
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{
		return _elmo.getStatusRegister();
	}
	
    /** return detailed motor fault information. */
    public int getFaultRegister()
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{
		return _elmo.getStatusRegister();
	}	
	
	/** Get difference (counts) between commanded 
	 and actual position if available 
	 @return position error (counts)
	 */
	public long getPositionError()
	throws TimeoutException, IOException,Exception{
		return _elmo.getPositionCounter();
	}	
	
	/** set motor velocity in counts 
	 Do not initiate motion
	 */
    public void setJoggingVelocity(int counts)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{
		validateSpeed(counts);
		_elmo.setJoggingVelocity(counts);
	}
	
	/** set motor PTP velocity (used for Absolute motion) in rpm 
	 Do not initiate motion
	 */
    public void setPTPSpeed(int counts)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{
		validateSpeed(counts);
		_elmo.setPTPSpeed(counts);
	}
	
	/** show current configuration information */
	public String showConfiguration()
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{
		return _elmo.showConfiguration();
	}
	
	/** read an Elmo register value */
	public String readRegister(String register)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{
		return _elmo.readRegister(register);
	}
	
    /** start motion using current settings n
	 */
	
    public void beginMotion()
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{
		_elmo.beginMotion();
	}
	
	/** command motor velocity in counts/sec */
    public void jog(int counts)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{
		validateSpeed(counts);
		_elmo.jog(counts);
	}
	
	/** Move motor relative to current position
	 @param distanceCounts distance to move (counts)
	 */
	public void ptpRelative(long distanceCounts,boolean wait) 
	throws TimeoutException, IOException,NullPointerException,IllegalArgumentException,Exception
	{
		_elmo.ptpRelative(distanceCounts,wait);
	}
		
	/** Move motor to absolute position
	 Motion may be subject to modulo position counting 
	 modes in effect.	 
	 @param position to move to (counts)
	 */
	public void ptpAbsolute(long position,boolean wait) 
	throws TimeoutException, IOException,NullPointerException,IllegalArgumentException,Exception
	{
		_elmo.ptpAbsolute(position,wait);
	}
	
	/** delay for specified number of milliseconds 
	 @param delayMsec delay duration in milliseconds
	 */
	public void delay(long delayMsec)
	{
		_elmo.delay(delayMsec);
	}
	
	/**
	 * Override base class readSample (defined in BaseInstrumentService)
	 *
	 * @param sample output buffer
	 */
	protected int readSample(byte[] sample) 
	throws TimeoutException,IOException, Exception{
		throw new Exception("readSample method not implemented - Subclasses should override readSample");
	} 
			
	public class Attributes extends InstrumentServiceAttributes {
		/** serial version, for Serializable interface */
		//public final static long serialVersionUID=0L;

		/*
		String calCounts   ="0 675 1350 2025 2700 3375 4050 5400";
		String calPositions="0 1 7 15 25 35 41 45";
		double calFPO=0.0;
		double calFPN=0.0;
		int calMode=CSpline.MODE_LINEAR;
		 */
		/** Serial mode */
		public int serialMode=Elmo.MODE_SERIAL_RFC1722;
		
		/** Elmo controller operating mode */
		public int unitMode=Elmo.MODE_SPEED;
		
		/** number of counts per motor revolution (before gear reduction) */
		public int countsPerRevolution=DEFAULT_COUNTS_PER_REVOLUTION;
		
		/** Motor acceleration (counts/sec^2) */
		public int motorAcceleration=100;
		
		/** Motor deceleration (counts/sec^2)
		 Used for changing jogging velocity.
		 Deceleration for stopping is controlled
		 separately.
		 */
		public int motorDeceleration=100;
		
		/** Motor stop deceleration (counts/sec^2) 
		 Controls rate at which motor stops 
		 when ST command is issued; deceleration
		 during jogging velocity change is handled
		 separately.
		 */
		public int motorStopDeceleration=1000;
		
		/** gear ratio motorTurns:outputTurns */
		public double gearRatio=67.5;
		
		/** default min motor speed */
		public int minMotorCounts=(-Elmo.MAX_VELOCITY_COUNTS);
		/** default max motor speed */
		public int maxMotorCounts=(Elmo.MAX_VELOCITY_COUNTS);
				
		/** operation mode
		 valid values:
		 closedLoop - let the ControlLoop service set motor velocity
		 openLoop   - set motor velocity using properties
		 */
		public String mode=OPEN_LOOP;
		
		/** enable motor if true.
		 motor will not operate if set to false.	   
		 */
		public boolean enabled=false;
				

		/** debug flag (bitfield) that may be used to 
			selectively enable/disable debug output
		 */	
		public int debugFlag=0x0;		
		
		/**
		 * Constructor, with required InstrumentService argument
		 */
		public Attributes(DeviceServiceIF service) {
			super(service);

		}
		
		/**
		 * Throw InvalidPropertyException if any invalid attribute values found
		 */
		public void checkValues() 
		throws InvalidPropertyException 
		{
			if(_elmo!=null && _elmo.getMotor()!=null){
				if(_elmo.getGearRatio()!=gearRatio){
					try{
						_elmo.setGearRatio(gearRatio);
					}catch (IllegalArgumentException iae) {
						iae.printStackTrace();
						throw new InvalidPropertyException("Invalid gearRatio ["+gearRatio+"]");
					}
				}
				if(_elmo.getCountsPerRevolution()!=countsPerRevolution){
					try{
						_elmo.setCountsPerRevolution(countsPerRevolution);
					}catch (IllegalArgumentException iae) {
						iae.printStackTrace();
						throw new InvalidPropertyException("Invalid countsPerRevolution ["+countsPerRevolution+"]");
					}
				}
				try{
					if(_elmo.getMotorAcceleration()!=motorAcceleration){
						try{
							_elmo.setMotorAcceleration(motorAcceleration);
						}catch (IllegalArgumentException iae) {
							iae.printStackTrace();
							throw new InvalidPropertyException("Invalid motorAcceleration ["+motorAcceleration+"]");
						}
					}
				}catch (Exception e) {
					e.printStackTrace();
				}
				
				try{
					if(_elmo.getMotorDeceleration()!=motorDeceleration){
						try{
							_elmo.setMotorDeceleration(motorDeceleration);
						}catch (IllegalArgumentException iae) {
							iae.printStackTrace();
							throw new InvalidPropertyException("Invalid motorDeceleration ["+motorDeceleration+"]");
						}catch (TimeoutException te) {
							te.printStackTrace();
						}catch (Exception e) {
							e.printStackTrace();
						}
					}
				}catch (Exception e) {
					e.printStackTrace();
				}
				try{
					if(_elmo.getMotorStopDeceleration()!=motorStopDeceleration){
						try{
							_elmo.setMotorStopDeceleration(motorStopDeceleration);
						}catch (IllegalArgumentException iae) {
							iae.printStackTrace();
							throw new InvalidPropertyException("Invalid motorStopDeceleration ["+motorStopDeceleration+"]");
						}catch (TimeoutException te) {
							te.printStackTrace();
						}catch (Exception e) {
							e.printStackTrace();
						}
					}
				}catch (Exception e) {
					e.printStackTrace();
				}
				
				int maxCounts=(Elmo.MAX_VELOCITY_COUNTS);
				int minCounts=(-Elmo.MAX_VELOCITY_COUNTS);
				if( (minMotorCounts < minCounts) ||
				   (maxMotorCounts> maxCounts) ){
					throw new InvalidPropertyException("motor min/max speed must be in range " + minCounts+" to "+maxCounts+" counts/sec");
				}else{

					if(_elmo.getMinMotorCounts()!=minMotorCounts){
						try{
							_elmo.setMinMotorCounts(minMotorCounts);
						}catch (IllegalArgumentException iae) {
							iae.printStackTrace();
							throw new InvalidPropertyException("Invalid minMotorCounts ["+minMotorCounts+"]");
						}
					}
					if(_elmo.getMaxMotorCounts()!=maxMotorCounts){
						try{
							_elmo.setMaxMotorCounts(maxMotorCounts);
						}catch (IllegalArgumentException iae) {
							iae.printStackTrace();
							throw new InvalidPropertyException("Invalid maxMotorCounts ["+maxMotorCounts+"]");
						}
					}
				}
				
			}
			
					
		}	
	}
}
