/****************************************************************************/
/* Copyright 2003 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.operations.utils;

import java.rmi.RemoteException;
import java.util.Date;
import java.text.DateFormat;
import java.rmi.Naming;
import java.io.FileInputStream;
import java.lang.StringBuffer;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.rmi.server.RMISocketFactory;
import java.io.IOException;
import java.net.MalformedURLException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;
import org.mbari.siam.utils.SiamSocketFactory;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.PortNotFound;
import org.mbari.siam.distributed.DeviceNotFound;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;

public class AnnotateService {

    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(AnnotateService.class);

    public static final int MAX_ANNOTATION_SIZE = 512;


    public void showUsage(String args[])
    {
        if ( args != null)
        {
            _log4j.debug("args.length=" + args.length);
            for (int i = 0; i < args.length; i++) {
                System.err.println("arg: " + args[i]);
            }
        }
        
        System.err.println("usage: AnnotateService " + 
            	       "nodeURL commPortName [-f|-s] annotation");
        System.err.println("-f get annotation data from a file");
        System.err.println("-s get annotation data from a cmd line");
        return;
    }

    public int execute(String args[])
    {
	Node node = null;
	Device devices[] = null;

        //need to setSecurityManager 
        if ( System.getSecurityManager() == null )
            System.setSecurityManager(new SecurityManager());
        
        //if the user does not enter 4 args
        if (args.length != 4)
        {
            showUsage(null);
            return 1;
        }

	String nodeURL = NodeUtility.getNodeURL(args[0]);
	String portName = PortUtility.getPortName(args[1]);
	String annotationSrc = args[2];
        String annotation = args[3];

	// Create socket factory; overcomes problems with RMI 'hostname'
	// property.
	try {
	    String host = NodeUtility.getHostName(nodeURL);
	    RMISocketFactory.setSocketFactory(new SiamSocketFactory(host));
	}
	catch (MalformedURLException e) {
	    System.err.println("Malformed URL \"" + nodeURL + "\": " + 
			       e.getMessage());
	}
	catch (IOException e) {
	    System.err.println("RMISocketFactory.setSocketFactory() failed\n"+e);
	}


	try {

	    System.out.println("Looking for node server stub at " + nodeURL);

	    node = (Node )Naming.lookup(nodeURL);

	    System.out.println("Got proxy for node service \"" + 
			       new String(node.getName()) + "\"");


	    Device device = node.getDevice(portName.getBytes());
	    if (!(device instanceof Instrument)) {
		System.err.println("Device service on port " + 
				   portName + " is not an Instrument");
		return 1;
	    }

            Instrument instrument = (Instrument)device;
            
            if ( annotationSrc.compareTo("-s") == 0)
            {
                instrument.annotate(annotation.getBytes());
            }
            else if (annotationSrc.compareTo("-f") == 0)
            {
                
                FileInputStream input = null;

                try
                {
                    input = new FileInputStream(annotation);
                }
                catch(FileNotFoundException e)
                {
                    System.err.println("Couldn't find file: " + annotation);
                    return 1;
                }

                StringBuffer str_buff = new StringBuffer(MAX_ANNOTATION_SIZE);
                
                try
                {
                    int i = 0;

                    while( (input.available() > 0) && 
                           (i++ < MAX_ANNOTATION_SIZE) )
                        str_buff.append((char)input.read());
                }
                catch(IOException e)
                {
                    System.err.println("Failure reading file: " + annotation);
                    return 1;
                }
                
                instrument.annotate(str_buff.toString().getBytes());
            }
            else
            {
                System.err.println("Invalid annotate source: " + 
                               annotationSrc);

                showUsage(null);
                return 1;
            }
	}
	catch (PortNotFound e) {
	    System.err.println("Port \"" + portName + "\" not found");
	    return 1;
	}
	catch (DeviceNotFound e) {
	    System.err.println("No device service found on port " + 
			       portName);
	    return 1;
	}
	catch (RemoteException e) {
	    System.err.println("RemoteException: " + e.getMessage());
	    return 1;
	}
	catch (Exception e) {
	    System.err.println("Exception: " + e.getMessage());
	    return 1;
	}

	return 0;
    }

    public static void main(String[] args) {
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

        AnnotateService app = new AnnotateService();
        int retval = app.execute(args);
	System.exit(retval);
    }
}
