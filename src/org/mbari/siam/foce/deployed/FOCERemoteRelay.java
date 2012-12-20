// Copyright MBARI 2003
package org.mbari.siam.foce.deployed;

import java.lang.NumberFormatException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Properties;


/** Class to manipulate relays on FOCE	*/
public class FOCERemoteRelay extends RemoteIOMapper
{
    public FOCERemoteRelay(String hostName)
	throws IOException, UnknownHostException
    {
	super(hostName);
    }

    public int relayState(int board) 
	throws IOException, NumberFormatException
    {
	return(Integer.parseInt(transact("relayState " + board + "\n")));
    }

    public int boardAddr(int board) 
	throws IOException, NumberFormatException
    {
	return(Integer.parseInt(transact("relayAddr " + board + "\n")));
    }

    public void powerOn(int board, int relayNum) throws IOException
    {
	transact("relayOn " + board + " " + 
			   Integer.toHexString(1 << relayNum) + "\n");
    }

    public void powerOff(int board, int relayNum) throws IOException
    {
	transact("relayOff " + board + " " + 
			   Integer.toHexString(1 << relayNum) + "\n");
    }

    public boolean relayIsOn(int board, int relayNum)
	throws IOException, NumberFormatException
    {
	return((relayState(board) & (1 << relayNum)) != 0);
    }

    public int switchElmo(int val) 
	throws IOException, NumberFormatException
    {
	String rtn = transact("switchElmo " + Integer.toHexString(val) + "\n");
	if (rtn.startsWith("0x")) {
	    rtn = rtn.substring(2);
	}

	return(Integer.parseInt(rtn, 16));
    }

    public int readElmo()
	throws IOException, NumberFormatException
    {
	String rtn = transact("readElmo\n").trim();
	if (rtn.startsWith("0x")) {
	    rtn = rtn.substring(2);
	}

	return(Integer.parseInt(rtn, 16));
    }

    /** Main method is for command-line testing */
    public static void main(String[] args)
    {
	String hostName = "localhost";
	FOCERemoteRelay util;

	if (args.length >= 1) {
	    hostName = args[0];
	}

	try {
	    util = new FOCERemoteRelay(hostName);
	    System.out.println("Connected to " + hostName);
	} catch (Exception e) {
	    System.out.println("Can't connect to " + hostName + ": " + e);
	    e.printStackTrace();
	    return;
	}

	for (int i = 0; i < 8; i++) {
	    try {
		int boardAddr = util.boardAddr(i);

		if (boardAddr > 0) {
		    System.out.println("Relay Board " + i + " address 0x" + 
				       Integer.toHexString(boardAddr) +
				       ", relay state: 0x" + 
				       Integer.toHexString(util.relayState(i)));
		}

	    } catch (Exception e) {
	    }
	}

	try {
	    System.out.println("Elmo state:  0x" + Integer.toHexString(util.readElmo()));
	} catch (Exception e) {
	    System.err.println("Cannot read Elmo: " + e);
	}

    }
}
