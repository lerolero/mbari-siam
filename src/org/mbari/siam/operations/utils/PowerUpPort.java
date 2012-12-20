// Copyright MBARI 2003
package org.mbari.siam.operations.utils;

import java.rmi.RemoteException;
import java.lang.NumberFormatException;
import java.lang.Integer;
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

/** Power up a power with a specified current limit and enable the 
 communications for RS-232 */
public class PowerUpPort extends PortUtility 
{
    static private Logger _logger = Logger.getLogger(PowerUpPort.class);

    //default current limit, the Mark Chaffey gold standard
    int _currentLimit = 2000;

    public static void main(String[] args) 
    {
	// Configure log4j
	PropertyConfigurator.configure(System.getProperties());
	BasicConfigurator.configure();

	PowerUpPort powerUpPort = new PowerUpPort();
	powerUpPort.processArguments(args);
	powerUpPort.run();

    }

    /** Get the current limit for this port. */
    public void processCustomOption(String[] args, int index) 
        throws InvalidOption 
    {
        _logger.debug("processCustomOption(...) got args[" + index + "] = " +
                      args[index]);
        
        try
        {
            _currentLimit = Integer.parseInt(args[index]);

            //quick and dirty way of removing the option '-'
            if ( _currentLimit < 0)
                _currentLimit *= -1;
            
            _logger.debug("processCustomOption(...) _currentLimit = " + 
                          _currentLimit);
        }
        catch(Exception e)
        {
            throw new InvalidOption("error parsing int: " + e);
        }
    }

    /** Print usage message. */
    public void printUsage() 
    {
        System.err.println("usage: PowerUpPort nodeURL commPortName " + 
                           "[-currentLimit(milliamps)]");
    }

    public void processPort(Node node, String portName) 
	throws RemoteException 
    {
        //try to power up the port
        try
        {
            _logger.debug("processPort(...) node.powerUpPort(" + portName + ", " 
                          + _currentLimit + ")");
            node.powerUpPort(portName.getBytes(), _currentLimit);
        }
        catch (Exception e)
        {
            _logger.error("processPort(...) Exception: " + e);
        }
    }
}
