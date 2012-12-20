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
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.RMISocketFactory;
import java.net.URL;
import java.net.MalformedURLException;

import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.ServiceAttributes;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.PropertyException;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.devices.*;

import org.mbari.siam.utils.SiamSocketFactory;
import org.mbari.siam.utils.TCPServer;
import org.mbari.siam.utils.TCPProtocol;
import org.mbari.siam.operations.utils.NodeUtility;

import org.mbari.siam.foce.devices.controlLoop.*;
import org.mbari.siam.foce.utils.ControlProtocol;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

/* An example of a ControlLoopService client.
 May be used to query and set Control Loop parameters.
 
 The basic pattern of operation is 
 - get a node reference
 - get an Instrument reference to the ControlLoopService
 - cast the Instrument to two interfaces implemented by the service:
   - ProcessConfigIF: for reading control process signals and setting parameters
   - ControlLoopConfigIF: for controlling the control loop (start, stop, pause, period)

 Clients like the GUI can use a similar pattern of operation to control the control loop 
 service through these two interfaces.
 */
/*
 $Id: $
 $Name: $
 $Revision: $
 */

public class ControlClient implements ProcessParameterIF  {
	
    static protected Logger _log4j = Logger.getLogger(ControlClient.class);  
	
	protected ProcessConfigIF  _processConfig;
	protected ControlLoopConfigIF  _controlLoop;
	protected Instrument  _instrument;
	protected ControlProtocol _protocol=new ControlProtocol();
	
	public  String _siamHost="localhost";
	public  String _siamPort=null;
	public String _registryName=null;
	protected boolean _lookupRegistry;
	protected boolean _isConnected=false;
	protected boolean _suppressStatus=false;
	
	protected  Node _siamNode=null;
	protected  Vector _commands;
	
	public ControlClient(){
		super();
		
		// vector to hold command strings from the command line
		_commands=new Vector();
		_isConnected=false;
	}
	
	public ControlClient(String args[]){
		this();
		try{
			// parse command line args to configure this ControlClient
			configure(args);
		}catch (Exception e) {
			_log4j.error("Could not configure client: ");
			e.printStackTrace();
			System.exit(-1);
		}
	}
	

	/** parse command line args to configure this ControlClient */
	protected void configure(String[] args) throws Exception{
		
		if(args.length<3){
			System.out.println("\n**** missing arguments:\n");
			System.out.println(_protocol.help());
			System.exit(1);
		}
		
		// process SIAM host/port
		int startIndex=2;
		// siam host name is always first argument
		String host=args[0];

		String port=null;
		boolean useRegistry=false;
		
		// take second arg to be port name or registry lookup
		if(args[1].equals("-r")){
			useRegistry=true;
			port=args[2];
			startIndex=3;
		}else{
			port=args[1];
			useRegistry=false;
			startIndex=2;
		}

		configure(host,port,useRegistry);

		// parse other arguments
		for(int i=startIndex;i<args.length;i++){
			
			String arg=args[i];
			if(arg.equals("-help") || arg.equals("--help")){
				// print help message and exit
				System.out.println(_protocol.help());
				System.exit(1);
			}else if(arg.equals("-c")){
				// any other arguments (with or with -c) are taken as commands
				_commands.add(args[i+1]);
				i++;
			}else{
				_commands.add(args[i]);			
			}
		}
	}
	
	public void configure(String host, String port, boolean useRegistry){
		setHost(host);
		setUseRegistry(useRegistry);
		if(useRegistry==true){
			setRegistryName(port);
		}else{
			setPort(port);
		}
	}
	public void setSuppressStatus(boolean suppressStatus){
		this._suppressStatus=suppressStatus;
		_protocol.setSuppressStatus(this._suppressStatus);
	}
	
	public boolean isConnected(){
		return _isConnected;
	}
	
	public void setCommands(Vector commands){
		_commands=commands;
	}
	
	protected void setHost(String host){
		_siamHost=host;
	}
	protected void setPort(String port){
		_siamPort=port;
	}
	protected void setRegistryName(String name){
		_registryName=name;
	}
	protected void setUseRegistry(boolean useRegistry){
		_lookupRegistry=useRegistry;
	}
	
	/** start this client */
	public void start(){
		
		try {
			// initialize (make connections)
			connect();
			// send commands 
			sendCommands();
		}
		catch (Exception e) {
			_log4j.error("Could not complete request:");
			e.printStackTrace();
		}
	}
	
	/** Get a SIAM node reference */
	protected Node getNodeReference(String siamHost) throws Exception{
		
		// don't try if it's null
		if(siamHost==null){
			return null;
		}
		
		// Set security manager, if not already set.
        if ( System.getSecurityManager() == null ) {
            System.setSecurityManager(new SecurityManager());
		}
		
		// node instance
		Node node;
		
		// generate a SIAM node URL
		String nodeURL=NodeUtility.getNodeURL(siamHost);
		_log4j.debug("using nodeURL "+nodeURL);
		
		// Create socket factory; overcomes problems with RMI 'hostname'
		// property.
		try {
			String host = NodeUtility.getHostName(nodeURL);
			//_log4j.debug("calling setSocketFactory using host "+host);
			//RMISocketFactory.setSocketFactory(new SiamSocketFactory(host));
			RMISocketFactory testFactory=RMISocketFactory.getSocketFactory();
			if(testFactory==null){
				_log4j.debug("calling setSocketFactory using host "+host);
				RMISocketFactory.setSocketFactory(new SiamSocketFactory(host));
			}else {
				_log4j.debug("socketFactory found: "+testFactory);
			}				
		}catch (MalformedURLException e) {
			_log4j.error("Malformed URL \"" + nodeURL + "\": " + 
						 e.getMessage());
		}catch (IOException e) {
			_log4j.error("RMISocketFactory.setSocketFactory() failed");
			throw e;
		}
		
		// Get the node's proxy
		try {
			_log4j.debug("Get node proxy from " + nodeURL);
			// get the SIAM node stub (node reference)
			node = (Node)Naming.lookup(nodeURL.toString());
		}catch (Exception e) {
			_log4j.error("Couldn't get node proxy at " + nodeURL + ":");
			throw e;
		}
		// return the node reference
		return node;
	}
	
	/** set up connections for this control client */
	public void connect() throws Exception{
		// get the SIAM node stub
		_siamNode=getNodeReference(_siamHost);
		
		// find service via SIAM registry
		if(_lookupRegistry){
			_log4j.debug("Have Node Proxy, requesting service via registry");
			// get Instrument 
			_instrument=_siamNode.lookupService(_registryName);
			if(_instrument!=null){
				
				if(_instrument instanceof ControlLoopConfigIF){
					// cast Instrument (service) as ControlLoopConfigIF
					_controlLoop=(ControlLoopConfigIF)_instrument;
				}else{
					_log4j.error("instrument is not a ControlLoopConfigIF");
				}
				if(_instrument instanceof ControlLoopConfigIF){
					// cast Instrument (service) as ProcessConfigIF
					_processConfig=(ProcessConfigIF)_instrument;
				}else{
					_log4j.error("instrument is not a ProcessConfigIF");
				}
			}else{
				_log4j.error("Could not find instrument in registry: ["+_registryName+"]");
			}
		}else{
			// Find service by port name
			_log4j.debug("Have Node Proxy, requesting service via node");
			// get a Device from the node
			Device device = _siamNode.getDevice(_siamPort.getBytes());
			// if it implements the Instrument interface...
			if (device instanceof Instrument){
				// cast Device as Instrument
				_instrument = (Instrument)device;
				if(_instrument!=null){
					if(_instrument instanceof ControlLoopConfigIF){
						// cast Instrument (service) as ControlLoopConfigIF
						_controlLoop=(ControlLoopConfigIF)_instrument;
					}else{
						_log4j.error("instrument is not a ControlLoopConfigIF");
					}
					if(_instrument instanceof ControlLoopConfigIF){
						// cast Instrument (service) as ProcessConfigIF
						_processConfig=(ProcessConfigIF)_instrument;
					}else{
						_log4j.error("instrument is not a ProcessConfigIF");
					}
				}else{
					_log4j.error("node returned null instrument");
				}
			}else{
				_log4j.error("Device on port "+_siamPort+" is not an Instrument");
			}
		}
		if(_controlLoop==null || _processConfig==null){
			throw new Exception("could not cast instrument ["+_instrument+"] service: controlLoop:"+_controlLoop+" processConfig:"+_processConfig+"");
		}
		_protocol=new ControlProtocol(_processConfig,_controlLoop);
		_protocol.setSuppressStatus(this._suppressStatus);
		_isConnected=true;
	}
	
	public String sendCommand(String command)
	throws Exception{
		return _protocol.processInput(command);
	}
	
	/** send all commands specified on the command line */
	protected void sendCommands()
	throws Exception{
		for(Iterator i=_commands.iterator();i.hasNext();){
			String command=(String)i.next();
			_log4j.debug("command ["+command+"]");
			String response=sendCommand(command);
			System.out.println(response);
		}
	}
	
	/** ControlClient main entry point */
	public static void main(String[] args) {
		/*
		 * Set up a simple configuration that logs on the console. Note that
		 * simply using PropertyConfigurator doesn't work unless JavaBeans
		 * classes are available on target. For now, we configure a
		 * PropertyConfigurator, using properties passed in from the command
		 * line, followed by BasicConfigurator which sets default console
		 * appender, etc.
		 */
		// use siam.log4j to configure log4j for this class
		Properties p=System.getProperties();
		String dflt_log4j=p.getProperty("siam_home",".")+File.separator+"properties"+File.separator+"siam.log4j";
		String siam_log4j=p.getProperty("siam.log4j",dflt_log4j);
		PropertyConfigurator.configure(siam_log4j);
		
		// create a ControlClient using
		// command line arguments
		ControlClient client=new ControlClient(args);
		
		_log4j.info("siam host   :"+client._siamHost);
		_log4j.info("siam port   :"+client._siamPort);
		
		// run the client
		client.start();
		
	}
	
}