/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.bcast;

import java.net.InetAddress;
import org.mbari.siam.distributed.NetworkManager;
import org.mbari.siam.moos.deployed.MOOSNetworkManager;

public class NetworkManagerTest {

    void run(InetAddress targetNode) {

	NetworkManager manager = null;
	try {
	    System.out.println("Creating NetworkManager...");
	    manager = new MOOSNetworkManager("localhost");
	}
	catch (Exception e) {
	    System.err.println(e);
	    return;
	}

	try {
	    System.out.println("Send wakeup message for " + targetNode);
	    manager.wakeupNode(targetNode);
	}
	catch (Exception e) {
	    System.err.println(e);
	}
    }


    public static void main(String[] args) {

	if (args.length != 1) {
	    System.err.println("Usage: nodeName");
	    return;
	}

	NetworkManagerTest test = new NetworkManagerTest();

	InetAddress targetNode = null;

	try {
	    targetNode = InetAddress.getByName(args[0]);
	}
	catch (Exception e) {
	    System.err.println(e);
	}

	test.run(targetNode);
    }
}
