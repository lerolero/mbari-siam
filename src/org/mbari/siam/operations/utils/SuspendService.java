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
import org.mbari.siam.distributed.PortNotFound;

/**
   Suspend service on specified port.
 */
public class SuspendService extends PortUtility {

    public void processPort(Node node, String portName) 
	throws RemoteException {
	try {
	    node.suspendService(portName.getBytes());
	}
	catch (DeviceNotFound e) {
	    System.err.println("No device service on port " + portName);
	}
	catch (PortNotFound e) {
	    System.err.println("Port \"" + portName + "\" not found");
	}
    }


    public void printUsage() {
	System.out.println("usage: SuspendService nodeURL port(s)");
    }


    public static void main(String[] args) {
	SuspendService app = new SuspendService();
	app.processArguments(args);
	app.run();
    }
}
