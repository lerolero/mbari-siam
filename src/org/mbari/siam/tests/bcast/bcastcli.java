/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.bcast;

import java.net.*;
import java.io.*;

/**
   bcastcli generates a UDP broadcast packet addressed to a specified UDP port and waits for it to be echoed back.
*/

public class bcastcli
{
    /** Prints use message */
    public static void printUsage(DatagramSocket socket,int DEST_PORT,byte data[], boolean waitForEcho){
	System.out.println("\nUsage: bcastcli [--help] [a <localAddress>] [p <localPort>] [d <destPort>] [D <data>] [w]\n");
	System.out.println("  localAddress: specify local host name or IP address for socket binding ["+socket.getLocalAddress()+"]");
	System.out.println("     localPort: specify local port for socket binding ["+socket.getLocalPort()+"]");
	System.out.println("      destPort: specify remote port for datagram addressing ["+DEST_PORT+"]");
	System.out.println("          data: specify datagram payload ["+(new String(data))+"]");
	System.out.println("          wait: wait for packet to be returned ["+waitForEcho+"]\n");
	System.exit(0);
    }
    
    /** main */
    public static void main(String args[])
    {
	DatagramSocket socket=null;
	DatagramPacket packet;
	int DEST_PORT=6789;
	int LOCAL_PORT=-1;
	String BCAST_ADDR="255.255.255.255";
	byte[] data={};
	InetAddress sockAddr=null;
	boolean waitForEcho=false;

	try{
	    // create default socket (bound to 0.0.0.0:<systemPort>)
	    socket=new DatagramSocket();

	    // get command line args
	    for(int i=0;i<args.length;i++){

		if(args[i].indexOf("--help")>=0){
		    // help message
		    printUsage(socket,DEST_PORT,data,waitForEcho);
		}else
		if(args[i].indexOf("a")>=0){
		    // local socket address
		    sockAddr=InetAddress.getByName(args[++i]);
		}else
		if(args[i].indexOf("p")>=0){
		    // local socket port
		    LOCAL_PORT=Integer.parseInt(args[++i]);
		}else
		if(args[i].indexOf("d")>=0){
		    // server UDP port
		    DEST_PORT=Integer.parseInt(args[++i]);
		}else
		if(args[i].indexOf("D")>=0){
		    // datagram payload (null terminated string, default payload is empty (0 bytes))
		    // Medusa expects empty payload (wake all nodes) 
		    // or dotted quad address (wake specific node)
		    String payload=args[++i];
		    payload+="\0";
		    data=payload.getBytes();
		    System.out.println("payload length="+data.length);
		}else
		if(args[i].indexOf("w")>=0){
		    waitForEcho=true;
		}
	    }
	}catch (IOException e){
	    System.out.println("Error: " + e);
	    System.exit(-1);
	}

	try {
	    InetAddress hostAddr = InetAddress.getByName(BCAST_ADDR);

	    // override default socket if specified
	    if(sockAddr!=null){
		if(LOCAL_PORT>0)
		    socket=new DatagramSocket(LOCAL_PORT,sockAddr);
		else
		    socket=new DatagramSocket(DEST_PORT,sockAddr);
	    }else{
		if(LOCAL_PORT>0)
		    socket=new DatagramSocket(LOCAL_PORT);
		// else default socket already created
	    }

	    System.out.println("socket address: "+socket.getLocalAddress()+":"+socket.getLocalPort());
	    System.out.println("packet payload: ["+(new String(data))+"]");
	    System.out.flush();

	    // create BCAST packet
	    packet = new DatagramPacket(data,data.length,hostAddr,DEST_PORT);

	    // something in this println takes a really long time...
	    //System.out.println("cli: Sending packet to "+packet.getAddress()
	    //		       +":"+packet.getPort()
	    //		       +" on socket "+socket.getLocalAddress()+":"+socket.getLocalPort());
	    System.out.println("cli: Sending packet...");
	    System.out.flush();

	    // send packet
	    socket.send(packet);

	    // wait for echoed packet
	    if(waitForEcho){
		System.out.println("cli: Waiting for echo...");
		socket.receive(packet);
		System.out.println("cli: Echo packet received");
	    }

	    socket.close();

	}
	catch (IOException e){
	    System.out.println("Error: " + e);
	    socket.close();
	}
    }
}

