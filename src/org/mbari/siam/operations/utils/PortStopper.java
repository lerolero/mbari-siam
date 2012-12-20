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
Try to start service on specified port.
*/
public class PortStopper extends PortUtility {

    public static void main(String[] args) {

	PortStopper stopper = new PortStopper();
	stopper.processArguments(args);
	stopper.run();
    }


    public void printUsage() {
	System.err.println("usage: StartServices nodeURL portName(s)");
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
	    System.out.println("Shutdown port " + portName);
	    byte[] msg = node.shutdownDeviceService(portName.getBytes());
	    System.out.println(new String(msg));
	}
	catch (DeviceNotFound e) {
	    System.err.println("Port " + portName + " not found");
	}
	catch (PortNotFound e) {
	    System.err.println("Can't find port \"" + 
			       portName + "\"");
	}
	catch (IOException e) {
	    System.err.println("IOException while scanning port " + 
			       portName + ": " + e.getMessage());
	}
    }
}
