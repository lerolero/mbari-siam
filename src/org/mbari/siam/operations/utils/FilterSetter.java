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
   FilterSetter adds default telemetry filtering for specified device ID
 */
public class FilterSetter extends NodeUtility {

    static private Logger _log4j = Logger.getLogger(FilterSetter.class);

    long _deviceID;
    int _includedTypes;
    int _skipInterval = 0;


    public static void main(String[] args) 
    {
	// Configure log4j
	PropertyConfigurator.configure(System.getProperties());
	BasicConfigurator.configure();
	
        FilterSetter app = new FilterSetter();

	try {
	    app.processArgs(args);
	}
	catch (Exception e) {
	    System.err.println(e.getMessage());
	    app.printUsage();
	    return;
	}
	app.run();
    }


    /** Process command line arguments. */
    public void processArgs(String[] args) throws Exception {

	if (args.length < 2) {
	    throw new Exception("Missing node URL and device ID");
	}

	// First argument is node URL
	_nodeURL = getNodeURL(args[0]);

	// Last argument is deviceID
	try {
	    _deviceID = Long.parseLong(args[args.length-1]);
	}
	catch (NumberFormatException e) {
	    throw new Exception("invalid device ID; must be integer");
	}
	
	_includedTypes = 0;
	_skipInterval = 0;

	// Process intervening options
	for (int i = 1; i < args.length-1; i++) {

	    if (args[i].equals("-types")) {
		// Assume that packet types follow
		while (i+1 < args.length-1 && !args[i+1].startsWith("-")) {
		    i++;
		    if (args[i].equals("metadata")) {
			_includedTypes |= DevicePacket.METADATA_FLAG;
		    }
		    else if (args[i].equals("data")) {
			_includedTypes |= DevicePacket.SENSORDATA_FLAG;
		    }
		    else if (args[i].equals("message")) {
			_includedTypes |= DevicePacket.DEVICEMESSAGE_FLAG;
		    }
		    else if (args[i].equals("summary")) {
			_includedTypes |= DevicePacket.SUMMARY_FLAG;
		    }
		    else if (args[i].equals("all")) {
			_includedTypes = DevicePacket.ALL_TYPES;
		    }
		}
	    }
	    else if (args[i].equals("-skip") && i < args.length-2) {
		try {
		    _skipInterval = Integer.parseInt(args[++i]);
		}
		catch (NumberFormatException e) {
		    throw new Exception("Invalid skip interval, " + 
					"integer required: " + args[i]);
		}
	    }
	    else {
		throw new Exception("Invalid option: " + args[i]);
	    }
	}

	if (_includedTypes == 0) {
	    // No types specified; assume all
	    _includedTypes = DevicePacket.ALL_TYPES;
	}
    }


    public void printUsage() {
	System.out.println("usage: nodeURL [options] deviceID ");
	System.out.println("Valid options:");
	System.out.println("-types type(s), where type is 'metadata', 'data', 'message', 'summary', or 'all'");
	System.out.println("-skip subsampleInterval");
    }


    /** Get packets from specified device over specified time range. */
    public void processNode(Node node) 
	throws Exception {

	_log4j.debug("processNode(): deviceID=" + _deviceID);


	PacketFilter[] filters = new PacketFilter[2];
	// First filter includes everything
	filters[0] = 
	    new PacketSubsampler(0, DevicePacket.ALL_TYPES);

	filters[1] = new PacketSubsampler(_skipInterval, _includedTypes);


	// Get instrument proxy from node
	Device device = null;
	try {
	    device = node.getDevice(_deviceID);
	}
	catch (DeviceNotFound e) {
	    System.err.println("Device " + _deviceID + " not found");
	    return;
	}

	if (!(device instanceof Instrument)) {
	    System.err.println("Device " + _deviceID + 
			       " is not an instrument");
	    return;
	}

	Instrument instrument = (Instrument )device;

	try {
	    _log4j.debug("get device packets");

	    // Set default filters
	    instrument.addDefaultPacketFilters(filters);

	}
	catch (Exception e) {
	    _log4j.error("Got exception from getPackets(): ", e);
	}
    }

}
