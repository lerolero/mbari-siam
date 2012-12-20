/****************************************************************************/
/* Copyright 2006 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.operations.utils;

import java.rmi.RemoteException;
import java.util.Date;
import java.text.DateFormat;
import java.rmi.Naming;
import java.net.InetAddress;
import org.mbari.siam.utils.PrintfFormat;
import org.mbari.siam.core.NodeService;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Port;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.DeviceNotFound;


public class SafeMode extends NodeUtility {

/** Enter Safe Mode operations. */
   public void processNode(Node node)
	throws Exception {
	node.enterSafeMode();
        }

/** Main method. */
   public static void main(String[] args) {
	SafeMode status = new SafeMode();
	status.processArguments(args);
	status.run();
   } //end of main

/** Print usage message. */
   public void printUsage() {
	System.err.println("usage: SafeMode nodeURL");
   }

} //end of SafeMode class