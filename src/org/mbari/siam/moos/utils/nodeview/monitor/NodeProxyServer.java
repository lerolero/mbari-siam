// Copyright MBARI 2003
package org.mbari.siam.moos.utils.nodeview.monitor;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.LocateRegistry;
import java.rmi.Naming;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URL;

import java.io.OutputStream;
import java.io.IOException;

import java.text.DateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.PortNotFound;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.NoDataException;

public class NodeProxyServer  {

    public static void main(String[] args) 
	throws RemoteException{

	// Start rmiregistry
	try {
	    System.out.println("Starting registry...");
	    LocateRegistry.createRegistry(1099);
	    System.out.println("Started registry");
	}
	catch (RemoteException e) {
	    // Already running on port 1099?
	    System.err.println(e.getMessage());
	}


	try{
	    System.out.println("NodeProxyServer creating implementations...");

	    NodeProxyImpl s = new NodeProxyImpl();
	    System.out.println("NodeProxyServer binding implementations to RMI registry...");
	    Naming.rebind("sampler",s);

	    System.out.println("NodeProxyServer waiting for customers...");
	}catch(Exception e){
	    System.err.println("NodeProxyServer error:"+e);
	}
    }

}



