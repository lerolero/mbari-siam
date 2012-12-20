/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.elmo.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.StringTokenizer;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

import org.mbari.siam.utils.StreamUtils;
import org.mbari.siam.distributed.TimeoutException;

import gnu.io.SerialPort;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.NoSuchPortException;
import gnu.io.UnsupportedCommOperationException;

/** Base class for Elmo motor controllers.
 */
/*
 $Id: Elmo.java,v 1.26 2011/04/12 21:12:18 headley Exp $
 $Name:  $
 $Revision: 1.26 $
 */

public abstract class Elmo{
    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(Elmo.class);
	
    /**
	 Constants
	 */
		
	/** Absolute max allowed commanded speed (counts per second) */
    public final static int MAX_VELOCITY_COUNTS=3400;//508;
    /** Absolute max allowed actual speed (counts per second) */
    public final static int MAX_VELOCITY_LIMIT=10800;//1080;
    /** Absolute min allowed acceleration (counts per second^2) */
    public final static int MIN_ACCELERATION=100;
    /** Absolute min allowed deceleration (counts per second^2) */
    public final static int MIN_DECELERATION=100;
    /** Absolute max allowed acceleration (counts per second^2) */
    public final static int MAX_ACCELERATION=1000000000;//20000000;
    /** Absolute max allowed deceleration (counts per second^2) */
    public final static int MAX_DECELERATION=1000000000;//20000000;
    /** Absolute min allowed stopping deceleration (counts per second^2) */
    public final static int MIN_STOP_DECELERATION=400;
    /** Absolute max allowed stopping deceleration (counts per second^2) */
    public final static int MAX_STOP_DECELERATION=1000000000;
	/** min position counter value */
	public final static long PX_MIN_VALUE=-1000000000;
	/** max position counter value */
	public final static long PX_MAX_VALUE=1000000000;
	/** min x-modulus value */
	public final static long MAX_XMODULUS=1000000000;
	/** max x-modulus value */
	public final static long MIN_XMODULUS=-1000000000;
	public final static int IL_MIN_INDEX=1;
	public final static int IL_MAX_INDEX=10;
	public final static int IL_MIN_VALUE=0;
	public final static int IL_MAX_VALUE=23;
	
    /** Motor counts per revolution 
	 Note: this depends on the motor commutation and feedback 
	 mechanism. This default value is for a simple DC brushless
	 motor with a single pole pair.
	 */
    public final static int COUNTS_PER_REVOLUTION=6;
	
    /** Timing Constants */
    /** Command timeout (milliseconds) */
    public final static long TM_CMD_MSEC=2000L;//1000L;
    /** motor startup timeout (milliseconds) */
    public final static long TM_START_MSEC=500L;
    /** motor stop timeout (milliseconds) */
    public final static long TM_STOP_MSEC=500L;
    /** polling delay during motor start/stop (milliseconds) */
    public final static long TM_POLL_DELAY_MSEC=10L;
    /** default timeout during input stream flush */
    public final static long TM_EMPTY_INPUT_MSEC=0L;//50L, 100L, 5L: setting this > 0 will cause CPU use to soar
    /** default motion wait polling interval */
    public final static long TM_MOTION_WAIT_MSEC=125L;
	/** time between writing and reading command (see cmdWriteRead) */
   // public final static long TM_CMD_DELAY_MSEC=75L;
	
    /** seconds per minute (int) */
    public final static int INT_SEC_PER_MIN=60;
    /** seconds per minute (double) */
    public final static double DBL_SEC_PER_MIN=60.0;
	
    /** Buffer size (32) */
    public final static int BUF32=32;
	
    /** Buffer size (128) */
    public final static int BUF128=128;
	
    /** Unit Mode Constants */
    /** Unit Mode: Speed */
    public final static int MODE_SPEED=2;
    /** Unit Mode: Stepper */
    public final static int MODE_STEPPER=3;
    /** Unit Mode: Torque */
    public final static int MODE_TORQUE=1;
    /** Unit Mode: Dual Feedback Position */
    public final static int MODE_DUAL_FEEDBACK_POSITION=4;
    /** Unit Mode: Single Feedback Position */
    public final static int MODE_SINGLE_FEEDBACK_POSITION=5;

	/** Driver serial mode - use local serial port
	 If this mode is selected command pacing is not used during cmdWriteRead
	 (not an Elmo mode) 
	 */
    public final static int MODE_SERIAL_LOCAL=0;
	/** Driver RFC1722 mode - use ethernet/serial converter
		e.g. Digi. 
		If this mode is selected, command pacing is used during cmdWriteRead
		(not an Elmo mode) 
	 */
    public final static int MODE_SERIAL_RFC1722=1;
	
	public static final int MOTOR_ENABLED=1;
	public static final int MOTOR_DISABLED=0;

	public static final int MASK_DIGITAL_INPUT_PORT=0x6FFFFFFF;
	public static final int MASK_DIGITAL_INPUT_ACTIVE=0x3F;
	public static final int MASK_DIGITAL_INPUT_MAINHOME=0x40;
	public static final int MASK_DIGITAL_INPUT_AUXHOME=0x80;
	public static final int MASK_DIGITAL_INPUT_SOFTSTOP=0x100;
	public static final int MASK_DIGITAL_INPUT_HARDSTOP=0x200;
	public static final int MASK_DIGITAL_INPUT_FLS=0x400;
	public static final int MASK_DIGITAL_INPUT_RLS=0x800;
	public static final int MASK_DIGITAL_INPUT_INH=0x1000;
	public static final int MASK_DIGITAL_INPUT_BG=0x2000;
	public static final int MASK_DIGITAL_INPUT_ABORT=0x4000;
	public static final int MASK_DIGITAL_INPUT_PIN=0x3FF0000;
	public static final int DIGITAL_INPUT_BIT0=0;
	public static final int DIGITAL_INPUT_BIT1=1;
	public static final int DIGITAL_INPUT_BIT2=2;
	public static final int DIGITAL_INPUT_BIT3=3;
	public static final int DIGITAL_INPUT_BIT4=4;
	public static final int DIGITAL_INPUT_BIT5=5;
	
	public static final int MS_POS_STABLIZED=0;
	public static final int MS_REF_STATIONARY=1;
	public static final int MS_REF_CONTROLLED=2;
	public static final int MS_RESERVED=3;

	public final static int HEX_MODE_DISABLED=0;
	public final static int HEX_MODE_ENABLED=1;

	/** Input Logic Mode */
    public final static int INPUT_LOGIC_FREEWHEEL_LO=0;    
    /** Input Logic Mode */
    public final static int INPUT_LOGIC_FREEWHEEL_HI=1;    
    /** Input Logic Mode */
    public final static int INPUT_LOGIC_HARDSTOP_LO=2;    
    /** Input Logic Mode */
    public final static int INPUT_LOGIC_HARDSTOP_HI=3;    
    /** Input Logic Mode */
    public final static int INPUT_LOGIC_IGNORE_LO=4;    
    /** Input Logic Mode */
    public final static int INPUT_LOGIC_IGNORE_HI=5;    
    /** Input Logic Mode */
    public final static int INPUT_LOGIC_GP_LO=6;    
    /** Input Logic Mode */
    public final static int INPUT_LOGIC_GP_HI=7;    
    /** Input Logic Mode */
    public final static int INPUT_LOGIC_RLS_LO=8;    
    /** Input Logic Mode */
    public final static int INPUT_LOGIC_RLS_HI=9;    
    /** Input Logic Mode */
    public final static int INPUT_LOGIC_FLS_LO=10;    
    /** Input Logic Mode */
    public final static int INPUT_LOGIC_FLS_HI=11;    
    /** Input Logic Mode */
    public final static int INPUT_LOGIC_BG_LO=12;    
    /** Input Logic Mode */
    public final static int INPUT_LOGIC_BG_HI=13;    
    /** Input Logic Mode */
    public final static int INPUT_LOGIC_SOFTSTOP_LO=14;    
    /** Input Logic Mode */
    public final static int INPUT_LOGIC_SOFTSTOP_HI=15;    
	/** Input Logic Mode */
    public final static int INPUT_LOGIC_MAINHOME_LO=16;    
    /** Input Logic Mode */
    public final static int INPUT_LOGIC_MAINHOME_HI=17;    
    /** Input Logic Mode */
    public final static int INPUT_LOGIC_AUXHOME_LO=18;    
    /** Input Logic Mode */
    public final static int INPUT_LOGIC_AUXHOME_HI=19;    
    /** Input Logic Mode */
    public final static int INPUT_LOGIC_BOTHSTOP_LO=20;    
    /** Input Logic Mode */
    public final static int INPUT_LOGIC_BOTHSTOP_HI=21;    
	/** Input Logic Mode */
    public final static int INPUT_LOGIC_ABORT_LO=22;    
    /** Input Logic Mode */
    public final static int INPUT_LOGIC_ABORT_HI=23; 

	/** Homing Behavior: stop motor */
    public final static String HOMING_BEHAVIOR_STOP="0";
    /** Homing Behavior: set digital output */
    public final static String HOMING_BEHAVIOR_SET_OUTPUT="1";
    /** Homing Behavior: do nothing */
    public final static String HOMING_BEHAVIOR_NONE="2";
	/** Homing Event Definition: trigger main switch closure */
    public final static String HOMING_EVENT_MAIN_HOME_SWITCH="1";
    /** Homing Event Definition: trigger main switch closure */
    public final static String HOMING_EVENT_DIN5="17";
    /** Homing Event Definition: trigger immediate (manual) */
    public final static String HOMING_EVENT_IMMEDIATE="0";
	/** Homing PX register behavior: absolute */
    public final static String HOMING_EVENT_ABS="0";
    /** Homing PX register behavior: relative */
    public final static String HOMING_EVENT_REL="1";
    /** Homing PX register behavior: no action */
    public final static String HOMING_EVENT_NONE="2";
	
	/** Echo mode enabled */
    public final static int ECHO_DISABLED=0;
    /** Echo mode disabled */
    public final static int ECHO_ENABLED=1;
	
	
    /** Motion Commands */

    /** Set/Get unit motion mode 
		torque:1 velocity:2 position:3-5
	 */
    public final static String CMD_UNIT_MODE="UM";
    /** Motor enable state*/
    public final static String CMD_MOTOR_ENABLE="MO";
    /** Begin motion */
    public final static String CMD_BEGIN_MOTION="BG";
    /** Stop motor */
    public final static String CMD_STOP_MOTION="ST";
    /** Get/Set acceleration */
    public final static String CMD_ACCELERATION="AC";
    /** Get/Set motion deceleration */
    public final static String CMD_DECELERATION="DC";
    /** Get/Set stop deceleration */
    public final static String CMD_STOP_DECELERATION="SD";
    /** Get/Set lower reference speed command */
    public final static String CMD_SPEED_REF_LO="VL[2]";
    /** Get/Set upper reference speed command */
    public final static String CMD_SPEED_REF_HI="VH[2]";
    /** Get/Set lower reference speed limit */
    public final static String CMD_SPEED_LIMIT_LO="LL[2]";
    /** Get/Set upper reference speed limit */
    public final static String CMD_SPEED_LIMIT_HI="HL[2]";
    /** Get/Set lower reference position */
    public final static String CMD_POSITION_REF_LO="VL[3]";
    /** Get/Set upper reference position */
    public final static String CMD_POSITION_REF_HI="VH[3]";
    /** Get/Set X Modulus */
    public final static String CMD_SET_XMODULUS_LO="XM[1]";
    /** Get/Set X Modulus */
    public final static String CMD_SET_XMODULUS_HI="XM[2]";

    /** Get/Set jogging velocity */
    public final static String CMD_JOGGING_VELOCITY="JV";
    /** Get/Set max speed for PTP (point to point) motion
	 Although SP is not used in velocity mode, it must be set
	 so that SP<|VL[2]| and SP<|VH[2]|. If not, parameters
	 VL,VH,LL,HL may not be correctly written to flash
	 */
    public final static String CMD_PTP_SPEED="SP";
    /** Get velocity main encoder velocity */
    public final static String CMD_GET_MAIN_ENC_VELOCITY="VX";
    /** Get velocity aux encoder velocity */
    public final static String CMD_GET_AUX_ENC_VELOCITY="VY";
    /** Get velocity error */
    public final static String CMD_GET_VELOCITY_ERROR="VE";
    /** Get commanded velocity */
    public final static String CMD_GET_DESIRED_VELOCITY="DV[2]";

	/** Get/Set relative position */
    public final static String CMD_RELATIVE_POSITION="PR";
    /** Get/Set absolute position */
    public final static String CMD_ABSOLUTE_POSITION="PA";
	/** Get/set position counter */
	public final static String CMD_POSITION_COUNTER ="PX";
	/** Get position error */
    public final static String CMD_POSITION_ERROR="PE";
	
	/** Get/Set Input Functions IL[N] */
    public final static String CMD_INPUT_LOGIC ="IL";
	
	
    /** Commutation, Filter and Tuning Parameters */
    /** Get/Set Digital input filter */
    public final static String CMD_DIGITAL_INPUT_FILTER="IF";
    /** Get/Set controller current PID gain (Integral) */
    public final static String CMD_CURRENT_I_FILTER="KI[1]";
    /** Get/Set controller current PID gain (Proportional) */
    public final static String CMD_CURRENT_P_FILTER="KP[1]";
    /** Get/Set controller velocity PID gain (Integral) */
    public final static String CMD_VELOCITY_I_FILTER="KI[2]";
    /** Get/Set controller velocity PID gain (Proportional) */
    public final static String CMD_VELOCITY_P_FILTER="KP[2]";
    /** Get/Set commutation array parameters */
    public final static String CMD_COMMUTATION_ARRAY="CA";
	
    /** Current Limits */
    /** Get (read-only) Max active current limit */
    public final static String CMD_MAX_CURRENT="MC";
    /** Get/set peak active current limit (A) 
		Max value=MC
	 */
    public final static String CMD_PEAK_CURRENT_LIMIT="PL[1]";
    /** Get/set peak active current duration (sec) */
    public final static String CMD_PEAK_CURRENT_DURATION="PL[2]";
    /** Get/set continuous active current limit 
	 Max value=MC/2
	 Supply current = (active current) * (PWM duty cycle)
	 */
    public final static String CMD_CONT_CURRENT_LIMIT="CL[1]";
    /** Get/set torque limit (% of continuous current limit)
	 If <2, stuck motor protection is disabled
	 */
    public final static String CMD_TORQUE_LIMIT="CL[2]";
    /** Get/set min stuck motor speed (counts/s)
     */
    public final static String CMD_STUCK_SPEED="CL[3]";
	
    /** Optional Mode Settings */
    /** Get/Set echo mode */
    public final static String CMD_ECHO="EO";//enable:1 disable:0
	
    /** Get/Set hex mode */
    public final static String CMD_HEXMODE="HX";
	
    /** Set hex mode off */
    public final static String CMD_DUMP_CPU="CD";
    /** Get/Set digital output state */
    public final static String CMD_DIGITIAL_OUTPUT="OP";
    /** Set digital input state */
    public final static String CMD_GET_DIGITAL_INPUTS="IP";
    /** Get status register */
    public final static String CMD_GET_STATUS_REGISTER="SR";
    /** Get motion status register */
    public final static String CMD_GET_MOTION_STATUS="MS";
    /** Get motor fault register */
    public final static String CMD_GET_MOTOR_FAULT="MF";
    /** Get serial numbers
	 SN[1] returns the vendor ID.
	 SN[2] returns the product code.
	 SN[3] returns the revision number.
	 SN[4] returns the serial number.
	 */
    public final static String CMD_GET_SERIAL_NUMBER="SN";
    /** Get software (firmware) version */
    public final static String CMD_GET_VERSION="VR\n";
	
    /** Homing Behavior Commands */
    /** Get/Set homing activation mode */
    public final static String CMD_HOMING_ACTIVATION_MODE="HM[1]";
    /**  Arm homing process */
    public final static String CMD_ARM_HOMING_PROCESS="HM[1]=1";
    /**  Disarm homing process */
    public final static String CMD_DISARM_HOMING_PROCESS="HM[1]=0";
    /** Get/Set homing absolute value */
    public final static String CMD_HOMING_ABSOLUTE_VALUE="HM[2]";
    /** Get/Set homing event */
    public final static String CMD_HOMING_EVENT_DEFINITION="HM[3]";
    /** Get/Set homing behavior (what to do on homing event trigger) */
    public final static String CMD_HOMING_BEHAVIOR="HM[4]";
    /** Get/Set homing event PX register behavior */
    public final static String CMD_HOMING_EVENT_PX="HM[5]";
    /** Get/Set homing digital output value */
    public final static String CMD_HOMING_OUTPUT_VALUE="HM[6]";
    /** Configure high speed input as active LO/ignore */
    public final static String CMD_SET_DIO5_IGNORE_LO="IL[5]=4";
    /** Configure high speed input as active HI/ignore */
    public final static String CMD_SET_DIO5_IGNORE_HI="IL[5]=5";
	/** Configure high speed input as active HI/general purpose */
    public final static String CMD_GENERAL_PURPOSE_HI="IL[5]=7";
	/** Configure high speed input as active LO/main homing trigger */
    public final static String CMD_ENABLE_MAIN_HOME_SEQUENCE_LO="IL[5]=16";
    /** Configure high speed input as active HI/main homing trigger */
    public final static String CMD_ENABLE_MAIN_HOME_SEQUENCE_HI="IL[5]=17";
    /** Configure high speed input as active LO/aux homing trigger */
    public final static String CMD_ENABLE_AUX_HOME_SEQUENCE_LO="IL[5]=18";
    /** Configure high speed input as active HI/aux homing trigger */
    public final static String CMD_ENABLE_AUX_HOME_SEQUENCE_HI="IL[5]=19";    
	
	//////////////////////////
	// Motor Fault Register 
	// Constants
	//////////////////////////
	
	public static final int MF_OK                = 0x0;
	public static final int MF_ANALOG_FEEBACK    = 0x1;
	public static final int MF_FEEDBACK_LOSS     = 0x4;
	public static final int MF_PEAK_CURRENT      = 0x8;
	public static final int MF_INHIBIT           = 0x10;
	public static final int MF_MULT_HALL         = 0x40;
	public static final int MF_SPEED_TRACKING    = 0x80;
	public static final int MF_POSITION_TRACKING = 0x100;
	public static final int MF_DATABASE          = 0x200;
	public static final int MF_ECAM              = 0x400;
	public static final int MF_HEARTBEAT         = 0x800;
	public static final int MF_SERVO             = 0x1000;
	public static final int MF_UNDER_VOLTAGE     = 0x3000;
	public static final int MF_OVER_VOLTAGE      = 0x5000;
	public static final int MF_SHORT_CIRCUIT     = 0xB000;
	public static final int MF_TEMPERATURE       = 0xF000;
	public static final int MF_ZERO_NOT_FOUND    = 0x10000;
	public static final int MF_SPEED             = 0x20000;
	public static final int MF_STACK_OVERFLOW    = 0x40000;
	public static final int MF_CPU               = 0x80000;
	public static final int MF_STUCK             = 0x200000;
	public static final int MF_POSITION          = 0x400000;
	public static final int MF_NO_START          = 0x20000000;
	
	//////////////////////////
	// Status Fault Register 
	// Field Constants and offsets
	//////////////////////////
	public static final int SR_DRIVE_READ            = 0x00000001;
	public static final int SR_STATUS                = 0x0000000E;
	public static final int SR_MOTOR_ON              = 0x00000010;
	public static final int SR_REFERENCE_MODE        = 0x00000020;
	public static final int SR_MOTOR_FAILURE_LATCHED = 0x00000040;
	public static final int SR_UNIT_MODE             = 0x00000380;
	public static final int SR_GAIN_SCHEDULING_ON    = 0x00000400;
	public static final int SR_HOMING_PROCESSING     = 0x00000800;
	public static final int SR_PROGRAM_RUNNING       = 0x00001000;
	public static final int SR_CURRENT_LIMIT_ON      = 0x00002000;
	public static final int SR_MOTION_STATUS         = 0x0000C000;
	public static final int SR_RECORDER_STATUS       = 0x00030000;
	public static final int SR_DIGITAL_HALLS         = 0x07000000;
	public static final int SR_CPU_STATUS            = 0x08000000;
	public static final int SR_LIMIT_STOP            = 0x10000000;
	public static final int SR_USER_PROGRAM_ERROR    = 0x20000000;
	
	public static final int SR_OFFSET_DRIVE_READ            = 0;
	public static final int SR_OFFSET_STATUS                = 1;
	public static final int SR_OFFSET_MOTOR_ON              = 4;
	public static final int SR_OFFSET_REFERENCE_MODE        = 5;
	public static final int SR_OFFSET_MOTOR_FAILURE_LATCHED = 6;
	public static final int SR_OFFSET_UNIT_MODE             = 7;
	public static final int SR_OFFSET_GAIN_SCHEDULING_ON    = 10;
	public static final int SR_OFFSET_HOMING_PROCESSING     = 11;
	public static final int SR_OFFSET_PROGRAM_RUNNING       = 12;
	public static final int SR_OFFSET_CURRENT_LIMIT_ON      = 13;
	public static final int SR_OFFSET_MOTION_STATUS         = 14;
	public static final int SR_OFFSET_RECORDER_STATUS       = 16;
	public static final int SR_OFFSET_DIGITAL_HALLS         = 24;
	public static final int SR_OFFSET_CPU_STATUS            = 27;
	public static final int SR_OFFSET_LIMIT_STOP            = 28;
	public static final int SR_OFFSET_USER_PROGRAM_ERROR    = 29;
	
	
    /** Serial port for Elmo control I/O */
    protected SerialPort _serialPort;
    /** Serial port input stream */
    protected InputStream _serialRx;
    /** Serial port output stream */
    protected OutputStream _serialTx;
    /** Response terminator (as String) */
    protected String _terminatorString=";";
	/** line end for terminating command */
 	public final static byte[] LINE_END_BYTES="\n".getBytes();
   /** Response terminator (as byte array) */
    protected byte[] _terminator=_terminatorString.getBytes();
	/** Buffer for reading disposable input */
	protected byte[] _junk32=new byte[BUF32];
	protected byte[] _longBuf=new byte[BUF32];
	protected byte[] _intBuf=new byte[BUF32];
	protected byte[] _doubleBuf=new byte[BUF32];
	protected byte[] _stringBuf=new byte[BUF32];
	
	
	/** Serial port mode. 
		Use MODE_SERIAL_LOCAL if using a standard hardware serial port
		User MODE_SERIAL_RFC_1722 if using a serial/ethernet converter (e.g. Digi)
	 */
	protected int _serialMode=MODE_SERIAL_LOCAL;

   //StringBuffer _dbg=new StringBuffer();
	
    /** Constructor */
    public Elmo(SerialPort port) throws IOException {
		super();
		setSerialPort(port);
    }
    /** Set the SerialPort for this controller to use.
	 @param port SerialPort to use
	 */
    public void setSerialPort(SerialPort port)throws IOException{
		_serialPort=port;
		if(_serialPort!=null){
			_serialRx=port.getInputStream();
			_serialTx=port.getOutputStream();
		}
    }
    /** Return SerialPort used by this controller.
	 @return serial port or null if not set.
     */
    public SerialPort getSerialPort(){
		return _serialPort;
    }

	/** Return serial port input stream used by this controller.
	 @return input stream or null if not set.
     */
    public InputStream getInputStream(){
		return _serialRx;
    } 
	/** Return serial port output stream used by this controller.
	 @return output stream or null if not set.
     */
	public OutputStream getOutputStream(){
		return _serialTx;
    }
		
    /** Sleep for specified time
	 @param delayMsec Time to sleep (milliseconds)
     */
    protected void delay(long delayMsec){
		try{
			Thread.sleep(delayMsec);
		}catch(Exception e){
		}
    }
		
	/** Convert motor speed from counts/second to RPM
	 @param rpm motor speed (RPM)
	 @param countsPerRevolution counts per motor revolution
	 @return  motor speed (counts/sec)
     */
	public static int rpm2counts(double rpm, int countsPerRevolution){
		double dres=(rpm*(double)countsPerRevolution/DBL_SEC_PER_MIN);
		//int ires=(int)dres;
					 
		//_log4j.info("rpm="+rpm+" cpr="+countsPerRevolution+" counts="+ires+"/"+dres);
		return (int)dres;
    }
	
    /** Convert motor speed from counts/second to RPM
	 @param counts motor speed (counts/second)
	 @param countsPerRevolution counts per motor revolution
	 @return  motor speed (RPM)
     */
    public static double counts2rpm(int counts, int countsPerRevolution){
		return counts*DBL_SEC_PER_MIN/(double)countsPerRevolution;
    }
	
 	public static String mkIndex(String cmd, int index){
		return (cmd+"["+index+"]");
	}
	public static String mkIndexCmd(String cmd, int index, String value){
		return cmd+"["+index+"]="+value;
	}
	public static String mkIndexCmd(String cmd, int index, int value){
		return cmd+"["+index+"]="+value;
	}
	public static String mkIndexCmd(String cmd, int index, long value){
		return cmd+"["+index+"]="+value;
	}
	public static String mkCmd(String cmd, String value){
		return cmd+"="+value;
	}
	public static String mkCmd(String cmd, int value){
		return cmd+"="+value;
	}
	public static String mkCmd(String cmd, long value){
		return cmd+"="+value;
	}
	
	/** Flush the serial input stream.
	 (using default (this) input stream)
	 @param timeoutMsec (milliseconds)
	 */
    public void emptyInput(long timeoutMsec)
	throws IOException {
		emptyInput(_serialRx,timeoutMsec);
    }
	
    /** Flush the serial input stream.
	 (using default (this) input stream)
	 @param instream an InputStream to flush
	 @param timeoutMsec (milliseconds)
	 */
    protected void emptyInput(InputStream instream, long timeoutMsec)
    throws IOException
    {
		synchronized (this) {
			/*
			 // debug vars
			 // StringBuffer buf=new StringBuffer();
			 //int count=0;
			 int spin=0;
			 */
			//if(_log4j.isDebugEnabled()){
			//_log4j.debug("emptyInput - t:"+timeoutMsec);
			//}
			int nextByte;
			if(timeoutMsec>0L){
				long start=System.currentTimeMillis();
				while( ((System.currentTimeMillis()-start)<timeoutMsec) ){
					if( (_serialRx.available()>0L) ){
						// if available, go ahead and read as many as
						// possible, even if it goes over the timeout
						while( (_serialRx.available()>0L) ){
							nextByte=_serialRx.read();
							//buf.append((char)nextByte);
							//if(_log4j.isDebugEnabled()){
							//_log4j.debug("emptyInput -ct:"+(char)nextByte);
							//}
							//count++;
						}
					}/*else{
					 // count how many times we loop w/o reading
					 spin++;
					 }*/
				}
				//if(_log4j.isDebugEnabled()){
				//_log4j.debug("emptyInput.ct:spin "+spin+"\n");
				//}
			}else{
				// if no timeout, check once and
				// read until none available
				while(_serialRx.available()>0L){
					nextByte=_serialRx.read();
					//count++;
					//buf.append((char)nextByte);
					//if(_log4j.isDebugEnabled()){
						//_log4j.debug("emptyInput.c1:"+(char)nextByte);
					//}
				}
			}
			if(_serialRx.available()>0L){
				// if there's any left on the way out, issue warning
				_log4j.warn("emptyInput - hey, there's still stuff in the buffer!!");
			}
			//if(_log4j.isDebugEnabled()){
			//_log4j.debug("emptyInput on exit: avail:"+_serialRx.available()+" read: "+buf.length()+" bytes:["+buf.toString()+"] count:"+count+"\n\n");
			//}
		}
    }
		
    /** Write a command to the serial port.
	 terminates the command with a newline and
	 flush the serial port. Only cmdWriteRead should call this directly.
	 Use CmdWriteReadX for write w/ no return value needed.
	 @param cmd command string to write
	 */
    protected void writeCommand(String cmd)
    throws IOException
    {
		synchronized (this) {
			//if(_log4j.isDebugEnabled()){
			//_log4j.debug("writeTheCommand - ["+cmd+"]");
			//_log4j.debug("writeTheCommand - emptying");
			//}			
			// check for chars in stream, return immediately if none
			emptyInput(_serialRx,0L);
			
			//if(_log4j.isDebugEnabled()){
			//_log4j.debug("writeTheCommand - writing "+cmd);
			//}			
			// write the command, newline and flush
			_serialTx.write(cmd.getBytes());
			_serialTx.write(LINE_END_BYTES);
			_serialTx.flush();
		}
    }

	
	/** Perform one I/O transaction, (write then read).  
	 @param cmd Command to send
	 @param buf Destination buffer
	 @param terminator End of response marker
	 @param timeoutMsec Timeout (milliseconds)
	 @return  number of bytes in response. 
	 */
	
    protected int cmdWriteRead(String cmd, byte[] buf, byte[] terminator,long timeoutMsec) 
	throws TimeoutException, IOException, NullPointerException, Exception
    {
		synchronized (this) {
			//if(_log4j.isDebugEnabled()){
			//_log4j.debug("cmdWriteRead - c:"+cmd+" tm:"+new String(terminator)+" to:"+timeoutMsec+" toe:"+TM_EMPTY_INPUT_MSEC);
			//_log4j.debug("cmdWriteRead - emptying");
			//}
			// do empty the input before writing
			emptyInput(_serialRx,TM_EMPTY_INPUT_MSEC);
			
			//_log4j.debug("cmdWriteRead - writing");
			// write the command
			writeCommand(cmd);
			
			//_log4j.debug("cmdWriteRead - reading");
			long start=System.currentTimeMillis();
			
			// read until the terminator
			int retval= StreamUtils.readUntil(_serialRx,buf,terminator,timeoutMsec);
			
			//_log4j.debug("cmdWriteRead - returning "+retval+" after "+(System.currentTimeMillis()-start)+" ms\n\n");
			return retval;
		}
    }
		
	/** write a command and cleanup returned terminator (discards characters read) */
	protected int cmdWriteReadX(String cmd)	
	throws IOException
    {
		synchronized (this) {
			try{
				//if(_log4j.isDebugEnabled()){
				//_log4j.debug("cmdWriteReadX ["+cmd+"]");
				//}
				Arrays.fill(_junk32,(byte)'\0');			
				return cmdWriteRead(cmd,_junk32,_terminator,TM_CMD_MSEC);	
			}catch(Exception e){
				e.printStackTrace();
				_log4j.error(e);
			}
			return 0;
		}
	}	
	
	/** Write one command and parse the resulting return value as an integer.
	 @param cmd command to send
	 @param len maximum expected return length (bytes)
	 @param timeoutMsec timeout for return value (milliseconds)
	 @return command return value (integer)
	 */
    protected int cmdWriteReadInt(String cmd, int len, long timeoutMsec) 
	throws TimeoutException, IOException, NullPointerException, Exception
    {
		synchronized (this) {
			if(len<=0){
				throw new Exception("len must be >0");
			}
			if(_intBuf.length!=len){
				_intBuf=new byte[len];
			}
			Arrays.fill(_intBuf,(byte)'\0');
			
			int ival=Integer.MIN_VALUE;
			int bytesRead=cmdWriteRead(cmd,_intBuf,_terminator,timeoutMsec);
			String returnString=new String(_intBuf);
			if(bytesRead<=0){
				throw new IOException("stream read returned 0 [c:"+cmd+" r:"+bytesRead+" v:"+returnString+"]");
			}
			try{
				StringTokenizer st=new StringTokenizer(returnString,_terminatorString);
				ival=Integer.parseInt(st.nextToken());
			}catch(NumberFormatException e){
				throw new IOException("could not parse ["+returnString+"]");
			}
			return ival;
		}
    }
	
    /** Write one command and parse the resulting return value as a long.
	 @param cmd command to send
	 @param len maximum expected return length (bytes)
	 @param timeoutMsec timeout for return value (milliseconds)
	 @return command return value (long)
	 */
    protected long cmdWriteReadLong(String cmd, int len, long timeoutMsec) 
	throws TimeoutException, IOException, NullPointerException, Exception
    {
		synchronized (this) {
			if(len<=0){
				throw new Exception("len must be >0");
			}
			if(_longBuf.length!=len){
				_longBuf=new byte[len];
			}
			Arrays.fill(_longBuf,(byte)'\0');
			
			long lval=Long.MIN_VALUE;
			int bytesRead=cmdWriteRead(cmd,_longBuf,_terminator,timeoutMsec);
			String returnString=new String(_longBuf);
			if(bytesRead<=0){
				throw new IOException("stream read returned ["+bytesRead+"]");
			}
			try{
				StringTokenizer st=new StringTokenizer(returnString,_terminatorString);
				lval=Long.parseLong(st.nextToken());
			}catch(NumberFormatException e){
				throw new IOException("could not parse ["+returnString+"]");
			}
			return lval;
		}
    }
	
	/** Write one command and parse the resulting return value as an double.
	 @param cmd command to send
	 @param len maximum expected return length (bytes)
	 @param timeoutMsec timeout for return value (milliseconds)
	 @return command return value (double)
	 */
	protected double cmdWriteReadDouble(String cmd, int len, long timeoutMsec) 
	throws TimeoutException, IOException, NullPointerException, Exception
    {
		synchronized (this) {
			if(len<=0){
				throw new Exception("len must be >0");
			}
			if(_doubleBuf.length!=len){
				_doubleBuf=new byte[len];
			}
			double dval=Double.MIN_VALUE;
			int bytesRead=cmdWriteRead(cmd,_doubleBuf,_terminator,timeoutMsec);
			String returnString=new String(_doubleBuf);
			if(bytesRead<=0){
				throw new IOException("stream read returned ["+bytesRead+"]");
			}
			try{
				StringTokenizer st=new StringTokenizer(returnString,_terminatorString);
				dval=Double.parseDouble(st.nextToken());
			}catch(NumberFormatException e){
				throw new IOException("could not parse ["+returnString+"]");
			}
			return dval;
		}
    }
	
    /** Write one command and parse the resulting return value as a String.
	 @param cmd command to send
	 @param len maximum expected return length (bytes)
	 @param timeoutMsec timeout for return value (milliseconds)
	 @return command return value (String)
	 */
    protected String cmdWriteReadString(String cmd,int len, long timeoutMsec) 
	throws TimeoutException, IOException, NullPointerException, Exception
    {
		synchronized (this) {
			if(len<=0){
				throw new Exception("len must be >0");
			}
			if(_stringBuf.length!=len){
				_stringBuf=null;
				_stringBuf=new byte[len];
			}
			Arrays.fill(_stringBuf,(byte)'\0');

			int bytesRead=cmdWriteRead(cmd,_stringBuf,_terminator,timeoutMsec);
			String returnString=new String(_stringBuf);
			if(bytesRead<=0){
				throw new IOException("stream read returned ["+bytesRead+"]");
			}
			return returnString ;
		}
    }
		
    /** Initialize motor controller. 
	 Abstract method to be implemented by subclasses.
	 @param serialMode serial comms mode (local or RFC1722)
	 @param unitMode elmo unit mode
	 */
    public abstract void initializeController(int serialMode, int unitMode) throws TimeoutException,IOException,Exception;
		
	/** Return value (String) of Elmo register */
	public String readRegister(String register)
	throws TimeoutException, IOException, NullPointerException, Exception
	{
		return cmdWriteReadString(register,BUF128,TM_CMD_MSEC);
	}
	
    /** Return controller version information.
	 @return Controller firmware version or "UNKNOWN" on error.
	 */
    public String getVersion(){
		byte[] buf=new byte[BUF32];
		String version="UNKNOWN";
		try{
			cmdWriteRead(Elmo.CMD_GET_VERSION,buf,_terminator,TM_CMD_MSEC);
			version=new String(buf);
		}catch (Exception e) {
		 // do nothing, return default
		}
		return version;
    }
	
	/** set serial mode */
	public void setSerialMode(int mode) throws IllegalArgumentException{
		switch(mode){
			case MODE_SERIAL_LOCAL:
			case MODE_SERIAL_RFC1722:
				_serialMode=mode;
				break;
			default:
				throw new IllegalArgumentException("Invalid serial mode:["+mode+"]");
		}
	}
	/** set serial mode */
	public int getSerialMode() {
		return _serialMode;
	}
	
	/** Get max current from read-only MC parameter (A) */
    public double getMaxCurrent() 
	throws TimeoutException, IOException, NullPointerException, Exception
    {
		return cmdWriteReadDouble(CMD_MAX_CURRENT,BUF32,TM_CMD_MSEC);
    }
	
    /** Set peak current (A).
	 Max peak current is MC.
	 
	 @param peakCurrent (A)
	 */
    public  void setPeakCurrent(double peakCurrent) 
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
    {
		double maxCurrent=getMaxCurrent();
		
		if(peakCurrent>maxCurrent){
			throw new IllegalArgumentException("peak current out of range ["+peakCurrent+" ] > "+maxCurrent);
		}
		
		cmdWriteReadX(CMD_PEAK_CURRENT_LIMIT+"="+peakCurrent);
    }
	
    /** Get peak current limit (A) */
    public double getPeakCurrent() 
	throws TimeoutException, IOException, NullPointerException, Exception
    {
		return cmdWriteReadDouble(CMD_PEAK_CURRENT_LIMIT,BUF32,TM_CMD_MSEC);
    }
	
    /** Set peak current duration (sec).
	 @param peakCurrentDurationSec (sec)
	 */
    public  void setPeakCurrentDuration(int peakCurrentDurationSec) 
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
    {
		
		if(peakCurrentDurationSec<0){
			throw new IllegalArgumentException("peak current duration out of range ["+peakCurrentDurationSec+" ] < 0 ");
		}
		
		cmdWriteReadX(CMD_PEAK_CURRENT_DURATION+"="+peakCurrentDurationSec);
    }
	
    /** Get peak current duration (sec) */
    public int getPeakCurrentDuration() 
	throws TimeoutException, IOException, NullPointerException, Exception
    {
		return cmdWriteReadInt(CMD_PEAK_CURRENT_DURATION,BUF32,TM_CMD_MSEC);
    }
	
	
    /** Set continuous current (A).
	 Max continuous current is MC/2.
	 
	 @param contCurrent max continous active current (A)
	 */
    public  void setContinuousCurrent(double contCurrent) 
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
    {
		double contLimit=getMaxCurrent()/2.0;
		
		if(contCurrent>contLimit){
			throw new IllegalArgumentException("continuous current out of range ["+contCurrent+" ] > "+contLimit);
		}
		
		cmdWriteReadX(CMD_CONT_CURRENT_LIMIT+"="+contCurrent);
    }
	
    /** Get continous current limit (A) */
    public double getContinuousCurrent() 
	throws TimeoutException, IOException, NullPointerException, Exception
    {
		return cmdWriteReadDouble(CMD_CONT_CURRENT_LIMIT,BUF32,TM_CMD_MSEC);
    }
	
    /** Set torque limit (% of continuous current).	
	 If the torque limit is exceeded and stuck motor protection
	 is enabled, a motor fault will occur.
	 max value: 100%
	 
	 @param torquePercent max torque, as a percentage of max continous active current (%)
	 */
    public  void setTorqueLimit(double torquePercent) 
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
    {
		if(torquePercent>100.0 || torquePercent<0.0){
			throw new IllegalArgumentException("torque limit out of range ["+torquePercent+" ]: 0<value<100");
		}
		
		cmdWriteReadX(CMD_TORQUE_LIMIT+"="+torquePercent);
    }
	
    /** Get torque limit (% continous active current) */
    public double getTorqueLimit() 
	throws TimeoutException, IOException, NullPointerException, Exception
    {
		return cmdWriteReadDouble(CMD_TORQUE_LIMIT,BUF32,TM_CMD_MSEC);
    }
	
    /** Set stuck speed (counts/sec).	
	 If the motor goes slower than the stuck speed for more than the
	 and peak current duration and the torque exceeds the torque limit
	 and motor protection is enabled (i.e., stuck motor speed > 2),
	 a motor fault will occur.
	 max value: 16000
	 
	 @param stuckSpeed min speed threshold indicating the motor is stuck (counts/sec)
	 */
    public  void setStuckSpeed(int stuckSpeed) 
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
    {
		
		if(stuckSpeed>16000 || stuckSpeed<0){
			throw new IllegalArgumentException("stuck speed out of range ["+stuckSpeed+" ]: 0<value<16000");
		}
		
		cmdWriteReadX(CMD_STUCK_SPEED+"="+stuckSpeed);
    }
	
    /** Get stuck speed (counts/sec) */
    public int getStuckSpeed() 
	throws TimeoutException, IOException, NullPointerException, Exception
    {
		return cmdWriteReadInt(CMD_STUCK_SPEED,BUF32,TM_CMD_MSEC);
    }
	
    /** Get status register value.
	 Reported Status Bits
	 0  : Drive read: 0
	 0: Conditions OK
	 1: Problem, as reported by bits 1-3
	 1-3 : Servo drive status indication details: refer to the following table
	 4 : Motor on (MO) 4
	 5 : Reference mode (RM) 5
	 6 : Motor failure latched (see MF for details) 
	 7-9 : Unit mode (UM) 
	 10 : Gain scheduling on
	 11 : Either Main or Auxiliary Homing being processed
	 12 : Program running 
	 13 : Current limit on (LC)
	 14-15 : Motion status reflection (MS)
	 16-17 : Recorder status
	 0: Recorder inactive, no valid recorded data
	 1: Recorder waiting for a trigger event
	 2: Recorder finished; valid data ready for use
	 3: Recording now
	 18-23 : Not used
	 24-26 : Digital Hall sensors A, B and C6
	 27 : CPU status:
	 0: CPU OK
	 1: Stack overflow or CPU exception
	 28 : Stopped by a limit ( RLS, FLS, Stop switch ) or by a VH[3]/VL[3] position command limit 
	 29 : Error in user program
	 30-31 : Unused
	 
	 0x8 0x4 0x2 Meaning
	 0   0   0 OK.
	 0   0   1 Under voltage: 
	 The power supply is shut off or it has too high an
	 impedance.
	 0   1   0 Over voltage: 
	 The power supply voltage is too large, or the servo
	 drive did not succeed in absorbing the kinetic energy while
	 braking a load. A shunt resistor may be needed.
	 1   0   1 Short circuit: 
	 The motor or its wiring may be defective.
	 1   1   0 Temperature: 
	 The drive is overheating.
	 
	 @return status 
	 */
    public int getStatusRegister() 
	throws TimeoutException, IOException, NullPointerException, Exception
    {
		return cmdWriteReadInt(CMD_GET_STATUS_REGISTER,BUF32,TM_CMD_MSEC);
    }
	
    /** Get detailed motor fault codes.
	 value/bit	
     0x0   : The motor is on, or the last motor shutdown was the normal result of a software command.
     0x1/0 : 1. Resolver or Analog Halls feedback is not ready - Resolver or Analog Halls angle was not found yet.
	 2. The amplitude of the analog sensor is lost or too low.
     0x1/0 : reserved
     0x2/1 : reserved
     0x4/2 : Feedback loss: no match between encoder and Hall location. 
	 Available in encoder + Hall feedback systems.
     0x8/3 : The peak current has been exceeded. 
	 Possible reasons are drive malfunction or bad tuning of the current controller.
     0x10/4 : inhibit
     0x20/5 : reserved
     0x40/6 : Two digital Hall sensors were changed at the same time. 
	 Error occurs because digital Hall sensors must be changed one at a time.
     0x80/7 : Speed tracking error DV[2] - VX (for UM=2 or UM=4, 5)
	 exceeded speed error limit ER[2]. This may occur due to:
	 - Bad tuning of the speed controller
	 - Too tight a speed error tolerance
	 - Inability of motor to accelerate to the required speed
	 due to too low a line voltage or not a powerful enough motor
     0x100/8 : Position tracking error DV[3] - PX (UM=5) or DV[3] - PY
	 (UM=4) exceeded position error limit ER[3]. This may occur due
	 to:
	 - Bad tuning of the position or speed controller
	 - Too tight a position error tolerance
	 - Abnormal motor load, or reaching a mechanical limit
     0x200/9 : Cannot start because of inconsistent database. The type of
	 database inconsistency is reflected in the status SR report, and in
	 the CD CPU dump report.
     0x400/10 : Too large a difference in ECAM table.
     0x800/11 : Heartbeat failure. Error occurs only if drive is set to abort under
	 heartbeat failure in a CANopen network (object 0x6007 in CAN
	 object dictionary is set to 2).
     0x1000/12 : Servo drive fault. Error described according to the servo drive
	 fault detail bits 13 - 15 in the MF report. Refer to following table.
     0x2000/13 : Servo drive fault detail bit 1. Refer to following table.
     0x4000/14 : Servo drive fault detail bit 2. Refer to following table.
     0x8000/15 : Servo drive fault detail bit 3. Refer to following table.
     0x10000/16 : Failed to find the electrical zero of the motor in an attempt to
	 start it with an incremental encoder and no digital Hall sensors.
	 The reason may be that the applied motor current did not
	 suffice for moving the motor from its position.
     0x20000/17 : Speed limit exceeded: VX<LL[2] or VX>HL[2].
     0x40000/18 : Stack overflow - fatal exception. This may occur if the CPU was
	 subject to a load that it could not handle. Such a situation can
	 arise only due to a software bug in the drive. Use the CD
	 command to get the CPU dump and report to your service center.
     0x80000/19 : CPU exception - fatal exception. Something such as an attempt
	 to divide in zero or another fatal firmware error has occurred.
	 Use the CD command to get the CPU dump and report to your service center.
     0x100000/20 : reserved
     0x200000/21 : Motor stuck - the motor is powered but is not moving according
	 to the definition of CL[2] and CL[3].
     0x400000/22 : Position limit exceeded: PX<LL[3] or PX>HL[3] (UM=5), or
	 PY<LL[3] or PY>HL[3] (UM=4).
     0x10000000/28 : reserved
     0x20000000/29 : Cannot start motor
     0x80000000/31 : reserved
	 
     bits 0x8000 0x4000 0x2000 : code
     000 : OK
     001 : Under voltage. The power supply is shut down or it has too high an output impedance.
     010 : Over voltage. The voltage of the power supply is too high, or the servo drive did 
	 not succeed in absorbing the kinetic energy while braking a load. A shunt resistor 
	 may be required.
     011 : reserved
     100 : reserved
     101 : Short circuit. The motor or its wiring may be defective, or the drive is faulty.
     110 : Temperature. Drive overheating. The environment is too hot, or lacks heat removal. 
	 There may be a large thermal resistance between the drive and its mounting.
     111 : reserved
	 
	 @return motor fault code
	 @see Elmo Command Reference
	 */
    public int getMotorFault() 
	throws TimeoutException, IOException, NullPointerException, Exception
    {
		
		return cmdWriteReadInt(CMD_GET_MOTOR_FAULT,BUF32,TM_CMD_MSEC);
    }
	
	
	/** Get motion status register.
	 @return MS_REF_CONTROLLED if moving in PTP mode, MS_REF_STATIONARY or MS_POS_STABILIZED if motor stopped.
     */
	public int getMotionStatus()
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{
		return cmdWriteReadInt(CMD_GET_MOTION_STATUS,BUF32,TM_CMD_MSEC);
	}
		
    /** Set echo mode 
	 @param mode ECHO_ENABLED or ECHO_DISABLED
	 */
    public void setEchoMode(int mode)
	throws IOException, IllegalArgumentException
    {
		switch(mode){
			case ECHO_ENABLED:
				cmdWriteReadX(mkCmd(CMD_ECHO,ECHO_ENABLED));
				break;
			case ECHO_DISABLED:
				cmdWriteReadX(mkCmd(CMD_ECHO,ECHO_DISABLED));
				break;
			default:
				throw new IllegalArgumentException("invalid mode ["+mode+"]");
		}
		return;
    }
	public int getEchoMode()
	throws TimeoutException, IOException, NullPointerException, Exception
	{
		return cmdWriteReadInt(CMD_ECHO,BUF32,TM_CMD_MSEC);
	}
	
    /** Set hex mode.
	 Integers returned in hexidecimal  format (0xn) 
	 if true or as base 10 if false. 
	 @param mode enable hex mode if true, disable if false
	 */
    public void setHexMode(boolean mode)
	throws TimeoutException, IOException, NullPointerException, Exception
    {
		if(mode==true){
			cmdWriteReadX(mkCmd(CMD_HEXMODE,HEX_MODE_ENABLED));
		}else{
			cmdWriteReadX(mkCmd(CMD_HEXMODE,HEX_MODE_DISABLED));
		}
    }
	public int getHexMode()
	throws TimeoutException, IOException, NullPointerException, Exception
	{
		return cmdWriteReadInt(CMD_HEXMODE,BUF32,TM_CMD_MSEC);
	}
	
	/** Get low reference speed limit (VL[2]) (counts/sec) */
    public int getReferenceSpeedLo() 
	throws TimeoutException, IOException, NullPointerException, Exception
    {
		return cmdWriteReadInt(CMD_SPEED_REF_LO,BUF32,TM_CMD_MSEC);
    }
	
    /** Get high reference speed limit (VH[2]) (counts/sec) */
    public int getReferenceSpeedHi() 
	throws TimeoutException, IOException, NullPointerException, Exception
    {
		return cmdWriteReadInt(CMD_SPEED_REF_HI,BUF32,TM_CMD_MSEC);
    }
	
    /** Set controller reference speed.
	 The motor will ignore commands greater
	 than this limit.
	 The reference speed can only be set when
	 the motor is stopped.
	 @param min minimum reference speed 
	 @param max maximum reference speed 	
	 */
    public void setReferenceSpeed(int min, int max)
	throws IOException, IllegalArgumentException
    {
		if(min>max){
			throw new IllegalArgumentException("min > max");
		}
		cmdWriteReadX(CMD_SPEED_REF_LO+"="+min);
		cmdWriteReadX(CMD_SPEED_REF_HI+"="+max);
    }
	
    /** Set controller reference speed limits.
	 The motor will stop if the speed exceeds
	 this limit; min should be <VL, max should
	 be greater than VH
	 The reference speed can only be set when
	 the motor is stopped.
	 @param min minimum motor speed 
	 @param max maximum motor speed 	
	 */
    public void setReferenceLimit(int min, int max)
	throws IOException, IllegalArgumentException
    {
		if(min>max){
			throw new IllegalArgumentException("min > max");
		}
		cmdWriteReadX(CMD_SPEED_LIMIT_LO+"="+min);
		cmdWriteReadX(CMD_SPEED_LIMIT_HI+"="+max);
    }
	/** Get low reference speed limit (VL[2]) (counts/sec) */
    public int getReferenceLimitLo() 
	throws TimeoutException, IOException, NullPointerException, Exception
    {
		return cmdWriteReadInt(CMD_SPEED_LIMIT_LO,BUF32,TM_CMD_MSEC);
    }
	
    /** Get high reference speed limit (VH[2]) (counts/sec) */
    public int getReferenceLimitHi() 
	throws TimeoutException, IOException, NullPointerException, Exception
    {
		return cmdWriteReadInt(CMD_SPEED_LIMIT_HI,BUF32,TM_CMD_MSEC);
    }
	
    /** Set controller operation mode.
	 The controller unit mode can only be set when
	 the motor is stopped.
	 @param mode one of: MODE_TORQUE,MODE_SPEED,MODE_SINGLE_FEEDBACK_POSITION,MODE_DUAL_FEEDBACK_POSITION,MODE_STEPPER
	 */
    public void setUnitMode(int mode)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
    {
		switch(mode){
			case MODE_TORQUE:
			case MODE_SPEED:
			case MODE_SINGLE_FEEDBACK_POSITION:
			case MODE_DUAL_FEEDBACK_POSITION:
			case MODE_STEPPER:
				stopMotor();
				cmdWriteReadX(CMD_UNIT_MODE+"="+mode);
				break;
			default:
				throw new IllegalArgumentException("Unsupported mode:"+mode);
		}
		return;
    }
	
	/** Get controller operation mode.
	 */
    public int getUnitMode()
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
    {
		return cmdWriteReadInt(CMD_UNIT_MODE,BUF32,TM_CMD_MSEC);
    }
	
	/** read digital input port */
	public int getInputPort()
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{
		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("e.getInputPort - cmd:"+CMD_GET_DIGITAL_INPUTS);
		//}
		return cmdWriteReadInt(CMD_GET_DIGITAL_INPUTS,BUF32,TM_CMD_MSEC);
	}
	
	/**Set Input Logic register. 
	 @param inputBit  input index (0-indexed; note adjustment for IL IL is 1-indexed)
	 @param value  value (bitfield, 0-23d)
	 */
	public void setInputLogic(int inputBit,int value)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException
	{
		// IL command is indexed from 1
		int input=inputBit+1;
		if(input<IL_MIN_INDEX || input>IL_MAX_INDEX){
			throw new IllegalArgumentException("Unsupported input:"+input);
		}
		if(value<IL_MIN_VALUE || value>IL_MAX_VALUE){
			throw new IllegalArgumentException("Unsupported configuration:"+value);
		}
		cmdWriteReadX(mkIndexCmd(CMD_INPUT_LOGIC,input,value));
	}
    /** Set XModulus. 
	 Sets X Modulus for cyclic position counting.
	 Motor must be stopped to set value
     */
    public  void setXModulus(long xmLo, long xmHi) 
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
    {
		if(xmLo>=xmHi || ((xmHi-xmLo)%2!=0)){
			throw new IllegalArgumentException("invalid XModulus values ["+xmLo+","+xmHi+"]");
		}		
		if(xmLo>MIN_XMODULUS || xmLo>MAX_XMODULUS){
			throw new IllegalArgumentException("XM[1] out of range ["+xmLo+"]");
		}		
		if(xmHi>MIN_XMODULUS || xmHi>MAX_XMODULUS){
			throw new IllegalArgumentException("XM[2] out of range ["+xmHi+"]");
		}		
		cmdWriteReadX(mkCmd(CMD_SET_XMODULUS_LO,xmLo));
		cmdWriteReadX(mkCmd(CMD_SET_XMODULUS_HI,xmLo));
    }
	public long getXModulusLo()
	throws TimeoutException, IOException, NullPointerException, Exception
	{
		return cmdWriteReadLong(CMD_SET_XMODULUS_LO,BUF32,TM_CMD_MSEC);
	}
	public long getXModulusHi()
	throws TimeoutException, IOException, NullPointerException, Exception
	{
		return cmdWriteReadLong(CMD_SET_XMODULUS_HI,BUF32,TM_CMD_MSEC);
	}
	
	/**Get Input Logic register. 
	 @param inputBit input index (0-indexed; note adjustment for IL IL is 1-indexed)
	 */
	public int getInputLogic(int inputBit)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{
		int input=inputBit+1;
		if(input<IL_MIN_INDEX || input>IL_MAX_INDEX){
			throw new IllegalArgumentException("Unsupported input:"+input);
		}
		return cmdWriteReadInt(mkIndex(CMD_INPUT_LOGIC,input),BUF32,TM_CMD_MSEC);
	}
			
    /** Set speed-up acceleration. Takes effect on next beginMotion command.
	 @param value - new value (counts/sec^2)
     */
    public  void setAcceleration(int value ) 
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
    {
		if(value<MIN_ACCELERATION || value>MAX_ACCELERATION){
			throw new IllegalArgumentException("accleration value out of range "+value);
		}
		cmdWriteReadX(CMD_ACCELERATION+"="+value);
    }
    /** Get speed-up acceleration. (counts/sec^2)
     */
    public  int getAcceleration() 
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
    {
		return cmdWriteReadInt(CMD_ACCELERATION,BUF32,TM_CMD_MSEC);
    }
	
    /** Set slow-down acceleration. Takes effect on next beginMotion command.
	 @param value - new value (counts/sec^2)
     */
    public  void setDeceleration(int value) 
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
    {
		if(value<MIN_DECELERATION || value>MAX_DECELERATION){
			throw new IllegalArgumentException("decleration value out of range "+value);
		}
		cmdWriteReadX(CMD_DECELERATION+"="+value);
    }
	/** Get slow-down acceleration. Motor deceleration (counts/sec^2)
	 Used for changing jogging velocity.
	 Deceleration for stopping is controlled
	 separately.
	 
     */
    public  int getDeceleration() 
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
    {
		return cmdWriteReadInt(CMD_DECELERATION,BUF32,TM_CMD_MSEC);
    }
	
    /** Set slow-down acceleration, Motor stop deceleration (counts/sec^2) 
	 Controls rate at which motor stops 
	 when ST command is issued; deceleration
	 during jogging velocity change is handled
	 separately.
	 Takes effect on next beginMotion command.
	 @param value - new value (counts/sec^2)
     */
    public  void setStopDeceleration(int value) 
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
    {
		if(value< MIN_STOP_DECELERATION || value>MAX_STOP_DECELERATION){
			throw new IllegalArgumentException("stop decleration value out of range "+value);
		}
		cmdWriteReadX(CMD_STOP_DECELERATION+"="+value);
    }
	/** Get slow-down stop acceleration. 
     */
    public  int getStopDeceleration() 
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
    {
		return cmdWriteReadInt(CMD_STOP_DECELERATION,BUF32,TM_CMD_MSEC);
    }
	
	/** Get motor position.
	 @return motor position (always 0 for now)
     */
    protected long getPositionCounter() 
	throws TimeoutException, IOException, NullPointerException, Exception
    {
		return cmdWriteReadLong(CMD_POSITION_COUNTER,BUF32,TM_CMD_MSEC);
    }    
	
	/**Set Position Counter. Motor must be stopped 
	 @param value - position counter value
	 */
	public void setPositionCounter(long value)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException
	{
		if(value<PX_MIN_VALUE || value>PX_MAX_VALUE){
			throw new IllegalArgumentException("Unsupported position:"+value);
		}
		setEnable(false,TM_CMD_MSEC);
		cmdWriteReadX(mkCmd(CMD_POSITION_COUNTER,value));
	}
		
    /** Position Relative. 
	 Sets relative position; does not take effect until BG.
	 Motion subject to AC, DC, JV, etc.
     */
    public  void setPositionRelative(long value) 
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
    {
		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("Elmo.setPositionRelative("+value+")");
		//}
		setEnable(true,TM_START_MSEC);
		cmdWriteReadX(mkCmd(CMD_RELATIVE_POSITION,value));
    }
	
	/** Position Absolute. 
	 Enables motor (required to set PA)
	 Sets absolute position; does not take effect until BG
     */
    public  void setPositionAbsolute(long value) 
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
    {
		//enableMotor();
		setEnable(true,TM_START_MSEC);
		cmdWriteReadX(mkCmd(CMD_ABSOLUTE_POSITION,value));
    }
	
	/** Get motor position error.
	 @return difference between commanded and actual position
     */
    protected long getPositionError() 
	throws TimeoutException, IOException, NullPointerException, Exception
    {
		return cmdWriteReadLong(CMD_POSITION_ERROR,BUF32,TM_CMD_MSEC);
    }    
	/** Set max speed for PTP (point to point) motion.
	 Although SP is not used in velocity mode, it must be set
	 so that SP<|VL[2]| and SP<|VH[2]|. If not, parameters
	 VL,VH,LL,HL may not be correctly written to flash
	 
	 @param ptpSpeed max PTP speed (counts/sec)
	 */
    public void setPTPSpeed(int ptpSpeed)
	throws IOException, IllegalArgumentException,TimeoutException,NullPointerException,Exception
    {
		int refLo=Math.abs(getReferenceSpeedLo());
		int refHi=Math.abs(getReferenceSpeedHi());
		if(ptpSpeed>refLo || ptpSpeed>refHi){
			throw new IllegalArgumentException("PTP speed out of range: SP<|VL[2]| and SP<|VH[2]|");
		}
		setEnable(true,TM_CMD_MSEC);
		cmdWriteReadX(mkCmd(CMD_PTP_SPEED,ptpSpeed));
    }
    
	public int getPTPSpeed()
	throws IOException, IllegalArgumentException,TimeoutException,NullPointerException,Exception
    {
		return cmdWriteReadInt(CMD_PTP_SPEED,BUF32,TM_CMD_MSEC);
	}		
	
    /** Set motor velocity.  Does not take effect until next BG command
	 @param velocityRPM - motor speed (RPM)
     */
    public  void setJoggingVelocity(double velocityRPM, int countsPerRev) 
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
    {
        setJoggingVelocity(rpm2counts(velocityRPM, countsPerRev));
    }
	
    /** Set jogging velocity (counts/second).
	 The new velocity does not take effect until the next begin motion command.
	 @param velocityCounts commanded velocity (counts/sec)
	 */
    public  void setJoggingVelocity(int velocityCounts) 
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
    {
		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("Elmo.setJoggingVelocity("+velocityCounts+")");
		//}
		if(velocityCounts>MAX_VELOCITY_COUNTS || velocityCounts<-MAX_VELOCITY_COUNTS){
			throw new IllegalArgumentException("velocity out of range ["+velocityCounts+" counts/sec]");
		}
		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("Elmo.setJoggingVelocity: enabling");
		//}
		setEnable(true,TM_CMD_MSEC);
		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("Elmo.setJoggingVelocity: writing command");
		//}
		cmdWriteReadX(CMD_JOGGING_VELOCITY+"="+velocityCounts);
		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("Elmo.setJoggingVelocity: OK");
		//}
    }
	
    /** Get jogging velocity. (requested motor velocity; reads value even if motor stopped)
	 @return velocity
	 */
    public int getJoggingVelocity() 
	throws TimeoutException, IOException, NullPointerException, Exception
    {
		
		return cmdWriteReadInt(CMD_JOGGING_VELOCITY,BUF32,TM_CMD_MSEC);
    }
	
    /** Get encoder velocity VX. 
	 @return velocity (instantaneous value, counts/sec)
	 */
    public int getEncoderVelocity() 
	throws TimeoutException, IOException, NullPointerException, Exception
    {
		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("getEncoderVelocity - cmd:"+CMD_GET_MAIN_ENC_VELOCITY);
		//}
		return cmdWriteReadInt(CMD_GET_MAIN_ENC_VELOCITY,BUF32,TM_CMD_MSEC);
    }
	
    /** Get commanded velocity. ( DV[2]; reads 0 if motor stopped)
	 @return velocity
	 */
    public int getDesiredVelocity() 
	throws TimeoutException, IOException, NullPointerException, Exception
    {
		return cmdWriteReadInt(CMD_GET_DESIRED_VELOCITY,BUF32,TM_CMD_MSEC);
    }
	
	/** Enable or disable the motor
	 fulfills MotorControlIF interface
	 */
	public void setEnable(boolean value, long timeoutMsec)
	throws TimeoutException{
		
		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("e.setEnable("+value+","+timeoutMsec+")");
		//}
		int counter=0;
		
		long start=System.currentTimeMillis();
		while( ((System.currentTimeMillis()-start)<timeoutMsec)){
			try{
				if(value==true){
					counter++;
					//if(_log4j.isDebugEnabled()){
					//_log4j.debug("setEnable(1) - enabling");
					//}
						// write the enable command
						cmdWriteReadX(mkCmd(CMD_MOTOR_ENABLE,MOTOR_ENABLED));
					
					if(isEnabled()==true){
						//if(_log4j.isDebugEnabled()){
						//_log4j.debug("setEnable(1) - success!\n");
						//}
						// return most triumphantly
						return;
					}
				}else{
					counter++;
					//if(_log4j.isDebugEnabled()){
					//_log4j.debug("setEnable(0) - disabling");
					//}
						// write the disable command
						cmdWriteReadX(mkCmd(CMD_MOTOR_ENABLE,MOTOR_DISABLED));
					if(isEnabled()==false){
						//if(_log4j.isDebugEnabled()){
						//_log4j.debug("setEnable(0) - success!\n");
						//}
						// return most triumphantly
						return;
					}
				}
			}catch(TimeoutException te){
				//te.printStackTrace();
				if(_log4j.isDebugEnabled()){
				_log4j.debug(te+" in setEnable\n");
				}
			}catch(IOException ie){
				_log4j.debug(ie+" in setEnable\n");
				ie.printStackTrace();
			}catch(NullPointerException ne){
				if(_log4j.isDebugEnabled()){
				_log4j.debug(ne+" in setEnable\n");
				}
				//ne.printStackTrace();
			}catch(IllegalArgumentException iae){
				if(_log4j.isDebugEnabled()){
				_log4j.debug(iae+" in setEnable\n");
				}
				//iae.printStackTrace();
			}catch(Exception e){
				if(_log4j.isDebugEnabled()){
				_log4j.debug(e+" in setEnable\n");
				}
				//e.printStackTrace();
			}
		}
		
		throw new TimeoutException("setEnable timed out ["+value+","+timeoutMsec+" n="+counter+"]");
		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("setEnable timed out ["+value+","+timeoutMsec+" n="+counter+"]\n\n");
		//}
	}
	/** Determine if motor is enabled.
	 @return true if enabled, false otherwise.
     */
    public  boolean isEnabled()
	throws TimeoutException, IOException, NullPointerException, Exception
    {
		int test=cmdWriteReadInt(CMD_MOTOR_ENABLE,BUF32,TM_CMD_MSEC);
		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("e.isEnabled - motor enabled returned "+test);
		//}
		return (test!=0);
    }
		
    /** Stop motor.
	 @return 0 on success
     */
	
    protected  int stopMotor() 
	throws TimeoutException, IOException, NullPointerException, Exception
    {
		cmdWriteReadX(CMD_STOP_MOTION);
		long start=System.currentTimeMillis();
		
		//while(  getEncoderVelocity()!=0 ){
		while(  true ){
			try{
				if(getEncoderVelocity()==0){
					break;
				}
			}catch (Exception e) {
				//if(_log4j.isDebugEnabled()){
				//_log4j.debug("stopMotor - "+(System.currentTimeMillis()-start) );
				//}
			}
			delay(TM_POLL_DELAY_MSEC);
			if( ((System.currentTimeMillis()-start) > TM_STOP_MSEC) )
				throw new TimeoutException("motor stop timeout");
		}
		
		return 0;
    }
	
	/** wait for relative/absolute motion to complete */
	public void motionWait(long distance,long pollMsec)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
	{
		
		int speedCounts=0;
		for(int i=0;i<5;i++){
			try{
				speedCounts=getPTPSpeed();
				if(speedCounts>0){
					break;
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		if(speedCounts<=0){
			throw new Exception("motionWait - Could not read PTP speed");
		}
		long motionTimeOutMsec=(long)(1000.0*Math.abs((double)distance/(double)speedCounts));
		
		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("motionWait - d:"+distance+" v: "+speedCounts+" t:"+motionTimeOutMsec);		
		//}
		if(distance==0 || motionTimeOutMsec==0){
			return;
		}
		long start=System.currentTimeMillis();
		while( ((System.currentTimeMillis()-start) < (2*motionTimeOutMsec))){
			delay(pollMsec);
			try{
				if(getMotionStatus()!=MS_REF_CONTROLLED){ 
					return;
				}
				int ip=getInputPort();
				//if(_log4j.isDebugEnabled()){
				//_log4j.debug("motionWait - ip:0x"+Integer.toHexString(ip)+" mask:0x"+Integer.toHexString(Elmo.MASK_DIGITAL_INPUT_HARDSTOP));
				//}
				int hardStop=(ip & Elmo.MASK_DIGITAL_INPUT_HARDSTOP);
				if(hardStop>0){
					return;
				}
				
			}catch(Exception e){
			}
		}
		throw new TimeoutException("motionWait timed out [p:"+pollMsec+" d:"+distance+" s:"+speedCounts+" t:"+motionTimeOutMsec+"]");
	}
	
	
	/** Begin motion. 
	 Enables motor and starts new motion (according to jogging velocity and acceleration settings).
     */
    public  void beginMotion() 
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
    {
		cmdWriteReadX(CMD_BEGIN_MOTION);
    }
		
	/** Command new absolute position w/ new acceleration parameters. 
	 Sets jogging velocity, acceleration and initiates new motion.
	 
	 @param position - counts
	 @param velocity - motor speed (counts/sec)
	 @param acceleration - new accleration value counts/sec^2
	 @param deceleration - new decleration value counts/sec^2
	 @param wait - delay when setting ptp absolute speed
     */
    public  void ptpAbsolute(long position, int velocity, int acceleration, int deceleration,boolean wait) 
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
    {
	    setAcceleration(acceleration);
	    setDeceleration(deceleration);
		setPTPSpeed(velocity);
		ptpAbsolute(position,wait);
		
		return;
    }
	
    /** Command new absolute position; changes velocity during motion based on current AC, DC and specified JV parameters
	 Enables motor and initiates new motion.
	 
	 @param position - motor position (counts)
	 @param wait - enable delay when setting ptp absolute speed
     */
    public  void ptpAbsolute(long position,boolean wait) 
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
    {
		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("e.commandAbsolutPosition("+position+")");
		//}
		setPositionAbsolute(position);
		beginMotion();
		if(wait==true){
			long currentPos=getPositionCounter();
			motionWait(Math.abs(currentPos-position),TM_MOTION_WAIT_MSEC);
		}
    }
	
	/** Command new relative position w/ new acceleration parameters. 
	 Sets jogging velocity, acceleration and initiates new motion.
	 
	 @param position - counts
	 @param velocity - motor speed (counts/sec)
	 @param acceleration - new accleration value counts/sec^2
	 @param deceleration - new decleration value counts/sec^2
	 @param wait - enable delay when setting ptp absolute speed
     */
    public  void ptpRelative(long position, int velocity, int acceleration, int deceleration,boolean wait) 
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
    {
	    setAcceleration(acceleration);
	    setDeceleration(deceleration);
		setPTPSpeed(velocity);
		ptpRelative(position, wait);
		
		return;
    }
	
    /** Command new relative position; changes velocity during motion based on current AC, DC and specified JV parameters
	 Enables motor and initiates new motion.
	 
	 @param position - motor position (relative counts)
	 @param wait - enable delay when setting ptp absolute speed
     */
    public  void ptpRelative(long position, boolean wait) 
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
    {
		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("Elmo.commandRelativePosition("+position+")");
		//}
		setPositionRelative(position);
		beginMotion();
		if(wait==true){
			motionWait(position,TM_MOTION_WAIT_MSEC);
		}
    }
	
    /** Command new velocity w/ new acceleration parameters. 
	 Sets jogging velocity, acceleration and initiates new motion.
	 If acceleration or deceleration <=0, they are not set.
	 Equivalent to commandVelocity if acceleration and deceleration both <=0
	 
	 @param velocity - motor speed (counts/sec)
	 @param acceleration - new accleration value counts/sec^2
	 @param deceleration - new decleration value counts/sec^2
     */
    public  void jog(int velocity, int acceleration, int deceleration) 
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
    {
	    setAcceleration(acceleration);
	    setDeceleration(deceleration);
		jog(velocity);
		return;
    }
	
    /** Command new velocity; changes velocity during motion based on current AC, DC and specified JV parameters
	 Sets jogging velocity and initiates new motion.
	 
	 @param velocity - motor speed (counts/sec)
     */
    public  void jog(int velocity) 
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception
    {
		//if(_log4j.isDebugEnabled()){
		//_log4j.debug("e.cmdVel("+velocity+")");
		//}
		setJoggingVelocity(velocity);
		beginMotion();
    }
	
		
	
}
