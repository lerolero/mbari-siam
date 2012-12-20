/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

import java.net.InetAddress;
import java.util.Date;
import java.io.Serializable;
import java.text.SimpleDateFormat;

/**
   Basic information about node and its subnodes
 */
public class NodeInfo implements Serializable {

    /** Ethernet address of node */
    public InetAddress _ethernetAddress;

    /** ISI ID of node */
    public long _nodeID;

    /** Time node service started (msec since epoch) */
    public long _nodeStartTime;

    /** Array of subnodes */
    public Subnode[] _subnodes;


    public NodeInfo(InetAddress ethernetAddress, 
		    long nodeID, Subnode[] subnodes, long startTime) {

	_ethernetAddress = ethernetAddress;
	_nodeID = nodeID;
	_nodeStartTime = startTime;
	_subnodes = subnodes;
    }


    public String toString() {

	SimpleDateFormat dateFormatter = 
	    new SimpleDateFormat("MM/dd/yyyy HH:mm:ss z");

	StringBuffer buf = 
	    new StringBuffer(_ethernetAddress.toString() + "\n");

	buf.append("device ID: " + _nodeID + "\n");
	buf.append("node started at " + 
		   dateFormatter.format(new Date(_nodeStartTime)) + "\n");

	buf.append("\nFound " + _subnodes.length + " subnodes\n");
	for (int i = 0; i < _subnodes.length; i++) {
	    buf.append(_subnodes[i].toString() + "\n\n");
	}

	return new String(buf);
    }
}
