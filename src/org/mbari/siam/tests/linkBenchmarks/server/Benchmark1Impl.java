/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.linkBenchmarks.server;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.RMISecurityManager;
import java.rmi.Naming;
import java.rmi.server.UnicastRemoteObject;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.MalformedURLException;
import java.io.IOException;
import org.mbari.siam.tests.linkBenchmarks.interfaces.*;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;

public class Benchmark1Impl extends UnicastRemoteObject
    implements Benchmark1 {

    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(Benchmark1Impl.class);

    public Benchmark1Impl() throws RemoteException {

	super();
        System.out.println("Done with constructor.");
    }

    public void emptyTest() {
	// Don't do anything!
	System.out.println("emptyTest()");
	//	System.out.print("#");
    }

    public long primitiveTest1(short a, double b, long retvalue) {
	System.out.println("primitiveTest1()");
	return retvalue;
    }


    public Struct1 structTest1(byte fillValue) {
	System.out.println("structTest1()");
	Struct1 s = new Struct1();
        s.x = fillValue;
	s.y = fillValue;
	s.z = fillValue;
	for (int i = 0; i < s.array.length; i++) {
	    s.array[i] = fillValue;
	}
	return s;
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
	    System.err.println("Usage: Benchmark1Impl hostname");
	    return;
	}


	if (System.getSecurityManager() == null) {
	    System.out.println("Setting security manager");
	    System.setSecurityManager(new RMISecurityManager());
	}

	try {
	    System.out.println("InetAddress.getLocalHost.getHostName(): " + 
			       InetAddress.getLocalHost().getHostName());
	}
	catch (Exception e) {
	    _log4j.error("Got exception while getting host name");
	}
	try {
	    System.out.println("InetAddress.getLocalHost.getHostAddres(): " + 
			       InetAddress.getLocalHost().getHostAddress());
	}
	catch (Exception e) {
	    _log4j.error("Got exception while getting host address");
	}

	// Start rmiregistry
	try {
	    System.out.print("Starting registry... ");
	    LocateRegistry.createRegistry(1099);
	    System.out.println("registry started.");
	}
	catch (RemoteException e) {
	    // Already running on port 1099?
	    System.err.println(e.getMessage());
	}

	try {

	    System.out.println("Constructing server...");

	    Benchmark1Impl server = new Benchmark1Impl();
	    System.out.println("server: " + server);

	    String url = "//localhost/benchmark1";
	    
	    System.out.println("Binding server to " + url + "...");

	    Naming.rebind(url, server);

	    System.out.println("Benchmark1Impl is bound to " + url);
	}
	catch (Exception e) {
	    System.err.println("Exception: " + e.getMessage());
	    e.printStackTrace();
	}
    }

}
