/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

import java.io.IOException;
import java.rmi.server.RMISocketFactory;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.NodeConfigurator;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

/**
Create a NodeManager object.
*/
public class NodeMain {

    static Logger _log4j = Logger.getLogger(NodeMain.class);

    public static void usage()
    {
	System.out.println("Usage: NodeMain [-config <class>] [-publish] [-testpublish] [-parent <node>]");
    }

    public static void main(String[] args)
    {

	String portalHost = null;
	boolean publish = false, testpublish = false;
	DevicePacketPublisher publisher = null;
	NodeConfigurator nodeConfig = new NodeConfiguratorImpl();

	/* Set up a simple configuration that logs on the console.
	   Note that simply using PropertyConfigurator doesn't work
	   unless JavaBeans classes are available on target. 
	   For now, we configure a PropertyConfigurator, using properties
	   passed in from the command line, followed by BasicConfigurator
	   which sets default console appender, etc.
	*/


	Properties p=System.getProperties();
	String dflt_log4j=p.getProperty("siam_home","/mnt/hda/siam")+"/properties/siam.log4j";
	String siam_log4j=p.getProperty("siam.log4j",dflt_log4j);
	PropertyConfigurator.configure(siam_log4j);

	/*
	PatternLayout layout = 
	   new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");

	BasicConfigurator.configure(new ConsoleAppender(layout));
	*/

	_log4j.debug("using log4j configuration file:"+siam_log4j);
	_log4j.debug("NodeMain.main(): args.length=" + args.length);

	for (int i = 0; i < args.length; i++)
	{
	    if (args[i].equalsIgnoreCase("-publish"))
		publish = true;
	    else if (args[i].equalsIgnoreCase("-testpublish"))
		testpublish = true;
	    else if (args[i].equalsIgnoreCase("-config") && (++i < args.length)) {
		try {
		    ClassLoader loader = ClassLoader.getSystemClassLoader();
		    Class configClass = loader.loadClass(args[i]);
		    Object cfg = configClass.newInstance();
		    if (cfg instanceof NodeConfigurator)
			nodeConfig = (NodeConfigurator)cfg;
		} catch (Exception e) {
		    _log4j.error("Error loading class " + args[i] + ": " + e);
		}
	    }
	    else if (args[i].equalsIgnoreCase("-h") ||
		     args[i].equalsIgnoreCase("-help")) {
		usage();
		System.exit(0);
	    }
	    else if (args[i].equalsIgnoreCase("-parent") && (++i < args.length)) {
		portalHost = args[i];
	    }
	}

//	if (portalHost == null)
//	    _log4j.info("NodeMain.main): portalHost not specified");
//	else
	    _log4j.info("NodeMain.main(): portalHost = " + portalHost);

	// Create socket factory
	try {
	    RMISocketFactory.setSocketFactory(new NodeSocketFactory());
	}
	catch (IOException e) {
	    _log4j.error("RMISocketFactory.setSocketFactory() failed\n"+e);
	}

	boolean startedOK = false;

	try {
	    NodeManager manager = NodeManager.getInstance(nodeConfig);
	    if (publish || testpublish)
		publisher = new DevicePacketPublisher(publish);
	    manager.start("node", portalHost);
	    startedOK = true;
	}
	catch (IOException e) {
	    _log4j.error("IOException: ", e);
	}
	catch (MissingPropertyException e) {
	    _log4j.error("MissingPropertyException: ", e);
	}
	catch (InvalidPropertyException e) {
	    _log4j.error("InvalidPropertyException: ", e);
	}

	if (!startedOK) {
	    System.err.println("NodeManager startup failed");
	    _log4j.error("NodeManager startup failed");
	    System.exit(1);
	}
    }
}
