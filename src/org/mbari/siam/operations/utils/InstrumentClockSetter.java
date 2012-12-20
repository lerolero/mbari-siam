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
public class InstrumentClockSetter extends PortUtility {

    public static void main(String[] args) {

	InstrumentClockSetter setter = 
	    new InstrumentClockSetter();

	setter.processArguments(args);
	setter.run();

    }

    /** No custom options for this application. */
    public void processCustomOption(String[] args, int index) 
	throws InvalidOption {

	// No custom options for this application... so throw 

	// invalid option exception if this method is called.
	throw new InvalidOption("unknown option");
    }

    /** Print usage message. */
    public void printUsage() {
	System.err.println("InstrumentClockSetter nodeURL port(s)");
    }

    /** Sample specified port. */
    public void processPort(Node node, String portName) 
	throws RemoteException {

	try {
	    Device device = node.getDevice(portName.getBytes());
	    if (device instanceof Instrument) {
		Instrument instrument = (Instrument )device;
		System.out.println("Set instrument clock for " + 
				   new String(instrument.getName()) + ":");

		try {
		    instrument.setClock();
		    System.out.println("Clock set.");
		}
		catch (Exception e) {
		    System.err.println(e.getMessage());
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
    }
}
