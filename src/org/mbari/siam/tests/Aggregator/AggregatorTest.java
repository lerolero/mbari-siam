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



public class AggregatorTest
{
    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(AggregatorTest.class);

    SimpleDateFormat _dateFormatter;
    long _isiId = 0;
    int _repetitions = 3;
    int _numBytes = 20000;
    int _timeout = 5000;
    int _nEntries = 0;
    long _workerDelay = 100;
    long _sleepMillisec = 500;
    DevicePacketAggregator _aggregator = null;

    public AggregatorTest(String propFile)
    {
	if (propFile == null)
	    PropertyConfigurator.configure(System.getProperties());
	else
	    PropertyConfigurator.configure(propFile);

	PatternLayout layout = new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");
	BasicConfigurator.configure(new ConsoleAppender(layout));

	_dateFormatter = 
	    new SimpleDateFormat("yyyy.DDD/HH:mm:ss");

	_dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));

    }

    public String className()
    {
	return("AggregatorTest");
    }

    void printFailure(String methodName, String message) {
	_log4j.error(_dateFormatter.format(new Date()) + " " +  className() + 
		     "." + methodName + " ERROR " +  message);
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

	_log4j.info("Running test with timeout = " + _timeout);
	try
	{
	    do
	    {
		packetSet = _aggregator.getDevicePackets(_isiId, latest+1, Long.MAX_VALUE, 
							 _numBytes, _timeout);

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
							 _numBytes, _timeout);
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

    public void run() throws IOException
    {
	for (int i = 0; i < _repetitions; i++)
	{
	    if (runTest()) {
		_timeout -= 500;
	    }
	    else {
		_timeout += 500;
	    }

	    System.out.println("");

	    try {
		Thread.sleep(_sleepMillisec);
	    }
	    catch (InterruptedException e) {
	    }
	}
    }

    public void exitTest()
    {
	if (_aggregator != null)
	    _aggregator.exitWorkers();
    }

    protected int setLog(long isiId, String directory)
      throws IOException, FileNotFoundException
    {
	_isiId = isiId;
	FilteredDeviceLog log = new FilteredDeviceLog(isiId, directory);
	_nEntries = log.nPackets();
	_aggregator = new DevicePacketAggregator(directory, _workerDelay);

	_log4j.info("Opened log file for deviceId " + isiId + " in directory " + directory
		     + ", nEntries = " + _nEntries);
	return(_nEntries);
    }

    protected void printUsage()
    {
	System.err.println("usage: " + className() +
			   " deviceID directory [repetitions] [numBytes] [timeout] [workerDelay]");
    }

    public void parseCmdLine(String args[])
    {
	int argnum = 0;

	if (args.length < 2) {
	    printUsage();
	    System.exit(1);
	}

	try {
	    _isiId = Long.parseLong(args[0]);
	} catch(NumberFormatException e) {
	    printUsage();
	    System.exit(1);
	}

	if (args.length >= 3)
	{
	    try {
		_repetitions = Integer.parseInt(args[2]);
	    } catch(NumberFormatException e) {
		System.err.println("Can't parse number of repetitions.  Using " + _repetitions);
	    }
	}

	if (args.length >= 4)
	{
	    try {
		_numBytes = Integer.parseInt(args[3]);
	    } catch(NumberFormatException e) {
		System.err.println("Can't parse size of DevicePacketSet.  Using " + _numBytes);
	    }
	}

	if (args.length >= 5)
	{
	    try {
		int timeout = Integer.parseInt(args[4]);
		_timeout = 1000 * timeout;
	    } catch(NumberFormatException e) {
		System.err.println("Can't parse timeout.  Using " + _timeout/1000);
	    }
	}

	if (args.length >= 6)
	{
	    try {
		_workerDelay = Long.parseLong(args[5]);
	    } catch(NumberFormatException e) {
		System.err.println("Can't parse workerDelay.  Using " + _workerDelay);
	    }
	}

	try {
	    _nEntries = setLog(_isiId, args[1]);
	}
	catch (Exception e) {
	    _log4j.error("Could not open log for deviceId " + _isiId + " in directory " + args[1] + e);
	    e.printStackTrace();
	    System.exit(1);
	}

	if (_nEntries <= 0)
	{
	    _log4j.error("No log data for deviceId " + _isiId + " in directory " + args[1]);
	    System.exit(1);
	}

    }

    public static void main(String args[])
    {
	AggregatorTest test = new AggregatorTest("AggregatorProperties");

	test.parseCmdLine(args);

	try {
	    test.run();
	} catch (IOException e) {
	    _log4j.error("IOException in test.run(): " + e);
	    e.printStackTrace();
	}

	test.exitTest();
    }

} /* class AggregatorTest */
