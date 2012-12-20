// Copyright MBARI 2003
package org.mbari.siam.foce.utils;

import org.mbari.siam.foce.deployed.FOCERemoteRelay;
import org.mbari.siam.core.NodeProperties;

import java.lang.NumberFormatException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Properties;

import org.mbari.siam.distributed.MissingPropertyException;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;


/** User utility to turn on/off a relay on a FOCERelayBoard */
public class FOCERemoteRelayUtil
{
    FOCERemoteRelay _relayMapper;

    public FOCERemoteRelayUtil(String hostName)
	throws IOException, UnknownHostException
    {
	_relayMapper = new FOCERemoteRelay(hostName);
    }

    public void relayOn(int board, int relay) throws IOException
    {
	_relayMapper.powerOn(board, relay);
    }

    public void relayOff(int board, int relay) throws IOException
    {
	_relayMapper.powerOff(board, relay);
    }

    public void printOne(int board, int relay, FOCERelayProperties props, boolean state)
	throws IOException
    {
	System.out.print("Board " + board + " relay " + relay);

	if (props != null) {
	    FOCERelayProperties.RelayProperty prop = props.findPropertyByRelay(board, relay);
	    if (prop != null) {
		System.out.print(" (" + prop.getDescriptor() + ")");
	    }
	}
	
	System.out.println(" is " + (state ? "ON" : "OFF"));
    }

    public void printOne(int board, int relay, FOCERelayProperties props)
	throws IOException
    {
	printOne(board, relay, props, _relayMapper.relayIsOn(board, relay));
    }

    public void printAll(FOCERelayProperties props)
    {
	int boardAddr, state;

	for (int i = 0; i < 8; i++) {
	    boardAddr = 0;
	    try {
		boardAddr = _relayMapper.boardAddr(i);
	    } catch (Exception e) {
	    }

	    if (boardAddr > 0) {
		try {
		    state = _relayMapper.relayState(i);
		    System.out.println("\nRelay Board " + i + " at address 0x" + 
				       Integer.toHexString(boardAddr) +
				       " relay state 0x" + Integer.toHexString(state)
				       + " :");
		    for (int j = 0; j < 16; j++) {
			printOne(i, j, props, (state & (1 << j)) != 0);
		    }
		} catch (Exception e) {
		    System.out.println("Exception: " + e);
		}
	    }
	}
    }


    public static void usage()
    {
	System.out.println("Usage:  relay <nodeName> [board] [relayNum] [ON|OFF]    OR");
	System.out.println("        relay <nodeName> [relayName] [ON|OFF]    OR");
	System.out.println("        relay <nodeName>");
	System.out.println("Where:  <nodeName> is the node to print/manipulate");
	System.out.println("        <board> is the board number, typically 0 or 1");
	System.out.println("        <relayNum> is the relay number, 0 to 15");
	System.out.println("        <relayName> is the relay name in the relays.properties file");
	System.out.println("relay <nodeName> will just print out the state of the relays");
    }

    /** Main method is for command-line testing */
    public static void main(String[] args)
    {
	String hostName = "localhost";
	FOCERemoteRelayUtil util = null;
	FOCERelayProperties props = null;
	FOCERelayProperties.RelayProperty relayProp = null;
	int firstArg = 0;

	int board, relay;
	int numNumerics = 0;
	int numericParms[] = new int[4];
	int operation = -1;  /* -1 = undefined, 0 = off, 1 = on*/

	if (args.length > 0) {
	    try {
		util = new FOCERemoteRelayUtil(args[0]);
		hostName = args[0];
		firstArg++;
	    } catch (Exception e) {
		System.out.println("Can't connect to " + args[0] + ": " + e);
		System.out.println("Trying localhost");
	    }
	}

	if (util == null) {
	    try {
		util = new FOCERemoteRelayUtil(hostName);
	    } catch (Exception e2) {
		System.out.println("Can't connect to " + hostName + ": " + e2);
		usage();
		return;
	    }
	}
	
	System.out.println("Connected to " + hostName);

	try {
	    props = new FOCERelayProperties("http://" + hostName + "/properties/relays.properties");
	} catch (Exception e) {
	    props = null;
	}

	for (int i = firstArg; i < args.length; i++) {
	    try {
		numericParms[numNumerics] = Integer.parseInt(args[i]);
		numNumerics++;
	    } catch (Exception e) {
	    }
	    if (args[i].trim().equalsIgnoreCase("on"))
		operation = 1;
	    else if (args[i].trim().equalsIgnoreCase("off"))
		operation = 0;
	    else if (props != null) {
		relayProp = props.findPropertyByDescriptor(args[i]);
	    }
	}

	if (relayProp != null) {
	    board = relayProp.getBoard();
	    relay = relayProp.getRelay();
	} 
	else if (numNumerics == 1) {
	    board = 0;
	    relay = numericParms[0];
	}
	else if (numNumerics > 1) {
	    board = numericParms[0];
	    relay = numericParms[1];
	}
	else {
	    util.printAll(props);
	    return;
	}

	try {
	    if (operation == 0) {
		util.relayOff(board, relay);
		System.out.println("Turning off board " + board + " relay " + relay);
	    }
	    else if (operation == 1) {
		util.relayOn(board, relay);
		System.out.println("Turning on board " + board + " relay " + relay);
	    }

	    util.printOne(board, relay, props);
	} catch (Exception e) {
	    System.out.println("Exception in utility: " + e);
	    e.printStackTrace();
	}

    }
}
