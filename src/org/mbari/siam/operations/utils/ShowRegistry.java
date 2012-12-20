// Copyright MBARI 2008
package org.mbari.siam.operations.utils;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.Vector;
import java.text.DateFormat;
import java.rmi.Naming;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.PortNotFound;
import org.mbari.siam.distributed.DeviceNotFound;

/**
   Print properties of specified instrument.
 */
public class ShowRegistry extends NodeUtility
{
    /** Print InstrumentRegistry status */
    public void processNode(Node node) throws RemoteException
    {
	System.out.println(new String(node.instrumentRegistryStatus()));
    }


    /** Print usage message. */
    public void printUsage() {
	System.out.println("usage: ShowRegistry nodeURL");
    }

    /** No custom options for this application. */
    public void processCustomOption(String[] args, int index) 
	throws InvalidOption {
	throw new InvalidOption("Invalid option: " + args[index]);
    }


    public static void main(String[] args) {

	ShowRegistry app = new ShowRegistry();
	app.processArguments(args);
	app.run();
    }
}
