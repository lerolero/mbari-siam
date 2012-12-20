/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.MOOSNode;
import org.mbari.siam.operations.utils.NodeUtility;

/**
   Notifies NodeService of shore link connect/disconnect.
 */
public class NodePropertySetter extends NodeUtility {

    Properties _settings = new Properties();

    public void processCustomOption(String[] args, int index) 
	throws InvalidOption {

	ByteArrayInputStream inputStream = 
	    new ByteArrayInputStream(args[index].getBytes());

	System.out.println("args[" + index + "]=" + args[index]);

	try {
	    _settings.load(inputStream);
	}
	catch (IOException e) {
	    throw new InvalidOption("Error while parsing property " + args[index]);
	}

    }

    /** Set specified node properties */
    public void processNode(Node node) 
	throws Exception {

	if (_settings.size() == 0) {
	    System.err.println("No properties specified");
	    return;
	}

	ByteArrayOutputStream output = 
	    new ByteArrayOutputStream();

	_settings.store(output, "");

	node.setProperties(output.toByteArray());
    }


    /** Print usage message. */
    public void printUsage() {

	System.out.println("usage: " + 
			   "NodePropertySetter nodeURL " + 
			   "key=value <key=value> <key=value>...");

    }

    public static void main(String[] args) {


	NodePropertySetter app = new NodePropertySetter();

	app.processArguments(args);
	app.run();
    }

}
