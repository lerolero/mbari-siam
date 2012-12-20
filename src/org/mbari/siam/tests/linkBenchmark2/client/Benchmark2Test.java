/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.linkBenchmark2.client;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.rmi.*;
import org.mbari.siam.tests.linkBenchmark2.interfaces.*;
import org.mbari.siam.tests.linkBenchmark2.compression.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.rmi.server.RMISocketFactory;
import org.mbari.siam.utils.SiamSocketFactory;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DevicePacketSet;
import org.mbari.siam.distributed.NoDataException;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;

public abstract class Benchmark2Test {

    /** Maximum number of packets to transfer in a DevicePacketSet */
    protected static final int MAX_PACKETS = 100;

    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(Benchmark2Test.class);

    Benchmark2 _server = null; 
    SimpleDateFormat _dateFormatter;
    long _deviceId = 0;
    int _repetitions = 10;
    int _numPackets = MAX_PACKETS;
    int _nEntries = 0;
    long _sleepMillisec = 100;

    public Benchmark2Test()
    {
	PropertyConfigurator.configure(System.getProperties());
	PatternLayout layout = new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");
	BasicConfigurator.configure(new ConsoleAppender(layout));

	_dateFormatter = 
	    new SimpleDateFormat("yyyy.DDD/HH:mm:ss");

	_dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));

    }

    protected String className()
    {
	return "Benchmark2Test";

    }

    protected abstract Benchmark2 setServer(String serverName) throws Exception;

    public int setLog(long deviceId, String directory)
      throws IOException, FileNotFoundException
    {
	if (_server == null)
	    throw new IOException("No server found!");

	_deviceId = deviceId;
	_nEntries = _server.setDeviceLog(deviceId, directory);
	_log4j.debug("Opened log file for deviceId " + deviceId + " in directory " + directory
		     + ", nEntries = " + _nEntries);
	return(_nEntries);
    }

    void printSuccess(String className, String methodName, long duration) {
	System.out.println(_dateFormatter.format(new Date()) + " " + 
			   className + "." + methodName + " OK " + duration);
    }

    void printFailure(String className, String methodName, String message) {
	_log4j.error(_dateFormatter.format(new Date()) + " " + 
			   className + "." + methodName + " ERROR " + 
			   message);
    }

    public void runTest(boolean zipIt) throws IOException
    {
	boolean done = false;
	long totalPackets = 0;
	long latest = 0;
	long start = System.currentTimeMillis();
	DevicePacketSet packetSet = null;
	ObjectGunzipper inflater = new ObjectGunzipper();

	if (_server == null)
	    throw new IOException("No server found!");

	try
	{
	    do
	    {
		if (zipIt)
		{
		    byte[] compressedPacket =
			_server.getCompressedPackets(latest+1, Long.MAX_VALUE, _numPackets);
		    packetSet = (DevicePacketSet)(inflater.decompress(compressedPacket));
		}
		else
		{
		    packetSet = _server.getPackets(latest+1, Long.MAX_VALUE, _numPackets);
		}
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
			       (zipIt ? " " : " un") + "compressed" +
			       " packets from DeviceID " + _deviceId + " in " + duration +
			       " milliseconds.");
	}
	catch (ConnectException e) {
	    printFailure("Benchmark2Test", "deviceLogTest", "ConnectException");
	}
	catch (RemoteException e) {
	    printFailure("Benchmark2Test", "deviceLogTest", "RemoteException");
	}
	catch (Exception e) {
	    printFailure("Benchmark2Test", "deviceLogTest", e.toString());
	    e.printStackTrace();
	}
    }

    public void run(boolean zipIt) throws IOException
    {
	for (int i = 0; i < _repetitions; i++)
	{
	    runTest(zipIt);
	    System.out.println("");

	    try {
		Thread.sleep(_sleepMillisec);
	    }
	    catch (InterruptedException e) {
	    }
	}
    }


    protected void printUsage()
    {
	System.err.println("usage: " + className() + " serverhostname deviceID directory [repetitions] [numPackets]");
    }


    public void parseCmdLine(String args[])
    {
	long deviceId = 0;
	int argnum = 0;

	if (args.length < 3) {
	    printUsage();
	    System.exit(1);
	}

	try {
	    deviceId = Long.parseLong(args[1]);
	} catch(NumberFormatException e) {
	    printUsage();
	    System.exit(1);
	}

	if (args.length >= 4)
	{
	    try {
		_repetitions = Integer.parseInt(args[3]);
	    } catch(NumberFormatException e) {
		System.err.println("Can't parse number of repetitions.  Using " + _repetitions);
	    }
	}

	if (args.length >= 5)
	{
	    try {
		_numPackets = Integer.parseInt(args[4]);
	    } catch(NumberFormatException e) {
		System.err.println("Can't parse size of DevicePacketSet.  Using " + _numPackets);
	    }
	}

	try {
	    _server = setServer(args[0]);
	} catch (Exception e) {
	    e.printStackTrace();
	    System.exit(1);
	}

	try {
	    _nEntries = setLog(deviceId, args[2]);
	}
	catch (Exception e) {
	    _log4j.error("Could not open log for deviceId " + deviceId + " in directory " + args[2] + e);
	    e.printStackTrace();
	    System.exit(1);
	}

	if (_nEntries <= 0)
	{
	    _log4j.error("No log data for deviceId " + deviceId + " in directory " + args[2]);
	    System.exit(1);
	}
    }
}
