/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.SocketException;
import java.io.IOException;

/**
NetworkManager defines the methods to manage power and communications on the MOOS deployed network.
*/

public interface NetworkManager {

    /** Send wakeup signal to specified node. */
    public void wakeupNode(InetAddress node) throws IOException;

    /** Send wakeup signals to all nodes. */
    public void wakeupAllNodes() throws IOException;

    /** Keep specified node awake for specified duration. */
    public void keepNodeAwake(InetAddress node, int msec) 
	throws IOException;
    
}
