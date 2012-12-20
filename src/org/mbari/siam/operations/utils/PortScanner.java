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
public class PortScanner extends PortUtility {

    String _serviceSource = null;

    public static void main(String[] args) {


	PortScanner scanner = new PortScanner();
	scanner.multiPortsAllowed(false);

	scanner.processArguments(args);
	scanner.run();
    }


    public void printUsage() {
	System.err.println("usage: StartServices nodeURL portName(s)");
    }


    /** No custom options for this application. */
    public void processCustomOption(String[] args, int index) 
	throws InvalidOption {

	_serviceSource = args[index];

	if (_serviceSource.equalsIgnoreCase("puck")) {
	    _serviceSource = "PUCK";
	}
	else if (!_serviceSource.endsWith(".jar")) {
	    _serviceSource += ".jar";
	}
    }


    public void processPort(Node node, String portName) 
	throws RemoteException {

	try {
	    System.out.println("Scan port " + portName);
	    byte[] source = null;
	    if (_serviceSource != null) {
		source = _serviceSource.getBytes();
	    }
	    node.scanPort(portName.getBytes(), source);
	}
	catch (DeviceNotFound e) {
	    System.err.println("Port " + portName + " not found");
	}
	catch (PortOccupiedException e) {
	    System.err.println("Service already running on port " + 
			       portName);
	}
	catch (PortNotFound e) {
	    System.err.println("Can't find port \"" + 
			       portName + "\"");
	}
	catch (DuplicateIdException e) {
	    System.err.println("Got duplicate instrument ID from port " + 
			       portName + ": " + e.getMessage());
	}
	catch (IOException e) {
	    System.err.println("IOException while scanning port " + 
			       portName + ": " + e.getMessage());
	}
    }
}
