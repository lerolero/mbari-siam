// Copyright 2002 MBARI
package org.mbari.siam.operations.portal;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.RMISecurityManager;
import javax.rmi.PortableRemoteObject;
import java.net.UnknownHostException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.text.DateFormat;
import java.util.Date;
import java.util.Vector;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.DevicePacketStream;
import org.mbari.siam.distributed.DevicePacketOutputStream;
import org.mbari.siam.distributed.DevicePacketServerThread;
import org.mbari.siam.distributed.Authentication;
import org.mbari.siam.distributed.AuthenticationException;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.portal.StreamServerProxy;

/**
Test generation of DevicePackets and writing to DevicePacketOutputStream
@author Tom O'Reilly
*/
public class StreamServer 
    extends PortableRemoteObject implements StreamServerProxy {

    boolean _debug = true;
    DevicePacketServerThread _packetServer;
    InetAddress _localHost;

    // DevicePackets are served from this port
    final static int _packetServerPort = 4444;


    /**
       Construct PortalServer for specified platform.
    */
    public StreamServer() 
	throws RemoteException, UnknownHostException, IOException {

	// Create thread which listens for DevicePacketStream
	// requests.
	_packetServer = new DevicePacketServerThread(_packetServerPort);
	_packetServer.start();

	_localHost = InetAddress.getLocalHost();
    }


    /** Get DevicePacketStream for all sensors. */
    public DevicePacketStream getDevicePacketStream(Authentication auth) 
    throws AuthenticationException {

	return new DevicePacketStream(_localHost, _packetServerPort);
    }

    /** Get DevicePacketStream for specified sensor. */
    public DevicePacketStream getDevicePacketStream(long sensorID, 
						    Authentication auth) 
    throws DeviceNotFound, AuthenticationException {

	return new DevicePacketStream(_localHost, _packetServerPort);
    }



    public void run() {

	Vector packets = new Vector();
	int nPackets = 20;
	long sleepTime = 45000;

	while (true) {

	    packets.clear();

	    System.out.println("Generating " + nPackets + " packets");

	    for (int i = 0; i < nPackets; i++) {
		SensorDataPacket packet = 
		    new SensorDataPacket(9999, 100);

		packets.add(packet);
	    }

	    distributeData(packets);

	    try {
		Thread.sleep(sleepTime);
	    }
	    catch (InterruptedException e) {
	    }
	}

    }


    /**
       Distribute retrieved DevicePackets to DevicePacketStream clients.
    */
    void distributeData(Vector packets) {

	System.out.println("# stream clients: " + 
			   _packetServer._clients.size());

	boolean valid[] = new boolean[_packetServer._clients.size()];

	// Give all packets to all clients:
	// Selective distribution not yet implemented!
	for (int i = 0; i < _packetServer._clients.size(); i++) {

	    DevicePacketOutputStream client = 
		(DevicePacketOutputStream )_packetServer._clients.get(i);

	    System.out.println("Distributing " + packets.size() + 
			       " packets to client");

	    for (int j = 0; j < packets.size(); j++) {

		DevicePacket packet = (DevicePacket )packets.get(j);
		try {
		    client.write(packet);
		    valid[i] = true;
		}
		catch (IOException e) {

		    valid[i] = false;

		    System.err.println("Caught IOException from " + 
				       "DevicePacketStream client");
		    System.err.println(e.getMessage());
		    break;
		}
	    }
	}

	// Remove invalid clients. 
	// Note that removing a vector element decrements the index
	// of subsequent elements.
	int nInvalid = 0;
	for (int i = 0; i < valid.length; i++) {
	    if (!valid[i]) {
		System.out.println("Removing invalid client...");
		_packetServer._clients.remove(i - nInvalid);
		nInvalid++;
	    }
	}
    }


    public static void main(String args[]) {

	if (System.getSecurityManager() == null) {
	    System.out.println("Setting security manager");
	    System.setSecurityManager(new RMISecurityManager());
	}

	if (args.length != 1) {
	    System.err.println("Usage: StreamServer hostname");
	    return;
	}

	String thisHost = args[0];

	try {

	    System.out.println("Constructing NMCPortalServer...");

	    StreamServer server = new StreamServer();

	    String url = "//" + thisHost + "/streamServer";
	    
	    System.out.println("Binding StreamServer to " + url + "...");

	    Naming.rebind(url, server);

	    System.out.println("StreamServer is bound to " + url);

	    server.run();
	}
	catch (Exception e) {
	    System.err.println("Exception: " + e.getMessage());
	    e.printStackTrace();
	}
    }

}


