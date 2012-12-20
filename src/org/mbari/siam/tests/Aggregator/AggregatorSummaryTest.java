/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.Aggregator;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.mbari.siam.core.DevicePacketAggregator;
import org.mbari.siam.core.DeviceLog;
import org.mbari.siam.core.FilteredDeviceLog;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DevicePacketSet;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.TimeoutException;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;



public class AggregatorSummaryTest extends AggregatorTest
{
    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(AggregatorSummaryTest.class);

    public AggregatorSummaryTest(String propFile)
    {
	super(propFile);
    }


    public String className()
    {
	return("AggregatorSummaryTest");
    }


    //
    // Returns true if test ran OK, false if Exception
    //
    public boolean runTest()
    {
	boolean done = false;
	long totalPackets = 0;
	long latest = 0;
	long start = System.currentTimeMillis();
	DevicePacketSet packetSet = null;
	int typeMask = 
	    DevicePacket.METADATA_FLAG |
	    DevicePacket.DEVICEMESSAGE_FLAG |
	    DevicePacket.SUMMARY_FLAG;

	_log4j.info("Running test with timeout = " + _timeout);
	try
	{
	    do
	    {
		packetSet = _aggregator.getDevicePackets(_isiId, latest+1, Long.MAX_VALUE, 
							 _numBytes, typeMask, _timeout);

		int setSize = packetSet._packets.size();
		_log4j.debug("Got PacketSet, key = " + latest + ", numPackets = " + setSize);
		totalPackets += setSize;

		for (int j = 0; j < packetSet._packets.size(); j++)
		{
		    DevicePacket packet = (DevicePacket) packetSet._packets.get(j);

		    if ((packet != null) && (packet.systemTime() > latest))
			latest = packet.systemTime();
		}


	    } while (!packetSet.complete());

	    long duration = System.currentTimeMillis() - start;
	    System.out.println(_dateFormatter.format(new Date()) + " " + className() + 
			       ": got " + totalPackets + 
			       " packets from DeviceID " + _isiId + " in " + duration +
			       " milliseconds.");

	    // Now try to read with a key that's later than all logged packets.
	    // It should return NoDataException.  Check for correct behavior
	    try {
		packetSet = _aggregator.getDevicePackets(_isiId, latest+1, Long.MAX_VALUE, 
							 _numBytes, typeMask, _timeout);
		System.out.println("FAILURE - Did not get NoDataException when trying " +
				   "a too-late key");
		System.out.println("Got packetSet with vector of size " +
				   packetSet._packets.size() + 
				   ", complete = " + 
				   (packetSet.complete() ? "TRUE" : "FALSE"));
		
	    } catch (NoDataException e) {
		System.out.println("Correctly got NoDataException when done\n");
	    }
	}
	catch (Exception e) {
	    printFailure("runTest()", e.toString());
	    e.printStackTrace();
	    return(false);
	}
	return(true);
    }


    public static void main(String args[])
    {
	AggregatorSummaryTest test = new AggregatorSummaryTest("AggregatorProperties");

	test.parseCmdLine(args);

	try {
	    test.run();
	} catch (IOException e) {
	    _log4j.error("IOException in test.run(): " + e);
	    e.printStackTrace();
	}

	test.exitTest();
    }

} /* class AggregatorSummaryTest */
