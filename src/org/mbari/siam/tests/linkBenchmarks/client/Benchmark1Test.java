/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.linkBenchmarks.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.*;
import org.mbari.siam.tests.linkBenchmarks.interfaces.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.rmi.server.RMISocketFactory;
import org.mbari.siam.utils.SiamSocketFactory;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;

public class Benchmark1Test {

    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(Benchmark1Test.class);

    Benchmark1 _server = null; 
    SimpleDateFormat _dateFormatter;

    public Benchmark1Test(Benchmark1 server) {

	_server = server;

	_dateFormatter = 
	    new SimpleDateFormat("yyyy.DDD/HH:mm:ss");

	_dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));

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

	
    public void run() {

	long start = 0;
	long duration = 0;

	try {
	    start = System.currentTimeMillis();
	    _server.emptyTest();
	    duration = System.currentTimeMillis() - start;
	    printSuccess("Benchmark1Test", "emptyTest", duration);
	}
	catch (ConnectException e) {
	    printFailure("Benchmark1Test", "emptyTest", "ConnectException");
	}
	catch (RemoteException e) {
	    printFailure("Benchmark1Test", "emptyTest", "RemoteException");
	}
	catch (Exception e) {
	    printFailure("Benchmark1Test", "emptyTest", e.toString());
	    e.printStackTrace();
	}

	try {
	    short a = 0;
	    double b = 0.;
	    long c = -1;

	    start = System.currentTimeMillis();
	    long ret = _server.primitiveTest1(a, b, c);
	    duration = System.currentTimeMillis() - start;
	    printSuccess("Benchmark1Test", "primtiveTest1", duration);
	    // System.out.println("return val = " + ret);
	}
	catch (Exception e) {
	    printFailure("Benchmark1Test", "primitiveTest1", e.getMessage());
	}


	try {
	    byte fillValue = 100;
	    start = System.currentTimeMillis();
	    Struct1 struct1 = _server.structTest1(fillValue);
	    duration = System.currentTimeMillis() - start;
	    printSuccess("Benchmark1Test", "structTest1", duration);
	    // _log4j.debug("struct1.x=" + struct1.x);
	    // _log4j.debug("struct1.y=" + struct1.y);
	    // _log4j.debug("struct1.z=" + struct1.z);
	}
	catch (Exception e) {
	    printFailure("Benchmark1Test", "structTest1", e.getMessage());
	}
    }

    public static void main(String args[]) {
	/*
	 * Set up a simple configuration that logs on the console. Note that
	 * simply using PropertyConfigurator doesn't work unless JavaBeans
	 * classes are available on target. For now, we configure a
	 * PropertyConfigurator, using properties passed in from the command
	 * line, followed by BasicConfigurator which sets default console
	 * appender, etc.
	 */
	PropertyConfigurator.configure(System.getProperties());
	PatternLayout layout = new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");
	BasicConfigurator.configure(new ConsoleAppender(layout));
	//Logger.getRootLogger().setLevel((Level)Level.INFO);

	if (args.length != 1) {
	    System.err.println("usage: Benchmark1Test serverhostname");
	    System.exit(1);
	}

	long sleepMillisec = 10;

	_log4j.debug("Try to get SecurityManager...");

	if (System.getSecurityManager() == null) {
	    _log4j.debug("No SecurityManager; set new one...");
	    System.setSecurityManager(new RMISecurityManager());
	}

	try {
	    RMISocketFactory.setSocketFactory(new SiamSocketFactory(args[0]));
	}
	catch (IOException e) {
	    System.err.println("RMISocketFactory.setSocketFactory() failed");
	    System.err.println(e);
	}

	Benchmark1 server = null;

	try {
	    InetAddress inetAddr = InetAddress.getByName(args[0]);
	    _log4j.debug(args[0] + " = " + inetAddr.getHostAddress());
	}
	catch (UnknownHostException e) {
	    System.err.println("Couldn't get host: " + e.getMessage());
	    return;
	}
	String serverHost = args[0];
	String url = "//" + serverHost + "/benchmark1";
	_log4j.debug("Looking for stub at " + url);

	try {
	    server = (Benchmark1 )Naming.lookup(url);
	    _log4j.debug("Found it!");
	}
	catch (Exception e) {
	    System.err.println("Couldn't find server: " + e.getMessage());
	    return;
	}

	Benchmark1Test test = new Benchmark1Test(server);

	while (true) {
	    test.run();
	    System.out.println("");

	    try {
		Thread.sleep(sleepMillisec);
	    }
	    catch (InterruptedException e) {
	    }
	}
    }
}
