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
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NoDataException;

public class ClientTest {

    public static void main(String[] args) {

	// Configure Log4J
	PropertyConfigurator.configure(System.getProperties());
	BasicConfigurator.configure();
	Node nodeService = null;
	Device devices[] = null;

        //need to setSecurityManager 
        if ( System.getSecurityManager() == null )
            System.setSecurityManager(new SecurityManager());
	
        if (args.length != 1) {
	    System.err.println("Usage: nodeURL");
	    return;
	}

	String nodeURL = NodeUtility.getNodeURL(args[0]);

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
	    System.out.println("Looking for node server stub at " + nodeURL);

	    nodeService = (Node )Naming.lookup(nodeURL);

	    System.out.println("Got proxy for node service \"" + 
			       new String(nodeService.getName()) + "\"");

	}
	catch (Exception e) {
	    System.err.println("Caught exception: " + e.getMessage());
	    System.err.println("Couldn't get service at \"" + nodeURL + "\"");
	    return;
	}

	try {
	    devices = nodeService.getDevices();
	    
	    System.out.println("Node has " + devices.length + " devices");
	    for (int i = 0; i < devices.length; i++) {

		System.out.println("\nDevice " + 
				   new String(devices[i].getName()) + 
				   ", port " + 
				   new String(devices[i].getCommPortName()));

		Instrument instrument = (Instrument )devices[i];

		System.out.println("Getting latest data...");
		try {
		    SensorDataPacket packet = instrument.acquireSample(false);
		    String timestamp = 
			DateFormat.getDateTimeInstance().format(new Date(packet.systemTime()));
		    System.out.println(timestamp + ":\n" + 
				       new String(packet.dataBuffer()) + "\n");
		}
		catch (NoDataException e) {
		    System.err.println("No data from instrument");
		}

	    }
	}
	catch (RemoteException e) {
	    System.err.println("RemoteException: " + e.getMessage());
	}
	catch (Exception e) {
	    System.err.println("Exception: " + e.getMessage());
	    return;
	}
    }
}
