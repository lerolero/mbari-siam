// Copyright MBARI 2003
package org.mbari.siam.operations.utils;

import java.rmi.RemoteException;
import java.util.Date;
import java.text.DateFormat;
import java.rmi.Naming;
import java.rmi.server.RMISocketFactory;
import java.io.IOException;
import java.net.MalformedURLException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;
import org.mbari.siam.utils.SiamSocketFactory;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Port;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.PortNotFound;
import org.mbari.siam.core.Scheduler;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;

/**
   Show node's schedules.
 */
public class ShowSchedule {

    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(ShowSchedule.class);

    public void header(){
	System.out.println(" ");
    }

    public static void main(String[] args) {

	// Configure Log4J
	PropertyConfigurator.configure(System.getProperties());
	BasicConfigurator.configure();

	ShowSchedule showSchedule = new ShowSchedule();

	Node nodeService = null;
	byte[] schedule = null;

        //need to setSecurityManager 
        if ( System.getSecurityManager() == null )
            System.setSecurityManager(new SecurityManager());

	if (args.length < 1) {
	    System.err.println();
	    System.err.println("Usage: nodeURL [[port [lookAheadSec]]...");
	    System.err.println();
	    System.err.println(" port:         Instrument port (must use full device name, e.g., /dev/ttySX0)");
	    System.err.println("               If no port is specified, all schedules are shown.");
	    System.err.println();
	    System.err.println(" lookaheadSec: Look ahead no more than lookaheadSec for next scheduled event.");
	    System.err.println("               Applies to absolute schedules. Default value is "+Scheduler.MAX_LOOKAHEAD_SEC);
	    System.err.println();
	    System.exit(1);
	}

	String nodeURL = NodeUtility.getNodeURL(args[0]);

	// Create socket factory; overcomes problems with RMI 'hostname'
	// property.
	try {
	    String host = NodeUtility.getHostName(nodeURL);
	    RMISocketFactory.setSocketFactory(new SiamSocketFactory(host));
	}
	catch (MalformedURLException e) {
	    _log4j.error("Malformed URL \"" + nodeURL + "\": " + 
			       e.getMessage());
	}
	catch (IOException e) {
	    _log4j.error("RMISocketFactory.setSocketFactory() failed");
	    _log4j.error(e);
	}

	long lookAheadSeconds=Scheduler.MAX_LOOKAHEAD_SEC;
	try{
	    if(args.length > 1)
		lookAheadSeconds = Long.parseLong(args[1]);
	}catch(NumberFormatException e){
	    _log4j.error("ShowSchedule: lookAhead value undefined or invalid; using default ("+Scheduler.MAX_LOOKAHEAD_SEC+")");
	    lookAheadSeconds=Scheduler.MAX_LOOKAHEAD_SEC;
	}

	try {
	    _log4j.debug("Looking for node service at " + nodeURL);

	    nodeService = (Node )Naming.lookup(nodeURL);

	    _log4j.debug("Got proxy for node service \"" + 
			       new String(nodeService.getName()) + "\"");

	}
	catch (Exception e) {
	    _log4j.error("Caught exception: " + e.getMessage());
	    _log4j.error("Couldn't get service at \"" + nodeURL + "\"");
	    System.exit(1);
	}

	try {
	    String s1 = null;

	    System.out.println("");
	    if(args.length==1){
		schedule = nodeService.getSchedule(lookAheadSeconds);
		showSchedule.header();
		System.out.println(new String(schedule));
	    }else
	    for(int i=1;i<args.length;i++){
		s1=args[i].trim();

		// Added following test so we don't have to look for "tty"
		// 23may2008, rah
		boolean isDevice = false;
		try {
		    nodeService.getDevice(s1.getBytes());
		    isDevice = true;
		} catch (DeviceNotFound e) {
		} catch (PortNotFound e) {
		}

		//_log4j.debug("arg["+i+"]="+args[i]);
		lookAheadSeconds=Scheduler.MAX_LOOKAHEAD_SEC;

		// there IS an arg after this one
		if( (i+1) < args.length ){
		    //_log4j.debug("arg["+(i+1)+"]="+args[i+1]);

//		    if(s1.indexOf("tty")>0){
		    if (isDevice) {
			// This IS a DEVICE, see if next is number
			try{
			    lookAheadSeconds=Long.parseLong(args[i+1]);
			    i++;
			}catch(NumberFormatException e){
			    lookAheadSeconds=Scheduler.MAX_LOOKAHEAD_SEC;
			}
			schedule = nodeService.getSchedule(s1.getBytes(),lookAheadSeconds);
			showSchedule.header();
			System.out.println(new String(schedule));
		    }else{
			// This IS NOT a DEVICE, see if this is a number; get all if so
			try{
			    lookAheadSeconds=Long.parseLong(args[i]);
			    schedule = nodeService.getSchedule(lookAheadSeconds);
			    showSchedule.header();
			    System.out.println(new String(schedule));
			    break;
			}catch(NumberFormatException e){
			    //do nothing, this is neither number nor device
			}
		    }
		}else{
		    // there IS NOT an arg after this one
//		    if(s1.indexOf("tty")>0){
		    if (isDevice) {
			// This IS a DEVICE
			schedule = nodeService.getSchedule(s1.getBytes(),lookAheadSeconds);
			showSchedule.header();
			System.out.println(new String(schedule));
		    }else{
			//This IS NOT a DEVICE,see if this is a number get all if so
			try{
			    lookAheadSeconds=Long.parseLong(args[i]);
			    schedule = nodeService.getSchedule(lookAheadSeconds);
			    showSchedule.header();
			    System.out.println(new String(schedule));
			    break;
			}catch(NumberFormatException e){}
		    }
		}
	    }
	}
	catch (RemoteException e) {
	    _log4j.error("RemoteException: " + e.getMessage());
	}
	catch (Exception e) {
	    _log4j.error("Got some exception: " + e.getMessage());
	    e.printStackTrace();
	    System.exit(1);
	}

	System.exit(0);
    }
}
