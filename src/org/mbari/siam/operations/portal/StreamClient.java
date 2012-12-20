// Copyright 2002 MBARI
package org.mbari.siam.operations.portal;

import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.RMISecurityManager;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.Authentication;
import org.mbari.siam.distributed.AuthenticationException;
import org.mbari.siam.distributed.DevicePacketStream;
// import org.mbari.siam.distributed.portal.StreamServerProxy;
import org.mbari.siam.distributed.portal.PortalProxy;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;

/**
StreamClient tests the Portal's DevicePacketStream interface.
@author Tom O'Reilly
*/
public class StreamClient {

    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(StreamClient.class);

    PortalProxy _portal;

    public StreamClient(String portalName, String portalHostName) 
	throws ConnectException, 
	       MalformedURLException, 
	       NotBoundException,
	       RemoteException {

	String url = "//" + portalHostName + "/" + portalName;

	_log4j.debug("Looking for portal server stub at " + url);
	_portal = (PortalProxy )Naming.lookup(url);
	_log4j.debug("Connected to portal");
    }


    void run() {

	// Get DevicePacketStream (Authentication ignored for now)
	Authentication authentication = new Authentication();

	DevicePacketStream stream;

	try {
	    _log4j.debug("portal.getDevicePacketStream()...");
	    stream = _portal.getDevicePacketStream(authentication);
	    _log4j.debug("Got DevicePacketStream()");
	}
	catch (RemoteException e) {
	    _log4j.error("RemoteException: " + e.getMessage());
	    return;
	}
	catch (AuthenticationException e) {
	    _log4j.error("AuthenticationException: " + e.getMessage());
	    return;
	}

	int nReceived = 0;

	while (true) {
	    try {
		// Read the next DevicePacket
		_log4j.debug("wait for packet; stream.read()");
		DevicePacket packet = stream.read();
		// Process the packet
		printPacket(packet);
		_log4j.debug("Received " + ++nReceived + " packets");
	    }
	    catch (IOException e) {
		_log4j.error("IOException: " + e.getMessage());
		e.printStackTrace();
		break;
	    }
	    catch (ClassNotFoundException e) {
		_log4j.error("ClassNotFoundException: " + 
				   e.getMessage());

		break;
	    }
	}
    }

    void printPacket(DevicePacket packet) {

	System.out.println("src: " + packet.sourceID() + 
			   " time: " + packet.systemTime() + 
			   " seqNo: " + packet.sequenceNo());

	if (packet instanceof SensorDataPacket) {

	    SensorDataPacket dataPacket = 
		(SensorDataPacket )packet;

	    System.out.println("data: ");
	    System.out.println(new String(dataPacket.dataBuffer()));
	    System.out.println("");
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

	if (System.getSecurityManager() == null) {
	    _log4j.debug("Setting security manager");
	    System.setSecurityManager(new RMISecurityManager());
	}

	if (args.length != 2) {
	    System.err.println("Usage: StreamClient portalName portalHost");
	    return;
	}

	String portalName = args[0];
	String portalHost = args[1];

	try {
	    StreamClient client = new StreamClient(portalName, portalHost);
	    client.run();
	}
	catch (ConnectException e) {
	    _log4j.error("ConnectException: " + e.getMessage());
	}
	catch (RemoteException e) {
	    _log4j.error("RemoteException: " + e.getMessage());
	}
	catch (NotBoundException e) {
	    _log4j.error("NotBoundException: " + e.getMessage());
	}
	catch (MalformedURLException e) {
	    _log4j.error("MalformedURLException: " + e.getMessage());
	}
    }
}

