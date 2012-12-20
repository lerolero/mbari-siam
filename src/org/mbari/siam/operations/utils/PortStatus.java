// Copyright MBARI 2003
package org.mbari.siam.operations.utils;

import java.rmi.RemoteException;
import java.util.Date;
import java.text.DateFormat;
import java.rmi.Naming;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.MOOSNode;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.DeviceMessagePacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.utils.PrintUtils;
import org.mbari.siam.moos.distributed.dpa.DpaPortStatus;

/**
  Return status of specified instrument.port.
 */
public class PortStatus extends PortUtility {

    public static void main(String[] args) {
	PortStatus status = new PortStatus();
	status.processArguments(args);
	status.run();
    }

    /** Print usage message */
    public void printUsage() {
	System.err.println("usage: PortStatus nodeURL port(s)");
    }

    /** No custom options for this application. */
    public void processCustomOption(String[] args, int index) 
	throws InvalidOption {

	// No custom options for this application... so throw 
	// invalid option exception if this method is called.
	throw new InvalidOption("unknown option");
    }

    /** Get status of specified port. */
    public void processPort(Node node, String portName) 
	throws RemoteException {

	try {
	    MOOSNode moosNode = (MOOSNode )node;

	    DpaPortStatus status = 
		moosNode.getDpaPortStatus(portName.getBytes());

	    System.out.println(status.toString());
	}
	catch (DeviceNotFound e) {
	    System.err.println("Device not found on port " + 
			       portName);
	}
	catch (NotSupportedException e) {
	    System.err.println(e);
	}
    }
}
