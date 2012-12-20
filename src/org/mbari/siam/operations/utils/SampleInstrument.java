// Copyright MBARI 2003
package org.mbari.siam.operations.utils;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.Vector;
import java.text.DateFormat;
import java.rmi.Naming;
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
public class SampleInstrument {

    public static void main(String[] args) {
	Node node = null;

	// Configure log4j
	PropertyConfigurator.configure(System.getProperties());
	BasicConfigurator.configure();

        //need to setSecurityManager 
        if ( System.getSecurityManager() == null )
            System.setSecurityManager(new SecurityManager());
        
        if (args.length < 2) {
	    System.err.println("usage: SampleInstrument nodeURL commPortName");
	    System.exit(1);
	}

	String nodeURL = NodeUtility.getNodeURL(args[0]);

	try {

	    System.out.println("Looking for node server stub at " + nodeURL);

	    node = (Node )Naming.lookup(nodeURL);

	    System.out.println("Got proxy for node service \"" + 
			       new String(node.getName()) + "\"");

	    
	    for (int i = 0; i < args.length-1; i++) {
		// Now sample instruments on specified ports
		String portName = PortUtility.getPortName(args[i+1]);
		try {
		    Device device = node.getDevice(portName.getBytes());
		    if (device instanceof Instrument) {
			Instrument instrument = (Instrument )device;
			System.out.println(new String(instrument.getName()) + 
					   ":");
			SensorDataPacket packet = 
			    instrument.acquireSample(false);

			System.out.println(PrintUtils.printAscii(packet.dataBuffer(), 0, 0));
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
		catch (NoDataException e) {
		    System.err.println("No data from instrument on port " + 
				       portName);
		}
	    }

	}
	catch (RemoteException e) {
	    System.err.println("RemoteException: " + e.getMessage());
	    System.exit(1);
	}
	catch (Exception e) {
	    System.err.println("Exception: " + e.getMessage());
	    e.printStackTrace();
	    System.exit(1);
	}
	System.exit(0);

    }
}
