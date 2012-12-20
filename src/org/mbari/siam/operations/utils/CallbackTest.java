// Copyright MBARI 2003
package org.mbari.siam.operations.utils;

import java.rmi.RemoteException;
import java.rmi.server.RMISocketFactory;
import java.util.Date;
import java.text.DateFormat;
import java.rmi.Naming;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.NodeEventCallback;
import org.mbari.siam.utils.NodeEventCallbackService;
import org.mbari.siam.distributed.NodeEventListener;
import org.mbari.siam.utils.PrintfFormat;
import org.mbari.siam.utils.SiamSocketFactory;

public class CallbackTest implements NodeEventListener {


    /** Called when a service is terminated. */
    public void serviceTerminated(long deviceID) {
	System.out.println("Got termination event for device " + deviceID);
    }

    /** Called when a service is started. */
    public void serviceStarted(long deviceID) {
	System.out.println("Got start event for device " + deviceID);
    }

    /** Called when a service changes state. */
    public void serviceChanged(long deviceID) {
	System.out.println("Got change event for device " + deviceID);
    }


    public static void main(String[] args) {

	// Configure Log4J
	PropertyConfigurator.configure(System.getProperties());
	BasicConfigurator.configure();

	Node nodeService = null;

        //need to setSecurityManager 
        if ( System.getSecurityManager() == null )
            System.setSecurityManager(new SecurityManager());

	if (args.length < 1) {
	    System.err.println("Usage: nodeURL");
	    System.exit(1);
	}

        String nodeURL = NodeUtility.getNodeURL(args[0]);


	// Create socket factory; overcomes problems with RMI 'hostname'
	// property.
	try {
	    String host = NodeUtility.getHostName(nodeURL);
	    RMISocketFactory.setSocketFactory(new SiamSocketFactory(host));
	}
	catch (MalformedURLException e) {
	    System.err.println("Malformed URL \"" + nodeURL + "\": " + 
			       e.getMessage());
	}
	catch (IOException e) {
	    System.err.println("RMISocketFactory.setSocketFactory() failed");
	    System.err.println(e);
	}

	try {
	    System.out.println("Looking for node service at " + nodeURL);

	    nodeService = (Node )Naming.lookup(nodeURL);

	    System.out.println("Got proxy for node service \"" + 
			       new String(nodeService.getName()) + "\"");

	}
	catch (Exception e) {
	    System.err.println("Caught exception: " + e.getMessage());
	    System.err.println("Couldn't get service at \"" + nodeURL + "\"");
	    System.exit(1);
	}

	CallbackTest test = new CallbackTest();

	try {
	    System.out.println("create callback service object");
	    NodeEventCallbackService callbackService = 
		new NodeEventCallbackService();

	    System.out.println("add listener");
	    callbackService.addListener(test);
	    NodeEventCallback callback = callbackService;

	    System.out.println("nodeService.addEventCallback");
	    nodeService.addEventCallback(callback);
	}
	catch (RemoteException e) {
	    System.err.println("RemoteException: " + e.getMessage());
	}
	catch (Exception e) {
	    System.err.println("Got some exception: " + e.getMessage());
	    System.exit(1);
	}

	while (true) {
	    try {
		Thread.sleep(1000);
	    }
	    catch (InterruptedException e) {
	    }
	}

    }
}
