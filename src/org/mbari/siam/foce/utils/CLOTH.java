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
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.ServiceAttributes;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.PropertyException;

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

/* CLOTH - Control Loop Test Harness
 */
/*
 $Id: $
 $Name: $
 $Revision: $
 */

public class CLOTH implements DeviceServiceIF  {
	
	/** Service attributes */
	protected ControlLoopAttributes _attributes;
	
	ControlResponseIF _phControlResponse;
	ControlProcessIF  _phControlProcess;
	
	ControlLoopWorker _worker;
	
	TCPServer _server;
	int _serverPort=4444;
	/** server accept() timeout (making it possible to check for exit request) */
	int _serverTimeout=20000;
	int _workerUpdatePeriod=10000;
	
    static protected Logger _log4j = Logger.getLogger(CLOTH.class);  

	/** Zero-arg constructor	*/
	public CLOTH() throws Exception,RemoteException
	{
		super();
		/*
		 * Set up a simple configuration that logs on the console. Note that
		 * simply using PropertyConfigurator doesn't work unless JavaBeans
		 * classes are available on target. For now, we configure a
		 * PropertyConfigurator, using properties passed in from the command
		 * line, followed by BasicConfigurator which sets default console
		 * appender, etc.
		 */
		// use siam.log4j to configure log4j for CLOTH
		Properties p=System.getProperties();
		String dflt_log4j=p.getProperty("siam_home","./")+"/properties/siam.log4j";
		String siam_log4j=p.getProperty("siam.log4j",dflt_log4j);
		PropertyConfigurator.configure(siam_log4j);
/*
		PropertyConfigurator.configure(System.getProperties());
		PatternLayout layout = new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");
		BasicConfigurator.configure(new ConsoleAppender(layout));
*/		
		_attributes=new ControlLoopAttributes(this,true);
		_phControlProcess=new FOCEProcess(_attributes);	
		ControlResponseIF pidResponse=new PH_PID_Responder((FOCEProcess)_phControlProcess,_attributes);
		_worker=new ControlLoopWorker(_phControlProcess, pidResponse,_workerUpdatePeriod);
	}
	public CLOTH(int updatePeriodMillisec) throws Exception,RemoteException
	{
		super();
		setUpdatePeriod(updatePeriodMillisec);
	}
	
	public void setUpdatePeriod(int updatePeriodMillisec){
		_workerUpdatePeriod=updatePeriodMillisec;
		if(_worker!=null){
			_worker.setUpdatePeriod(updatePeriodMillisec);
		}
	}
	
	/** Set the ServiceAttributes object for specified DeviceService object. */
    public void setAttributes(ServiceAttributes attributes){
	}
	
    /** Get the name of the service. */
    public byte[] getName(){
		return "CLOTH".getBytes();
	}
	
	public void start(){
		try{
			_phControlProcess.startProcess();
			new Thread(_worker).start();
			_server=new TCPServer(_serverPort,new CLOTHProtocol(_worker,_phControlProcess),_serverTimeout);
			_server.listen();
		}catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}
	public void stop() throws Exception{
		_log4j.debug("waiting for server to exit...");
		_server.quit();		
		_log4j.debug("stopping control process...");
		_worker.terminate();
		_phControlProcess.stopProcess();
	}
	public void resume(){
		_worker.pause(false);
	}
	public void pause(){
		_worker.pause(true);
		return;
	}
	
	public void printHelp(){
		StringBuffer sb=new StringBuffer();
		sb.append("\n");
		sb.append("CLOTH - Closed Loop Test Harness\n");
		sb.append("\n");
		sb.append("CLOTH [-p <port> -t <timeout> -P <period>]\n");
		sb.append("\n");
		sb.append("-p <port>    : server TCP/IP port                  ["+_serverPort+"]\n");
		sb.append("-t <timeout> : server thread accept timeout (msec) ["+_serverTimeout+"]\n");
		sb.append("-P <period>  : worker update period (msec)         ["+_workerUpdatePeriod+"]\n");
		sb.append("\n");
		System.out.println(sb.toString());
	}
	
	public void processArgs(String[] args) throws Exception{
		if(args.length<=0){
			return;
		}
		for(int i=0;i<args.length;i++){
			String arg=args[i];
			if(arg.equals("-p")){
				_serverPort=Integer.parseInt(args[i+1]);
				i++;
			}
			if(arg.equals("-t")){
				_serverTimeout=Integer.parseInt(args[i+1]);
				i++;
			}
			if(arg.equals("-P")){
				_workerUpdatePeriod=Integer.parseInt(args[i+1]);
				setUpdatePeriod(_workerUpdatePeriod);
				i++;
			}
			if(arg.equals("-help") || arg.equals("--help")){
				printHelp();
				System.exit(1);
			}
		}
	}
	
    public static void main(String[] args) {
				
		try{
			CLOTH cloth=new CLOTH();
			cloth.processArgs(args);
			
			_log4j.info("server port   :"+cloth._serverPort);
			_log4j.info("server timeout:"+cloth._serverTimeout);
			_log4j.info("worker period :"+cloth._workerUpdatePeriod);
			_log4j.debug("starting server...");
			cloth.start();
			_log4j.debug("stopping...");
			cloth.stop();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
}