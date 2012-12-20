/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.moos.deployed;
import java.io.IOException;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.AssertionFailedError;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.MetadataPacket;
import org.mbari.siam.distributed.DevicePacketSet;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.core.DeviceLog;
import org.mbari.siam.core.DeviceLogIterator;

/**
 * JUnit test harness for testing DeviceLog and related classes.
 * 
 * @author Tom O'Reilly
 */
public class DeviceLogTest extends TestCase {

	static Logger _logger = Logger.getLogger(DeviceLogTest.class);

	static long _deviceID = 999;

	static String _tmpDir = null;

	public DeviceLogTest(String methodName) {
		super(methodName);
	}

	public void setUp() {
		BasicConfigurator.configure();

		_tmpDir = System.getProperty("java.io.tmpdir");
		if (_tmpDir == null) {
			fail("java.io.tmpdir property not set");
		}
	}

	/** Verify that packet is properly appended to DeviceLog. */
	public void append() {

		DeviceLog log = null;

		DevicePacket packet = new DevicePacket(_deviceID);
		long key = System.currentTimeMillis();
		packet.setSystemTime(key);

		long totalPackets = 0;
		long prevTotalPackets = 0;

		try {
			log = new DeviceLog(_deviceID, _tmpDir);
			prevTotalPackets = log.nPackets();
			// Set sequence number
			packet.setSequenceNo(totalPackets);
			log.appendPacket(packet, true, true);
		} catch (Exception e) {
			fail("Got Exception: " + e);
		}

		// Test DeviceLogIterator; Iterate through all packets in the log
		try {
			log = new DeviceLog(_deviceID, _tmpDir);
		} catch (Exception e) {
			fail("Got Exception: " + e);
		}

		DeviceLogIterator iterator = new DeviceLogIterator(log);

		// Count packets
		totalPackets = 0;
		while (iterator.hasNext()) {
			try {
				iterator.next();
				totalPackets++;
			} catch (Exception e) {
				fail("Got Exception while iterating: " + e);
			}
		}

		// Packet count should have increased by 1
		assertTrue("Iterated thru " + totalPackets + ": expected "
				+ (prevTotalPackets + 1), totalPackets == prevTotalPackets + 1);

		try {
			log.close();
		} catch (Exception e) {
			fail("Exception while closing log: " + e);
		}

		DevicePacketSet packetSet = null;

		try {
			log = new DeviceLog(_deviceID, _tmpDir);
			totalPackets = log.nPackets();

			// Check that packet count has incremented by 1
			assertTrue("Packet count did not increment by 1",
					totalPackets == (prevTotalPackets + 1));

			// Look for packet with this timestamp
			packetSet = log.getPackets(key, key, 100);
			_logger.debug("nPackets=" + log.nPackets());
			log.close();
		} catch (NoDataException e) {
			fail("No data found: " + e);
		} catch (IOException e) {
			fail("IOException while retrieving data: " + e);
		}

		// Should be just one packet that meets timestamp criterion
		assertTrue("Should be only 1 matching packet", packetSet._packets
				.size() == 1);

		DevicePacket outputPacket = (DevicePacket) (packetSet._packets
				.elementAt(0));

		assertEquals("Timestamp does not match", packet.systemTime(),
				outputPacket.systemTime());

		assertEquals("Source ID does not match", packet.sourceID(),
				outputPacket.sourceID());

		assertEquals("Sequence number does not match", packet.sequenceNo(),
				outputPacket.sequenceNo());

		assertEquals("Metadata reference does not match", packet.metadataRef(),
				outputPacket.metadataRef());
	}

	/**
	 * Append a metadata packet, then a subsequent packet. Check metadata
	 * reference on subsequent packet.
	 */
	public void appendMetadata() {

		_logger.debug("appendMetadata()");
		
		// Sleep for a bit so that subsequent timetags aren't the same
		// as any previous.
		try {
			Thread.sleep(1000);
		}
		catch (InterruptedException e) {
			//
		}
		long totalPackets = 0;

		DevicePacket packet = new DevicePacket(_deviceID);
		// Set packet timestamp
		long startKey = System.currentTimeMillis();
		packet.setSystemTime(startKey);

		MetadataPacket metadataPacket = new MetadataPacket(_deviceID, "test"
				.getBytes(), "this is a test".getBytes());

		// Set packet timestamp
		metadataPacket.setSystemTime(System.currentTimeMillis());

		DevicePacket packet2 = new DevicePacket(_deviceID);
		// Set packet timestamp
		long stopKey = System.currentTimeMillis();
		packet2.setSystemTime(stopKey);

		DeviceLog log = null;

		try {
			log = new DeviceLog(_deviceID, _tmpDir);
			totalPackets = log.nPackets();

			// Set sequence number and write the packet
			packet.setSequenceNo(totalPackets);
			log.appendPacket(packet, true, true);

			// Set sequence number and write the metadata packet
			metadataPacket.setSequenceNo(totalPackets + 1);
			log.appendPacket(metadataPacket, true, true);

			// Set sequence number and write the next packet
			packet2.setSequenceNo(totalPackets + 2);
			log.appendPacket(packet2, true, true);

			log.close();
		} catch (IOException e) {
			fail("Got IOException while appending packets: " + e);
		}

		// Open the log again, and read the packets just written.
		DevicePacketSet packetSet = null;

		try {
			log = new DeviceLog(_deviceID, _tmpDir);

			// Check that packet count has incremented by 3
			assertTrue("Packet count did not increment by 3",
					log.nPackets() == (totalPackets + 3));

			// Look for packet in timestamp range
			packetSet = log.getPackets(startKey, stopKey, 100);
			_logger.debug("#2. nPackets=" + log.nPackets());
			log.close();
		} catch (NoDataException e) {
			fail("No data found: " + e);
		} catch (IOException e) {
			fail("IOException while retrieving data: " + e);
		}

		assertTrue("Should be 3 packets retrieved, but got " + 
				packetSet._packets.size(),
				packetSet._packets.size() == 3);

		packet = (DevicePacket) (packetSet._packets.elementAt(0));
		metadataPacket = (MetadataPacket) (packetSet._packets.elementAt(1));
		packet2 = (DevicePacket) (packetSet._packets.elementAt(2));

		_logger.debug("First metadata ref=" + packet.metadataRef());

		// First device packet and following metadata packet should
		// both refer to same earlier metadata packet.
		assertTrue("Incorrect metadata reference on metadata packet",
				metadataPacket.metadataRef() == packet.metadataRef());

		assertTrue("Incorrect metadata reference number",
				packet2.metadataRef() == metadataPacket.sequenceNo());
	}
	
	/** Append a packet with timetag that is less than previous timetag. 
	 * Verify that DeviceLog automatically appends a message packet with 
	 * error message. */
	public void appendBadTime() {
		DeviceLog log = null;


		long totalPackets = 0;
		long prevTotalPackets = 0;

		try {
			log = new DeviceLog(_deviceID, _tmpDir);
			prevTotalPackets = log.nPackets();
			
			// Create a packet, set its timetag to '0', and append.
			// This should result in a "clock rollback" message 
			// packet.
			DevicePacket packet = new DevicePacket(_deviceID);
			long timeStamp = 0;
			packet.setSystemTime(timeStamp);
			log.appendPacket(packet, true, true);
			
			_logger.debug("prev total packets: " + prevTotalPackets + 
					",  totalPackets: " + log.nPackets());
			
			assertTrue("Total packets should increment by 2", 
					log.nPackets() == prevTotalPackets + 2);
			
		} catch (Exception e) {
			fail("Got Exception: " + e);
		}

	}

	static public Test suite() {
		TestSuite suite = new TestSuite();
		suite.addTest(new DeviceLogTest("append"));
		suite.addTest(new DeviceLogTest("appendMetadata"));
		suite.addTest(new DeviceLogTest("appendBadTime"));
		return suite;
	}
}

