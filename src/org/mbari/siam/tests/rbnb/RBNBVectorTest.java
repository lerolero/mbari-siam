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

public class RBNBVectorTest
{
    public static void main(String[] args) {

	if (args.length < 1) {
	    System.err.println("usage: serverHost");
	    return;
	}
    
	String serverHost = args[0];
	String matchStr = "Vector";
	Sink sink = new Sink();
	ChannelMap cMap = new ChannelMap(), resultMap = new ChannelMap();

	try {
	    sink.OpenRBNBConnection(serverHost, "rbnbTest");
	} catch (Exception e) {
	    System.err.println("Exception opening sink: " + e);
	    return;
	}
    
	System.out.println("RBNB connection to " + serverHost +
			   " has been opened");

	if (args.length > 1) {
	    matchStr = args[1];
	}
        
	try {
	    sink.RequestRegistration();
	    ChannelMap reqMap = sink.Fetch(20000);
	    String[] channels = reqMap.GetChannelList();

	    for (int i = 0; i < channels.length; i++) {
		if ((channels[i].startsWith(matchStr)) && 
		    (channels[i].indexOf("avgVelocity") >= 0)) {
		    System.out.println("Adding " + channels[i]);
		    cMap.Add(channels[i]);
		}
	    }

	} catch (Exception e) {
	    System.out.println("Exception looking up channels: " + e);
	    return;
	}
    
	if (cMap.NumberOfChannels() <= 0) {
	    System.out.println("Found no matching channels.  Exiting");
	    return;
	}

	try {
	    sink.Subscribe(cMap);

	    while (true) {
		sink.Fetch(30000, resultMap);
		for (int i = 0; i < resultMap.NumberOfChannels(); i++) {
		    double[] avgData = resultMap.GetDataAsFloat64(i);
		    System.out.println(resultMap.GetName(i) + ": " + avgData[0]);
		}
	    }
	} catch (Exception e) {
	    System.out.println("Exception getting channel data: " + e);
	    return;
	}

    }
}

