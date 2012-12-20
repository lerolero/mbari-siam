// Copyright MBARI 2004
package org.mbari.siam.operations.utils;

import java.rmi.RemoteException;
import java.util.Date;
import java.text.DateFormat;
import java.rmi.Naming;
import java.net.InetAddress;
import org.mbari.siam.utils.PrintfFormat;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Port;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.DeviceNotFound;


public class SubNodes extends NodeUtility {

/** Do not accept any "custom" arguments. */
   public void processCustomOption(String[] args, int index)
	throws InvalidOption {
	throw new InvalidOption("Invalid option: " + args[index]);
   }


/** Get and print subNode(s) status. */
   public void processNode(Node node)
	throws Exception {
	InetAddress[] subnodes = node.getSubnodes();
	System.out.println("Found " + subnodes.length + " Subnodes");
	for(int i=0; i<subnodes.length; i++) {
	   System.out.println("SubNode " + i + ": " + subnodes[i]);
        }
   }

/** Print usage message. */
   public void printUsage() {
	System.err.println("usage: SubNode nodeURL");
   }


/** Main method. */
   public static void main(String[] args) {
	SubNodes status = new SubNodes();
	status.processArguments(args);
	status.run();
   } //end of main

} //end of SubNodes class

