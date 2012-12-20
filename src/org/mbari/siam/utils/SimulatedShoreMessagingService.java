/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils;

import java.util.Iterator;
import java.rmi.RemoteException;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.registry.LocateRegistry;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.mbari.siam.distributed.ShoreMessaging;
import org.mbari.siam.distributed.ShoreMessagingHelper;

/** Simulated implementation of ShoreMessaging */
public class SimulatedShoreMessagingService 
    extends ShoreMessagingService 
    implements ShoreMessaging {

    protected static final int MAX_DOWNLINK_MSG_BYTES = 1960;

    public SimulatedShoreMessagingService() throws RemoteException {
	super();
    }


    public synchronized void connect(int timeoutSec) 
	throws ShoreMessagingHelper.ModemInUse {

	while (!_downlinkMsgQ.empty()) {

	    try {
		ShoreMessagingHelper.Message message = 
		    (ShoreMessagingHelper.Message )_downlinkMsgQ.popFront();

	    _log4j.info("Downlink msg: " + 
			 new String(message.getBytes()));
	    }
	    catch (Queue.EmptyQueue e) {
		_log4j.error("connect(): downlinkMsgQ is empty!");
	    }
	}
	_log4j.info("done with simulated connect()");
    }


    public int maxDownlinkMsgBytes() {
	return MAX_DOWNLINK_MSG_BYTES;
    }


    public static void main(String[] args) {

	PatternLayout layout = 
	   new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");

	BasicConfigurator.configure(new ConsoleAppender(layout));

	// Set security manager, if not already set.
        if ( System.getSecurityManager() == null ) {
	    System.out.println("Setting RMI security manager");
            System.setSecurityManager(new RMISecurityManager());
	}

	SimulatedShoreMessagingService service = null;

	try {
	    service = new SimulatedShoreMessagingService();
	}
	catch (Exception e) {
	    _log4j.error("Got exception from service constructor:", e);
	    return;
	}

	// Start rmiregistry
	try {
	    _log4j.info("Starting registry... ");
	    LocateRegistry.createRegistry(1099);
	    _log4j.info("registry started.");
	}
	catch (RemoteException e) {
	    // Already running on port 1099?
	    _log4j.info(e.getMessage());
	}

	// Bind to localhost, so bind() succeeds in absence of
	// network connection.
	String url = "//localhost/" + 
	    ShoreMessagingHelper.SERVICE_NAME;

	_log4j.info("rebinding NodeService to " + url);

	try {
	    Naming.rebind(url, service);
	    _log4j.info("service is bound to " + url);
	}
	catch (Exception e) {
	    _log4j.error("Caught exception binding service: ", e);
	}

    }

}
