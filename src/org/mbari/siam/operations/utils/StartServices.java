// Copyright MBARI 2003
package org.mbari.siam.operations.utils;

import java.rmi.RemoteException;
import java.util.Date;
import java.text.DateFormat;
import java.rmi.Naming;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NoDataException;

/**
   Try to start services on all ports.
 */
public class StartServices {

    public static void main(String[] args) {

	Node node = null;
	Device devices[] = null;

        //need to setSecurityManager 
        if ( System.getSecurityManager() == null )
            System.setSecurityManager(new SecurityManager());

	if (args.length != 1) {
	    System.err.println("usage: StartServices nodeURL");
	    System.exit(1);
	}

	String nodeURL = NodeUtility.getNodeURL(args[0]);

	try {
	    System.out.println("Looking for node server stub at " + nodeURL);

	    node = (Node )Naming.lookup(nodeURL);

	    System.out.println("Got proxy for node service \"" + 
			       new String(node.getName()) + "\"");

	    devices = node.getDevices();
	    
	    System.out.println("Node has " + devices.length + " devices");
	    // List the devices
	    for (int i = 0; i < devices.length; i++) {

		System.out.println("\nDevice " + 
				   new String(devices[i].getName()) + 
				   ", port " + 
				   new String(devices[i].getCommPortName()));

	    }

	    // Now scan all ports and (re-)start services....
	    node.scanPorts();

	}
	catch (RemoteException e) {
	    System.err.println("RemoteException: " + e.getMessage());
	    System.exit(1);
	}
	catch (Exception e) {
	    System.err.println("Exception: " + e.getMessage());
	    System.exit(1);
	}
	System.exit(0);
    }
}
