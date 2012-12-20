/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.socket;

import java.net.*;
import java.io.*;

/**
   SockClient connects to a given IP address and port, and sends the
   user keyboard input to that port
*/

public class SockClient
{
    /** Prints use message */
    public static void printUsage()
    {
	System.out.println("\nUsage: bcastcli <IPaddress> <IPport>");
	System.exit(0);
    }
    
    /** main */
    public static void main(String args[])
    {
	int		port = 0, ch;
	Socket		sock = null;
	InputStream	in = null;
	OutputStream	out = null;

	if (args.length < 2)
	    printUsage();

	try
	{
	    port = Integer.parseInt(args[1]);
	} catch (NumberFormatException e) {
	    System.err.println("Bad port number: " + args[1]);
	    printUsage();
	}

	try
	{
	    sock = new Socket(args[0], port);
	    in = sock.getInputStream();
	    out = sock.getOutputStream();
	} catch (UnknownHostException e) {
	    System.err.println("UnknownHost: " + args[0]);
	    printUsage();
	} catch (IOException e) {
	    System.err.println("IOException: " + e);
	    e.printStackTrace();
	    printUsage();
	}

	while(true)
	    try
	    {
		while(System.in.available() > 0)
		{
		    ch = System.in.read();

		    if ((ch == 4) || (ch < 0))
		    {
			System.out.println("Got ctrl-D or EOF.  Goodbye");
			sock.close();
			System.exit(0);
		    }
		    out.write(ch);
		    out.flush();
		}

		while (in.available() > 0)
		{
		    ch = in.read();
		    System.out.write(ch);
		    System.out.flush();
		}

		
	    } catch (Exception e) {
		System.err.println("Exception: " + e);
		e.printStackTrace();
	    }
    }
}

