/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.moos.deployed;

import org.mbari.siam.core.DeviceLog;
import org.mbari.siam.core.FilteredDeviceLog;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DevicePacketSet;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.PacketFilter;
import org.mbari.siam.distributed.PacketSubsampler;
import org.mbari.siam.distributed.DeviceMessagePacket;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;

/**
 * This test appends packets to a DeviceLog. For every 'n' packets appended
 * to the log, the log is closed and the latest packets are read. The test
 * prints out the time required to read as a function of the total number of
 * packets in the file.
 * 
 */
public class RetrievalTest {

    static final long ID = 9999;
    static final int MAX_RETRIEVED_PACKETS = 10;

    // Append this many packets between retrievals
    long _incrementPackets;

    // Low end of packet-search time window (from "now")
    long _intervalMsec;

    // Append message packets at this packet interval
    int _msgInterval;

    /**
     * @param incrementPackets
     *            Number of packets to write between retrievals
     * @param intervalMsec
     * @param msgInterval
     */
    public RetrievalTest(long incrementPackets, long intervalMsec,
			 int msgInterval) {

	_incrementPackets = incrementPackets;
	_intervalMsec = intervalMsec;
	_msgInterval = msgInterval;
    }

    public void run() throws Exception {

	int maxBytes = 512;

	long timeTag = 0;
		
	String logDir = ".";

	PacketFilter[] filters = new PacketFilter[1];

	filters[0] = new PacketSubsampler(0, DevicePacket.ALL_TYPES);

	// Open device log
	DeviceLog log = new FilteredDeviceLog(ID, logDir, filters);
		
	SensorDataPacket packet = new SensorDataPacket(ID, maxBytes);
	byte[] data = "This is test data".getBytes();
	packet.setDataBuffer(data);

	// Create a message packet
	DeviceMessagePacket message = new DeviceMessagePacket(ID);

	int seqNo = 0;
	int nGenerated = 0;
	while (true) {
	    // Append packets
	    for (int i = 0; i < _incrementPackets; i++) {
		timeTag = System.currentTimeMillis();
		// Set packet's time tag
		packet.setSystemTime(timeTag);

		// Append packet to the log
		log.appendPacket(packet);
		// System.out.println("Wrote " + ++seqNo + " packets.");

		if (_msgInterval >= 0 && 
		    (nGenerated % _msgInterval) == 0) {
		    // append message packet
		    message.setMessage(System.currentTimeMillis(), 
				       "Sweet yo".getBytes());

		    log.appendPacket(message);
		    nGenerated++;
		}
	    }

	    // NOTE: Do not close the DeviceLog before reading packets
	    // (InstrumentService never closes the log...)
			
	    try {

		// Starting NOW...
		long startMsec = System.currentTimeMillis();

		// Determine retrieval time window
		long earliest = startMsec - _intervalMsec;
		long latest = Long.MAX_VALUE;

		// Read packets created within specified time window
		int nRead = 0;
		int nSets = 0;
		// System.out.println("Read packets:");
		while (true) {
		    DevicePacketSet packetSet = 
			log.getPacketKeyRange(earliest,
					      latest, MAX_RETRIEVED_PACKETS);
		    nSets++;
		    nRead += packetSet._packets.size();

		    if (packetSet.complete()) {
			break;
		    }

		    // Get last (i.e. newest) packet in set
		    DevicePacket devicePacket = 
			(DevicePacket) packetSet._packets
			.elementAt(packetSet._packets.size() - 1);

		    // Set "earliest" retrieval criterion
		    earliest = devicePacket.systemTime() + 1;

		}

		// System.out.println("Compute time...");
		// Calculate access time
		long accessMsec = System.currentTimeMillis() - startMsec;

		// Print total number of packets in log, access time, and
		// number of packets just read
		// System.out.println("log.nPackets=" + log.nPackets() + " nSets="
		//		+ nSets + " nRead=" + nRead + " accessMsec="
		//		+ accessMsec);
				
		System.out.println("totalPacketsInLog; " + log.nPackets() + 
				   ";  packetsRead; " + nRead + 
				   "; accessMsec; " + accessMsec);
				
	    } catch (Exception e) {
		System.err.println("Caught exception while reading packets: "
				   + e);
	    }
	}
    }

    public static void main(String[] args) {

	PropertyConfigurator.configure(System.getProperties());
	PatternLayout layout = new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");
	BasicConfigurator.configure(new ConsoleAppender(layout));

	if (args.length != 2) {
	    System.err.println("usage: incrementPackets  intervalMsec");
	    return;
	}
		
	long incrementPackets = Long.parseLong(args[0]);
	long intervalMsec = Long.parseLong(args[1]);

	RetrievalTest test = new RetrievalTest(incrementPackets, 
					       intervalMsec, 10);
		
	try {
	    test.run();
	}
	catch (Exception e) {
	    System.err.println("Caught exception from run():" + e);
	}
	System.out.println("Done.");
    }
}

