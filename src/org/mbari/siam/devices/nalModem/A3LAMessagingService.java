/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.nalModem;

import java.util.Iterator;
import java.rmi.RemoteException;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.registry.LocateRegistry;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.mbari.siam.distributed.ShoreMessaging;
import org.mbari.siam.distributed.ShoreMessagingHelper;

/** Simulated implementation of ShoreMessaging */
public class A3LAMessagingService 
    extends NALMessagingService 
    implements ShoreMessaging {

    protected static final int MAX_DOWNLINK_MSG_BYTES = 1960;
    protected static final int MAX_UPLINK_MSG_BYTES = 1890;

    public A3LAMessagingService(String serialPortName) 
	throws Exception {
	super(serialPortName);
    }


    /** Return maximum bytes in downlinked message */
    public int maxDownlinkMsgBytes() {
	return MAX_DOWNLINK_MSG_BYTES;
    }


    public static void main(String[] args) {

	if (args.length < 2) {
	    System.err.println("usage: serverhost serialPort");
	    return;
	}

	String serverHost = args[0];
	String serialPortName = args[1];
	
	PropertyConfigurator.configure(System.getProperties());

	PatternLayout layout = 
	   new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");

	BasicConfigurator.configure(new ConsoleAppender(layout));

	// Set security manager, if not already set.
        if ( System.getSecurityManager() == null ) {
	    System.out.println("Setting RMI security manager");
            System.setSecurityManager(new RMISecurityManager());
	}

	A3LAMessagingService service = null;

	try {
	    service = new A3LAMessagingService(serialPortName);
	}
	catch (Exception e) {
	    _log4j.error("Error from service constructor: " + e);
	    return;
	}

	try {
	    ShoreMessagingHelper.bind(service);
	    _log4j.info("service is bound");
	}
	catch (Exception e) {
	    _log4j.error("Error binding service: ", e);
	}

    }

}
