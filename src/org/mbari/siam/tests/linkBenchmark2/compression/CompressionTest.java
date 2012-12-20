/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.linkBenchmark2.compression;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import org.mbari.siam.core.DeviceLog;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DevicePacketSet;
import org.mbari.siam.distributed.NoDataException;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;

public class CompressionTest {

    /** Default number of packets to transfer in a DevicePacketSet */
    protected static final int DFLT_PACKETS = 100;

    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(CompressionTest.class);

    SimpleDateFormat _dateFormatter;
    long _deviceId = 0;
    int  _numPackets = DFLT_PACKETS;
    int  _nEntries = 0;
    DeviceLog _log = null;

    public CompressionTest()
    {
	PropertyConfigurator.configure(System.getProperties());
	PatternLayout layout = new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");
	BasicConfigurator.configure(new ConsoleAppender(layout));

	_dateFormatter = 
	    new SimpleDateFormat("yyyy.DDD/HH:mm:ss");

	_dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));

    }

    public int setDeviceLog(long sensorId, String directory)
	throws IOException, FileNotFoundException
    {
	_log = new DeviceLog(sensorId, directory);
	return(_log.nPackets());
    }

    public DevicePacketSet getPackets(long startKey, long endKey, int maxEntries) 
	throws NoDataException
    {
	if (_log == null)
	    throw new NoDataException("No log opened");

	return(_log.getPackets(startKey, endKey, maxEntries));
    }

    public void runTest()
	throws NoDataException, FileNotFoundException, SecurityException, IOException,
	       ClassNotFoundException, DataFormatException
    {
	DevicePacketSet packetSet = getPackets(0, Long.MAX_VALUE, _numPackets);

	String fileBase = Long.toString(_deviceId) + "." + Integer.toString(_numPackets);
	File origFile = new File(fileBase + ".orig");
	File compFile = new File(fileBase + ".compressed");
	File uncompFile = new File(fileBase + ".uncompressed");
	ObjectOutputStream origStream = new ObjectOutputStream(new FileOutputStream(origFile));
	ObjectOutputStream compStream = new ObjectOutputStream(new FileOutputStream(compFile));
	ObjectOutputStream uncompStream = new ObjectOutputStream(new FileOutputStream(uncompFile));
	ObjectGZipper deflater = new ObjectGZipper();
	ObjectGunzipper inflater  = new ObjectGunzipper();

	origStream.writeObject(packetSet);
	origStream.close();

	byte[] compObj = deflater.compress(packetSet);
	compStream.writeObject(compObj);
	compStream.close();

	Object obj = inflater.decompress(compObj);
	uncompStream.writeObject(obj);
	uncompStream.close();

	long compLen = compFile.length();
	long uncompLen = uncompFile.length();
	_log4j.info("Original: " + origFile.length() + "  compressed: " + compLen +
		     " uncompressed: " + uncompLen);
	_log4j.info("Compression:  " + (100.0 * (double)(uncompLen - compLen) / (double)uncompLen) + "%");
    }


    protected void printUsage()
    {
	System.err.println("usage: CompressionTest deviceID directory [packets]");
    }


    protected void parseCmdLine(String args[])
    {
	if (args.length < 2)
	{
	    printUsage();
	    System.exit(1);
	}

	try {
	    _deviceId = Long.parseLong(args[0]);
	} catch(NumberFormatException e) {
	    printUsage();
	    System.exit(1);
	}

	if (args.length >= 3)
	{
	    try {
		_numPackets = Integer.parseInt(args[2]);
	    } catch(NumberFormatException e) {
		System.err.println("Can't parse number of packets.  Using " + _numPackets);
	    }
	}

	try {
	    _nEntries = setDeviceLog(_deviceId, args[1]);
	}
	catch (Exception e) {
	    _log4j.error("Could not open log for deviceId " + _deviceId + " in directory " + args[1] + e);
	    e.printStackTrace();
	    System.exit(1);
	}

	if (_nEntries <= 0)
	{
	    _log4j.error("No log data for deviceId " + _deviceId + " in directory " + args[1]);
	    System.exit(1);
	}

	if (_nEntries < _numPackets)
	{
	    _log4j.info("Device log has only " + _nEntries + " packets.");
	    _numPackets = _nEntries;
	}

    } /* parseCmdLine() */


    public static void main(String args[])
    {
	CompressionTest test = new CompressionTest();

	test.parseCmdLine(args);

	try {
	    test.runTest();
	} catch (Exception e) {
	    _log4j.error("Exception in runTest(): " + e);
	    e.printStackTrace();
	    System.exit(1);
	}

    } /* main() */

}
