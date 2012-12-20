// Copyright MBARI 2004
package org.mbari.siam.operations.utils;

import java.rmi.RemoteException;
import java.util.Date;
import java.text.DateFormat;
import java.rmi.Naming;
import java.io.EOFException;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.PortNotFound;

import org.mbari.siam.distributed.devices.Power;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;

/** Shut down the specified SIAM node and alternatively
    halt the controller after exiting. Also includes
    option to enter safe mode, notify parent, etc.
 */
public class KillBIN extends NodeUtility 
{

    // CVS revision 
    private static String _versionID = "$Revision: 1.1 $";

     String portName="";
    public static boolean _enableSafemodeDflt=false;
    public static final boolean _enableHaltDflt=true;
    public static final boolean _enableBackupsDflt=false;
    public static final boolean _enableNotifyDflt=true;
    public static final boolean _enableRecursionDflt=false;
    public static final int _quitDelaySecDflt=0;
    public static final int _haltDelaySecDflt=0;

    private boolean _enableSafemode=_enableSafemodeDflt;
    private boolean _enableHalt=_enableHaltDflt;
    private boolean _enableBackups=_enableBackupsDflt;
    private boolean _enableNotify=_enableNotifyDflt;
    private boolean _enableRecursion=_enableRecursionDflt;
    private int _quitDelaySec=_quitDelaySecDflt;
    private int _haltDelaySec=_haltDelaySecDflt;

    public void printUsage() 
    {
	System.err.println("\nusage: killBIN " + 
			   "nodeURL [-bkqrs][-D quitDelaySec][-H haltDelaySec]\n\n"+
			   "nodeURL:   node name or IP address (e.g., surface, node4, 134.89.36.24, etc.)\n"+
			   "b: enable backup batteries and holdup capacitors before quitting SIAM   ["+_enableBackupsDflt+"]\n"+
			   "k: keep controller on after exiting SIAM  (do not halt controller)      ["+!_enableHaltDflt+"]\n"+
			   "q: quiet mode; do not notify parent node when shutting down             ["+!_enableNotifyDflt+"]\n"+
			   "r: enable recursive shutdown (not implemented)                          ["+_enableRecursionDflt+"]\n"+
			   "s: enable safe mode before quitting SIAM                                ["+_enableSafemodeDflt+"]\n"+
			   "D: delay quitDelaySec before exiting SIAM                               ["+_quitDelaySecDflt+"]\n"+
			   "H: delay haltDelaySec before halting controller                         ["+_haltDelaySecDflt+"]\n");
	System.err.println("\n");
    }

    public void showConfig(){
        _log4j.debug("\n\nkillBIN configuration:"+
		      "\n enableBackups:"+_enableBackups+
		      "\n enableHalt:"+_enableHalt+
		      "\n enableNotify:"+_enableNotify+
		      "\n enableRecursion:"+_enableRecursion+
		      "\n enableSafemode:"+_enableSafemode+
		      "\n quitDelaySec:"+_quitDelaySec+
		      "\n haltDelaySec:"+_haltDelaySec+
		      "\n\n");
    }

    public void processNode(Node node) throws Exception 
    {

	try{
	    _node.exitApplication(_enableSafemode,
				  _enableHalt,
				  _enableBackups,
				  _enableNotify,
				  _enableRecursion,
				  _quitDelaySec,
				  _haltDelaySec,
				  "shutdown initiated from KillBIN");
	} 
	catch ( EOFException e ){
	    // don't respond, this is expected as a 
	    // result of exiting the SIAM app from within
	    // an RMI call
	}
	catch ( Exception e ){
	    System.err.println("KillBIN: Exception: " + e);
	    e.printStackTrace();
	    exitError("KillBIN error - processNode failed",1);
	} 

	System.exit(0);
    
    }

    public void exitError(String msg,int errorCode){
	System.err.println("\n");
	System.out.println(msg);
	printUsage();
	System.exit(errorCode);
    }


    /** No custom options for this application. */
    public void processCustomOption(String[] args, int index) 
        throws InvalidOption 
    {
	
	if(args.length<1)
	    exitError("Missing arguments",1);

	if(index>(args.length-1))
	    exitError("Index > args.length",1);

	_log4j.debug("arg["+index+"]="+args[index]);
	String a=args[index].trim();

	if(a.equalsIgnoreCase("-help")||a.equalsIgnoreCase("--help")){
	    exitError("",0);
	}
	
	if(a.startsWith("-")){
	    if(a.indexOf("b")>0){
		_enableBackups=true;
	    }
	    if(a.indexOf("k")>0){
		_enableHalt=false;
	    }
	    if(a.indexOf("q")>0){
		_enableNotify=false;
	    }
	    if(a.indexOf("r")>0){
		_enableRecursion=true;
	    }
	    if(a.indexOf("s")>0){
		_enableSafemode=true;
	    }
	}

	if(a.endsWith("D")||a.equals("-D")){
	    try{
		if((args.length-1)<(index+1)){
		    exitError("Must specify quitDelaySec >= 0",1);		    
		}
		_quitDelaySec=Integer.parseInt(args[index+1]);

		if(_quitDelaySec<0)
		    exitError("Must specify quitDelaySec >= 0",1);		    
		return;
	    }catch(Exception e){
		exitError("Invalid quitDelaySec ("+args[index+1]+")",1);
	    }
	}
	if(a.endsWith("H")||a.equals("-H")){
	    try{
		if((args.length-1)<(index+1)){
		    exitError("Must specify haltDelaySec >= 0",1);		    
		}
		_haltDelaySec=Integer.parseInt(args[index+1]);
		if(_haltDelaySec<0)
		    exitError("Must specify haltDelaySec >= 0",1);		    
		return;
	    }catch(Exception ex){
		exitError("Invalid haltDelaySec ("+args[index+1]+")",1);
	    }
	}

	// No custom options for this application... so throw 
	// invalid option exception if this method is called.
	//throw new InvalidOption("unknown option");
    }

    public static void main(String[] args) 
    {
 
	System.out.println("args.length="+args.length);
	KillBIN t = new KillBIN();
	t.processArguments(args,1);
	t.showConfig();
	t.run();
    }
}
