// Copyright MBARI 2003
package org.mbari.siam.operations.utils;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.Vector;
import java.text.DateFormat;
import java.rmi.Naming;
import java.text.ParseException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.PortNotFound;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.utils.PrintUtils;

/**
   Acquire and print data sample from specified instrument.
 */
public class PortSampler extends PortUtility {

    boolean _logSample = false;

    public static void main(String[] args) {

	PortSampler sampler = new PortSampler();
	sampler.processArguments(args);
	sampler.run();

    }

    /** No custom options for this application. */
    public void processCustomOption(String[] args, int index) 
	throws InvalidOption {

	boolean error = false;
	// No custom options for this application... so throw 
	for (int i = 0; i < args.length; i++) {
	    if (args[i].equals("-log")) {
		_logSample = true;
	    }
	}

	if (error) {
	    // invalid option exception if this method is called.
	    throw new InvalidOption("unknown option");
	}

	return;
    }

    /** Print usage message. */
    public void printUsage() {
	System.err.println("PortSampler nodeURL port(s) [-log]");
    }

    /** Sample specified port. */
    public void processPort(Node node, String portName) 
	throws RemoteException {

	try {
	    Device device = node.getDevice(portName.getBytes());
	    if (device instanceof Instrument) {
		Instrument instrument = (Instrument )device;
		System.out.println(new String(instrument.getName()) + 
				   ":");
		SensorDataPacket packet = 
		    instrument.acquireSample(_logSample);

		System.out.println(packet);

		try {
		    PacketParser parser = instrument.getParser();
		    PacketParser.Field[] fields = 
			parser.parseFields(packet);

		    System.out.println("");

		    for (int j = 0; j < fields.length; j++) {

			if (fields[j] == null) {
			    continue;
			}

			System.out.println(fields[j].getName() + 
					   ": " +
					   fields[j].getValue() +
					   " " + 
					   fields[j].getUnits());
		    }
		}
		catch (NotSupportedException e) {
		    System.err.println("Parser not implemented.");
		}
		catch (ParseException e) {
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
	    System.err.println("Device not found on port " + portName);
	}
	catch (NoDataException e) {
	    System.err.println("No data from instrument on port " + 
			       portName);
	    if (e.getMessage() != null) {
		System.err.println(e.getMessage());
	    }
	}

    }
}
