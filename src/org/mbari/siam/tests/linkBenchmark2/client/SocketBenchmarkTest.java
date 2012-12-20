/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.linkBenchmark2.client;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.Socket;
import org.mbari.siam.tests.linkBenchmark2.interfaces.*;
import org.apache.log4j.Logger;


/** 
    Benchmark2Test that uses raw sockets for communication with server.
*/

public class SocketBenchmarkTest extends Benchmark2Test
{
    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(SocketBenchmarkTest.class);

    protected SocketBenchmarkProxy _proxy = null;

    protected String className()
    {
	return "SocketBenchmarkTest";
    }

    /** Connect to the BenchmarkServer
    */
    protected Benchmark2 setServer(String serverName) throws Exception
    {
	_proxy = new SocketBenchmarkProxy();
	_proxy.connectNode(serverName, Benchmark2.SOCKET_TCP_PORT);
	return(_proxy);
    }

    public void testExit()
    {
	if (_proxy != null)
	    _proxy.disconnectNode();
    }

    public static void main(String args[])
    {
	SocketBenchmarkTest test = new SocketBenchmarkTest();

	test.parseCmdLine(args);

	try {
	    test.run(false);
	    test.testExit();
	} catch (IOException e) {
	    _log4j.error("IOException in test.run(): " + e);
	    e.printStackTrace();
	}
    }

}
