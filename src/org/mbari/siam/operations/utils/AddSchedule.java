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
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.core.Scheduler;


public class AddSchedule {

    public static void main(String[] args) {

	// Configure Log4J
	PropertyConfigurator.configure(System.getProperties());
	BasicConfigurator.configure();

	Node nodeService = null;

        //need to setSecurityManager 
        if ( System.getSecurityManager() == null )
            System.setSecurityManager(new SecurityManager());

	if (args.length < 2) {
	    System.err.println("Usage: nodeURL port scheduleName schedule [(o)verwrite]");
	    System.err.println("'schedule' can be specified as interval sec");
	    System.exit(1);
	}


	String nodeURL = NodeUtility.getNodeURL(args[0]);

	String port = PortUtility.getPortName(args[1]);

	String scheduleName = args[2];
	String schedule = args[3];
	boolean bOverwrite=false;


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

	if(args.length > 4){
	    if(args[4].toUpperCase().startsWith("O"))
		bOverwrite=true;
	}

	// Check for default schedule name, ignoring case
	if (scheduleName.equalsIgnoreCase(ScheduleSpecifier.DEFAULT_SCHEDULE_NAME)) {
	    // Make it the proper case for further processing
	    scheduleName = ScheduleSpecifier.DEFAULT_SCHEDULE_NAME;
	}
	// Check for simple schedule interval
	try {
	    int period = Integer.parseInt(schedule);
	    // User specified interval in seconds; convert to millisec
	    period *= 1000;
	    schedule = Integer.toString(period);
	}
	catch (NumberFormatException e) {
	}


	System.out.println("AddSchedule:"+nodeURL+","+port+","+scheduleName+","+schedule+","+bOverwrite);
	try {
	    System.out.println("Looking for node service at " + nodeURL);

	    nodeService = (Node )Naming.lookup(nodeURL);

	    System.out.println("Got proxy for node service \"" + 
			       new String(nodeService.getName()) + "\"");

	}
	catch (Exception e) {
	    System.err.println("Caught exception: " + e.getMessage());
	    System.err.println("Couldn't get service at \"" + nodeURL + "\"");
	    System.exit(1);
	}

	try {
	    byte[] stat=nodeService.addSchedule(port.getBytes(),scheduleName.getBytes(),schedule.getBytes(),bOverwrite);
	    System.out.println("\nAddSchedule "+port+":"+schedule+" returned "+new String(stat));
	}
	catch (RemoteException e) {
	    System.err.println("RemoteException: " + e.getMessage());
	}
	catch (Exception e) {
	    System.err.println("Got some exception: " + e.getMessage());
	    System.exit(1);
	}

	System.exit(0);
    }
}
