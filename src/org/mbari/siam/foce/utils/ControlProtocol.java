/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.utils;

import java.util.*;
//import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import org.mbari.siam.distributed.InvalidPropertyException;

import org.mbari.siam.foce.devices.controlLoop.*;
import org.mbari.siam.distributed.devices.*;
import org.mbari.siam.utils.TCPServer;
import org.mbari.siam.utils.TCPProtocol;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

public class ControlProtocol implements TCPProtocol{
    static protected Logger _log4j = Logger.getLogger(ControlProtocol.class);  
    
	public static final String COMMAND_DELIMITER =",";
    public static final String COMMAND_SETP      ="setp";
    public static final String COMMAND_GETP      ="getp";
    public static final String COMMAND_GETS      ="gets";
    public static final String COMMAND_PNAMES    ="pnames";
    public static final String COMMAND_SNAMES    ="snames";
    public static final String COMMAND_PCMODE    ="pcmode";
    public static final String COMMAND_PRMODE    ="prmode";
    public static final String COMMAND_VCMODE    ="vcmode";
    public static final String COMMAND_VRMODE    ="vrmode";
    public static final String COMMAND_HELP      ="help";
    public static final String COMMAND_START     ="start";
    public static final String COMMAND_STOP      ="stop";
    public static final String COMMAND_PAUSE     ="pause";
    public static final String COMMAND_RESUME    ="resume";
    public static final String COMMAND_QUIT      ="quit";
    public static final String COMMAND_EXIT      ="exit";
    public static final String RESPONSE_OK       ="OK";
    public static final String RESPONSE_ERR      ="ERR";
    public static final String MSG_DISCONNECT    ="quit";
    public static final String MNEM_LIN          ="LIN";
    public static final String MNEM_RMLIN        ="RMODE_LIN";
    public static final String MNEM_PID          ="PID";
    public static final String MNEM_RMPID        ="RMODE_PID";
    public static final String MNEM_EXP          ="EXP";
    public static final String MNEM_RMEXP        ="RMODE_EXP";
    public static final String MNEM_MANUAL       ="MANUAL";
    public static final String MNEM_CMMANUAL     ="CMODE_MANUAL";
    public static final String MNEM_OFFSET       ="OFFSET";
    public static final String MNEM_CMOFFSET     ="CMODE_OFFSET";
    public static final String MNEM_CONSTANT     ="CONSTANT";
    public static final String MNEM_CMCONSTANT   ="CMODE_CONSTANT";
    public static final String MNEM_DEADBAND     ="DEADBAND";
    public static final String MNEM_CMDEADBAND   ="CMODE_DEADBAND";
	
	protected ControlLoopConfigIF  _controlLoop;
	protected ProcessConfigIF  _processConfig;
	private boolean _suppressStatus=false;
	
	static Vector _terminators=new Vector();
	static {
		_terminators.add(RESPONSE_OK);
		_terminators.add(RESPONSE_ERR);
	}

	public ControlProtocol(ProcessConfigIF  processConfig, ControlLoopConfigIF  controlLoop){
		this();
		_controlLoop=controlLoop;
		_processConfig=processConfig;
	}
	public ControlProtocol(){
		super();
	}
	public void setSuppressStatus(boolean suppressStatus){
		_suppressStatus=suppressStatus;
	}
	public Iterator terminators(){
		return _terminators.iterator();
	}
	public String disconnectString(){
		return (MSG_DISCONNECT+"\n");
	}
		
	public String help(){
		StringBuffer sb=new StringBuffer();
		sb.append("\n");
		sb.append("Control Client - Change values of running (SIAM) ControlLoopService \n");
		sb.append("\n");
		sb.append("controlClient <siamHost> [<siamPort>|-r <registryName>] [-help] -c <command> [-c <command>...]]\n");
		sb.append("\n");
		sb.append("-c <command> : command (see below)\n");
		sb.append("\n");
		sb.append("Valid commands:\n");
		sb.append("start,<id>            : start control loop\n");
		sb.append("stop,<id>             : stop control loop\n");
		sb.append("pause,<id>            : suspend control loop updates\n");
		sb.append("resume,<id>           : continue a paused loop process\n");
		sb.append("pcmode,<id or name>   : set pH control mode\n");
		sb.append("prmode,<id or name>   : set pH response mode\n");
		sb.append("vcmode,<id or name>   : set velocity control mode\n");
		sb.append("setp,<name>,<value>   : set process parameter\n");
		sb.append("getp,<name>           : get value of process parameter\n");
		sb.append("gets                  : get value of process signal\n");
		sb.append("pnames                : get list of parameter names\n");
		sb.append("snames                : get list of signal names\n");
		sb.append("help                  : show this message\n");
		sb.append("quit                  : quit session\n");
		sb.append("exit                  : halt server\n");
		return sb.toString();
	}
	
	public String  processInput(String inputLine) throws Exception{
		
		// validate command format
		if(inputLine==null || inputLine.length()<4){
			return RESPONSE_ERR+" invalid input ["+inputLine+"]\n";
		}
		// make a tokenizer (commands are comma delimited: command[,arg[,arg...]]
		StringTokenizer st=new StringTokenizer(inputLine,",");
		// all commands have a command as the first token...
		String command=st.nextToken().trim();
		// make a response buffer to fill out as we parse
		StringBuffer response=new StringBuffer();
		String status=null;
		
		// parse command and additional args
		// and generate response...
		if(command.equals(COMMAND_HELP)){
			// show help
			response.append(help());
			//response.append(RESPONSE_OK+"\n");
			status=RESPONSE_OK+"\n";
			//return response.toString();
		}else if(command.equals(COMMAND_START)){
			// set input
			//response.append(" [not implemented]\n");
			status=RESPONSE_ERR+" [not implemented]\n";
			//return response.toString();
		}else if(command.equals(COMMAND_STOP)){
			// set input
			//response.append(" [not implemented]\n");
			status=RESPONSE_ERR+" [not implemented]\n";
			//return response.toString();
		}else if(command.equals(COMMAND_PAUSE)){
			String id=st.nextToken().trim();
			// pause control loop worker
			_controlLoop.pauseControl(Integer.parseInt(id));
			status=RESPONSE_OK;
			//return RESPONSE_OK+"\n";
		}else if(command.equals(COMMAND_RESUME)){
			String id=st.nextToken().trim();
			// resume control loop worker
			_controlLoop.resumeControl(Integer.parseInt(id));
			//response.append(RESPONSE_OK+"\n");
			status=RESPONSE_OK+"\n";
			//return response.toString();
		}else if(command.equals(COMMAND_PCMODE)){
			// set control mode
			// consisting of name and numeric or string mnemonic value
			String mode=st.nextToken().trim();
			try{
				if(mode.equalsIgnoreCase(MNEM_MANUAL) || mode.equals(MNEM_CMMANUAL)){
					_controlLoop.setPHControlMode(ControlLoopConfigIF.MODE_MANUAL);
				}else if(mode.equalsIgnoreCase(MNEM_CONSTANT) || mode.equals(MNEM_CMCONSTANT)){
					_controlLoop.setPHControlMode(ControlLoopConfigIF.MODE_CONSTANT);
				}else if(mode.equalsIgnoreCase(MNEM_OFFSET) || mode.equals(MNEM_CMOFFSET)){
					_controlLoop.setPHControlMode(ControlLoopConfigIF.MODE_OFFSET);
				}else{
					// assume its a number
					_controlLoop.setPHControlMode(Integer.parseInt(mode));
				}
				//response.append(RESPONSE_OK+"\n");
				status=RESPONSE_OK+"\n";
				//return response.toString();				
			}catch (InvalidPropertyException e) {
				//response.append(RESPONSE_ERR+" invalid property exception "+e+"\n");
				//return response.toString();
				status=RESPONSE_ERR+" invalid property exception "+e+"\n";
			}
		}else if(command.equals(COMMAND_PRMODE)){
			// set control mode
			// consisting of name and numeric or string mnemonic value
			String mode=st.nextToken().trim();
			try{
				if(mode.equalsIgnoreCase(MNEM_LIN) || mode.equals(MNEM_RMLIN)){
					_controlLoop.setPHResponseMode(ControlLoopConfigIF.MODE_LIN);
				}else if(mode.equalsIgnoreCase(MNEM_PID) || mode.equals(MNEM_RMPID)){
					_controlLoop.setPHResponseMode(ControlLoopConfigIF.MODE_PID);
				}else if(mode.equalsIgnoreCase(MNEM_EXP) || mode.equals(MNEM_RMEXP)){
					_controlLoop.setPHResponseMode(ControlLoopConfigIF.MODE_EXP);
				}else{
					// assume its a number
					_controlLoop.setPHControlMode(Integer.parseInt(mode));
				} 
				//response.append(RESPONSE_OK+"\n");
				status=RESPONSE_OK+"\n";
				//return response.toString();				
			}catch (InvalidPropertyException e) {
				//response.append(RESPONSE_ERR+" invalid property exception "+e+"\n");
				//return response.toString();
				status=RESPONSE_ERR+" invalid property exception "+e+"\n";
			}
		}else if(command.equals(COMMAND_VCMODE)){
			// set control mode
			// consisting of name and numeric or string mnemonic value
			String mode=st.nextToken().trim();
			try{
				if(mode.equalsIgnoreCase(MNEM_MANUAL) || mode.equals(MNEM_CMMANUAL)){
					_controlLoop.setVelocityControlMode(ControlLoopConfigIF.MODE_MANUAL);
				}else if(mode.equalsIgnoreCase(MNEM_CONSTANT) || mode.equals(MNEM_CMCONSTANT)){
					_controlLoop.setVelocityControlMode(ControlLoopConfigIF.MODE_CONSTANT);
				}else if(mode.equalsIgnoreCase(MNEM_OFFSET) || mode.equals(MNEM_CMOFFSET)){
					_controlLoop.setVelocityControlMode(ControlLoopConfigIF.MODE_OFFSET);
				}else if(mode.equalsIgnoreCase(MNEM_DEADBAND) || mode.equals(MNEM_CMDEADBAND)){
					_controlLoop.setVelocityControlMode(ControlLoopConfigIF.MODE_DEADBAND);
				}else{
					// assume its a number
					_controlLoop.setVelocityControlMode(Integer.parseInt(mode));
				} 
				//response.append(RESPONSE_OK+"\n");
				status=RESPONSE_OK+"\n";
				//return response.toString();				
			}catch (InvalidPropertyException e) {
				//response.append(RESPONSE_ERR+" invalid property exception "+e+"\n");
				//return response.toString();
				status=RESPONSE_ERR+" invalid property exception "+e+"\n";
			}
		}else if(command.equals(COMMAND_SETP)){
			// set control parameter
			// consisting of name and value
			// (names may be gotten with pnames command)
			String name=st.nextToken().trim();
			String value=st.nextToken().trim();
			try{
				_processConfig.setParameter(name, value);
				//response.append(RESPONSE_OK+"\n");
				status=RESPONSE_OK+"\n";
				//return response.toString();
			}catch (InvalidPropertyException e) {
				//response.append(RESPONSE_ERR+" invalid property exception "+e+"\n");
				//return response.toString();
				status=RESPONSE_ERR+" invalid property exception "+e+"\n";
			}
		}else if(command.equals(COMMAND_GETP)){
			// get parameter
			// consisting of name
			// (names may be gotten with pnames command)
			String name=st.nextToken();
			try{
				int id=_processConfig.parameterID(name);
				Number value=_processConfig.getParameter(id);
				_log4j.debug("name="+name+" id="+id+" parameter value="+value);
				//response.append(name+"="+value+"\n"+RESPONSE_OK+"\n");				
				//return response.toString();
				response.append(name+"="+value+"\n");				
				status=RESPONSE_OK+"\n";
			}catch (InvalidPropertyException e) {
				//response.append(RESPONSE_ERR+" invalid property exception "+e+"\n");
				//return response.toString();
				status=RESPONSE_ERR+" invalid property exception "+e+"\n";
			}
		}else if(command.equals(COMMAND_GETS)){
			// get signal
			// consisting of name
			// (signal names may be gotten with snames command)
			String name=st.nextToken();
			try{
				int id=_processConfig.signalID(name);
				Number value=_processConfig.getSignal(id);
				_log4j.debug("name="+name+" id="+id+" signal value="+value);
				//response.append(name+"="+value+"\n"+RESPONSE_OK+"\n");				
				//return response.toString();
				response.append(name+"="+value+"\n");				
				status=RESPONSE_OK+"\n";
			}catch (InvalidPropertyException e) {
				//response.append(RESPONSE_ERR+" invalid property exception "+e+"\n");
				//return response.toString();
				status=RESPONSE_ERR+" invalid property exception "+e+"\n";
			}
		}else if(command.equals(COMMAND_PNAMES)){
			// get parameter names
			String[] names=_processConfig.parameterNames();
			for(int i=0;i< names.length;i++){
				response.append(names[i]+"\n");
			}
			//response.append(RESPONSE_OK+"\n");
			//return response.toString();
			status=RESPONSE_OK+"\n";
		}else if(command.equals(COMMAND_SNAMES)){
			// get signal names
			String[] names=_processConfig.signalNames();
			for(int i=0;i< names.length;i++){
				response.append(names[i]+"\n");
			}
			//response.append(RESPONSE_OK+"\n");
			//return response.toString();
			status=RESPONSE_OK+"\n";
		}
		if(status==null){
			// bad command
			response.append(RESPONSE_ERR+" invalid command ["+command+"]\n");
			return response.toString();		
		}
	    if (_suppressStatus==true) {
			return response.toString();
		}
		return response.toString()+status;
	}
	
}
