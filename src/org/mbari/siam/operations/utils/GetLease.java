// Copyright MBARI 2004
package org.mbari.siam.operations.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.RMISocketFactory;

import org.mbari.siam.distributed.leasing.LeaseDescription;
import org.mbari.siam.distributed.leasing.LeaseRefused;
import org.mbari.siam.utils.SiamSocketFactory;

import org.mbari.siam.distributed.Node;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;

/** Establish, renew, or terminate a communications lease with the Node */
public class GetLease
{
    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(GetLease.class);

    private final static int OP_ESTABLISH = 0;
    private final static int OP_RENEW = 1;
    private final static int OP_TERMINATE = 2;
    private final static int LEASE_TIME = 900;	//Default lease is 15 min.
    private final static String[] opStrings = 
        {"-establish", "-renew", "-terminate"};

    static void usage()
    {
	System.out.println("Usage:");
	System.out.println("lease -establish <time> [-aux]       [nodeURL],  OR");
	System.out.println("lease -renew <leaseID> <time> [-aux] [nodeURL],  OR");
	System.out.println("lease -terminate <leaseID> [-aux]    [nodeURL]");
	System.out.println("-aux: use auxilliary shore link (e.g., redundant globalstar)");
	System.out.println();
	System.out.println("Defaults to \"lease -establish 600 //localhost/node\"");
    }

    public static void main(String[] args) 
    {
	/*
	 * Set up a simple configuration that logs on the console. Note that
	 * simply using PropertyConfigurator doesn't work unless JavaBeans
	 * classes are available on target. For now, we configure a
	 * PropertyConfigurator, using properties passed in from the command
	 * line, followed by BasicConfigurator which sets default console
	 * appender, etc.
	 */
	PropertyConfigurator.configure(System.getProperties());
	PatternLayout layout = new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");
	BasicConfigurator.configure(new ConsoleAppender(layout));
	//Logger.getRootLogger().setLevel((Level)Level.INFO);

	int operation = OP_ESTABLISH;
	int leaseId = 0;
	int leaseTime = LEASE_TIME;
	String nodeURL = "//localhost/node";
	Node node = null;
	boolean gotLeaseID = false;
	boolean printedUserMessage = false;
	boolean userInterrupted = false;
	boolean isPrimary=true;

	//Additions for getLeases() utility.  KAS 2/11/05
	// Get the user name
	String username;
	username = System.getProperty("user.name");
	
	// Get the host name
	String hostName;
	String clientNote;
	try {
		InetAddress localhost = InetAddress.getLocalHost();
		hostName = localhost.getHostName();
	} catch (Exception e) {
		hostName = "";
		System.err.println("Error Determining Hostname");
		System.exit(1);
	}
	
	clientNote = "user " + username + "@" + hostName;
		

		
        //need to setSecurityManager 
        if ( System.getSecurityManager() == null )
            System.setSecurityManager(new SecurityManager());

        if(args.length==0){
	    usage();
	    System.exit(0);
	}

	for (int i = 0; i < args.length; i++)
	{
	    String s = args[i].trim();
	    int val;

	    if (s.equalsIgnoreCase("--help"))
		usage();
	    else if (s.equalsIgnoreCase("-establish"))
		operation = OP_ESTABLISH;
	    else if (s.equalsIgnoreCase("-renew"))
		operation = OP_RENEW;
	    else if (s.equalsIgnoreCase("-terminate") 
		     || s.equalsIgnoreCase("-cancel"))
		operation = OP_TERMINATE;
	    else if (s.startsWith("//") || 
		     Character.isLetter(s.charAt(0)) ||
		     (i==(args.length-1)))
		nodeURL = s;
	    else if (s.equalsIgnoreCase("-aux"))
	        isPrimary=false;
	    else try
	    {
		val = Integer.parseInt(s);
		if ((operation != OP_ESTABLISH) && !gotLeaseID)
		{
		    leaseId = val;
		    gotLeaseID = true;
		}
		else{
		    leaseTime = val;
		}
	    }
	    catch(NumberFormatException e) {
	    }
	}

	nodeURL = NodeUtility.getNodeURL(nodeURL);


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


	if ((operation != OP_ESTABLISH) && !gotLeaseID)
	{
	    System.err.println("Need a Lease ID!");
	    usage();
	    System.exit(1);
	}

	try			//Flush input stream
	{
	    while(System.in.available()> 0)
		System.in.read();
	} catch (IOException e) {
	}

	while(!userInterrupted)
	{
	    try 
	    {
		if (!printedUserMessage)
		    System.out.println("Looking for node server stub at "
				       + nodeURL);

		node = (Node )Naming.lookup(nodeURL);

		System.out.println("Got proxy for node service \"" + 
				   new String(node.getName()) + "\"");

		switch(operation)
		{	
	            case GetLease.OP_ESTABLISH:
			System.out.println("Executing 'lease -establish " +
					   leaseTime + " " + nodeURL);

			// Try to establish lease.
			leaseId = node.establishLease(1000*leaseTime,
						      clientNote.getBytes(),isPrimary);
			System.out.println("Success: Lease ID = " + leaseId);
			break;

	            case OP_RENEW:
			System.out.println("Executing 'lease -renew " +
					   + leaseId + " " + leaseTime +
					   " " + nodeURL);
			node.renewLease(leaseId, 1000*leaseTime,isPrimary);
			System.out.println("Success");
			break;

	            case OP_TERMINATE:
			System.out.println("Executing 'lease -terminate " +
					   + leaseId + " " + nodeURL);
			node.terminateLease(leaseId,isPrimary);
			System.out.println("Success");
			break;
		}

		System.exit(0);
	    }
	    catch (LeaseRefused e) {
		System.err.println(e.getMessage());
		break;
	    }
	    catch (Exception e) {
		if (!(e instanceof RemoteException) || (!printedUserMessage)) {
		    System.err.println(e);
		}
		if (!printedUserMessage) {
		    System.out.println(nodeURL + 
				       " is not active or link is down");
		    System.out.println("Will continue to retry.");
		    System.out.println("Hit any key to stop");
		    printedUserMessage = true;
		}
	    }


	    try
	    {
		if (System.in.available() > 0)
		    userInterrupted = true;
		Thread.sleep(2000);
	    } catch (Exception e) {
	    }
	}
    }
}
