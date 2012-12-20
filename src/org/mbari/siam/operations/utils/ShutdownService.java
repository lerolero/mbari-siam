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
import org.mbari.siam.distributed.DeviceNotFound;

/**
Terminate specified service.
*/
public class ShutdownService {

    public static void main(String[] args) {
	Node node = null;
	Device devices[] = null;

        //need to setSecurityManager 
        if ( System.getSecurityManager() == null )
            System.setSecurityManager(new SecurityManager());
        
        if (args.length < 2) {
	    System.err.println("usage: ShutdownService nodeURL commPortName");
	    System.exit(1);
	}

	String nodeURL = NodeUtility.getNodeURL(args[0]);

	try {

	    System.out.println("Looking for node server stub at " + nodeURL);

	    node = (Node )Naming.lookup(nodeURL);

	    System.out.println("Got proxy for node service \"" + 
			       new String(node.getName()) + "\"");

	    devices = node.getDevices();
	    
	    for (int i = 0; i < args.length-1; i++) {
		// Now shut down the specified service
		String portName = PortUtility.getPortName(args[i+1]);
		try {
		    node.shutdownDeviceService(portName.getBytes());
		}
		catch (DeviceNotFound e) {
		    System.err.println("Port " + portName + " not found");
		    System.exit(1);
		}
	    }

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
