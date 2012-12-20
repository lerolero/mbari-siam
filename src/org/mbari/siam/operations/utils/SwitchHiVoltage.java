// Copyright MBARI 2004
package org.mbari.siam.operations.utils;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.Vector;
import java.text.DateFormat;
import java.rmi.Naming;
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
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.PortNotFound;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.utils.PrintUtils;
import org.mbari.siam.distributed.devices.Power;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;

/**
   Switch the Hi Voltage to the sub sea nodes
  @author Mike Risi
 */
public class SwitchHiVoltage 
{
    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(SwitchHiVoltage.class);

    public static void main(String[] args) 
    {
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

        Node node = null;
        final int POWER_SWITCH_ON = 0;
        final int POWER_SWITCH_OFF = 1;
        final int POWER_SWITCH_QUERY = 2; 
                  
        int switchRequest = POWER_SWITCH_QUERY; 

        //need to setSecurityManager 
        if ( System.getSecurityManager() == null )
            System.setSecurityManager(new SecurityManager());

        if ( args.length < 2 )
        {
            System.err.println("usage: SwitchHiVoltage " + 
                               "nodeURL portName [ON|OFF]");
            System.exit(1);
        }

        String nodeURL = NodeUtility.getNodeURL(args[0]);
        String portName = PortUtility.getPortName(args[1]);

	// Create socket factory; overcomes problems with RMI 'hostname'
	// property.
	try {
	    String host = NodeUtility.getHostName(nodeURL);
	    RMISocketFactory.setSocketFactory(new SiamSocketFactory(host));
	}
	catch (MalformedURLException e) {
	    _log4j.error("Malformed URL \"" + nodeURL + "\": " + 
			       e.getMessage());
	}
	catch (IOException e) {
	    _log4j.error("RMISocketFactory.setSocketFactory() failed");
	    _log4j.error(e);
	}

        
        /* figure out what state the user is trying to put the high voltage switch in */
        if ( args.length < 3)
        {
            switchRequest = POWER_SWITCH_QUERY;
        }
        else if ( args[2].compareToIgnoreCase("OFF") == 0 )
        {
            System.out.println("Switching high voltage: OFF");
            switchRequest = POWER_SWITCH_OFF;
        
        }
        else if ( args[2].compareToIgnoreCase("ON") == 0 )
        {
            System.out.println("Switching high voltage: ON");
            switchRequest = POWER_SWITCH_ON;
        }
        else
        {
            System.err.println("Don't understand '" + args[2] + "' request");
            System.err.println("Aborting power switching operation");
            System.exit(1);
        }
        
        
        try
        {
            _log4j.debug("Looking for node server stub at " + nodeURL);

            node = (Node )Naming.lookup(nodeURL);

            _log4j.debug("Got proxy for node service \"" + 
                               new String(node.getName()) + "\"");

            try
            {
                Device device = node.getDevice(portName.getBytes());
                if ( device instanceof Power )
                {
                    Power power = (Power)device;

                    if ( switchRequest == POWER_SWITCH_QUERY )
                    {
                        if ( power.isHighVoltageEnabled() )
                            System.out.println("High voltage switch is: ON");
                        else
                            System.out.println("High voltage switch is: OFF");
                    }
                    else if ( switchRequest == POWER_SWITCH_ON )
                    {
                        power.enableHiVoltage();
                    }
                    else if ( switchRequest == POWER_SWITCH_OFF )
                    {
                        power.disableHiVoltage();
                    }
                    else
                    {
                        //should never get here
                        _log4j.error("Switch request unknown");
                    }
                } 
                else
                {
                    _log4j.error("Device on port " + portName + 
                                       " is not an Power");
                }
            } 
            catch ( PortNotFound e )
            {
                _log4j.error("Port " + portName + " not found");
            } 
            catch ( DeviceNotFound e )
            {
                _log4j.error("Device not found on port " + portName);
            } 
        } 
        catch ( RemoteException e )
        {
            _log4j.error("RemoteException: " + e.getMessage());
            System.exit(1);
        } 
        catch ( Exception e )
        {
            _log4j.error("Exception: " + e.getMessage());
            System.exit(1);
        }
        
        System.exit(0);

    }
}
