/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Vector;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.MOOSNode;
import org.mbari.siam.operations.utils.NodeUtility;

/**
   Notifies NodeService of shore link connect/disconnect.
 */
public class NodePropertyGetter extends NodeUtility {


    public void processCustomOption(String[] args, int index) 
	throws InvalidOption {
	throw new InvalidOption("Invalid option: " + args[index]);
    }

    /** Get and print node properties */
    public void processNode(Node node) 
	throws Exception {

	Vector properties = node.getProperties();

	for (int j = 0; j < properties.size(); j++) {
	    byte[] property = (byte[] )properties.elementAt(j);
	    System.out.println(new String(property));
	}
	System.out.println("");

    }


    /** Print usage message. */
    public void printUsage() {

	System.out.println("usage: " + 
			   "NodePropertyGetter nodeURL"); 
    }


    public static void main(String[] args) {


	NodePropertyGetter app = new NodePropertyGetter();

	app.processArguments(args);
	app.run();
    }

}
