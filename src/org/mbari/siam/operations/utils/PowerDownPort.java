// Copyright MBARI 2004
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

/** Power down and isolate the port */
public class PowerDownPort extends PortUtility
{
    static private Logger _logger = Logger.getLogger(PowerDownPort.class);

    public static void main(String[] args) 
    {
	// Configure log4j
	PropertyConfigurator.configure(System.getProperties());
	BasicConfigurator.configure();

	PowerDownPort powerDownPort = new PowerDownPort();
	powerDownPort.processArguments(args);
	powerDownPort.run();

    }

    /** No custom options for this application. */
    public void processCustomOption(String[] args, int index) 
        throws InvalidOption 
    {
        throw new InvalidOption("unknown option: " + args[index]);
    }

    /** Print usage message. */
    public void printUsage() 
    {
        System.err.println("usage: PowerDownPort nodeURL commPortName ");
    }

    public void processPort(Node node, String portName) 
	throws RemoteException 
    {
        //try to power up the port
        try
        {
            node.powerDownPort(portName.getBytes());
        }
        catch (Exception e)
        {
            _logger.error("processPort(...) Exception: " + e);
        }
    }
}
