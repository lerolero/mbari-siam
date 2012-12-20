/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.bcast;


import java.net.*;
import java.io.*;

/**
   bcastsvr receives a UDP broadcast packet from a bcastcli and echoes it back.
*/

public class bcastsvr
{

    /** Prints use message */
    public static void printUsage(DatagramSocket socket,int DEST_PORT){
	System.out.println("\nUsage: bcastsvr [--help] [a <localAddress>] [p <localPort>]\n");
	System.out.println("  localAddress: specify local host name or IP address for socket binding ["+socket.getLocalAddress()+"]");
	System.out.println("  localPort:    specify local port for socket binding ["+socket.getLocalPort()+"]");

	System.exit(0);
    }
    
    /** main */
    public static void main(String args[])
    {
	DatagramSocket socket=null;
	DatagramPacket packet;
	int BUFFER_LENGTH=256;
	int LOCAL_PORT=6789;
	byte[] b = new byte[BUFFER_LENGTH];
	InetAddress sockAddr=null;

	try{

	    // create default socket (bound to 0.0.0.0:<systemPort>)
	    socket=new DatagramSocket(LOCAL_PORT);

	    // get command line args
	    for(int i=0;i<args.length;i++){

		if(args[i].indexOf("--help")>=0){
		    // help message
		    printUsage(socket,LOCAL_PORT);
		}else
		if(args[i].indexOf("a")>=0){
		    // local socket address
		    sockAddr=InetAddress.getByName(args[++i]);
		}else
		if(args[i].indexOf("p")>=0){
		    // local socket port
		    LOCAL_PORT=Integer.parseInt(args[++i]);
		}
	    }
	}catch (IOException e){
	    System.out.println("Error: " + e);
	    System.exit(-1);
	}

	try{

	    // override default socket if specified
	    if(sockAddr!=null){
		    socket=new DatagramSocket(LOCAL_PORT,sockAddr);
	    }
	    // else socket already created 

	    // create UDP packet for receiving
	    packet = new DatagramPacket(b, b.length);

	    System.err.println("svr: listening on "+socket.getLocalAddress()+":"+socket.getLocalPort());

	    // Wait for packets
	    while(true) {
		System.err.println("svr: Waiting for packets...");
	    
		socket.receive(packet); // blocks until a datagram is received
		System.err.println("svr: Received " + packet.getLength()
				   +" bytes from " + packet.getAddress()
				   +"; Echoing packet...");

		System.err.println("svr: packet payload=" + 
				   new String(packet.getData()));

		// must reset length field!
		packet.setLength(b.length);

		// echo packet
		socket.send(packet);
	    }

	}catch(Exception e){
	    System.out.println("Error: " + e);
	    socket.close();
	}
    }
}
