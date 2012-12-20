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
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.MetadataPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.PortNotFound;
import org.mbari.siam.distributed.DeviceNotFound;

/**
   Run the service after specified delay.
   For relative schedules, adjust the schedule to
   begin at the specified time.
   For absolute schedules, return to the normal schedule.
 */
public class Sync {

    public static void printUsage(){
	    System.err.println("\n usage: sync nodeURL port schedule <delay ms (0)>");
    }

    public static void main(String[] args) {

	// Configure Log4J
	PropertyConfigurator.configure(System.getProperties());
	BasicConfigurator.configure();

	Node node = null;
	Device devices[] = null;

        //need to setSecurityManager 
        if ( System.getSecurityManager() == null )
            System.setSecurityManager(new SecurityManager());
        
        if (args.length < 3) {
	    printUsage();
	    System.exit(1);
	}

	String nodeURL = NodeUtility.getNodeURL(args[0]);
	String port = PortUtility.getPortName(args[1]);
	String schedule = args[2];

	// Create socket factory; overcomes problems with RMI 'hostname'
	// property.
	try {
	    String host = NodeUtility.getHostName(nodeURL);
	    RMISocketFactory.setSocketFactory(new SiamSocketFactory(host));
	}
	catch (MalformedURLException e) {
	    System.err.println("Malformed URL \"" + nodeURL + "\": " + 
			       e.getMessage());
	}
	catch (IOException e) {
	    System.err.println("RMISocketFactory.setSocketFactory() failed");
	    System.err.println(e);
	}


	long delayMillis=0L;

	if(args.length>3){
	    if(args[3].toLowerCase().indexOf("h") >= 0){
		printUsage();
		System.exit(1);
	    }
	    try{
		delayMillis = Long.parseLong(args[3]);
	    }catch(NumberFormatException e){
		printUsage();
		System.exit(1);
	    }
	}
	    
	try {

	    System.out.println("Looking for node server stub at " + nodeURL);

	    node = (Node )Naming.lookup(nodeURL);

	    System.out.println("Got proxy for node service \"" + 
			       new String(node.getName()) + "\"");

	    System.out.println("Synchronizing instrument service...");
	    byte retval[] = node.syncSchedule(port.getBytes(),schedule.getBytes(),delayMillis);
	    System.out.println("Synchronization complete; returned\n"+new String(retval));

	}
	catch (RemoteException e) {
	    System.err.println("RemoteException: " + e);
	    e.printStackTrace();
	    System.exit(1);
	}
	catch (Exception e) {
	    System.err.println("Exception: " + e.getMessage());
	    System.exit(1);
	}
	System.exit(0);

    }
}
