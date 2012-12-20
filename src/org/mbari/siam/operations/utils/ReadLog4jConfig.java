// Copyright MBARI 2003
package org.mbari.siam.operations.utils;

import java.rmi.RemoteException;
import java.rmi.Naming;
import org.mbari.siam.distributed.Node;

/**
	Read Log4J configuration.
	Loads/Re-loads logging configuratin from siam.log4j at runtime
 */
public class ReadLog4jConfig extends NodeUtility {

    public void processNode(Node node) 
	throws RemoteException {
	    node.readLog4jConfig();
	}

    public void printUsage() {
	System.out.println("usage: readLog4jConfig nodeURL");
    }

    public static void main(String[] args) {
	ReadLog4jConfig app = new ReadLog4jConfig();
	app.processArguments(args);
	app.run();
    }
}
