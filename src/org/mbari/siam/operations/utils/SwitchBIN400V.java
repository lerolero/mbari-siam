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

/**
   Switch the 400V power to the Benthic Instrument Nodes (BINs)
  @author Kent Headley
 */
public class SwitchBIN400V extends NodeUtility
{

    int _localSwitch = Power.POWER_SWITCH; 
    int _powerSwitch0 = Power.BIN_SWITCH;
    int _powerSwitch1 = Power.BIN_SWITCH;
    int _powerSwitch2 = Power.BIN_SWITCH;
    int _powerSwitch3 = Power.BIN_SWITCH;

    String commandString="";
    String portName = "";

    public  void exitError(String msg,int errorCode){
	System.err.println("\n");
	System.err.println(msg);
	printUsage();
	System.exit(errorCode);
    }

    public void printUsage(){
	System.err.println("\nusage: switchBIN400V " + 
			   "nodeURL portName [ON|OFF] [ <binSwitch>ON | <binSwitch>OFF | ALLON | ALLOFF ]\n\n"+
			   "  nodeURL: node name or IP address                  (e.g., surface, node4, 134.89.36.24, etc.)\n"+
			   " portName: power system service port name or number (e.g., 5, /dev/ttySX0, etc.)\n"+
			   "   ON|OFF: switch local single channel 400V         (same as switchHiVoltage)\n"+
			   "binSwitch: BIN 400V switch number                   (0|1|2|3)\n\n");
    }

    public void showConfig() 
    {
	_log4j.debug("\nswitchBIN400V configuration:" );
	_log4j.debug("_localSwitch="+Power.SWITCH_STATES[_localSwitch]);
	_log4j.debug("_powerSwitch0="+Power.SWITCH_STATES[_powerSwitch0]);
	_log4j.debug("_powerSwitch1="+Power.SWITCH_STATES[_powerSwitch1]);
	_log4j.debug("_powerSwitch2="+Power.SWITCH_STATES[_powerSwitch2]);
	_log4j.debug("_powerSwitch3="+Power.SWITCH_STATES[_powerSwitch3]);
	_log4j.debug("");
    }

    public static boolean validatePort(String port){
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

    /** No custom options for this application. */
    public void processCustomOption(String[] args, int index) 
        throws InvalidOption 
    {
	_log4j.debug("args["+index+"]="+args[index]);
        /* parse command line args 
	   This backwards compatible with SwitchHiVoltage, for P2/power system
	   that supports only one High Voltage channel
	*/
	if(index==1){
	    _log4j.debug("parsing index "+index+" (portName):"+args[index]);
	    portName=PortUtility.getPortName(args[index]);
	    if(!validatePort(portName)){
		exitError("Invalid service port "+portName+"; portName=0, /dev/ttySX9, COM1, etc. ",1);
	    }
	    return;
	}

        if ( args.length < 3)
        {
            _localSwitch = Power.POWER_SWITCH_QUERY;
            _powerSwitch0 = Power.BIN_SWITCH_QUERY;
            _powerSwitch1 = Power.BIN_SWITCH_QUERY;
            _powerSwitch2 = Power.BIN_SWITCH_QUERY;
            _powerSwitch3 = Power.BIN_SWITCH_QUERY;
	    return;
        }


        if ( args[index].compareToIgnoreCase("OFF") == 0 )
        {
            _localSwitch = Power.POWER_SWITCH_OFF;
	    return;
        
        }
        if ( args[index].compareToIgnoreCase("ON") == 0 )
        {
            _localSwitch = Power.POWER_SWITCH_ON;
	    return;
        }

        if ( args[index].equalsIgnoreCase("ALLON") )
        {
            _log4j.debug("Setting BIN ALLON");
            _powerSwitch0 = Power.BIN_SWITCH_ON;
            _powerSwitch1 = Power.BIN_SWITCH_ON;
            _powerSwitch2 = Power.BIN_SWITCH_ON;
            _powerSwitch3 = Power.BIN_SWITCH_ON;
	    return;
        }
        if ( args[index].equalsIgnoreCase("ALLOFF") )
        {
            _log4j.debug("Setting BIN ALLOFF");
            _powerSwitch0 = Power.BIN_SWITCH_OFF;
            _powerSwitch1 = Power.BIN_SWITCH_OFF;
            _powerSwitch2 = Power.BIN_SWITCH_OFF;
            _powerSwitch3 = Power.BIN_SWITCH_OFF;
	    return;
        }

	/* this is the new stuff for BINs */
	/* Validate command line args and build command string */
	String arg=args[index].trim().toUpperCase();

	String sChannel="";
	int iChannel=-1;
	String sState="";
	if(arg.indexOf("ON")>0 || arg.indexOf("OFF")>0){
	    sState=arg.substring(arg.indexOf("O"));
	    sChannel=arg.substring(0,arg.indexOf("O"));

	    if(!sChannel.equals("ALL")){
		iChannel=Integer.parseInt(sChannel);
		if(iChannel<0 || iChannel>3){
		    exitError("Error: Invalid channel number: "+iChannel+"; must be 0<=i<=3\n",1);
		}
	    }
	    if(!sState.equals("ON") && !sState.equals("OFF")){
		exitError("Error: Invalid command: "+sState+"; valid commands are ON|OFF\n",1);
	    }
	}else{
	    exitError(("Error:Invalid command specification: "+arg+"\n"),1);
	}

	//_log4j.debug("iChannel="+iChannel+" sState="+sState);

	switch(iChannel){
	case 0:
	    if(sState.equals("ON")){
		_powerSwitch0=Power.BIN_SWITCH_ON;
	    }
	    if(sState.equals("OFF")){
		_powerSwitch0=Power.BIN_SWITCH_OFF;
	    }
	    break;
	case 1:
	    if(sState.equals("ON")){
		_powerSwitch1=Power.BIN_SWITCH_ON;
	    }
	    if(sState.equals("OFF")){
		_powerSwitch1=Power.BIN_SWITCH_OFF;
	    }
	    break;
	case 2:
	    if(sState.equals("ON")){
		_powerSwitch2=Power.BIN_SWITCH_ON;
	    }
	    if(sState.equals("OFF")){
		_powerSwitch2=Power.BIN_SWITCH_OFF;
	    }
	    break;
	case 3:
	    if(sState.equals("ON")){
		_powerSwitch3=Power.BIN_SWITCH_ON;
	    }
	    if(sState.equals("OFF")){
		_powerSwitch3=Power.BIN_SWITCH_OFF;
	    }
	    break;
	default:
	    exitError(("Error:Invalid BIN switch: "+iChannel+"\n"),1);
	}
    }

    public void processNode(Node node) throws Exception {
	String switchStatus="";
	try{
	    Device device = node.getDevice(portName.getBytes());
	    if ( device instanceof Power ){
		Power power = (Power)device;

		// Process local switch operations
		if ( (_localSwitch == Power.POWER_SWITCH_QUERY) || (_localSwitch == Power.POWER_SWITCH) ){
		    _log4j.debug("requesting local switch status");
		    if ( power.isHighVoltageEnabled() )
			switchStatus="localSwitch: HV Switch: ON";
		    else
			switchStatus="localSwitch: HV Switch: OFF";
		    _log4j.debug("switch status:"+switchStatus);
		}
		else if ( _localSwitch == Power.POWER_SWITCH_ON ){
		    _log4j.debug("requesting local switch ON");
		    power.enableHiVoltage();
		    _log4j.debug("ON request complete");

		}
		else if ( _localSwitch == Power.POWER_SWITCH_OFF ){
		    _log4j.debug("requesting local switch OFF");
		    power.disableHiVoltage();
		    _log4j.debug("OFF request complete");
		}

		// Process BIN switch operations
		int[] switchStates={_powerSwitch0,_powerSwitch1,_powerSwitch2,_powerSwitch3};

		if ( _powerSwitch0 == Power.BIN_SWITCH_QUERY &&
		     _powerSwitch1 == Power.BIN_SWITCH_QUERY && 
		     _powerSwitch2 == Power.BIN_SWITCH_QUERY &&
		     _powerSwitch3 == Power.BIN_SWITCH_QUERY ){

		    _log4j.debug("requesting BIN switch status");
		    byte[] rtn=power.queryBIN400V();
		    if(rtn!=null)
			switchStatus+="\n"+new String(rtn);
		    _log4j.debug("switch status:"+switchStatus);
		}else
		if ( _powerSwitch0 != Power.BIN_SWITCH || 
		     _powerSwitch1 != Power.BIN_SWITCH || 
		     _powerSwitch2 != Power.BIN_SWITCH ||
		     _powerSwitch3 != Power.BIN_SWITCH ){

		    _log4j.debug("requesting BIN switch operation");
		    byte[] rtn=power.switchBIN400V(switchStates);
		    if(rtn!=null)
			switchStatus+="\nBIN switch:"+new String(rtn);
		}else
		if ( _powerSwitch0 == Power.BIN_SWITCH &&
		     _powerSwitch1 == Power.BIN_SWITCH && 
		     _powerSwitch2 == Power.BIN_SWITCH &&
		     _powerSwitch3 == Power.BIN_SWITCH ){

		    _log4j.debug("requesting BIN switch status");
		    byte[] rtn=power.queryBIN400V();
		    if(rtn!=null)
			switchStatus+="\nBIN switch:"+new String(rtn);
		}
		else{
		    //should never get here
		    _log4j.error("SwitchBIN400V:Invalid switch request");
		}
	    } 
	    else{
		_log4j.error("SwitchBIN400V:Device on port " + portName + 
				   " does not implement the Power interface");
	    }
	} 
	catch ( PortNotFound e ){
	    _log4j.error("SwitchBIN400V:Port (" + portName + ") not found");
	} 
	catch ( DeviceNotFound e ){
                _log4j.error("SwitchBIN400V:Device not found on port " + portName);
	} 
        catch ( RemoteException e ){
            _log4j.error("SwitchBIN400V:RemoteException: " + e.getMessage());
            System.exit(1);
        } 
        catch ( Exception e ){
            _log4j.error("SwitchBIN400V:Exception: " + e.getMessage());
            System.exit(1);
        }

	// Display status returned
        if(switchStatus!=null)
	    System.out.println("operation returned:\n"+switchStatus);
        System.exit(0);
    }

    public static void main(String[] args) {
	SwitchBIN400V t = new SwitchBIN400V();
	t.processArguments(args,2);
	t.showConfig();
	t.run();
    }
}
