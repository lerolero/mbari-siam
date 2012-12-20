// Copyright MBARI 2011
package org.mbari.siam.foce.utils;

import java.rmi.RemoteException;
import java.util.Date;
import java.text.DateFormat;
import java.rmi.Naming;
import java.io.IOException;

import org.mbari.siam.operations.utils.PortUtility;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.PortOccupiedException;
import org.mbari.siam.distributed.DuplicateIdException;
import org.mbari.siam.distributed.PortNotFound;

import org.mbari.siam.distributed.devices.CO2SubsystemMonitorIF;
import org.mbari.siam.distributed.PacketParser;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;

/**
 *  Utility to invoke FOCE CO2 Subsystem Monitor methods
 *  @author K. Headley
 */
public class CO2MonitorUtil extends PortUtility{
	/** Log4J logger */
	static private Logger _log4j = Logger.getLogger(CO2MonitorUtil.class);

	/** Instrument service reference (node returns a Device instance) */
	Device		device ;
	/** Instrument service reference (we'll cast the Device to this interface) */
	CO2SubsystemMonitorIF	_CO2Subsys;
	/** Cooling fan command */
	int	_fan_cmd=-1;
	/** Enable fan setting action if true */
	boolean _setFan=false;
	/** Enable state reporting if true */
	boolean _getState=true;
	
	/** Perform the utility logic */
	public void processPort(Node node, String portName)
	throws RemoteException{
		
		try {
			// the node provides a reference to a Device
			device = node.getDevice(portName.getBytes());
		} catch (Exception e) {
			System.err.println("Exception looking up port " + portName + " : " + e);
			return;
		}
		
		// Ensure that the Device implements CO2SubsystemMonitorIF
		if (!(device instanceof CO2SubsystemMonitorIF)){
			System.err.println("Service on port ["+portName + "] is not a CO2SubsystemMonitor.  Exiting.");
			return;
		}
		
		// Cast service as CO2SubsystemMonitorIF
		_CO2Subsys = (CO2SubsystemMonitorIF)device;
		
		// do operations with via CO2SubsystemMonitorIF interface
		if(_setFan){
			try{
			// call remote method to turn fan on/off
			_CO2Subsys.setFanControl(_fan_cmd);
			}catch (Exception e) {
				_log4j.error("setFanControl failed:");
				e.printStackTrace();
			}
		}
		if(_getState){
			PacketParser.Field[] fields=null;
			String error=null;
			for(int i=0;i<5;i++){
				try{
					// call remote method to report state
					// (displays a parsed SensorDataPacket)
					fields= _CO2Subsys.getMonitorState();
					if(_log4j.isDebugEnabled()){
						_log4j.debug("retries:"+i);
					}
					break;
				}catch (Exception e) {
					// let it go
					fields=null;
					error=e.getMessage();
				}
			}
			if(fields==null){
				System.err.println("Could not read CO2MonitorState ["+error+"]");
				System.exit(-1);
			}
			for(int i=0;i<fields.length;i++){
				System.out.println("   "+fields[i].getName()+" "+fields[i].getValue()+" "+fields[i].getUnits());
			}
		}
	}
	
	/** Process application-specific option. */
	public void processCustomOption(String[] args, int index)
	throws InvalidOption{
				
		for(int i=0;i<args.length;i++){
			if(args[i].equals("-f")){
				// process fan command option
				String test=args[i+1];
				if(test.equalsIgnoreCase("ON")){
					// set command to ON
					_fan_cmd=CO2SubsystemMonitorIF.FAN_CONTROL_ON;
					// enable fan setting
					_setFan=true;
					i++;
				}else if(test.equalsIgnoreCase("OFF")){
					// set command to OFF
					_fan_cmd=CO2SubsystemMonitorIF.FAN_CONTROL_OFF;
					// enable fan setting
					_setFan=true;
					i++;
				}
			}else if(args[i].equals("-q")){
				// quiet option: disable state reporting
				_getState=false;
			}
		}
	}
	
	/** Print usage message. */
	public void printUsage(){
		// If the exec_name property is set on the java 
		// command line for this util:
		// -Dexec_name=<some name>
		// use that, otherwise use "cosm"
		String execName=System.getProperty("exec_name","cosm");
		System.err.println("");
		System.err.println(execName+" nodeURL port [-f <on|off>][-q]");
		System.err.println("");
		System.err.println(" Options:");
		System.err.println("  -f <on|off> : set cooling fan ON or OFF");
		System.err.println("  -q          : quiet - do not print current state");
		System.err.println("");
	}
	
	
	/** Main method (so utility may be invoked from command line) */
	public static void main(String[] args) {
		// Configure log4j
		PropertyConfigurator.configure(System.getProperties());
		BasicConfigurator.configure();
		
		// get utility instance
		CO2MonitorUtil util = new CO2MonitorUtil();
		// process command line arguments
		util.processArguments(args,2);
		// do utility action
		util.run();
	}
}
