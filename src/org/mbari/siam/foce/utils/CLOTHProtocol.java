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
//import java.rmi.RemoteException;

//import org.mbari.siam.distributed.TimeoutException;
//import org.mbari.siam.distributed.InstrumentServiceAttributes;
//import org.mbari.siam.distributed.ServiceAttributes;
//import org.mbari.siam.distributed.DeviceServiceIF;
//import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.InvalidPropertyException;
//import org.mbari.siam.distributed.PropertyException;

import org.mbari.siam.foce.devices.controlLoop.*;
import org.mbari.siam.distributed.devices.*;
import org.mbari.siam.utils.TCPServer;
import org.mbari.siam.utils.TCPProtocol;

/*
 import gnu.io.SerialPort;
 import gnu.io.CommPortIdentifier;
 import gnu.io.PortInUseException;
 import gnu.io.NoSuchPortException;
 import gnu.io.UnsupportedCommOperationException;
 */
import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

public class CLOTHProtocol implements TCPProtocol{
    static protected Logger _log4j = Logger.getLogger(CLOTHProtocol.class);  
    
	public static final String COMMAND_DELIMITER =",";
    public static final String COMMAND_SETP      ="setp";
    public static final String COMMAND_GETP      ="getp";
    public static final String COMMAND_GETS      ="gets";
    public static final String COMMAND_PNAMES    ="pnames";
    public static final String COMMAND_SNAMES    ="snames";
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
	
	ControlProcessIF  _phControlProcess;
	ControlLoopWorker _worker;
	
	static Vector _terminators=new Vector();
	static {
		_terminators.add(RESPONSE_OK);
		_terminators.add(RESPONSE_ERR);
	}

	public CLOTHProtocol(ControlLoopWorker worker,ControlProcessIF phControlProcess){
		this();
		_phControlProcess=phControlProcess;
		_worker=worker;
	}
	public CLOTHProtocol(){
		super();
	}
	
	public Iterator terminators(){
		return _terminators.iterator();
	}
	public String disconnectString(){
		return (MSG_DISCONNECT+"\n");
	}
	
	public String help(){
		StringBuffer sb=new StringBuffer();
		sb.append("start                : start control process\n");
		sb.append("stop                 : stop control process\n");
		sb.append("pause                : suspend control process updates\n");
		sb.append("resume               : continue a paused control process\n");
		sb.append("setp,<name>,<value>  : set process parameter\n");
		sb.append("getp,<name>          : get value of process parameter\n");
		sb.append("gets                 : get value of process signal\n");
		sb.append("pnames               : get list of parameter names\n");
		sb.append("snames               : get list of signal names\n");
		sb.append("help                 : show this message\n");
		sb.append("quit                 : quit session\n");
		sb.append("exit                 : halt server\n");
		return sb.toString();
	}
	
	public String  processInput(String inputLine) throws Exception{
		if(inputLine==null || inputLine.length()<4){
			return "ERR invalid input ["+inputLine+"]\n";
		}
		StringTokenizer st=new StringTokenizer(inputLine,",");
		String command=st.nextToken().trim();
		StringBuffer response=new StringBuffer();

		if(command.equals(COMMAND_HELP)){
			// show help
			response.append(help());
			response.append(RESPONSE_OK+"\n");
			return response.toString();
		}else if(command.equals(COMMAND_START)){
			// set input
			response.append(RESPONSE_ERR+" [not implemented]\n");
			return response.toString();
		}else if(command.equals(COMMAND_STOP)){
			// set input
			response.append(RESPONSE_ERR+" [not implemented]\n");
			return response.toString();
		}else if(command.equals(COMMAND_PAUSE)){
			// pause worker
			_worker.pause(true);
			return RESPONSE_OK+"\n";
		}else if(command.equals(COMMAND_RESUME)){
			// resume worker
			_worker.pause(false);
			response.append(RESPONSE_OK+"\n");
			return response.toString();
		}
		else if(command.equals(COMMAND_SETP)){
			// set parameter
			String name=st.nextToken().trim();
			String value=st.nextToken().trim();
			try{
				_phControlProcess.setParameter(name, value);
			}catch (InvalidPropertyException e) {
				response.append(RESPONSE_ERR+" invalid property exception "+e+"\n");
				return response.toString();
			}
			response.append(RESPONSE_OK+"\n");
			return response.toString();
		}else if(command.equals(COMMAND_GETP)){
			// set parameter
			String name=st.nextToken();
			try{
				int id=_phControlProcess.parameterID(name);
				Number value=_phControlProcess.getParameter(id);
				_log4j.debug("name="+name+" id="+id+" parameter value="+value);
				response.append(name+"="+value+"\n"+RESPONSE_OK+"\n");				
				return response.toString();
			}catch (InvalidPropertyException e) {
				response.append(RESPONSE_ERR+" exception "+e+"\n");				
				return response.toString();
			}
		}else if(command.equals(COMMAND_GETS)){
			// get signal
			String name=st.nextToken();
			try{
				int id=_phControlProcess.signalID(name);
				Number value=_phControlProcess.getSignal(id);
				_log4j.debug("name="+name+" id="+id+" signal value="+value);
				response.append(name+"="+value);				
				return response.toString();
			}catch (InvalidPropertyException e) {
				response.append(RESPONSE_ERR+" exception "+e+"\n");				
				return response.toString();
			}
		}else if(command.equals(COMMAND_PNAMES)){
			// get parameter names
			String[] names=_phControlProcess.parameterNames();
			for(int i=0;i< names.length;i++){
				response.append(names[i]+"\n");
			}
			response.append(RESPONSE_OK+"\n");
			return response.toString();			
		}else if(command.equals(COMMAND_SNAMES)){
			// get signal names
			// get parameter names
			String[] names=_phControlProcess.signalNames();
			for(int i=0;i< names.length;i++){
				response.append(names[i]+"\n");
			}
			response.append(RESPONSE_OK+"\n");
			return response.toString();
		}			
		// bad command
		response.append(RESPONSE_ERR+" invalid command ["+command+"]\n");
		return response.toString();
	}
	
}
