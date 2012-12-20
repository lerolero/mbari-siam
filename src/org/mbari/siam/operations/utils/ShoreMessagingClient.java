/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.utils;

import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileInputStream;
import org.mbari.siam.distributed.ShoreMessaging;
import org.mbari.siam.distributed.ShoreMessagingHelper;
import org.mbari.siam.utils.NodeProbe;

/**
   ShoreMessagingClient performs various functions. Can queue messages with 
   ShoreMessaging service, connect the service to shore, shutdown the service.
   If service is not found, exits with status=2; otherwise status=0 (no errors)
   or status=1 (some other error).
*/
public class ShoreMessagingClient {

    final static int GENERAL_ERROR_EXIT = 1;
    final static int SERVICE_NOT_FOUND_EXIT = 2;
    final static int DEFAULT_TIMEOUT_SEC = 60;
    final static byte[] TRUNCATION_MSG = "<MESSAGE TRUNCATED>".getBytes();

    public static void main(String[] args) {

	boolean shutdown = false;
	boolean queueMessages = false;
	boolean queueFile = false;
	boolean connect = false;
	String fileName = null;
	boolean prependHeader = false;

	int timeoutSec = DEFAULT_TIMEOUT_SEC;

	boolean error = false;

	if (args.length == 0) {
	    error = true;
	    printUsage();
	    System.exit(GENERAL_ERROR_EXIT);
	}

	if (args[0].startsWith("-")) {
	    error = true;
	}

	// Service host is always first argument
	String serviceHost = args[0];

	// If service host is only argument, then send messages and connect
	if (args.length == 1) {
	    queueMessages = true;
	    connect = true;
	}

	for (int i = 1; i < args.length; i++) {
	    if (args[i].equals("-t") && i < args.length - 1) {
		// Get timeout value
		try {
		    timeoutSec = Integer.parseInt(args[++i]);
		}
		catch (Exception e) {
		    System.out.println("Invalid timeout value: " + args[i]);
		    error = true;
		}
	    }
	    else if (args[i].equals("-qi")) {
		queueMessages = true;
	    }
	    else if (args[i].equals("-c")) {
		connect = true;
	    }
	    else if (args[i].equals("-qf") && i < args.length -1) {
		queueFile = true;
		fileName = args[++i];
	    }
	    else if (args[i].equals("-x")) {
		shutdown = true;
	    }

	    else if (args[i].equals("-p")) {
		prependHeader = true;
	    }
	    else {
		error = true;
	    }
	}

	if (shutdown && (connect || queueMessages || queueFile)) {
	    System.out.println("-x (shutdown) not compatible with " + 
			       "-qi,-qf or -c options");
	    error = true;
	}

	if (error) {
	    printUsage();
	    System.exit(GENERAL_ERROR_EXIT);
	}


	// Set security manager, if not already set.
        if ( System.getSecurityManager() == null ) {
            System.setSecurityManager(new RMISecurityManager());
	}


	// Look for response on RMI port of service host
	try {
	    InetAddress serviceHostAddress = 
	    InetAddress.getByName(serviceHost);

	    int port = 1099;
	    NodeProbe nodeProbe = new NodeProbe();

	    if (!nodeProbe.probe(serviceHostAddress, 1099, 10000)) {
		System.err.println("No response from " + serviceHost + 
				   ", port " + port);

		System.exit(GENERAL_ERROR_EXIT);
	    }
	}
	catch (UnknownHostException e) {
	    System.err.println(serviceHost + ": unknown host");
	    System.exit(GENERAL_ERROR_EXIT);
	}
	catch (Exception e) {
	    System.err.println("Exception probing host " + serviceHost);
	    System.exit(GENERAL_ERROR_EXIT);
	}

	String url = 
	    "rmi://" + serviceHost + "/" + ShoreMessagingHelper.SERVICE_NAME;

	
	ShoreMessaging shoreMessaging = null;

	try {
	    System.out.println("Get service at " + url);
	    Object stub = Naming.lookup(url);
	    if (stub instanceof ShoreMessaging) {
		shoreMessaging = (ShoreMessaging )stub;
	    }
	    else {
		System.out.println("stub is not a ShoreMessaging object?");
		System.exit(GENERAL_ERROR_EXIT);
	    }
	    // Print queued downlink message count
	    try {
		int nQueued = shoreMessaging.nQueuedDownlinkMsgs();
		System.out.println(nQueued + " messages queued for downlink");
	    }
	    catch (Exception e) {
		System.out.println("Exception getting queued msg count: " + e);
	    }
	}
	catch (Throwable e) {
	    System.out.println("Error looking up service: " + e);
	    System.exit(SERVICE_NOT_FOUND_EXIT);
	}

	if (shutdown) {
	    shutdown(shoreMessaging);
	}

	if (queueMessages) {
	    queueMessages(shoreMessaging, prependHeader);
	}

	if (connect) {
	    connect(shoreMessaging, timeoutSec);
	}

	if (queueFile) {
	    try {
		queueFile(shoreMessaging, fileName, prependHeader);
	    }
	    catch (Exception e) {
		System.out.println("Error queueing file: " + e);
	    }
	}
    }


    /** Queue file contents as SBD message. Truncate message if too
	large to fit in a single SBD message.*/
    static void queueFile(ShoreMessaging shoreMessaging, String fileName,
			  boolean prependHeader) 
	throws Exception {

	File file = new File(fileName);
	if (!file.exists()) {
	    throw new Exception("File \"" + fileName + "\" not found");
	}

	if (!file.canRead()) {
	    throw new Exception("No permission to read file \"" + 
			       fileName + "\"");
	}


	byte[] msg = new byte[(int )file.length()];

	FileInputStream input = new FileInputStream(file);

	// Read file bytes
	for (int i = 0; i < (int )file.length(); i++) {
	    msg[i] = (byte )input.read();
	}

	// Close stream
	input.close();


	if (prependHeader) {
	    msg = ShoreMessagingHelper.prependHeader(msg, -1,
						     System.currentTimeMillis());
	}

	int maxMsgBytes = shoreMessaging.maxDownlinkMsgBytes();
	System.out.println("maxMsgBytes=" + maxMsgBytes);

	if (file.length() > maxMsgBytes) {

	    int nMsgBytes = maxMsgBytes - TRUNCATION_MSG.length;

	    byte[] msg2 = new byte[maxMsgBytes];

	    System.arraycopy(msg, 0, msg2, 0, nMsgBytes);

	    System.arraycopy(TRUNCATION_MSG, 0, 
			     msg2, nMsgBytes, 
			     TRUNCATION_MSG.length);

	    msg = msg2;
	}

	// Queue message for downlink
	System.out.println("Queue " + msg.length + 
			   " bytes for downlink - msg:\n" + new String(msg));

	shoreMessaging.queueDownlinkMessage(msg);
    }


    static void queueMessages(ShoreMessaging shoreMessaging,
			      boolean prependHeader) {

	try {
	    System.out.println("Maximum allowed downlink message bytes: " + 
			       shoreMessaging.maxDownlinkMsgBytes());
	}
	catch (Exception e) {
	    System.out.println("Exception while getting downlink msg limit: " + 
			       e);
	    return;
	}

	// Prompt user for downlink messages
	BufferedReader stdInput = 
	    new BufferedReader(new InputStreamReader(System.in));

	while (true) {

	    System.out.print("Enter message: " );

	    try {
		String line = stdInput.readLine();

		if (line.length() == 0) {
		    break;
		}

		byte[] fullMsg;
		if (prependHeader) {
		    fullMsg = 
			ShoreMessagingHelper.prependHeader(line.getBytes(),
							   -1, 
							   System.currentTimeMillis());
		}
		else {
		    fullMsg = line.getBytes();
		}

		shoreMessaging.queueDownlinkMessage(fullMsg);
	    }
	    catch (Exception e) {
		System.out.println("Exception reading user input: " + e);
		e.printStackTrace(System.out);
	    }
	}

    }


    static void connect(ShoreMessaging shoreMessaging, int timeoutSec) {

	// Check to see if there are any queued messages for downlink
	int nQueued = 0;
	try {
	    nQueued = shoreMessaging.nQueuedDownlinkMsgs();
	}
	catch (Exception e) {
	    System.out.println("Exception getting queued msg count: " + e);
	}

	if (nQueued <= 0) {
	    System.out.println("No queued messages; no need to connect");
	    return;
	}

	try {
	    System.out.println("Connecting modem...");
	    shoreMessaging.connect(timeoutSec);
	    System.out.println("connect() successful");
	}
	catch (Exception e) {
	    System.out.println("Exception connecting modem: " + e);
	}
    }


    static void shutdown(ShoreMessaging shoreMessaging) {

	// Check to see if there are any queued messages for downlink
	int nQueued = 0;
	try {
	    nQueued = shoreMessaging.nQueuedDownlinkMsgs();
	}
	catch (Exception e) {
	    System.out.println("Exception getting queued msg count: " + e);
	}

	if (nQueued > 0) {
	    System.out.println("Discarding " + nQueued + 
			       " queued downlink messages");
	}

	try {
	    System.out.println("shutting down service...");
	    shoreMessaging.shutdown();
	    System.out.println("service is shutting down");
	}
	catch (Exception e) {
	    System.out.println("Exception from shutdown(): " + e);
	}
    }



    static void printUsage() {
	System.err.println("usage:   serviceHost [options]");
	System.err.println("-qf file queue file contents for downlink");
	System.err.println("-qi      interactively prompt for messages");
	System.err.println("-p       prepend standard msg header");
	System.err.println("-c       connect server to shore");
	System.err.println("-x       shutdown server");
	System.err.println("-t sec   specify connect timeout in seconds " + 
			   "(default " + DEFAULT_TIMEOUT_SEC + ")");
    }

}
