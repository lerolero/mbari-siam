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
import java.net.InetAddress;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;

import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.DevicePacketSet;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Exportable;
import org.mbari.siam.distributed.PacketSubsampler;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.NoDataException;
import moos.ssds.jms.PublisherComponent;
import org.mbari.siam.core.DeviceLog;
import org.mbari.siam.utils.TimeUtils;
import org.mbari.siam.operations.portal.PortalConnection;

/**
   PacketRetriever retrieves packets from specified device ID, over
   specific time range.
 */
public class PacketRetriever extends NodeUtility {

    static private Logger _log4j = Logger.getLogger(PacketRetriever.class);

    boolean _save = true;
    boolean _publish = false;
    boolean _defaultFilter = false;
    long _deviceID;
    boolean _specifiedTime = false;
    boolean _excludeStale = true;  // By default, exclude stale packets
    long _startTime = 0;
    long _endTime = System.currentTimeMillis();
    int _skipInterval = 0;
    int _includedTypes;
    int _maxBytes = 1024;
    int _timeoutMsec = 20000;
    String _logDirectory = ".";


    PublisherComponent _publisher = null;
    DeviceLog _deviceLog = null;


    public static void main(String[] args) 
    {
	// Configure log4j
	PropertyConfigurator.configure(System.getProperties());
	BasicConfigurator.configure();
	
        PacketRetriever retriever = new PacketRetriever();
	try {
	    retriever.processArgs(args);
	}
	catch (Exception e) {
	    System.err.println(e.getMessage());
	    retriever.printUsage();
	    return;
	}


	retriever.run();
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
	    else if (args[i].equals("-between") && i < args.length-3) {
		// Parse start time
		_specifiedTime = true;
		i++;
		if (args[i].equalsIgnoreCase("now")) {
		    _startTime = System.currentTimeMillis();
		}
		else {
		    try {
			_startTime = TimeUtils.parseDateTime(args[i]);
		    }
		    catch (ParseException e) {
			throw new Exception("Invalid start time:\n" + 
					    e.getMessage());
		    }
		}

		// Parse end time
		i++;
		if (args[i].equalsIgnoreCase("now")) {
		    _endTime = System.currentTimeMillis();
		}
		else {
		    try {
			_endTime = TimeUtils.parseDateTime(args[i]);
		    }
		    catch (ParseException e) {
			throw new Exception("Invalid end time:\n" + 
					    e.getMessage());
		    }
		}
	    }
	    else if (args[i].equals("-timeout") && i < args.length-2) {
		// Parse timeout value
		try {
		    _timeoutMsec = Integer.parseInt(args[++i]) * 1000;
		}
		catch (NumberFormatException e) {
		    throw new Exception("Invalid timeout, integer required: " + args[i]);
		}
	    }
	    else if (args[i].equals("-maxbytes") && i < args.length-2) {
		// Parse max bytes per packet set
		try {
		    _maxBytes = Integer.parseInt(args[++i]);
		}
		catch (NumberFormatException e) {
		    throw new Exception("Invalid max bytes per packetset, integer required: " + args[i]);
		}
	    }
	    else if (args[i].equals("-default")) {
		_defaultFilter = true;
	    }
	    else if (args[i].equals("-nosave")) {
		_save = false;
	    }
	    else if (args[i].equals("-stale")) {
		_excludeStale = false;
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
	System.out.println("usage: nodeURL [options] deviceID");
	System.out.println("Valid options:");
	System.out.println("-between startTime endTime");
	System.out.println("-default  use instrument' default filters");
	System.out.println("-types type(s), where type is 'metadata', 'data', 'message', 'summary', or 'all'");
	System.out.println("-skip subsampleInterval");
	System.out.println("-timeout service retrieval timeout seconds");
	System.out.println("-maxbytes maximum bytes per packetset");
	System.out.println("-stale include any 'stale' packets");
	System.out.println("-nosave Don't save retrieved packets");

	System.out.println("\nstartTime and endTime can have the following formats:");
	System.out.println("m/d/yyyy\n" + "m/d/yyyyTh:m\n" + 
			   "m/d/yyyyTh:m:s");
    }


    /** Get packets from specified device over specified time range. */
    public void processNode(Node node) 
	throws Exception {


	if (_save) {
	    createOutputLog();
	}

	DevicePacketSet packetSet = null;
	long startTime = _startTime;

	_log4j.debug("processNode(): deviceID=" + _deviceID);

	int nPackets = 0;
	do {

	    try {

		packetSet = node.getDevicePackets(_deviceID, startTime, _endTime,
						  _maxBytes, _includedTypes, _timeoutMsec);
	    }
	    catch (NoDataException e) {
		System.err.println("No data retrieved for time-range and filters");
		break;

	    }
	    catch (Exception e) {
		_log4j.error("Got exception from getPackets(): ", e);
		break;
	    }

	    _log4j.debug("process retrieved packets");
	    // What was the newest packet actually retrieved?
	    for (int i = 0; i < packetSet._packets.size(); i++) {
		DevicePacket packet = 
		    (DevicePacket )packetSet._packets.get(i);

		// DEBUG
		System.out.println(packet);

		if (packet.systemTime() > startTime) {
		    startTime = packet.systemTime();
		}

		startTime += 1;
	    }


	    if (_save) {
		savePackets(packetSet._packets);
	    }
	    else {
		_log4j.warn("Not saving packets");
	    }

	    if (_publish && _publisher != null) {
		    publishPackets(packetSet._packets);
	    }

	    nPackets += packetSet._packets.size();
	    _log4j.debug(packetSet._packets.size() + " packets in set");

	} while (!packetSet.complete());

	_log4j.debug("got total of " + nPackets + " packets");
    }


    void createOutputLog() throws Exception {
	_deviceLog = new DeviceLog(_deviceID, _logDirectory);
	if (!_specifiedTime) {
	    // By default, start retrieving packets from latest time in log
	    _startTime = _deviceLog.getMaxTimestamp() + 1;
	}
    }

    /** Publish packets to JMS server. */
    void publishPackets(Vector packets) {
	Iterator i = packets.iterator();
	while (i.hasNext()) {

	    try{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		Exportable packet = (Exportable)i.next();
		packet.export(dos);
		dos.flush();
		byte[] exportedBytes = bos.toByteArray();
		dos.close();
		_publisher.publishBytes(exportedBytes);
	    }catch(IOException e){
		e.printStackTrace();
	    }
	    //_ssdsPublisher.publish((Serializable )i.next());
	}
    }


    /**
       Write data into log file.
     */
    void savePackets(Vector packets) {

	try {

	    // Write each packet to log file
	    for (int i = 0; i < packets.size(); i++) {
		DevicePacket packet = (DevicePacket )packets.get(i);

		// Append packet, but don't modify sequence number or 
		// metadata reference.
		_log4j.debug("append packet to device log");
		_deviceLog.appendPacket(packet, false, false);
	    }
	}
	catch (Exception e) {
	    _log4j.error(e);
	    e.printStackTrace();
	}
    }

}
