/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.portal.Portals;
import org.mbari.siam.distributed.Subnode;
import org.mbari.siam.distributed.MOOSSubnode;
import org.mbari.siam.distributed.MOOSNodeNotifyMessage;

/**
   SubnodeListener listens for contacts from subnodes, and maintains
   list of of subnodes.
 */
public class SubnodeListener extends Thread {
    
    static private Logger _log4j = Logger.getLogger(SubnodeListener.class);

    // Socket on which thread listens for subnode contacts
    protected ServerSocket _serverSocket = null;

    // Vector of subnode IP addresses
    protected Vector _subnodes = new Vector();

    public SubnodeListener() throws IOException {
	_log4j.debug("listening on port " + Portals.portalTCPPort());
	_serverSocket = new ServerSocket(Portals.portalTCPPort());
    }


    /** Listen for contacts by subnode; if subnode is not yet in 
     subnodes list, add it. */
    public void run() {

	_log4j.debug("SubnodeListener.run()");
	long deviceID = -1;
	InetAddress switchAddress = null;

	while (true) {
	    try {
		// Listen for subnode
		Socket socket = _serverSocket.accept();
		InetAddress subnodeAddress = socket.getInetAddress();

		MOOSNodeNotifyMessage nodeInfo = null;

		try {
		    nodeInfo = 
			(MOOSNodeNotifyMessage )(new ObjectInputStream(socket.getInputStream()).readObject());
		    
		    deviceID = nodeInfo._deviceID;
		    switchAddress = nodeInfo._switchCardCpuAddress;
		}
		catch (Exception e) {
		    _log4j.error("run() - got exception from readOject(): " + e);
		}

		_log4j.debug("Got msg from subnode " + subnodeAddress);

		// Check to see if this subnode is already in the list.
		// If not, add it to the list.
		boolean found = false;
		for (int i = 0; i < _subnodes.size(); i++) {
		    Subnode subnode = (Subnode )_subnodes.elementAt(i);
		    if (subnode.getAddress().equals(subnodeAddress)) {
			// Already in list
			found = true;
			// Update subnode contact time, other info
			subnode.update(System.currentTimeMillis(), nodeInfo);
			break;
		    }
		}

		if (!found) {
		    // Not yet in list; add it
		    _log4j.debug("Adding subnode " + subnodeAddress + 
				 " to list");
		    _subnodes.add(new MOOSSubnode(subnodeAddress, 
						  System.currentTimeMillis(), 
						  deviceID, switchAddress));
		}
	    }
	    catch (IOException e) {
		_log4j.error(e);
	    }
	}
    }

    /** Return vector of subnode IP addresses. */
    public Vector getSubnodes() {
	return _subnodes;
    }


    /** Remove specified subnode */
    public void removeSubnode(InetAddress address) throws Exception {
	boolean found = false;
	int i;
	for (i = 0; i < _subnodes.size(); i++) {
	    Subnode subnode = (Subnode )_subnodes.elementAt(i);
	    if (subnode.getAddress().equals(address)) {
		_log4j.debug("removeSubnode() - Found target node " + address);
		found = true;
		break;
	    }
	}
	if (!found) {
	    _log4j.debug("removeSubnode() - couldn't find target node " + address);
	    throw new Exception("Subnode '" + address + "' not found");
	}
	// Remove the specified element from the vector
	_subnodes.remove(i);
	
    }
}
