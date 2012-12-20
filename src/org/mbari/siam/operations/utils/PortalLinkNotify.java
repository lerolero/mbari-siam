/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.utils;

import java.io.IOException;
import java.net.URL;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import org.mbari.siam.distributed.portal.PortalInterface;
import org.mbari.siam.distributed.portal.Portals;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;

/**
Simple RMI utility to talk to the local portal <b>ONLY!</b>
to notify it that the comms link is coming up or going down

@author Bob Herlien
 */
public class PortalLinkNotify
{

    static protected Logger _log4j = Logger.getLogger(PortalLinkNotify.class);
    private static final String _releaseName = new String("$Name: HEAD $");

    public PortalLinkNotify()
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

	System.out.println(_releaseName);

	// Set security manager, if not already set.
        if ( System.getSecurityManager() == null ) {
            System.setSecurityManager(new SecurityManager());
	}
    }

    /** Process command line options. The first command line argument
     is always the node server's URL or hostname. */
    public void run(String[] args)
    {
	InetAddress remoteAddr = null;
	PortalInterface portal = null;
	boolean on = false;
	String name;

	try
	{
	    remoteAddr = InetAddress.getByName(args[0]);
	} catch (UnknownHostException e) {
	    _log4j.error("Bad internet address.  Exiting." + e);
	    e.printStackTrace();
	    return;
	} catch (Exception e) {
	    _log4j.error("Exception in InetAddress.getByName()." + e);
	    e.printStackTrace();
	    return;
	}

	String s = args[1].toLowerCase();
	if (s.equals("on") || s.equals("true") || s.equals("up"))
	    on = true;
	else if (s.equals("off") || s.equals("false") || s.equals("down"))
	    on = false;
	else
	{
	    _log4j.error("Must specify \"true\" or \"false\"");
	    return;
	}

	name = Portals.portalURL("localhost");

	try
	{
	    portal = (PortalInterface)Naming.lookup(name);
	} catch (Exception e) {
	    _log4j.error("Exception in Naming.lookup(" + name + "). " + e);
	    e.printStackTrace();
	    return;
	}
	try
	{
	    portal.nodeLinkNotify(remoteAddr, on);
	} catch (Exception e) {
	    _log4j.error("Exception in portal.nodeLinkNotify()." + e);
	    e.printStackTrace();
	    return;
	}
    }

    public static void main(String[] args) {
	new PortalLinkNotify().run(args);
    }
}
