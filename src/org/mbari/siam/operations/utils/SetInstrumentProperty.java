// Copyright MBARI 2003
package org.mbari.siam.operations.utils;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.Properties;
import java.util.Enumeration;
import java.text.DateFormat;
import java.rmi.Naming;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.PortNotFound;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.InvalidPropertyException;

/**
   Set specified property of specified instrument service
 */
public class SetInstrumentProperty extends PortUtility {

    Properties _settings = new Properties();

    /** Print properties associated with SIAM service on this port. */
    public void processPort(Node node, String portName)
	throws RemoteException {

	if (_settings.size() == 0) {
	    System.err.println("No property settings specified");
	    return;
	}

	try {
	    // Convert properties to byte array

	    ByteArrayOutputStream output = 
		new ByteArrayOutputStream();
	    try {
		_settings.store(output, "");
	    }
	    catch (IOException e) {
		System.err.println("Caught exception while printing properties");
		return;
	    }

	    Device device = node.getDevice(portName.getBytes());
	    if (device instanceof Instrument) {
		Instrument instrument = (Instrument )device;

		try {
		    // Try to set the properties (note that second argument
		    // is unused).
		    instrument.setProperty(output.toByteArray(), 
					   "".getBytes());
		}
		catch (InvalidPropertyException e) {
		    System.err.println(e);
		}
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
	System.out.println("usage: " + 
			   "SetInstrumentProperty nodeURL port " + 
			   "key=value <key=value> <key=value>...");
    }


    /** Process key=value arguments. */
    public void processCustomOption(String[] args, int index) 
	throws InvalidOption {

	ByteArrayInputStream inputStream = 
	    new ByteArrayInputStream(args[index].getBytes());

	System.out.println("args[" + index + "]=" + args[index]);

	try {
	    _settings.load(inputStream);
	}
	catch (IOException e) {
	    System.err.println("Error while parsing property " + args[index]);
	}
    }


    public static void main(String[] args) {

	System.out.println("main() - args.length=" + args.length);
	for (int i = 0; i < args.length; i++) {
	    System.out.println("args[" + i + "]=" + args[i]);
	}

	SetInstrumentProperty app = new SetInstrumentProperty();
	app.multiPortsAllowed(false);
	app.processArguments(args);
	app.run();
    }
}
