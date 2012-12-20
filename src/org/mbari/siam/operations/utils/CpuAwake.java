/****************************************************************************/
/* Copyright 2004 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/** @author Bob Herlien							    */
/****************************************************************************/
package org.mbari.siam.operations.utils;

import java.rmi.RemoteException;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.server.RMISocketFactory;
import java.io.IOException;
import java.net.MalformedURLException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;
import org.mbari.siam.utils.SiamSocketFactory;
import org.mbari.siam.distributed.Node;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;

/** Utility that the user can invoke to keep the CPU awake for a duration,
    or to cause it to wake up at a future time.  The usage is:
    <p>
    cpuAwake <when> <howLong> [-r rqstID] [-n] [nodeURL]
    <p>
    <when> - seconds in future that you'll need CPU awake
    <p>
    <howLong> - how many seconds that you'll need CPU awake
    <p>
    [-r rqstID] - requestor ID.  Defaults to 1, the user ID
    <p>
    [-n] - don't retry if fail to find <nodeURL>.  Useful for
    local scripts.
    <p>
    [nodeURL] - node you want awake.  Defaults to "localhost"
*/

public class CpuAwake
{
    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(CpuAwake.class);

    private final static int RQST_USER = 1;
    private final static int RQST_SHORTHAUL = 2;
    private final static long DFLT_DURATION = 600;

    static void usage()
    {
	System.err.println("Usage:");
	System.err.println("cpuAwake <when> <howLong> [-r rqstID] [nodeURL]");
	System.err.println(
	  "    <when> - seconds in future that you'll need CPU awake");
	System.err.println(
	  "    <howLong> - how many seconds that you'll need CPU awake");
	System.err.println(
	  "    [-r rqstID] - requestor ID.  Defaults to 1, the user ID");
	System.err.println(
	  "    [nodeURL] - node you want awake.  Defaults to \"localhost\"");
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

	int	rqstID = RQST_USER;
	long	when = 0;
	long	howLong = DFLT_DURATION;
	boolean getRqId = false;
	boolean doRetry = true;
	long[]	parms = {0, 0};
	int	nparms = 0;
	boolean printedUserMessage = false;
	boolean userInterrupted = false;
	String	nodeURL = "localhost";
	Node	node = null;

        //need to setSecurityManager 
        if ( System.getSecurityManager() == null )
            System.setSecurityManager(new SecurityManager());
        
	for (int i = 0; i < args.length; i++)
	{
	    String s = args[i].trim();
	    int val;

	    if (s.equalsIgnoreCase("-r"))
		getRqId = true;
	    else if (s.equalsIgnoreCase("-n"))
		doRetry = false;
	    else if (s.startsWith("//") || Character.isLetter(s.charAt(0)))
		nodeURL = s;
	    else if (getRqId || (nparms < 2))
		try
		{
		    val = Integer.parseInt(s);
		    if (getRqId)
		    {
			rqstID = val;
			getRqId = false;
		    }
		    else
			parms[nparms++] = val;
		}
		catch(NumberFormatException e) {
		    System.err.println("NumberFormatException: " + s);
		    e.printStackTrace();
		}
	    else
		System.err.println("Unknown parameter: " + s);
	}

	if (nparms >= 2)
	{
	    when = parms[0];
	    howLong = parms[1];
	}
	else if (nparms == 1)
	    howLong = parms[0];

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

	try			//Flush input stream
	{
	    while(System.in.available()> 0)
		System.in.read();
	} catch (IOException e) {
	}

	do
	{
	    try 
	    {
		if (!printedUserMessage)
		    System.out.println("Looking for node server stub at "
				       + nodeURL);

		node = (Node)Naming.lookup(nodeURL);

		System.out.println("Got proxy for node service \"" + 
				   new String(node.getName()) + "\"");

		node.cpuLease(rqstID, 1000*when, 1000*howLong);
		System.out.println("Got CPU lease for " + howLong +
				   " seconds, starting in " + when +
				   " seconds");
		System.exit(0);
	    }
	    catch (Exception e) 
	    {
		if (!(e instanceof RemoteException) || (!printedUserMessage))
		    System.err.println(e);
		if (!printedUserMessage)
		{
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
		Thread.sleep(1000);
	    } catch (Exception e) {
	    }
	} while(doRetry && !userInterrupted);
    }
}
