// Copyright MBARI 2011
package org.mbari.siam.foce.utils;

import java.io.IOException;
import java.rmi.RemoteException;
import org.mbari.siam.operations.utils.NodeUtility;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.FOCENode;
import org.mbari.siam.distributed.NotSupportedException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;

/**
 *  Utility to invoke FOCENode methods
 *  @author Bob Herlien
 */
public class FOCECO2SubsysUtil extends NodeUtility 
{
    static private Logger _log4j = Logger.getLogger(FOCECO2SubsysUtil.class);

    protected String[] _funcs;

    /** Call the function specified on the command-line */
    public void processNode(Node node) throws Exception
    {
	FOCENode foceNode;

	if (!(node instanceof FOCENode))
	    throw new NotSupportedException("The target isn't a FOCENode!");

	foceNode = (FOCENode)node;

	for (int i = 1; i < _funcs.length; i++)
	{
	    try {
		if (_funcs[i].equalsIgnoreCase("powerOn")) {
		    _log4j.info("calling powerUpCO2Subystem()");
		    foceNode.powerUpCO2Subsystem();
		}
		else if (_funcs[i].equalsIgnoreCase("powerOff")) {
		    _log4j.info("calling powerDownCO2Subystem()");
		    foceNode.powerDownCO2Subsystem();
		}
		else if (_funcs[i].equalsIgnoreCase("start")) {
		    _log4j.info("calling startCO2SubystemServices()");
		    foceNode.startCO2SubsystemServices();
		}
		else if (_funcs[i].equalsIgnoreCase("stop")) {
		    _log4j.info("calling stopCO2SubystemServices()");
		    foceNode.stopCO2SubsystemServices();
		}
		else {
		    _log4j.error("Unrecognized command-line function: " + _funcs[i]);
		}
	    } catch (Exception e) {
		_log4j.error("Caught Exception: " + e);
		e.printStackTrace();
	    }
	}

    }

    /** Process application-specific option. */
    public void processCustomOption(String[] args, int index)
	throws InvalidOption
    {
	_funcs = args;
    }

    /** Print usage message. */
    public void printUsage() 
    {
	System.err.println("co2 nodeURL <powerOn | powerOff | start | stop>");
    }


    public static void main(String[] args) 
    {
	// Configure log4j
	PropertyConfigurator.configure(System.getProperties());
	BasicConfigurator.configure();

	FOCECO2SubsysUtil util = new FOCECO2SubsysUtil();
	util.processArguments(args, 2);
	util.run();
    }
}
