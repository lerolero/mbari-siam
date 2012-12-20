// Copyright MBARI 2003
package org.mbari.siam.operations.utils;

import java.rmi.RemoteException;
import java.util.Date;
import java.text.DateFormat;
import java.rmi.Naming;
import java.io.IOException;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.PortOccupiedException;
import org.mbari.siam.distributed.DuplicateIdException;
import org.mbari.siam.distributed.PortNotFound;


/**
Try to restart service on specified port.
*/
public class PortRestarter extends PortUtility {

    public static void main(String[] args) {

	PortRestarter restarter = new PortRestarter();
	restarter.processArguments(args);
	restarter.run();
    }


    public void printUsage() {
	System.err.println("usage: nodeURL portName(s)");
    }


    /** No custom options for this application. */
    public void processCustomOption(String[] args, int index) 
	throws InvalidOption {

	// No custom options for this application... so throw 
	// invalid option exception if this method is called.
	throw new InvalidOption("unknown option");
    }


    public void processPort(Node node, String portName) 
	throws RemoteException {

	try {
	    System.out.println("Restarting service on port " + portName);
	    node.restartService(portName.getBytes());
	}
	catch (Exception e) {
	    System.err.println("Got exception of type " + 
			       e.getClass().getName());

	    System.err.println("Exception: " + e.getMessage());
	}

    }
}
