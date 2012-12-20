// Copyright MBARI 2004
package org.mbari.siam.operations.utils;

import java.rmi.RemoteException;
import java.util.Date;
import java.text.DateFormat;
import java.rmi.Naming;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.DeviceNotFound;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;

/** Remove all running services and exit the SIAM application */
public class ExitApplication extends NodeUtility 
{
    static private Logger _logger = Logger.getLogger(ExitApplication.class);

    public static void main(String[] args) 
    {
	// Configure log4j
	PropertyConfigurator.configure(System.getProperties());
	BasicConfigurator.configure();
	
        ExitApplication exitApp = new ExitApplication();
	exitApp.processArguments(args);
	exitApp.run();
    }
    
    public void printUsage() 
    {
        System.err.println("usage: ExitApplication nodeURL");
    }

    public void processNode(Node node) throws Exception 
    {
        node.exitApplication();
    }

    /** No custom options for this application. */
    public void processCustomOption(String[] args, int index) 
        throws InvalidOption 
    {
	// No custom options for this application... so throw 
	// invalid option exception if this method is called.
	throw new InvalidOption("unknown option");
    }
}
