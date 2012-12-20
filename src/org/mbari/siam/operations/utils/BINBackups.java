// Copyright MBARI 2004
package org.mbari.siam.operations.utils;

import java.util.Date;
import java.util.Vector;
import java.text.DateFormat;
import java.lang.Integer;
import java.rmi.RemoteException;
import java.rmi.Naming;
import java.rmi.server.RMISocketFactory;
import java.io.IOException;
import java.net.MalformedURLException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;
import org.mbari.siam.utils.SiamSocketFactory;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.PortNotFound;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.utils.PrintUtils;
import org.mbari.siam.distributed.devices.Power;
import org.mbari.siam.devices.serialadc.PowerCan;

/**
   Disable power system battery backups and/or hold-up capacitors on
   the Benthic Instrument Nodes (BINs)
  @author Kent Headley, Mike Risi
 */
public class BINBackups extends NodeUtility{

    String portName="";
    int _enableBatteries=Power.BIN_BACKUP;
    int _enableCapacitors=Power.BIN_BACKUP;

    public void showConfig(){
        _log4j.debug("\n\nBINBackups configuration:"+
			   "\n enableBatteries:"+Power.BACKUP_STATES[_enableBatteries]+
			   "\n enableCapacitors:"+Power.BACKUP_STATES[_enableCapacitors]+
			   "\n\n");
    }

    public void exitError(String msg,int errorCode){
	System.err.println("\n");
	System.out.println(msg);
	printUsage();
	System.exit(errorCode);
    }

    public void printUsage(){
	System.err.println("\nusage: binBackups " + 
			   "nodeURL portName [ B<E|D> C<E|D> ALLEN ALLDI]\n\n"+
			   "nodeURL:   node name or IP address                  (e.g., surface, node4, 134.89.36.24, etc.)\n"+
			   "portName:  power system service port name or number (e.g., 5, /dev/ttySX0, etc.)\n"+
			   "B:         Battery Backup    E: Enable D: Disable\n"+
			   "C:         Holdup Capacitors E: Enable D: Disable\n"+
			   "ALLEN:     Enable both\n"+
			   "ALLDI:     Disable both\n");
	System.err.println("\n");
    }

    public boolean validateArg(String arg){
	arg=arg.trim();

	if(arg.equalsIgnoreCase("BE")){
	    _enableBatteries=Power.BIN_BACKUP_EN;
	    return true;
	}
	if(arg.equalsIgnoreCase("BD")){
	    _enableBatteries=Power.BIN_BACKUP_DI;
	    return true;
	}
	if(arg.equalsIgnoreCase("CE")){
	    _enableCapacitors=Power.BIN_BACKUP_EN;
	    return true;
	}
	if(arg.equalsIgnoreCase("CD")){
	    _enableCapacitors=Power.BIN_BACKUP_DI;
	    return true;
	}

	if(arg.equalsIgnoreCase("ALLEN")){
	    _enableBatteries=Power.BIN_BACKUP_ALLEN;
	    _enableCapacitors=Power.BIN_BACKUP_ALLEN;
	    return true;
	}
	if(arg.equalsIgnoreCase("ALLDI")){
	    _enableBatteries=Power.BIN_BACKUP_ALLDI;
	    _enableCapacitors=Power.BIN_BACKUP_ALLDI;
	    return true;
	}

	exitError("Error: invalid argument ("+arg+")",1);

	// should never get here
	return false;

    }

    public  boolean validatePort(String port){
	try{
	    Integer.parseInt(port);
	    return true;
	}catch(NumberFormatException e){
	    // nope, not a number
	}
	if(port.indexOf("ttySX")>=0)
	    return true;
	if(port.indexOf("COM")>=0 || port.indexOf("com")>=0)
	    return true;
	return false;
    }

    /** Do application-specific processing of node. */
    public void processNode(Node node) throws Exception 
    {

	_log4j.debug("\nPerforming BIN backup operation(s):\n"+
		      "enableBatteries="+Power.BACKUP_STATES[_enableBatteries]+"\n"+
		      "enableCapacitors="+Power.BACKUP_STATES[_enableCapacitors]+"\n");
        
	String status=null;
 
	try{
	    Device device = _node.getDevice(portName.getBytes());
	    if ( device instanceof Power ){
		Power power = (Power)device;
		
		byte[] rtn=power.binBackups(_enableBatteries,_enableCapacitors);
		if(rtn!=null)
		    status=new String(rtn);
	    }else{
		System.err.println("Device on port " + portName + 
				   " is not an Power");
	    }
	} 
	catch ( PortNotFound e ){
	    System.err.println("Port " + portName + " not found");
	} 
	catch ( DeviceNotFound e ){
	    System.err.println("Device not found on port " + portName);
	} 

	System.out.println("operation returned: "+status);

	System.exit(0);
    
    }

    /** Process custom options for this application. */
    public void processCustomOption(String[] args, int index) 
        throws InvalidOption 
    {	
	if(args.length<2)
	    exitError("Missing arguments",0);

	_log4j.debug("arg["+index+"]="+args[index]);
	String a=args[index].trim().toUpperCase();
	if(index==1){
	    _log4j.debug("parsing index "+index+" (portName):"+args[index]);
	    portName=PortUtility.getPortName(args[index]);
	    if(!validatePort(portName)){
		exitError("Invalid service port "+portName+"; portName=0, /dev/ttySX9, COM1, etc. ",1);
	    }
	    return;
	}else{
	    if(validateArg(a)){
		return;
	    }
	}

	// No custom options for this application... so throw 
	// invalid option exception if this method is called.
	throw new InvalidOption("unknown option (arg["+index+"]="+args[index]+")");
    }
    
    public static void main(String[] args) 
    {

	BINBackups b=new BINBackups();
	b.processArguments(args,2);
b.showConfig();
	b.run();
    }
}
