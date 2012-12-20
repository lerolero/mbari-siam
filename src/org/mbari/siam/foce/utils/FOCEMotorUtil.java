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

import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.foce.devices.elmo.*;
import org.mbari.siam.foce.devices.elmo.base.*;

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

/* FOCEMotorUtil - Control FOCE Elmo motor controllers 
   when SIAM is not running. 

   If SIAM is running, motors may be controlled via
   the Elmo motor services.

  $Id: FOCEMotorUtil.java,v 1.15 2012/12/17 21:37:36 oreilly Exp $
  $Name: HEAD $
  $Revision: 1.15 $
*/

public class FOCEMotorUtil {
	
    static protected Logger _log4j = Logger.getLogger(FOCEMotorUtil.class);  
	
    /** Gear ratio between motor and thrusters. */
    public double GEAR_RATIO=ElmoSoloSvc.DEFAULT_GEAR_RATIO;
    /** Default forward controller serial port */
    protected static final String FORWARD_MOTOR_PORT="/dev/ttyS8";
    /** Default aft controller serial port */
    protected static final String AFT_MOTOR_PORT="/dev/ttyS10";
    /** Verbose output flag */
    protected boolean VERBOSE=false;
    /** Motor acceleration value (counts/second^2) */
    protected int ACCELERATION=1000;
    /** Motor deceleration value (counts/second^2) */
    protected int DECELERATION=1000;
    /** Number of counts per motor revolution */
    protected int _countsPerRevolution=6;
    /** forward controller serial port */
    private  SerialPort fwdSerialPort=null;
    /** aft controller serial port */
    private  SerialPort aftSerialPort=null;
    /** forward motor controller */
    private  ElmoSolo fwdController;
    /** aft motor controller */
    private  ElmoSolo aftController;
    /** forward motor speed (RPM) */
    protected Double _forwardMotorRPM=null;
    /** forward motor enable */
    protected Boolean _forwardMotorEnable=null;
    /** aft motor speed (RPM) */
    protected Double _aftMotorRPM=null;
    /** aft motor enable */
    protected Boolean _aftMotorEnable=null;
    /** forward motor serial port path */
    protected String _forwardMotorPort=null;
    /** forward motor serial port path */
    protected String _aftMotorPort=null;
    /** interpret specified speed as shaft speed */
    protected boolean useShaftSpeed=true;
    /** set UnitMode during controller initialization (stops motor) */
    protected boolean setUnitMode=false;
	
    /** Contstructor */
    public FOCEMotorUtil(){
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
    }
	
    /** Print application-specific usage message to stdout. 
     */
    public  void printUsage(){
		StringBuffer sb=new StringBuffer();
		String cmdName=System.getProperty("EXEC_NAME","FOCEMotorUtil");

		sb.append("\n");
		sb.append( " #\n");
		sb.append( " # motor: Change motor settings (speed, enable) via SIAM service\n");
		sb.append( " #\n");
		sb.append("\n");
		sb.append( " usage: motor [options] <forwardMotorRPM> [<forwardEnable=d|e> [<aftMotorRPM> [<aftEnable=d|e>]]]\n");
		sb.append("\n");
		sb.append( " Options:\n");
		sb.append( " -fp <port> : set forward motor port                ["+FORWARD_MOTOR_PORT+"]\n"); 
		sb.append( " -ap <port> : set aft motor port                    ["+AFT_MOTOR_PORT+"]\n"); 
		sb.append( " -f         : next parameters are for forward motor [forward parameters first]\n");
		sb.append( " -m         : command motor RPM                     [command shaft RPM]\n");
		sb.append( " -u         : set UnitMode (note: halts motor)      ["+setUnitMode+"]\n");
		sb.append( " -a         : next parameters are for aft motor     [aft parameters first]\n");
		sb.append( " -v         : verbose output                        ["+VERBOSE+"]\n");
		sb.append( " --help     : print this help message\n");
		sb.append("\n");
		sb.append( " Examples:\n");
		sb.append( "\n");
		sb.append( "  set forwardMotorSpeed=200 aftMotorSpeed=300 (enables: n/c)\n");
		sb.append( "   "+cmdName+" 200 300\n");
		sb.append("\n");
		sb.append( "  set forwardMotorSpeed=200 aftMotorSpeed=300 (forwardMotor:enable aftMotor:n/c)\n");
		sb.append( "   "+cmdName+" 200 e 300 \n");
		sb.append("\n");
		sb.append( "  set forwardMotorSpeed=300 aftMotorSpeed=200 (forwardMotor:disable aftMotor:n/c)\n");
		sb.append( "   "+cmdName+" -a 200 300 d\n"); 
		sb.append("\n");
		sb.append( "  set forwardMotorSpeed=n/c aftMotorSpeed=n/c (forwardMotor:enable aftMotor:disable)\n");
		sb.append( "   "+cmdName+" e d\n"); 
		sb.append("\n");
		sb.append( "  set forwardMotorSpeed=n/c aftMotorSpeed=n/c (forwardMotor:n/c aftMotor:enable)\n");
		sb.append( "   "+cmdName+" -a e\n"); 
		sb.append("\n");
		System.out.println(sb.toString());
    }
	
    /** Process command line options. 
	 @param args command line arguments
     */
    public void processArguments(String[] args) {
		boolean getForward=true;
		boolean getForwardEnable=true;
		boolean getAft=false;
		boolean getAftEnable=false;
		
		for(int i=0;i<args.length;i++){
			if(args[i].equals("e")){
				if(getForwardEnable){
					_forwardMotorEnable=new Boolean(true);
					getForwardEnable=false;
					getAftEnable=true;
				}else if(getAftEnable){
					_aftMotorEnable=new Boolean(true);
					getForwardEnable=true;
					getAftEnable=false;
					
				}
			}else
				if(args[i].equals("d")){
					if(getForwardEnable){
						_forwardMotorEnable=new Boolean(false);
						getForwardEnable=false;
						getAftEnable=true;
					}else if(getAftEnable){
						_aftMotorEnable=new Boolean(false);
						getForwardEnable=true;
						getAftEnable=false;
						
					}
				}else
					if(args[i].equals("-m")){
						useShaftSpeed=false;
					}else
						if(args[i].equals("-f")){
							getForward=true;
							getForwardEnable=true;
							getAft=false;
							getAftEnable=false;
						}else
							if(args[i].equals("-a")){
								getForward=false;
								getForwardEnable=false;
								getAft=true;
								getAftEnable=true;
							}else
								if(args[i].equals("-fp")){
									_forwardMotorPort=args[i+1];
									i++;
								}else
									if(args[i].equals("-ap")){
										_aftMotorPort=args[i+1];
										i++;
									}else
										if(args[i].equals("-u")){
											setUnitMode=true;
										}else
											if(args[i].equals("--help")){
												printUsage();
												System.exit(0);
											}else
												if(args[i].equals("-v")){
													VERBOSE=true;
												}else{
													try{
														Double val=new Double(args[i]);
														if(getForward){
															_forwardMotorRPM=val;
															getForward=false;
															getForwardEnable=true;
															getAft=true;
															getAftEnable=false;
														}else if(getAft){
															_aftMotorRPM=val;
															getForward=true;
															getForwardEnable=false;
															getAft=false;
															getAftEnable=true;
														}
													}catch(NumberFormatException e){
														// let it go quietly by
														_log4j.error("Invalid argument ["+args[i]+"]");
													}
												}
		}
    }
	
    /** Configure the serial ports used to communicate with 
	 the motor controllers.
	 */
    public void configurePorts(String forwardPort,String aftPort){
		
		String fileSeparator=":";
		String os=System.getProperty("os.name","unix").trim().toLowerCase();
		if(os.indexOf("win")>=0)
			fileSeparator=";";
		
		String cmdLinePorts=null;
		if(forwardPort!=null)
			cmdLinePorts=forwardPort;
		if(aftPort!=null){
			if(cmdLinePorts!=null){
				cmdLinePorts+=fileSeparator+aftPort;
			}else{
				cmdLinePorts=fileSeparator+aftPort;
			}
		}
		
		// get parameters passed in from wrapper script
		String propertyPorts=System.getProperty("devices","/dev/ttyS8"+fileSeparator+"/dev/ttyS10");
		_log4j.debug("propertyPorts: "+propertyPorts+" cmdLinePorts="+cmdLinePorts);
		int baud=Integer.parseInt(System.getProperty("baud","19200"));
		
		String serialPorts=null;
		if(cmdLinePorts!=null){
			serialPorts=cmdLinePorts;
		}else{
			serialPorts=propertyPorts;
		}
		_log4j.debug("using serialPorts: "+serialPorts);
		// set RXTX serial ports environment
		System.setProperty("gnu.io.rxtx.SerialPorts",serialPorts);
		
		try{
			if(forwardPort!=null){
				CommPortIdentifier fwdCommPortId = 
				CommPortIdentifier.getPortIdentifier(forwardPort);
				
				fwdSerialPort = 
				(SerialPort)fwdCommPortId.open(getClass().getName(), 1000);
				
				fwdSerialPort.setSerialPortParams(baud, 
												  fwdSerialPort.getDataBits(), 
												  fwdSerialPort.getStopBits(), 
												  fwdSerialPort.getParity());
				_log4j.debug("Serial port " + 
							 fwdSerialPort.getName() + 
							 "["+baud+","+
							 (fwdSerialPort.getParity()==0?"N":new Integer(fwdSerialPort.getParity()).toString())+","+
							 fwdSerialPort.getDataBits()+","+
							 fwdSerialPort.getStopBits()+"]");
			}
		}catch(Exception e){
			_log4j.error("Error configuring forward serial port:");
			e.printStackTrace();
		}
		try{
			if(aftPort!=null){
				CommPortIdentifier aftCommPortId = 
				CommPortIdentifier.getPortIdentifier(aftPort);
				
				aftSerialPort = 
				(SerialPort)aftCommPortId.open(getClass().getName(), 1000);
				
				aftSerialPort.setSerialPortParams(baud, 
												  aftSerialPort.getDataBits(), 
												  aftSerialPort.getStopBits(), 
												  aftSerialPort.getParity());
				_log4j.debug("Serial port " + 
							 aftSerialPort.getName() + 
							 "["+baud+","+
							 (aftSerialPort.getParity()==0?"N":new Integer(aftSerialPort.getParity()).toString())+
							 ","+
							 aftSerialPort.getDataBits()+","+
							 aftSerialPort.getStopBits()+"]");
			}
			
		}catch(Exception e){
			_log4j.error("Error configuring aft serial port:");
			e.printStackTrace();
		}
    }
	
    /** Initialize the specified motor controller.
	 @param controller ElmoSolo motor controller to initialize
	 */
    protected void initializeController(ElmoSolo controller ){
		if(controller!=null && controller.getSerialPort()!=null){
			_log4j.debug("configuring controller on port "+controller.getPort().getName());
			try{
				controller.setCountsPerRevolution(_countsPerRevolution);
				controller.setEchoMode(Elmo.ECHO_DISABLED);
				controller.setHexMode(false);
				if(setUnitMode==true){
					try{
						controller.setUnitMode(Elmo.MODE_SPEED);
					}catch(TimeoutException te){
						te.printStackTrace();
					}
				}
				//controller.setReferenceSpeed(-MAX_VELOCITY_COUNTS,MAX_VELOCITY_COUNTS);
				controller.emptyInput(1000L);
				controller.setCommandLogging(false);
			}catch(IOException ie){
				ie.printStackTrace();
			}catch(IllegalArgumentException iae){
				iae.printStackTrace();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
    }
	
    /** Enable or disable the motor.
	 fulfills MotorControlIF interface
	 @param motor motor controller to enable
	 @param value enable if trye, disable if false
	 @param timeoutMsec timeout (milliseconds)
	 */
    public void setEnable(ElmoSolo motor,boolean value, long timeoutMsec)
	throws TimeoutException{
		if(motor==null){
			_log4j.error("motor is NULL");
			return;
		}
		try{
			long start=System.currentTimeMillis();
			while((System.currentTimeMillis()-start)<timeoutMsec){
				if(value==true){
					motor.enableMotor();
					if(motor.isEnabled()==true){
						return;
					}
				}else{
					motor.disableMotor();
					if(motor.isEnabled()==false){
						return;
					}
				}
			}
		}catch(TimeoutException te){
			_log4j.error("Timeout Error - "+te.getMessage());
		}catch(IOException ie){
			_log4j.error("IO Error - "+ie.getMessage());
		}catch(NullPointerException ne){
			_log4j.error("Null Pointer Error - "+ne.getMessage());
		}catch(IllegalArgumentException iae){
			_log4j.error("Illegal Argument Error - "+iae.getMessage());
		}catch(Exception e){
			_log4j.error("Error - "+e.getMessage());
		}
		throw new TimeoutException("setEnable timed out ["+value+","+timeoutMsec+"]");
    }
	
    /** Command motor velocity (in rpm)
	 fulfills MotorControlIF interface
	 @param motor motor controller to enable
	 @param rpm motor speed to set (RPM)
	 */
    public void setVelocity(ElmoSolo motor,double rpm){
		if(motor==null){
			_log4j.error("motor is NULL");
			return;
		}
		if(useShaftSpeed==true){
			rpm*=Math.abs(GEAR_RATIO);
		}
		// convert rpm to counts/sec...
		double Vcount=motor.rpm2counts(rpm);
		
		try{
			_log4j.debug("Setting velocity to "+rpm+" rpm ["+Vcount+" counts]");
			motor.commandVelocity((int)Vcount,ACCELERATION,DECELERATION);
		}catch(TimeoutException te){
			_log4j.error("Timeout Error - "+te.getMessage());
		}catch(IOException ie){
			_log4j.error("IO Error - "+ie.getMessage());
		}catch(NullPointerException ne){
			_log4j.error("Null Pointer Error - "+ne.getMessage());
		}catch(IllegalArgumentException iae){
			_log4j.error("Illegal Argument Error - "+iae.getMessage());
		}catch(Exception e){
			_log4j.error("Error - "+e.getMessage());
		}
		return;
    }
	
    /** Perform the actions indicated on the command line.
	 @return 0 on success
     */
    public int run() {
		if ((_forwardMotorRPM != null) || (_forwardMotorEnable != null))
		{
			if (_forwardMotorPort == null)
				_forwardMotorPort = FORWARD_MOTOR_PORT;
			_log4j.debug("forwardMotorRPM="+_forwardMotorRPM);
			_log4j.debug("forwardMotorEnable="+_forwardMotorEnable);
			_log4j.debug("forwardMotorPort="+_forwardMotorPort);
		}
		
		if ((_aftMotorRPM != null) || (_aftMotorEnable != null))
		{
			if (_aftMotorPort == null)
				_aftMotorPort = AFT_MOTOR_PORT;
			_log4j.debug("aftMotorEnable="+_aftMotorEnable);
			_log4j.debug("aftMotorPort="+_aftMotorPort);
			_log4j.debug("aftMotorRPM="+_aftMotorRPM);
		}
		
		try{
			configurePorts(_forwardMotorPort,_aftMotorPort);
			if ((_forwardMotorRPM != null) || (_forwardMotorEnable != null))
			{
				fwdController=new ElmoSolo(fwdSerialPort);
				initializeController(fwdController);
			}
			if ((_aftMotorRPM != null) || (_aftMotorEnable != null))
			{
				aftController=new ElmoSolo(aftSerialPort);
				initializeController(aftController);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		try{
			if( _forwardMotorRPM!=null && fwdController!=null){
				_log4j.debug("setting fwd motor RPM ["+_forwardMotorRPM+"]");
				setVelocity(fwdController,_forwardMotorRPM.doubleValue());
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		try{
			if( _aftMotorRPM!=null && aftController!=null){
				_log4j.debug("setting aft motor RPM ["+_aftMotorRPM+"]");
				setVelocity(aftController,_aftMotorRPM.doubleValue());
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		try{
			if(_forwardMotorEnable!=null && fwdController!=null){
				_log4j.debug("setting fwd motor enable ["+_forwardMotorEnable+"]");
				setEnable(fwdController,_forwardMotorEnable.booleanValue(),3000L);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		try{
			if(_aftMotorEnable!=null && aftController!=null){
				_log4j.debug("setting aft motor enable ["+_aftMotorEnable+"]");
				setEnable(aftController,_aftMotorEnable.booleanValue(),3000L);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return 0;
    }
    /** Main entry point.
	 @param args command line arguments
	 */
    public static void main(String[] args) {
		
		FOCEMotorUtil motorUtil = new FOCEMotorUtil();
		motorUtil.processArguments(args);
		motorUtil.run();
		System.exit(0);
    }
}
