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

public class RBNBShowSource
{
  public static void main(String[] args) {

    if (args.length < 1) {
      System.err.println("usage: serverHost");
      return;
    }
    
    String serverHost = args[0];
    String match = null;

    Sink sink;
    try {
      sink = new Sink();
      sink.OpenRBNBConnection(serverHost, "rbnbTest");
    }
    catch (Exception e) {
      System.err.println("Exception opening sink: " + e);
      return;
    }
    
    System.out.println("RBNB connection to " + serverHost +
               " has been opened");

    if (args.length > 1) {
	match = args[1];
    }
    
    try {
	sink.RequestRegistration();
	ChannelMap cMap = sink.Fetch(20000);

	ChannelTree cTree = ChannelTree.createFromChannelMap(cMap);
	Iterator it = cTree.rootIterator();

	while (it.hasNext()) {
	    ChannelTree.Node node = (ChannelTree.Node)(it.next());

	    if (node.getType() == ChannelTree.SOURCE)
	    {
		if ((match == null) || (node.getName().startsWith(match)))
		{
		    System.out.println(node.getName());
		}
	    }
	}
    } catch (Exception e) {
      System.err.println("Exception while getting channel tree: " + e);
    }
  }
}
