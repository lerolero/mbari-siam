// Copyright MBARI 2003
package org.mbari.siam.operations.utils;

import java.rmi.RemoteException;
import java.util.Date;
import java.text.DateFormat;
import java.rmi.Naming;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Port;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.utils.PrintfFormat;


public class NodeStatus extends NodeUtility {


    /** Don't accept any "custom" arguments. */
    public void processCustomOption(String[] args, int index) 
	throws InvalidOption {
	throw new InvalidOption("invalid option: " + args[index]);
    }

    /** Get and print node status. */
    public void processNode(Node node) 
	throws Exception {
	byte[] msg = node.getStatus(false);
	System.out.println(new String(msg));
    }

    /** Print usage message. */
    public void printUsage() {
	System.err.println("Nodestatus nodeURL");
    }

    public static void main(String[] args) {
	NodeStatus status = new NodeStatus();
	status.processArguments(args);
	status.run();
    }
}
