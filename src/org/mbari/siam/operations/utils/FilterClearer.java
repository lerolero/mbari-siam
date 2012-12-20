/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.utils;

import java.io.DataOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.Iterator;
import java.text.ParseException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;

import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.DevicePacketSet;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Exportable;
import org.mbari.siam.distributed.PacketFilter;
import org.mbari.siam.distributed.PacketSubsampler;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.leasing.LeaseRefused;
import org.mbari.siam.core.DeviceLog;
import org.mbari.siam.utils.TimeUtils;

/**
   FilterClearer gets and displays default telemetry filtering for 
   specified device ID
 */
public class FilterClearer extends NodeUtility {

    static private Logger _log4j = Logger.getLogger(FilterClearer.class);

    long _deviceID;
    int _includedTypes;
    int _skipInterval = 0;


    public static void main(String[] args) 
    {
	// Configure log4j
	PropertyConfigurator.configure(System.getProperties());
	BasicConfigurator.configure();
	
        FilterClearer app = new FilterClearer();

	try {
	    app.processArguments(args);
	}
	catch (Exception e) {
	    System.err.println(e.getMessage());
	    app.printUsage();
	    return;
	}
	app.run();
    }

    public void printUsage() {
    }


    /** Process application-specific option. */
    public void processCustomOption(String[] args, int index)
	throws InvalidOption {
	super.processCustomOption(args, index);
	if (index == args.length-1) {
	    // Last argument is device ID
	    try {
		_deviceID = Long.parseLong(args[index]);
	    }
	    catch (NumberFormatException e) {
		throw new InvalidOption("Invalid device ID: " + args[index]);
	    }
	}
    }


    /** Get packets from specified device over specified time range. */
    public void processNode(Node node) 
	throws Exception {

	_log4j.debug("processNode(): deviceID=" + _deviceID);

	// Get instrument proxy from node
	Device device = null;
	try {
	    device = node.getDevice(_deviceID);
	    Instrument instrument = (Instrument )device;
	    instrument.clearDefaultPacketFilters();
	}
	catch (DeviceNotFound e) {
	    System.err.println("Device " + _deviceID + " not found");
	    return;
	}
    }
}
