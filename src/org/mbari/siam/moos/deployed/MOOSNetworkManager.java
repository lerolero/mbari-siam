/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.moos.deployed;

import java.net.InetAddress;
import java.net.Socket;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.UnknownHostException;
import java.net.SocketException;
import java.io.IOException;
import org.mbari.siam.core.CpuLease;
import org.mbari.siam.distributed.NetworkManager;
import org.apache.log4j.Logger;

/**
 * MOOSNetworkManager includes methods to manage power and communications on the MOOS deployed network.
*/

public class MOOSNetworkManager implements NetworkManager {

    private static Logger _log4j = Logger.getLogger(MOOSNetworkManager.class);

    protected DatagramSocket _dgSocket = null;
    public final static String BCAST_NAME = "255.255.255.255";
    protected final static int WAKEUP_SERVICE_PORT = 6789;
    protected final static int SO_TIMEOUT = 5000;
    protected InetAddress _bcastAddress = null;

    /** Create the NetworkManager. Specify name of local interface to MOOS 
	network (e.g. "mooring", "shore", etc). */
    public MOOSNetworkManager(String localInterfaceName) 
	throws UnknownHostException, SocketException {

	_dgSocket = new DatagramSocket();


	_bcastAddress = InetAddress.getByName(BCAST_NAME);

    }

    /** Send wakeup signal to specified node. */
    public void wakeupNode(InetAddress node) throws IOException {
	sendWakeupPacket(node);
    }


    /** Send wakeup signals to all nodes. */
    public void wakeupAllNodes() throws IOException {
	sendWakeupPacket(null);
    }


    /** Keep specified node awake for specified duration. */
    public void keepNodeAwake(InetAddress node, int msec) 
	throws IOException {

	_log4j.debug("keepNodeAwake() - get socket to " + node + " port " + 
		     CpuLease.CPULEASE_TCP_PORT);

	Socket socket = new Socket(node, CpuLease.CPULEASE_TCP_PORT);

	String cmd = "awake 3 0 " + msec + "\n";
	_log4j.debug("keepNodeAwake() - write: " + cmd);
	socket.getOutputStream().write(cmd.getBytes());
	_log4j.debug("keepNodeAwake() - flush and close");
	socket.getOutputStream().flush();
	socket.close();
	_log4j.debug("keepNodeAwake() - done");
    }
    
    /** Broadcast wakeup packet, with address of target node as payload. 
	If specified node is null, form packet 
	payload to indicate wakeup all nodes. */
    protected void sendWakeupPacket(InetAddress node) throws IOException {

	byte[] payload = null;

	if (node != null) {

	    String buf = node.getHostAddress() + "\0";
	    payload = buf.getBytes();
	    _log4j.debug("sendWakeupPacket(" + buf + ")");
	}
	else {
	    // No node specified; set payload to empty string, meaning
	    // wakeup all nodes.
	    payload = "".getBytes();
	    _log4j.debug("sendWakeupPacket() - wakeup all nodes");
	}

	DatagramPacket packet = 
	    new DatagramPacket(payload, payload.length, 
			       _bcastAddress, WAKEUP_SERVICE_PORT);

	// Send a set of bcast packets
	int nPackets = 5;
	_log4j.debug("sendWakeupPacket() - sending " + nPackets + " packets");
	for (int i = 0; i < nPackets; i++) {
	    _dgSocket.send(packet);
	}
    }


}
