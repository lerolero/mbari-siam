// Copyright MBARI 2003
package org.mbari.siam.operations.utils;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.Vector;
import java.text.DateFormat;
import java.rmi.Naming;
import org.apache.log4j.BasicConfigurator;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.PortNotFound;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.utils.PrintUtils;
import org.mbari.siam.distributed.devices.Environmental;

/**
   Reset compass turns counter on specified node's environmental processor
 */
public class ResetTurnsCounter extends PortUtility {

    int _nTurns = 0;

    /** If service on specified port is Environmental, reset the turns
     counter to specified value. */
    public void processPort(Node node, String portName) 
	throws RemoteException {
	try {
	    Device device = node.getDevice(portName.getBytes());
	    if (device instanceof Environmental) {
		Environmental environmental = (Environmental )device;

		environmental.resetTurnsCounter(_nTurns);
	    }
	    else {
		System.err.println("Device on port " + portName + 
				   " is not an Environmental");
	    }
	}
	catch (PortNotFound e) {
	    System.err.println("Port " + portName + " not found");
	}
	catch (DeviceNotFound e) {
	    System.err.println("Device not found on port " + portName);
	}
    }

    /** Process command-line arguments. */
    public void processArguments(String[] args, int requiredArgs) {

	if (args.length != 3) {
	    printUsage();
	    System.exit(1);
	}

	_nodeURL = NodeUtility.getNodeURL(args[0]);
	_portNames.add(PortUtility.getPortName(args[1]));

	try {
	    _nTurns = Integer.parseInt(args[2]);
	}
	catch (NumberFormatException e) {
	    System.err.println("Invalid nTurns: " + args[2]);
	    System.exit(1);
	}
    }

    /** Print usage message. */
    public void printUsage() {
	System.out.println("usage: ResetTurnsCounter nodeURL portname nTurns");
    }

    public static void main(String[] args) {

	ResetTurnsCounter app = new ResetTurnsCounter();
	app.processArguments(args);
	app.run();
    }
}
