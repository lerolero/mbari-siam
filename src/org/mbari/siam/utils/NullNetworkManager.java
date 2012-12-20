/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils;

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
NullNetworkManager is a null implementation of org.mbari.isi.interfaces.NetworkManager
*/

public class NullNetworkManager implements NetworkManager {

    /** Create the NullNetworkManager. Specify name of local interface to MOOS 
	network (e.g. "mooring", "shore", etc). */
    public NullNetworkManager(String localInterfaceName) 
    {
    }

    /** Send wakeup signal to specified node. */
    public void wakeupNode(InetAddress node) throws IOException {
    }


    /** Send wakeup signals to all nodes. */
    public void wakeupAllNodes() throws IOException {
    }


    /** Keep specified node awake for specified duration. */
    public void keepNodeAwake(InetAddress node, int msec) 
	throws IOException {
    }
}
