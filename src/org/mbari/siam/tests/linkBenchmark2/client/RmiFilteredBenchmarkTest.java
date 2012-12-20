/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.linkBenchmark2.client;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.*;
import org.mbari.siam.tests.linkBenchmark2.interfaces.*;
import java.rmi.server.RMISocketFactory;
import org.mbari.siam.utils.SiamSocketFactory;
import org.apache.log4j.Logger;


/** 
    Benchmark2Test that uses RMI for communication with server.
*/
public class RmiFilteredBenchmarkTest extends RmiBenchmarkTest
{
    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(RmiFilteredBenchmarkTest.class);

    protected String className()
    {
	return "RmiFilteredBenchmarkTest";
    }

    public int setLog(long deviceId, String directory)
      throws IOException, FileNotFoundException
    {
	if (_server == null)
	    throw new IOException("No server found!");

	_deviceId = deviceId;
	_nEntries = _server.setFilteredDeviceLog(deviceId, directory);
	_log4j.debug("Opened log file for deviceId " + deviceId + " in directory " + directory
		     + ", nEntries = " + _nEntries);
	return(_nEntries);
    }

    public static void main(String args[])
    {
	RmiFilteredBenchmarkTest test = new RmiFilteredBenchmarkTest();

	test.parseCmdLine(args);

	try {
	    test.run(false);
	} catch (IOException e) {
	    _log4j.error("IOException in test.run(): " + e);
	    e.printStackTrace();
	}
    }

}
