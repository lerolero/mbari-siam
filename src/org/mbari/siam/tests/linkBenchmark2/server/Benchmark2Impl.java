/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.linkBenchmark2.server;

import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.RMISecurityManager;
import java.rmi.Naming;
import java.rmi.server.UnicastRemoteObject;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.MalformedURLException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import org.mbari.siam.tests.linkBenchmark2.interfaces.*;
import org.mbari.siam.tests.linkBenchmark2.compression.*;
import org.mbari.siam.core.DeviceLog;
import org.mbari.siam.core.FilteredDeviceLog;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DevicePacketSet;
import org.mbari.siam.distributed.NoDataException;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;

public class Benchmark2Impl extends UnicastRemoteObject
    implements Benchmark2 {

    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(Benchmark2Impl.class);

    /** DeviceLog that we're going to get data from */
    protected DeviceLog _log = null;

    public Benchmark2Impl() throws RemoteException {

	super();

	PropertyConfigurator.configure(System.getProperties());
	PatternLayout layout = new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");
	BasicConfigurator.configure(new ConsoleAppender(layout));

        System.out.println("Done with constructor.");
    }

    protected String className()
    {
	return "Benchmark2Impl";
    }

    public int setDeviceLog(long sensorId, String directory)
	throws RemoteException, IOException, FileNotFoundException
    {
	_log = new DeviceLog(sensorId, directory);
	return(_log.nPackets());
    }

    public int setFilteredDeviceLog(long sensorId, String directory)
	throws RemoteException, IOException, FileNotFoundException
    {
	_log = new FilteredDeviceLog(sensorId, directory);
	return(_log.nPackets());
    }

    public DevicePacketSet getPackets(long startKey, long endKey, int maxEntries) 
	throws NoDataException, RemoteException
    {
	if (_log == null)
	    throw new NoDataException("No log opened");

	return(_log.getPackets(startKey, endKey, maxEntries));
    }

    public byte[] getCompressedPackets(long startKey, long endKey, int maxEntries) 
	throws NoDataException, InvalidClassException, NotSerializableException, IOException, RemoteException
    {
	if (_log == null)
	    throw new NoDataException("No log opened");

	ObjectGZipper deflater = new ObjectGZipper();

	return(deflater.compress(_log.getPackets(startKey, endKey, maxEntries)));
    }

    protected static void setupRegistry()
    {
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
    }


    public static void main(String args[])
    {
	if (args.length != 1) {
	    System.err.println("Usage: Benchmark2Impl hostname");
	    return;
	}

	setupRegistry();

	try {

	    System.out.println("Constructing server...");

	    Benchmark2Impl server = new Benchmark2Impl();
	    System.out.println("server: " + server);

	    String url = "//localhost" + Benchmark2.RMI_SERVER_NAME;
	    
	    System.out.println("Binding server to " + url + "...");

	    Naming.rebind(url, server);

	    System.out.println("Benchmark2Impl is bound to " + url);
	}
	catch (Exception e) {
	    System.err.println("Exception: " + e.getMessage());
	    e.printStackTrace();
	}
    }

}
