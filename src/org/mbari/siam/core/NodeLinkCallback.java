/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.MOOSNode;
import org.mbari.siam.operations.utils.NodeUtility;

/**
   Notifies NodeService of shore link connect/disconnect.
 */
public class NodeLinkCallback extends NodeUtility {

    boolean _linkUp = false;
    InetAddress _localAddress;
    InetAddress _remoteAddress;
    String _interfaceName;
    String _serialName;
    String dummy;

    public void processCustomOption(String[] args, int index) 
	throws InvalidOption {

	try {
	    switch (index) {

	    case 1:
		_interfaceName = args[index];
		break;

	    case 2:
		_serialName = args[index];
		break;

	    case 3:
		dummy = args[index];  // speed
		break;

	    case 4:
		_localAddress = InetAddress.getByName(args[index]);
		break;

	    case 5:
		_remoteAddress = InetAddress.getByName(args[index]);
		break;

	    case 6:
		dummy = args[index];  // "Name" (e.g. "mbari")
		break;

	    default:
		throw new InvalidOption("invalid option: " + args[index]);
	    }
	}
	catch (UnknownHostException e) {
	    throw new InvalidOption("\nUnknown host: " + e.getMessage());
	}

    }

    /** Get and print node status. */
    public void processNode(Node node) 
	throws Exception {

	MOOSNode moosNode = (MOOSNode )node;

	if (_linkUp) {
	    moosNode.shoreLinkUpCallback(_interfaceName, _serialName, 
					 _localAddress, _remoteAddress);
	}
	else {
	    moosNode.shoreLinkDownCallback(_interfaceName, _serialName, 
					 _localAddress, _remoteAddress);
	}
    }

    /** Print usage message. */
    public void printUsage() {
	System.err.println("NodeLinkCallback nodeURL interface serialName speed localAddress remoteAddress name");
    }

    public static void main(String[] args) {


	NodeLinkCallback app = new NodeLinkCallback();
	if (args.length != 7) {
	    app.printUsage();
	    return;
	}

	app.processArguments(args);
	app.run();
    }

}
