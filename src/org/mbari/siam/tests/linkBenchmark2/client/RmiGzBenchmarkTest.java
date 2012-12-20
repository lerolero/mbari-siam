/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.linkBenchmark2.client;

import java.io.IOException;
import org.apache.log4j.Logger;


/** 
    Benchmark2Test that uses compression and RMI for communication with server.
*/
public class RmiGzBenchmarkTest extends RmiBenchmarkTest
{
    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(RmiGzBenchmarkTest.class);

    protected String className()
    {
	return "RmiGzBenchmarkTest";
    }

    public static void main(String args[])
    {
	RmiGzBenchmarkTest test = new RmiGzBenchmarkTest();

	test.parseCmdLine(args);

	try {
	    test.run(true);
	} catch (IOException e) {
	    _log4j.error("IOException in test.run(): " + e);
	    e.printStackTrace();
	}
    }

}
