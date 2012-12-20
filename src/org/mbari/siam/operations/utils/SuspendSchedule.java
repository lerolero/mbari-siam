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
import org.mbari.siam.core.Scheduler;


public class SuspendSchedule{

    public static void main(String[] args) {

	// Configure Log4J
	PropertyConfigurator.configure(System.getProperties());
	BasicConfigurator.configure();

	Node nodeService = null;

	// parse args
	String nodeURL = NodeUtility.getNodeURL(args[0]);
	String port = PortUtility.getPortName(args[1]);
	String schedule = args[2];

        //need to setSecurityManager 
        if ( System.getSecurityManager() == null )
            System.setSecurityManager(new SecurityManager());

	if (args.length < 3) {
	    System.err.println("Usage: nodeURL port schedule");
	    System.exit(1);
	}

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
	    byte[] stat=nodeService.suspendSchedule(port.getBytes(),schedule.getBytes());
	    System.out.println("\nSuspendSchedule "+port+":"+schedule+" returned "+new String(stat));
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
