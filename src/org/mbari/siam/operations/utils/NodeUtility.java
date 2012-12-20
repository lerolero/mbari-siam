/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.utils;

import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.RMISocketFactory;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;

import org.mbari.siam.distributed.Node;
import org.mbari.siam.utils.SiamSocketFactory;


/**
NodeUtility forms the base class of an "application framework",
intended for SIAM RMI client applications. An application can be
built by extending NodeUtility and then implementing NodeUtility's
virtual methods. Note also that an application which is not a 
subclass of NodeUtility can use the useful getNodeURL() method,
which is public static.

@author Tom O'Reilly
 */
/*
  Test: expand CVS $Name: HEAD $
*/

public abstract class NodeUtility {

    static protected Logger _log4j = Logger.getLogger(NodeUtility.class);
    private static final String _releaseName = new String("$Name: HEAD $");

    /** URL of node server. */
    protected String _nodeURL;
    protected String _nodeHost;
    protected Node _node;
    /** minimum number of arguments required by a NodeUtility */
    static final int _NODE_UTILITY_REQUIRED_ARGS = 1;

    public NodeUtility() {

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

	System.out.println("SIAM version " + _releaseName);

	// Set security manager, if not already set.
        if ( System.getSecurityManager() == null ) {
            System.setSecurityManager(new SecurityManager());
	}
    }

    /** Process command line options. The first command line argument
     is always the node server's URL or hostname. */
    public void processArguments(String[] args)
    {
        processArguments(args, _NODE_UTILITY_REQUIRED_ARGS);
    }

    /** Process command line options and specify the minimum number of 
     arguments required. The first command line argument is always the 
     node server's URL or hostname. */
    public void processArguments(String[] args, int requiredArgs) {


	if (args.length < requiredArgs ) {
	    System.err.println("Invalid usage:");
	    printUsage();
	    System.exit(1);
	}

	boolean error = false;

	for (int i = 0; i < args.length; i++) {

	    if (i == 0) {
		// Get the node URL string representation
		_nodeURL = getNodeURL(args[0]);

		// Process next argument
		continue;
	    }
	    else {
		// Application-specific option
		try {
		    processCustomOption(args, i);
		}
		catch (InvalidOption e) {
		    System.err.println("Invalid option: " + e);
		    error = true;
		}
	    }
	}


	if (error) {
	    printUsage();
	    System.exit(1);
	}
    }


    /**
       Return SIAM node URL corresponding to input. Input may already
       be in URL format, in which case the input is simply returned. Input may
       also be abbreviated as just a hostname, in which case it is converted 
       a SIAM node RMI URL and returned.
    */
    public static final String getNodeURL(String input) {

	_log4j.debug("getNodeURL(): input=" + input);

	if (input.startsWith("rmi://")) {
	    return input;
	}
	else if (input.startsWith("//")) {
	    return "rmi:" + input;
	}
	else {
	    return "rmi://" + input + "/node";
	}
    }


    /** Return the 'hostname' portion of the node's URL. Note that this
	is a TOTAL hack, since Java's URL class does not support
	"rmi" as a valid protocol!!! */

    public static String getHostName(String nodeURL) 
	throws MalformedURLException {

	// NOTE: This method only works if the node's url starts with "rmi"!
	// Build a temporary valid URL by replacing "rmi" with "http"
	_log4j.debug("getHostName(" + nodeURL + ")");
	StringBuffer buf = new StringBuffer(nodeURL);
	if (nodeURL.startsWith("rmi:")) {
	    buf = new StringBuffer("http" + nodeURL.substring(3));
	}
        _log4j.debug("getHostName() - url string: " + new String(buf));
	URL url = new URL(new String(buf));
	_log4j.debug("getHostName() - hostname: " + url.getHost());
	return url.getHost();
    }


    /** Get node proxy and do application-defined processing. */
    public int run() {

	// Create socket factory; overcomes problems with RMI 'hostname'
	// property.
	try {
	    String host = getHostName(_nodeURL);
	    RMISocketFactory.setSocketFactory(new SiamSocketFactory(host));
	}
	catch (MalformedURLException e) {
	    System.err.println("Malformed URL \"" + _nodeURL + "\": " + 
			       e.getMessage());
	}
	catch (IOException e) {
	    System.err.println("RMISocketFactory.setSocketFactory() failed");
	    System.err.println(e);
	}


	// Get the node's proxy
	try {
	    _log4j.debug("Get node proxy from " + _nodeURL);
	    _node = (Node )Naming.lookup(_nodeURL.toString());
	    _log4j.debug("Got node proxy ");
	}
	catch (Exception e) {
	    System.err.println("Couldn't get node proxy at " + _nodeURL + ":");
	    System.err.println(e);
	    return(-1);
	}

	// Now do application-specific processing
	try 
        {
            processNode(_node);
	}
	catch (Exception e) 
        {
	    System.err.println("Caught exception: "+e);
	    e.printStackTrace();
	    return(-2);
	}
	return(0);
    }


    /** Process application-specific option. */
    public void processCustomOption(String[] args, int index)
	throws InvalidOption {
	if (args[index].equals("-help")) {
	    printUsage();
	}
    }

    /** Do application-specific processing of node. */
    public abstract void processNode(Node node) throws Exception;

    /** Print application-specific usage message to stdout. */
    public abstract void printUsage();


    /** InvalidOption exception indicates an invalid option was
	encountered. */
    public class InvalidOption extends Exception {

	public InvalidOption(String msg) {
	    super(msg);
	}
    }
}
