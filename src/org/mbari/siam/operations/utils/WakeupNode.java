// Copyright MBARI 2005
package org.mbari.siam.operations.utils;

import java.net.InetAddress;
import java.util.Vector;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.MOOSNode;


/** 
    Wakeup specified node(s), by invoking the Node.wakeupNode() or 
    Node.wakeupNodes() method on the specified node.
*/
public class WakeupNode extends NodeUtility {

    private Vector _subnodes = new Vector();

    public static void main(String[] args) {
	WakeupNode wakeup = new WakeupNode();
	wakeup.processArguments(args);
	wakeup.run();
    }

    /** Trailing arguments specify nodes to be awakened. */
    public void processCustomOption(String[] args, int index) 
	throws InvalidOption {
	// Trailing options are nodes to be awakened
	for (int i = 1; i < args.length; i++) {
	    try {
		_subnodes.add(InetAddress.getByName(args[i]));
	    }
	    catch (Exception e) {
		System.err.println("Unknown node: " + args[i]);
	    }
	}
    }

    /** Print usage message. */
    public void printUsage() {
	System.err.println("WakeupNode nodeURL [subnode1_IP][subnode2_IP]...");
    }

    /** Wakeup specified subnodes, using specified node. If no subnodes 
	have been specified, wake up all of 'em. */
    public void processNode(Node node) {
	MOOSNode moosNode = (MOOSNode )node;
	if (_subnodes.size() > 0) {
	    // Wakeup each specified subnode
	    for (int i = 0; i < _subnodes.size(); i++) {
		InetAddress subnode = (InetAddress )_subnodes.elementAt(i);
		try {
		    System.out.println("processNode() - wakeup " + subnode);
		    moosNode.wakeupNode(subnode);
		}
		catch (Exception e) {
		    System.err.println("Caught exception from node.wakeup()");
		    System.err.println(e);
		}
	    }
	}
	else {
	    // No subnodes specified; wake up all of them.
	    try {
		System.out.println("processNode() - wakeupAllnodes");
		moosNode.wakeupAllNodes();
	    }
	    catch (Exception e) {
		System.err.println(e);
	    }
	}
    }
}

