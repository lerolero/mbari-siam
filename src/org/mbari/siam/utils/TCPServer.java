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

public class TCPServer{
	static protected Logger _log4j = Logger.getLogger(TCPServer.class);  
	
	protected int _tcpPort;
	protected TCPProtocol _protocol;
	protected int _socketTimeoutMillisec=0;
	protected boolean _isConnected=false;
	boolean _exit=false;
	
	public TCPServer(){
		super();
	}
	public TCPServer(int port, TCPProtocol protocol){
		this();
		_tcpPort=port;
		_protocol=protocol;
	}
	public TCPServer(int port, TCPProtocol protocol,int timeoutMillisec){
		this(port,protocol);
		setTimeout(timeoutMillisec);
	}
	
	public boolean isConnected(){
		return _isConnected;
	}
	
	public void setTimeout(int timeout){
		_socketTimeoutMillisec=timeout;
	}

	public void quit(){
		_exit=true;
	}
	
	public class ConnectionWorker extends Thread{
		private Socket clientSocket=null;
		public ConnectionWorker(Socket socket){
			super("ConnectionWorker");
			clientSocket=socket;
		}
		public void run(){
			ServerSocket serverSocket=null;
			PrintWriter toClient=null;
			BufferedReader fromClient=null;
			try {
				
				toClient = new PrintWriter(clientSocket.getOutputStream(), true);
				fromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				String inputLine=null, outputLine=null;
				
				while ((inputLine = fromClient.readLine().trim()) != null) {
					if(inputLine.equals("exit")){
						_exit=true;
						break;
					}
					if(inputLine.equals("quit")){
						break;
					}
					try{
						outputLine = _protocol.processInput(inputLine);
						toClient.println(outputLine);
					}catch(Exception e){
						e.printStackTrace();
						toClient.println("ERROR - "+e);
					}
				}
				if(fromClient!=null){
					fromClient.close();
				}
				if(toClient!=null){
					toClient.close();
				}
				if(serverSocket!=null){
					serverSocket.close();
				}
			}  catch (InterruptedIOException ste) {
				_log4j.debug("socket timed out");
			}catch (IOException ioe) {
				_log4j.error("Accept failed ["+_tcpPort+"]");
				//throw ioe;
			}
			
			return;
		}
	}
	
	public void listen() throws IOException{
		boolean exit=false;
		ServerSocket serverSocket=null;
		PrintWriter toClient=null;
		BufferedReader fromClient=null;
		try {
			_log4j.debug("TCPServe listening on port ["+_tcpPort+"]");

			serverSocket = new ServerSocket(_tcpPort);
			if(_socketTimeoutMillisec>0){
				serverSocket.setSoTimeout(_socketTimeoutMillisec);
			}
		} catch (IOException ioe) {
			_log4j.error("Could not listen on port ["+_tcpPort+"]");
			throw ioe;
		}
		_isConnected=true;
		while(_exit==false){
			Socket clientSocket = null;
			try {
				new ConnectionWorker(serverSocket.accept()).start();
			}  catch (InterruptedIOException ste) {
				_log4j.debug("socket timed out");
			}catch (IOException ioe) {
				_log4j.error("Accept failed ["+_tcpPort+"]");
				throw ioe;
			}
		}
		fromClient.close();
		toClient.close();
		serverSocket.close();
		_isConnected=false;
		return;
	}
}