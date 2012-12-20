// Copyright MBARI 2003
package org.mbari.siam.foce.utils;

import org.mbari.siam.foce.deployed.FOCERelayBoard;
import org.mbari.siam.foce.deployed.FOCENodeConfigurator;
import org.mbari.siam.core.NodeProperties;

import java.io.IOException;
import java.util.Properties;

import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.MissingPropertyException;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;



/** User utility to turn on/off a relay on a FOCERelayBoard */
public class FOCERelayBoardUtil
{
    static Logger _log4j = Logger.getLogger(FOCERelayBoardUtil.class);

    FOCERelayBoard _relayBoard;

    public FOCERelayBoardUtil(int boardAddress)	throws IOException
    {
	_relayBoard = new FOCERelayBoard(boardAddress);
    }

    /** Creates board at default address.
     */
    public FOCERelayBoardUtil()	throws IOException
    {
	_relayBoard = new FOCERelayBoard();
    }

    public void runOne(int relayNum, boolean onOff) throws IOException
    {
	_log4j.debug("runOne()");

	if (onOff)
	    _relayBoard.powerOnBit(relayNum);
	else
	    _relayBoard.powerOffBit(relayNum);

	_relayBoard.close();
    }

    public void runMult(int relayNum, int numRelays, boolean onOff)
	throws IOException
    {
	_log4j.debug("runMult()");

	if (onOff)
	    _relayBoard.powerOnBits(relayNum, numRelays);
	else
	    _relayBoard.powerOffBits(relayNum, numRelays);

	_relayBoard.close();
    }

    public void runAll(boolean onOff) throws IOException
    {
	_log4j.debug("runAll()");

	if (onOff)
	    _relayBoard.powerOnAll();
	else
	    _relayBoard.powerOffAll();

	_relayBoard.close();
    }

    public void getState() throws IOException, NumberFormatException
    {
	_log4j.debug("getState()");

	System.out.println("Relay state = " + 
			   Integer.toHexString(_relayBoard.relayState()) +
			   " hex");

	_relayBoard.close();
    }

    static void usage()
    {
	System.out.println("Usage: relay [-propertyFile file] [-address addr] <number|range>|all <on|off>");
	System.out.println("    -propertyFile name is offset from $SIAM_HOME");
	System.out.println("    -address is board address.  If none, use default of 0x310.");
	System.out.println("    <number|range|all>: e.g. 1, or 2-6 no spaces, or 'all'");
    }

    /** Main method is for command-line testing */
    public static void main(String[] args)
    {
	int		relayNum = 0;
	int		numRelays = 1;
	int		address = 0x310;
	boolean		onOff = false, gotAddr = false, all = false, getState = false;
	int		index = 0;
	FOCENodeConfigurator config = new FOCENodeConfigurator();
	NodeProperties   properties = null;
	String propFileName = "/properties/relays.properties";

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

	_log4j.info("FOCE Relay Board Utility");

        //need to setSecurityManager 
        if ( System.getSecurityManager() == null )
            System.setSecurityManager(new SecurityManager());

	/* Look to see if user passed a properties file */
	for (index = 0; index < args.length; )
	{
	    if (args[index].equalsIgnoreCase("-propertyFile"))
	    {
		index++;
		propFileName = args[index++];
	    }
	    else if (args[index].equalsIgnoreCase("-address"))
	    {
		index++;
		try {
		    address = Integer.parseInt(args[index++], 16);
		    gotAddr = true;
		} catch (NumberFormatException e) {
		}
	    }
	    else
		break;
	}

	/* Check to make sure we still have relay number and state */
	if (args.length < index + 2)
	{
	    getState = true;
	}

	String home = null;
	try {
	    home = config.getSiamHome();
	    _log4j.debug("SiamHome = " + home);
	} catch (MissingPropertyException e) {
	    _log4j.debug("getSiamHome: " + e);
	}

	try {
	    properties = config.createNodeProperties(propFileName);
	} catch (Exception e) {
	    _log4j.debug("Could not open Properties file " + 
			 home + propFileName);
	    _log4j.debug("Continuing with no relay mapping.  Relay number must be integer.");
	}

	_log4j.debug("index = " + index + ", args.length = " + args.length);
	/* Get the relay number */
	boolean	gotRelay = false;
	int	rangeIndex;
	String  start, end;

	if (getState)
	    gotRelay = true;
	else if (args[index].equalsIgnoreCase("all"))
	{
	    all = true;
	    gotRelay = true;
	}
	else if ((rangeIndex = args[index].indexOf('-')) > 0)
	{
	    try {
		relayNum = Integer.parseInt(args[index].substring(0,rangeIndex));
		_log4j.debug("Start relay is " + relayNum);
		int lastRelay = Integer.parseInt(args[index].substring(rangeIndex+1));
		_log4j.debug("Last relay is " + lastRelay);
		numRelays = lastRelay - relayNum + 1;
		if (numRelays < 0)
		    numRelays = -numRelays;
		if (numRelays > 0)
		    gotRelay = true;
	    } catch (NumberFormatException e) {
	    }
	}
	else
	{
	    try {
		relayNum = Integer.parseInt(args[index]);
		numRelays = 1;
		gotRelay = true;
	    } catch (NumberFormatException e) {
	    }
	}

	if (!gotRelay && (properties != null))
	    try {
		relayNum = properties.getIntegerProperty(args[index].trim());
		gotRelay = true;
	    } catch (Exception e) {
	    }

	if (!gotRelay)
	{
	    System.out.println("Could not parse \"" + args[index] + "\" as a relay number. Exiting.");
	    return;
	}

	index++;
	if (!getState)
	{
	    if (args[index].trim().equalsIgnoreCase("on"))
		onOff = true;
	    else if (args[index].trim().equalsIgnoreCase("off"))
		onOff = false;
	    else
	    {
		usage();
		return;
	    }
	}

	try {
	    FOCERelayBoardUtil util = gotAddr ? 
		new FOCERelayBoardUtil(address) : new FOCERelayBoardUtil();

	    if (getState)
		util.getState();
	    else if (all)
		util.runAll(onOff);
	    else if (numRelays > 1)
		util.runMult(relayNum, numRelays, onOff);
	    else
		util.runOne(relayNum, onOff);
	}
	catch (Exception e) {
	    System.out.println("Exception in running FOCERelayBoardUtil:  " + e);
	    e.printStackTrace();
	}
    }

}
