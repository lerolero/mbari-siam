/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.utils.osdt;

import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import java.net.*;
import java.io.*;

import com.rbnb.sapi.*;
import org.mbari.siam.foce.devices.controlLoop.OSDTConnectorWorker;
import org.mbari.siam.foce.devices.controlLoop.OSDTInputConnector;
import org.mbari.siam.foce.devices.controlLoop.WorkerThread;
import org.mbari.siam.distributed.devices.ControlInputIF;
import org.mbari.siam.utils.TCPProtocol;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

public class OSDTTestProtocol implements TCPProtocol{
    static protected Logger _log4j = Logger.getLogger(OSDTTestProtocol.class);  
	
	public static final String COMMAND_DELIMITER=",";
	public static final String COMMAND_SETP="setp";
	public static final String COMMAND_SETV="setv";
	public static final String COMMAND_SHOW="show";
	public static final String COMMAND_CLIST="clist";
	public static final String COMMAND_HELP="help";
	public static final String COMMAND_QUIT="quit";
	
	OSDTTestServer _instance;
	static Vector _terminators=new Vector();
	static {
		_terminators.add("OK");
		_terminators.add("ERR");
	}
	public OSDTTestProtocol(OSDTTestServer instance){
		_instance=instance;
	}
	public OSDTTestProtocol(){
		super();
	}
	
	public void setServerInstance(OSDTTestServer instance){
		_instance=instance;
	}
	
	public Iterator terminators(){
		return _terminators.iterator();
	}
	public String disconnectString(){
		return ("quit\n");
	}
	
	public String help(){
		StringBuffer sb=new StringBuffer();
		sb.append("setp,<channel>,<period> : set channel worker update period (milliseconds)\n");
		sb.append("setv,<channel>,<value>  : set channel output value\n");
		sb.append("show,<channel>          : show channel configuration and value\n");
		sb.append("clist                   : get channel list\n");
		sb.append("help                    : show this message\n");
		sb.append("quit                    : quit session\n");
		sb.append("exit                    : halt server\n");
		return sb.toString();
	}
	
	
	public String processInput(String inputLine)
	throws Exception{
		
		if(_instance==null){
			_log4j.error("server instance is null");
		}
		StringTokenizer st=new StringTokenizer(inputLine,COMMAND_DELIMITER);
		String cmd=st.nextToken();
		StringBuffer response=new StringBuffer();

		if(cmd.equals(COMMAND_HELP)){
			response.append(help());
			response.append("OK\n");
			return response.toString();
		}			
		
		String channel=st.nextToken();
		int iChannel=-1;
		OSDTTestWorker worker=null;
		// try channel as number...
		try {
			iChannel=Integer.parseInt(channel);
		}
		catch (NumberFormatException e) {
			// do nothing, could be name
		}
		if(iChannel>=0){
			worker=_instance.getWorker(iChannel);
		}else {
			worker=_instance.getWorker(channel);
		}

		
		if(worker==null){
			_log4j.error("channel "+channel+" worker is null");
			response.append("ERR channel "+channel+" worker is null\n");
		}
		
		if(cmd.equals(COMMAND_SETP)){
			long period=Long.parseLong(st.nextToken());
			_log4j.debug("setting channel "+channel+" period to "+period);
			worker.setUpdatePeriod(period);
			worker._channel.setPeriod(period);
			response.append("OK\n");
		}else if(cmd.equals(COMMAND_SETV)){
			String newValue=st.nextToken();
			_log4j.debug("setting channel "+channel+" value to "+newValue);
			worker._channel.setValue(newValue);
			response.append("OK\n");
		}else if(cmd.equals(COMMAND_SHOW)){
			String foo=worker._channel.toString();
			response.append(foo+"\n");
			response.append("\nOK");
		}else if(cmd.equals(COMMAND_CLIST)){
			Vector channels=_instance.getChannels();
			_log4j.debug("got channel list ["+channels.size()+"]");
			StringBuffer sb=new StringBuffer();
			for (int i=0; i<channels.size(); i++) {
				OSDTTestServer.ChannelSpec nextChannel=(OSDTTestServer.ChannelSpec)channels.get(i);
				_log4j.debug("got channel["+i+"] name: ["+nextChannel._name+"]");
				sb.append(nextChannel._name+"\n");
			}
			response.append(sb.toString());
			response.append("\nOK");
		}
		return response.toString();
	}
}