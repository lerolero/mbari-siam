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



public class AggregatorThreadTest
{
    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(AggregatorThreadTest.class);

    SimpleDateFormat _dateFormatter;
    long _isiId = 0;
    int _repetitions = 3;
    int _numBytes = 20000;
    int _timeout = 5000;
    int _nEntries = 0;
    int _numThreads = 2;
    long _workerDelay = 100;
    long _sleepMillisec = 500;
    DevicePacketAggregator _aggregator = null;
    AggregatorTestThread _threads[];

    public AggregatorThreadTest(String propFile)
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
	return("AggregatorThreadTest");
    }

    void printFailure(String methodName, String message) {
	_log4j.error(_dateFormatter.format(new Date()) + " " +  
		     Thread.currentThread().getName() +
		     " ERROR " +  message);
    }

    
    protected class AggregatorTestThread extends Thread
    {
    //
    // Returns true if test ran OK, false if Exception
    //
	protected int _threadNum;
	protected AggregatorTestThread(int num)
	{
	    super(new String("TestThread" + num));
	    _threadNum = num;
	}

	protected boolean runTest()
	{
	    boolean done = false;
	    long totalPackets = 0;
	    long latest = 0;
	    long start = System.currentTimeMillis();
	    DevicePacketSet packetSet = null;

	    _log4j.info("Running test thread " + _threadNum + " with timeout = " + _timeout);
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
		System.out.println(_dateFormatter.format(new Date()) + " " + 
				   Thread.currentThread().getName() +
				   " got " + totalPackets + 
				   " packets from DeviceID " + _isiId + " in " + duration +
				   " milliseconds.");
	    }
	    catch (Exception e) {
		printFailure("runTest()", e.toString());
		e.printStackTrace();
		return(false);
	    }
	    return(true);
	}

	public void run()
	{
	    for (int i = 0; i < _repetitions; i++)
	    {
		runTest();

		System.out.println("");

		try {
		    Thread.sleep(_sleepMillisec);
		}
		catch (InterruptedException e) {
		}
	    }
	}

    } /* class AggregatorTestThread */


    public void run() throws IOException
    {
	_log4j.debug("Creating " + _numThreads + " test threads");

	_threads = new AggregatorTestThread[_numThreads];

	for (int i = 0; i < _numThreads; i++) {
	    _threads[i] = new AggregatorTestThread(i);
	    _threads[i].start();
	}
    }

    protected int setLog(long isiId, String directory)
      throws IOException, FileNotFoundException
    {
	_isiId = isiId;
	FilteredDeviceLog log = new FilteredDeviceLog(isiId, directory);
	_nEntries = log.nPackets();
	_aggregator = new DevicePacketAggregator(directory, _workerDelay);

	_log4j.debug("Opened log file for deviceId " + isiId + " in directory " + directory
		     + ", nEntries = " + _nEntries);
	return(_nEntries);
    }

    public void exitTest()
    {
	for (int i = 0; i < _numThreads; i++) {
	    try {
		_threads[i].join();
	    } catch (Exception e) {
		_log4j.error("Exception in Thread.join(): " + e);
	    }
	}

	if (_aggregator != null)
	    _aggregator.exitWorkers();
    }

    protected void printUsage()
    {
	System.err.println("usage: " + className() +
			   " deviceID directory [repetitions] [numBytes] [timeout] [threads] [workerDelay]");
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
		_numThreads = Integer.parseInt(args[5]);
	    } catch(NumberFormatException e) {
		System.err.println("Can't parse numThreads.  Using " + _numThreads);
	    }
	}

	if (args.length >= 7)
	{
	    try {
		_workerDelay = Long.parseLong(args[6]);
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
	AggregatorThreadTest test = new AggregatorThreadTest("AggregatorProperties");

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
