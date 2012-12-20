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
public class RmiBenchmarkTest extends Benchmark2Test
{
    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(RmiBenchmarkTest.class);

    protected String className()
    {
	return "RmiBenchmarkTest";
    }

    protected Benchmark2 setServer(String serverName) throws Exception
    {
	Benchmark2 server = null;

	_log4j.debug("Try to get SecurityManager...");

	if (System.getSecurityManager() == null) {
	    _log4j.debug("No SecurityManager; set new one...");
	    System.setSecurityManager(new RMISecurityManager());
	}

	try {
	    RMISocketFactory.setSocketFactory(new SiamSocketFactory(serverName));
	}
	catch (IOException e) {
	    System.err.println("RMISocketFactory.setSocketFactory() failed");
	    System.err.println(e);
	    throw e;
	}

	try {
	    InetAddress inetAddr = InetAddress.getByName(serverName);
	    _log4j.debug(serverName + " = " + inetAddr.getHostAddress());
	}
	catch (UnknownHostException e) {
	    System.err.println("Couldn't get host: " + e.getMessage());
	    throw e;
	}
	String url = "//" + serverName + Benchmark2.RMI_SERVER_NAME;
	_log4j.debug("Looking for stub at " + url);

	try {
	    server = (Benchmark2)Naming.lookup(url);
	    _log4j.debug("Found it!");
	}
	catch (Exception e) {
	    System.err.println("Couldn't find server: " + e.getMessage());
	    throw e;
	}

	return(server);
    }

    public static void main(String args[])
    {
	RmiBenchmarkTest test = new RmiBenchmarkTest();

	test.parseCmdLine(args);

	try {
	    test.run(false);
	} catch (IOException e) {
	    _log4j.error("IOException in test.run(): " + e);
	    e.printStackTrace();
	}
    }

}
