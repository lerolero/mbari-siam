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
   Cache properties of instrument service on specified port
 */
public class CacheInstrumentProperties extends PortUtility {

    boolean _clearCache = false;

    /** Print properties associated with SIAM service on this port. */
    public void processPort(Node node, String portName)
	throws RemoteException {

	_log4j.debug("processPort()");
	// TODO: Accept user notation from command line
	try {

	    Device device = node.getDevice(portName.getBytes());
	    _log4j.debug("processPort() - instrument?");
	    if (device instanceof Instrument) {
		Instrument instrument = (Instrument )device;

		String note = null;

		try {
		    if (_clearCache) {
			// Try to clear the property cache
			_log4j.debug("processPort() - clearCache");
			note = "clear properties cache";
			instrument.clearPropertiesCache(note.getBytes());
		    }
		    else {
			// Try to cache the properties
			_log4j.debug("processPort() - cacheProperties");
			note = "cache properties";
			instrument.cacheProperties(note.getBytes());
		    }
		}
		catch (Exception e) {
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
			   "CacheInstrumentProperties nodeURL port");
    }


    /** Look for options. */
    public void processCustomOption(String[] args, int index) 
        throws InvalidOption 
    {
	if (args[index].endsWith("clear")) {
	    _clearCache = true;
	}
	else {
	    System.err.println("Invalid option: " + args[index]);
	}
    }



    public static void main(String[] args) {

	System.out.println("main() - args.length=" + args.length);
	for (int i = 0; i < args.length; i++) {
	    System.out.println("args[" + i + "]=" + args[i]);
	}

	CacheInstrumentProperties app = new CacheInstrumentProperties();
	app.multiPortsAllowed(true);
	app.processArguments(args);
	app.run();
    }
}
