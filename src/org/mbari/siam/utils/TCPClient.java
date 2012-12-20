/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils;

import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import java.net.*;
import java.io.*;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

public class TCPClient{
	static protected Logger _log4j = Logger.getLogger(TCPClient.class);  
	
	protected String _serverHost;
	protected int _tcpPort;
	protected PrintWriter _toServer;
	protected BufferedReader _fromServer;
	protected Socket _socket;
	protected TCPProtocol _protocol;
	protected boolean _isConnected;
	
	public TCPClient(String serverHost, int port, TCPProtocol protocol){
		this(protocol);
		_serverHost=serverHost;
		_tcpPort=port;
		_protocol=protocol;
	}
	
	public TCPClient(TCPProtocol protocol){
		super();
		_protocol=protocol;
	}
	public TCPClient(){
		super();
		_isConnected=false;
	}
	
	public void configure(String[] args) throws Exception{

		if(args.length<=0){
			return;
		}
		for(int i=0;i<args.length;i++){
			String arg=args[i];
			if(arg.equals("-h")){
				_serverHost=args[i+1];
				i++;
			}else if(arg.equals("-p")){
				_tcpPort=Integer.parseInt(args[i+1]);
				i++;
			}
		}
	}
	public boolean isConnected(){
		return _isConnected;
	}
	
	public void connect()
	throws Exception{
		_socket=new Socket(_serverHost,_tcpPort);
		_toServer=new PrintWriter(_socket.getOutputStream(),true);
		_fromServer=new BufferedReader(new InputStreamReader(_socket.getInputStream()));
		_isConnected=true;
	}
	
	public void disconnect()
	throws Exception{
		_toServer.println(_protocol.disconnectString());
		_socket.close();
		_toServer.close();
		_fromServer.close();
		_isConnected=false;
	}

	public String writeRead(String data)
	throws Exception{
		_log4j.debug("sending command ["+data+"]");
		_toServer.println(data);
		_log4j.debug("reading response ["+(_fromServer==null?"fromServer is null":"fromServer not null")+"]");
		String serverResponse=_fromServer.readLine();
		if(serverResponse==null){
			_log4j.debug("response [null]");		
		}else {
			_log4j.debug("response ["+serverResponse+"]");
		}

		_log4j.debug("response ["+(serverResponse==null?"null":serverResponse)+"]");
		StringBuffer returnLine=new StringBuffer();
		returnLine.append(serverResponse+"\n");
		
		_log4j.debug("reading additional lines");
		while( _fromServer.ready()){
			serverResponse=_fromServer.readLine();
			if(serverResponse==null){
				_log4j.debug("readLine response [null]");		
			}else {
				_log4j.debug("readLine response ["+serverResponse+"]");
			}
			returnLine.append(serverResponse+"\n");
		}
		_log4j.debug("returning ["+returnLine.toString()+"]");
		return returnLine.toString();
	}
	
}