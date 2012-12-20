/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.linkBenchmark2.client;

import java.io.IOException;
import org.apache.log4j.Logger;


/** 
    Benchmark2Test that uses compression and raw sockets for communication with server.
*/

public class SocketGzBenchmarkTest extends SocketBenchmarkTest
{
    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(SocketGzBenchmarkTest.class);

    protected SocketBenchmarkProxy _proxy = null;

    protected String className()
    {
	return "SocketGzBenchmarkTest";
    }

    public static void main(String args[])
    {
	SocketGzBenchmarkTest test = new SocketGzBenchmarkTest();

	test.parseCmdLine(args);

	try {
	    test.run(true);
	    test.testExit();
	} catch (IOException e) {
	    _log4j.error("IOException in test.run(): " + e);
	    e.printStackTrace();
	}
    }

}
