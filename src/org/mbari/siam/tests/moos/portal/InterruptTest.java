// Copyright MBARI 2005
package org.mbari.siam.tests.moos.portal;

import java.net.InetAddress;
import java.util.Vector;
import java.rmi.Naming;
import java.rmi.server.RMISocketFactory;
import java.io.IOException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.mbari.siam.utils.SiamSocketFactory;

import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.MOOSNode;
import org.mbari.siam.operations.portal.PortalSocketFactory;

/** 
    Wakeup specified node(s), by invoking the Node.wakeupNode() or 
    Node.wakeupNodes() method on the specified node.
*/
public class InterruptTest {

    private Vector _subnodes = new Vector();
    int _preInvokeWaitSec = 5;
    int _preInterruptWaitSec = 10;
    int _readTimeout = 60000;

    public InterruptTest() {

	PropertyConfigurator.configure(System.getProperties());
	PatternLayout layout = new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");
	BasicConfigurator.configure(new ConsoleAppender(layout));

	// Set security manager, if not already set.
        if ( System.getSecurityManager() == null ) {
	    System.out.println("set security manager");
            System.setSecurityManager(new SecurityManager());
	}

    }

    public static void main(String[] args) {

	String serverHost = args[0];

	InterruptTest wakeup = new InterruptTest();
	wakeup.run(serverHost);
    }


    public void run(String serverHost) {

	/** Create socket factory. */

	try {
	    System.out.println("Set socket factory; readTimeout=" + _readTimeout + " msec");
	    RMISocketFactory.setSocketFactory(new PortalSocketFactory(_readTimeout));
	    System.out.println("done setting socket factory");
	}
	catch (IOException e) {
	    System.err.println("RMISocketFactory.setSocketFactory() failed:\n" + e);
	}



	MOOSNode node;

	String nodeURL = "rmi://" + serverHost + "/node";

	// Get the node's proxy
	try {
	    System.out.println("get node proxy from " + nodeURL);
	    node = (MOOSNode )Naming.lookup(nodeURL);
	    System.out.println("Got node proxy");
	}
	catch (Exception e) {
	    System.err.println("Couldn't get node proxy at " + nodeURL + ":");
	    System.err.println(e);
	    return;
	}

	Worker worker = new Worker(this, node);
	worker.start();

	try {
	    Thread.sleep(_preInterruptWaitSec*1000);
	}
	catch (InterruptedException e) {
	}

	System.out.println("Interrupt worker thread:");
	worker.interrupt();
	System.out.println("Done with interrupt");

    }



    class Worker extends Thread {

	InterruptTest _test;
	MOOSNode _node;

	Worker(InterruptTest test, MOOSNode node) {
	    _test = test;
	    _node = node;
	}

	public void run() {

	    try {
		System.out.println("Wait " + _preInvokeWaitSec + " before invoking methods...");
		Thread.sleep(_test._preInvokeWaitSec * 1000);
	    }
	    catch (Exception e) {
		System.err.println("Worker.run() - got interruption PRIOR to contacting node");
		return;
	    }

	    try {
		System.out.println("processNode() - wakeupAllnodes");
		_node.wakeupAllNodes();
		System.out.println("processNode() - done with wakeupAllNodes()");
	    }
	    catch (Exception e) {
		System.err.println(e);
	    }
	}
    }
}

