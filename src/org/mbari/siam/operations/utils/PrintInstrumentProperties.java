// Copyright MBARI 2003
package org.mbari.siam.operations.utils;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.Vector;
import java.text.DateFormat;
import java.rmi.Naming;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.PortNotFound;
import org.mbari.siam.distributed.DeviceNotFound;

/**
   Print properties of specified instrument.
 */
public class PrintInstrumentProperties extends PortUtility {

    /** Print properties associated with SIAM service on this port. */
    public void processPort(Node node, String portName)
	throws RemoteException {

	try {
	    Device device = node.getDevice(portName.getBytes());
	    if (device instanceof Instrument) {
		Instrument instrument = (Instrument )device;

		System.out.println("\nService properties for port " + 
				   portName + ":");

		Vector properties = instrument.getProperties();

		for (int j = 0; j < properties.size(); j++) {
		    byte[] property = (byte[] )properties.elementAt(j);
		    System.out.println(new String(property));
		}
		System.out.println("");
	    }
	    else {
		System.err.println("Device on port " + portName + 
				   " is not an Instrument");
	    }
	}
	catch (PortNotFound e) {
	    System.err.println("Port " + portName + " not found");
	}
	catch (DeviceNotFound e) {
	    System.err.println("Device not found on port " + 
			       portName);
	}
    }


    /** Print usage message. */
    public void printUsage() {
	System.out.println("usage: PrintInstrumentProperties nodeURL port(s)");
    }

    /** No custom options for this application. */
    public void processCustomOption(String[] args, int index) 
	throws InvalidOption {
	throw new InvalidOption("Invalid option: " + args[index]);
    }


    public static void main(String[] args) {

	PrintInstrumentProperties app = new PrintInstrumentProperties();
	app.processArguments(args);
	app.run();
    }
}
