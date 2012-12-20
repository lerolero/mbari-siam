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
import org.mbari.siam.distributed.Subnode;


public class SubnodeDeleter extends NodeUtility {

    String _subnodeName;

    /** Do not accept any "custom" arguments. */
    public void processCustomOption(String[] args, int index)
	throws InvalidOption {
       if (index == args.length-1) {
	   _subnodeName = args[index];
       }
       else {
	   throw new InvalidOption(args[index] + ": Invalid option");
       }
   }


    /** Get and print subNode(s) status. */
    public void processNode(Node node)
	throws Exception {

	InetAddress address = InetAddress.getByName(_subnodeName);

	node.removeSubnode(address);
    }


    /** Print usage message. */
    public void printUsage() {
	System.err.println("usage: SubNode nodeURL");
    }


    /** Main method. */
    public static void main(String[] args) {
	SubnodeDeleter deleter = new SubnodeDeleter();
	deleter.processArguments(args);
	deleter.run();
    }

}

