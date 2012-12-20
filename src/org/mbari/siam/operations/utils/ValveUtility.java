// Copyright MBARI 2003
package org.mbari.siam.operations.utils;

import java.rmi.RemoteException;
import java.util.Date;
import java.text.DateFormat;
import java.rmi.Naming;
import java.io.IOException;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.PortOccupiedException;
import org.mbari.siam.distributed.DuplicateIdException;
import org.mbari.siam.distributed.PortNotFound;

import org.mbari.siam.distributed.devices.ValveIF;
import org.mbari.siam.distributed.devices.Valve2WayIF;
import org.mbari.siam.distributed.devices.ValveServiceIF;

/**
Utility to display/set a valve
@see org.mbari.siam.distributed.devices.ValveIF
*/
public class ValveUtility extends PortUtility
{
    int		_valve = -1;
    String	_newPosition = null;

    protected int getValveArg(String arg) throws InvalidOption
    {
	try {
	    int valveNum = Integer.parseInt(arg);
	    if ((valveNum < ValveServiceIF.VALVE_FWD) || 
		(valveNum > ValveServiceIF.VALVE_EXTRA1))
		throw new InvalidOption("Valve number out of range");
	    return(valveNum);
	} catch (NumberFormatException e) {
	}

	if (arg.equalsIgnoreCase("FWD") || arg.equalsIgnoreCase("VALVE_FWD"))
	    return(ValveServiceIF.VALVE_FWD);
	else if (arg.equalsIgnoreCase("AFT") || arg.equalsIgnoreCase("VALVE_AFT"))
	    return(ValveServiceIF.VALVE_AFT);
	else if (arg.equalsIgnoreCase("EXTRA0") || arg.equalsIgnoreCase("VALVE_EXTRA0"))
	    return(ValveServiceIF.VALVE_EXTRA0);
	else if (arg.equalsIgnoreCase("EXTRA1") || arg.equalsIgnoreCase("VALVE_EXTRA1"))
	    return(ValveServiceIF.VALVE_EXTRA1);

	throw new InvalidOption("Unknown valve identifier");
    }

    /** No custom options for this application. */
    public void processCustomOption(String[] args, int index) throws InvalidOption
    {
	if (_valve < 0)		// Haven't gotten valve position yet
	    _valve = getValveArg(args[index]);
	else if (_newPosition == null)
	    _newPosition = args[index];
	else
	    throw new InvalidOption("unknown option");
    }


    public void processPort(Node node, String portName) throws RemoteException
    {
	Device		device ;
	ValveServiceIF	valveSvc;
	ValveIF		valve;
	int		position;

	try {
	    device = node.getDevice(portName.getBytes());
	} catch (Exception e) {
	    System.err.println("Exception looking up port " + portName + " : " + e);
	    return;
	}

	if (!(device instanceof ValveServiceIF))
	{
	    System.err.println(portName + " is not a valid ValveService.  Exiting.");
	    return;
	}

	valveSvc = (ValveServiceIF)device;

	if ((_valve < 0) || (_newPosition == null))
	{
	    for (int i = 0; i < ValveServiceIF.MAX_VALVES; i++)
	    {
		try {
		    valve = valveSvc.getValve(i);
		    position = valve.getPosition();
		    System.out.print("Valve " + i + " is in " +
				     valve.getPositionName(position) 
				     + " position ");
		    if (valve instanceof Valve2WayIF)
		    {
			Valve2WayIF valve2 = (Valve2WayIF)valve;
			if (valve2.isOpen())
			    System.out.print("(open)");
			if (valve2.isClosed())
			    System.out.print("(closed)");
		    }
		    System.out.println();
		} catch (Exception e) {
		}
	    }
	}
	else
	    try
	    {
		valve = valveSvc.getValve(_valve);
		position = valve.parsePositionName(_newPosition);
		System.out.println("Commanding valve " + _valve + " to " +
				   valve.getPositionName(position) + " position.");
		valve.setPosition(position);
	    } catch (Exception e) {
		System.err.println("Exception in Valve service: " + e);
		e.printStackTrace();
	    }
    }

    public void printUsage()
    {
	System.err.println("usage: ValveUtility nodeURL portName [ValveID] [position]");
    }


    public static void main(String[] args)
    {
	try {
	    ValveUtility valveUtil = new ValveUtility();
	    valveUtil.multiPortsAllowed(false);
	    valveUtil.processArguments(args);
	    valveUtil.run();
	} catch (Exception e) {
	    System.err.println("Exception in ValveUtility: " + e);
	    e.printStackTrace();
	}
    }

} /* class ValveUtility */
