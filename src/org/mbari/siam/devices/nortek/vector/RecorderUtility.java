/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.nortek.vector;

import java.rmi.RemoteException;

import org.mbari.siam.operations.utils.PortStatus;
import org.mbari.siam.operations.utils.PortUtility;

import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.Node;

/**
 * Nortek Aquadopp recorder utility. Includes method to erase recorder.
 * @author oreilly
 *
 */
public class RecorderUtility extends PortUtility {

    public static void main(String[] args) {
    	RecorderUtility utility = new RecorderUtility();
    	utility.processArguments(args);
    	utility.run();
        }

	/** Print usage */
	public void printUsage() {
	   // No special arguments
	}
	
	/** Do application-specific processing of specified port. */
    public void processPort(Node node, String portName)
	    throws RemoteException {
    	
    	Device device = null;
    	
    	try {
    	    device = node.getDevice(portName.getBytes());
    	}
    	catch (Exception e) {
    		System.err.println("Got exception while trying to get service on port " +
    				portName);
    		return;
    	}
    	if (!(device instanceof AquadoppIF)) {
    		System.err.println("Service on port " + portName + " is not an Aquadopp");
    		return;
    	}
    	
    	try {
    		((AquadoppIF )device).eraseRecorder();
    	}
    	catch (Exception e) {
    		System.err.println("Caught exception while trying to erase recorder: " +
    				e);
    	}
    	
    }


}
