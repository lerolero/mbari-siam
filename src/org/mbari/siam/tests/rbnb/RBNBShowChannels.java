/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.rbnb;

import com.rbnb.sapi.Sink;
import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.ChannelTree;
import java.util.Iterator;

public class RBNBShowChannels
{
    public static void main(String[] args) {

	if (args.length < 1) {
	    System.err.println("usage: serverHost");
	    return;
	}
    
	String serverHost = args[0];
	String match = null;
	ChannelMap cMap = new ChannelMap();

	Sink sink = new Sink();

	try {
	    sink.OpenRBNBConnection(serverHost, "rbnbTest");
	} catch (Exception e) {
	    System.err.println("Exception opening sink: " + e);
	    return;
	}
    
	System.out.println("RBNB connection to " + serverHost +
			   " has been opened");

	if (args.length > 1) {
	    match = args[1];
	}
    
	try {
	    sink.RequestRegistration(cMap);
	    cMap = sink.Fetch(20000, cMap);

	} catch (Exception e) {
	    System.out.println("Exception fetching channelMap: " + e);
	    return;
	}
    
	System.out.println("Fetched channelMap: ");
	String[] channels = cMap.GetChannelList();

	for (int i = 0; i < channels.length; i++)
	    System.out.println(channels[i]);
    }
}
