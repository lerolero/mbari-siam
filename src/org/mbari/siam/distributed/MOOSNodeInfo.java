/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

import java.text.SimpleDateFormat;
import java.net.InetAddress;
import java.util.Date;

public class MOOSNodeInfo extends NodeInfo {

    /** Address of switch (e.g. Medusa) */
    public InetAddress _switchAddress;

    public MOOSNodeInfo(InetAddress ethernetAddress,
			long nodeID, Subnode[] subnodes,
			long startTime, InetAddress switchAddress) {

	super(ethernetAddress, nodeID, subnodes, startTime);
	_switchAddress = switchAddress;
    }

    public String toString() {

	SimpleDateFormat dateFormatter = 
	    new SimpleDateFormat("MM/dd/yyyy HH:mm:ss z");

	StringBuffer buf = 
	    new StringBuffer(_ethernetAddress.toString() + "\n");

	if (_switchAddress != null) {
	    buf.append("switch addr: " + _switchAddress.toString() + "\n");
	}
	else {
	    buf.append("Unknown switch IP addr\n");
	}

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
