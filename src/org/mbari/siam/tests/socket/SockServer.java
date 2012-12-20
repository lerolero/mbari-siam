/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.socket;


import java.net.*;
import java.io.*;

/**
   bcastsvr receives a UDP broadcast packet from a bcastcli and echoes it back.
*/

public class SockServer
{
    /** Prints use message */
    public static void printUsage()
    {
	System.out.println("\nUsage: server <localPort>\n");
	System.exit(0);
    }
    
    /** main */
    public static void main(String args[])
    {
	ServerSocket	srvSock = null;
	int		port = 0, ch;

	if (args.length < 1)
	    printUsage();

	try
	{
	    port = Integer.parseInt(args[0]);
	} catch (Exception e) {
	    printUsage();
	}

	try
	{
	    srvSock = new ServerSocket(port);
	    Socket sock = srvSock.accept();
	    System.out.println("Connected to " + 
			       sock.getInetAddress().getHostName());
	    InputStream in = sock.getInputStream();
	    OutputStream out = sock.getOutputStream();

	    while((ch = in.read()) != 4)
	    {
		out.write(ch);
		out.flush();
	    }

	} catch (IOException e) {
	    System.err.println("IOException: " + e);
	    e.printStackTrace();
	}
    }
}
