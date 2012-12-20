/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.utils;

import java.util.*;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.rmi.RemoteException;

import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.foce.devices.elmo.base.*;
import org.mbari.siam.distributed.devices.ElmoIF;

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

/* ElmoLocal - Control FOCE Elmo motor controllers 
   when SIAM is not running. 

   If SIAM is running, motors may be controlled via
   the Elmo motor services.

  $Id: ElmoLocal.java,v 1.12 2009/06/06 04:58:32 headley Exp $
  $Name:  $
  $Revision: 1.12 $
*/

public class ElmoLocal extends ElmoImpl {
	
    static protected Logger _log4j = Logger.getLogger(ElmoLocal.class);  

    /** Default controller serial port */
    protected static final String DEFAULT_SERIAL_PORT_NAME="/dev/ttyS8";
	/** Command Interface */
	private ElmoCI _elmoCI=null;
	
    /** motor controller serial port */
    private  SerialPort motorSerialPort=null;
    /** forward motor serial port path */
    protected String _motorSerialPortName=DEFAULT_SERIAL_PORT_NAME;

    /** Gear ratio between motor and thrusters. */
    public double _gearRatio=15.0;

	/** number of counts per motor revolution (before gear reduction) */
	int countsPerRevolution=6;
		
	/** Motor acceleration (counts/sec^2) */
	int motorAccelerationCounts=1000;
	
	/** Motor deceleration (counts/sec^2)
	 Used for changing jogging velocity.
	 Deceleration for stopping is controlled
	 separately.
	 */
	int motorDecelerationCounts=1000;
	
	/** Motor stop deceleration (counts/sec^2) 
	 Controls rate at which motor stops 
	 when ST command is issued; deceleration
	 during jogging velocity change is handled
	 separately.
	 */
	int motorStopDecelerationCounts=10000;
	
	/** gear ratio motorTurns:outputTurns */
	double gearRatio=15.0;
	
	/** default min motor speed */
	double motorMinRPM=Elmo.counts2rpm(Elmo.MAX_VELOCITY_COUNTS,countsPerRevolution);
	/** default max motor speed */
	double motorMaxRPM=Elmo.counts2rpm(-Elmo.MAX_VELOCITY_COUNTS,countsPerRevolution);

	/** speed to use for homing (counts/sec) */
	int homingSpeedCounts     = 50;
	/** timeout margin added to computed value for some homing/centering motions */
	long motionTimeoutMsec=200L;

	/** Elmo input port bit used for hall feedback bit 0 */
	int DIGITAL_INPUT_HALL_BIT0 = 2;
	/** Elmo input port bit used for hall feedback bit 1 */
	int DIGITAL_INPUT_HALL_BIT1 = 3;
	/** Elmo input port bit used for hall feedback bit 2 */
	int DIGITAL_INPUT_HALL_BIT2 = 4;
	/** Elmo input port bit used for hall feedback bit 3 */
	int DIGITAL_INPUT_HALL_BIT3 = 5;
	
	/** louver position */
	protected double _louverPosition=0.0;

	/** forward motor speed (RPM) */
    protected Double _motorRPM=null;

    /** Verbose output flag */
    protected boolean VERBOSE=false;

    /** interpret specified speed as shaft speed */
    protected boolean useShaftSpeed=true;
    
	/** set UnitMode during controller initialization (stops motor) */
    protected boolean setUnitMode=false;

	protected int homePosition=-1;
	protected boolean boundary=ElmoImpl.ABOVE;

	protected String _cmds[];
	protected String _portName;
	
    /** Constructor */
	public ElmoLocal() throws IOException{
		super();
		/*
		 * Set up a simple configuration that logs on the console. Note that
		 * simply using PropertyConfigurator doesn't work unless JavaBeans
		 * classes are available on target. For now, we configure a
		 * PropertyConfigurator, using properties passed in from the command
		 * line, followed by BasicConfigurator which sets default console
		 * appender, etc.
		 */
		PropertyConfigurator.configure(System.getProperties());
		PatternLayout layout = new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");
		BasicConfigurator.configure(new ConsoleAppender(layout));
		//Logger.getRootLogger().setLevel((Level)Level.INFO);
		_elmoCI=new ElmoCI();
    }
	
	public ElmoLocal(String cmds[]) throws IOException,Exception{
		this();
		processArgs(cmds);
		_motorSerialPort=configurePort(_portName);
		_elmo=new ElmoSolo(_motorSerialPort);
		_elmoCI.setElmo(this);
	}
	
	/** Perform the actions indicated on the command line.
	 @return 0 on success
     */
    public int run() {
		_elmoCI.run(_cmds);
		return 0;
	}
	
	
    /** Print application-specific usage message to stdout. 
     */
    public  void printUsage(){
		_elmoCI.printUsage();
	}

	///////////////////////////////////////////////
	//        FOCELouverIF Implementations       //
	///////////////////////////////////////////////
	
		
    /** Configure the serial ports used to communicate with 
	 the motor controllers.
	 */
    public void initializeController()
	throws IOException,TimeoutException,Exception{

		this.initializeController(Elmo.MODE_SERIAL_LOCAL,
								  countsPerRevolution,
								  gearRatio,
								  Elmo.MODE_SINGLE_FEEDBACK_POSITION,
								  motorAccelerationCounts,
								  motorDecelerationCounts,
								  motorStopDecelerationCounts);
		
		//this.setCountsPerRevolution(countsPerRevolution);
		//this.setGearRatio(gearRatio);		
		//this.setDisplacementRPU(displacementRPU);		
		this.setDIHallBit(Elmo.DIGITAL_INPUT_BIT0,DIGITAL_INPUT_HALL_BIT0);		
		this.setDIHallBit(Elmo.DIGITAL_INPUT_BIT1,DIGITAL_INPUT_HALL_BIT1);		
		this.setDIHallBit(Elmo.DIGITAL_INPUT_BIT2,DIGITAL_INPUT_HALL_BIT2);		
		this.setDIHallBit(Elmo.DIGITAL_INPUT_BIT3,DIGITAL_INPUT_HALL_BIT3);
 
		this.setDigitalInputFunction(DIGITAL_INPUT_HALL_BIT0,Elmo.INPUT_LOGIC_GP_HI);
		this.setDigitalInputFunction(DIGITAL_INPUT_HALL_BIT1,Elmo.INPUT_LOGIC_GP_HI);
		this.setDigitalInputFunction(DIGITAL_INPUT_HALL_BIT2,Elmo.INPUT_LOGIC_GP_HI);
		this.setDigitalInputFunction(DIGITAL_INPUT_HALL_BIT3,Elmo.INPUT_LOGIC_GP_HI);
		
		this.setHomingSpeedCounts(homingSpeedCounts);		
		this.setMotionTimeoutOffsetMsec(motionTimeoutMsec);	
		/* flash defaults
		VL[2]=-340  VH[2]=340
		LL[2]=-1080 HL[2]=1080
		*/
		this.setReferenceSpeed(-3400,3400);
		this.setReferenceLimit(-10800,10800);
    }
	
	public void processArgs(String args[])
	throws Exception 
	{
		_cmds=new String[args.length-2];
		for(int i=0;i<args.length;i++){
			if(args[i].equals("-p") && i<2){
				// get the serial port name
				_portName= args[i+1];
			}
			if(i>=2){
				// rebuild the arguments list
				// without the port options
				// to pass to elmoCI
				_cmds[i-2]=args[i];
			}
		}
		
		if(_portName==null)
			throw new Exception("\nError: serial port name not specified\n");
	}
		
	/** Main entry point.
	 @param args command line arguments
	 */
	public static void main(String[] args) {
		//PropertyConfigurator.configure(System.getProperties());
		//PatternLayout layout = new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");
		//BasicConfigurator.configure(new ConsoleAppender(layout));
		try{
			
			// need to specify a port
			if(args.length<3){
				ElmoLocal app = new ElmoLocal();
				app.printUsage();
				System.exit(0);
			}
			
			ElmoLocal app=new ElmoLocal(args);
			
			//app.initializeController();
			int status=app.run();
			
			System.exit(status);
		}catch (IOException ioe) {
			ioe.printStackTrace();
		}catch (Exception e) {
			//e.printStackTrace();
			System.err.println(e.getMessage());
		}
		System.exit(-1);
	}
}
