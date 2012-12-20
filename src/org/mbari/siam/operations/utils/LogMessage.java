// Copyright MBARI 2003
package org.mbari.siam.operations.utils;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.Vector;
import java.text.DateFormat;
import java.rmi.Naming;
import java.text.ParseException;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.MOOSNode;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.PortNotFound;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.utils.PrintUtils;

/**
   LogMessage: Send a text message to be logged in the specified node's 
   logged data stream.
 */
public class LogMessage extends NodeUtility {
    String _message=null;
    public static void main(String[] args) {

	LogMessage renew = new LogMessage();
	renew.processArguments(args,2);
	renew.run();

    }

    /** No custom options for this application. */
    public void processCustomOption(String[] args, int index) 
	throws InvalidOption {

	switch(index){
	case 0:
	    // ignore arg 0
	    return;
	case 1:
	// arg 1 is message
	    _message=args[index];
	    return;
	default:
	    // other options invalid
	    break;
	}

	// No other custom options for this application... so throw 
	// invalid option exception if this method is called.

	throw new InvalidOption("unknown option at index "+index+": "+args[index]);
    }

    /** Print usage message. */
    public void printUsage() {
	System.err.println("\n");
	System.err.println("logMessage: enter message into specified node's data stream\n");
	System.err.println("Usage: logMessage nodeURL \"<message>\"");
	System.err.println("  message: message text");
	System.err.println("\n");
    }

    /** Sample specified port. */
    public void processNode(Node node) 
	throws RemoteException {
	try{
	    MOOSNode moosNode = (MOOSNode )node;
	    moosNode.annotate(_message.getBytes());
	    System.out.println("message sent");
	}catch(IOException i){
	    i.printStackTrace();
	}

    }
}
