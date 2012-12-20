/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.utils;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.Vector;
import org.mbari.siam.distributed.Node;

/** PortUtility is a NodeUtility that operates on a node's
    instrument ports. PortUtility is the base class of an "application
    framework"; to build an application, extend PortUtility and 
    implement PortUtility's virtual methods. Note that an application
    that is not subclassed from PortUtility can make use of the
    public static getPortName() method.
    @author Tom O'Reilly
*/
public abstract class PortUtility extends NodeUtility {

    /** minimum number of arguments required by a PortUtility */
    static final int _PORT_UTILITY_REQUIRED_ARGS = 2;

    protected Vector _portNames = new Vector();

    // Indicates if application can accept multiple ports in command line
    private boolean _multiPortsAllowed = true;

    /** Process command line options. The first command line argument
     is always the node server's URL or hostname, followed by one or
     more port specifiers. */
    public void processArguments(String[] args) 
    {
        processArguments(args, _PORT_UTILITY_REQUIRED_ARGS);
    }

    /** Set whether multiple ports can be specified on command line. */
    public void multiPortsAllowed(boolean allowed) {
	_multiPortsAllowed = allowed;
    }


    /** Process command line options and specify the minimum number of arguments 
     required. The first command line argument is always the node server's URL 
     or hostname, followed by one or more port specifiers. */
    public void processArguments(String[] args, int requiredArgs) {


	if (args.length < requiredArgs ) {
	    System.err.println("Invalid usage:");
	    printUsage();
	    System.exit(1);
	}

	boolean error = false;
	boolean inGenericArgs = true;

	for (int i = 0; i < args.length; i++) {

	    if (i == 0) {
		// Get the node URL string representation
		_nodeURL = getNodeURL(args[0]);

		// Process next argument
		continue;
	    }
	    
	    if (!_multiPortsAllowed && i >= _PORT_UTILITY_REQUIRED_ARGS) {
		inGenericArgs = false;
	    }

	    // Argument starting with a '-' indicates a non-generic
	    // option
	    if (args[i].charAt(0) == '-') {
		inGenericArgs = false;
	    }

	    if (inGenericArgs) {
		// Must be a port specifier
		_portNames.add(getPortName(args[i]));
	    }
	    else {
		// Custom option
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


    /** Do application-specific processing on each port. */
    public void processNode(Node node) {

	for (int i = 0; i < _portNames.size(); i++) {
	    String portName = (String )_portNames.elementAt(i);
	    
	    // Do application-defined processing for this port
	    try {
		processPort(_node, portName);
	    }
	    catch (RemoteException e) {
		System.err.println(e);
	    }
	}
    }


    /** 
	Return full port name corresponding to input, which may be abbreviated.
	If the input is simply an integer, the string "/dev/ttySX" + input is
	returned. If the input begins with "tty", the string "/dev/" + input is
	returned. If the input is not simply an integer string and the input 
	does not begin with "tty", it is assumed that the input is a full
	unabbreviated port name, and the input is returned as-is.
     */
    public static final String getPortName(String input) {

	try {
	    Integer.parseInt(input);
	    // Input just specifies an integer; prepend standard port prefix
	    // and return.
	    return "/dev/ttySX" + input;
	}
	catch (NumberFormatException e) {
	    // Input is not just an integer
	}

	if (input.startsWith("tty")) {
	    // Assume Linux-style port name; prepend "/dev/" and return
	    return "/dev/" + input;
	}

	// Assume full port name was specified; just return input as-is.
	return input;
    }


    /** Do application-specific processing of specified port. */
    public abstract void processPort(Node node, String portName)
	throws RemoteException;
}
