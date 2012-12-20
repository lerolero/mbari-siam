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
   Renew the WDT (actually a Command Loss Timer)
   The WDT resets the node after a 24 hr communication
   outage. Renew WDT "drugs the dog", causing it to
   remain in a sleeping state.
   This utility is invoked every time a ppp connection
   is established.
 */
public class RenewWDT extends NodeUtility {

    public static void main(String[] args) {

	RenewWDT renew = new RenewWDT();
	renew.processArguments(args);
	renew.run();

    }

    /** No custom options for this application. */
    public void processCustomOption(String[] args, int index) 
	throws InvalidOption {

	// No custom options for this application... so throw 
	// invalid option exception if this method is called.

	throw new InvalidOption("unknown option");
    }

    /** Print usage message. */
    public void printUsage() {
	System.err.println("RenewWDT nodeURL");
    }

    /** Sample specified port. */
    public void processNode(Node node) 
	throws RemoteException {
	try{
	    MOOSNode moosNode = (MOOSNode )node;
	    byte[] msg = moosNode.renewWDT();
	    System.out.println(new String(msg));
	}catch(FileNotFoundException f){
	    f.printStackTrace();
	}catch(IOException i){
	    i.printStackTrace();
	}

    }
}
