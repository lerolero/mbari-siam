/****************************************************************************/
/* Copyright 2003 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.operations.utils;

import java.io.IOException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.AccessException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.server.RMISocketFactory;
import java.io.IOException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;
import org.mbari.siam.utils.SiamSocketFactory;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.RemoteSerialPort;
import org.mbari.siam.distributed.PortOccupiedException;
import org.mbari.siam.distributed.PortNotFound;

/** 
    GetRemoteSerialPortParams creates a RemoteSerialPort and returns it's 
    address and port number to stdout
*/

public class GetRemoteSerialPortParams
{
    Node _node;
    RemoteSerialPort _remoteSerialPort;

    public GetRemoteSerialPortParams(String nodeURL, String portName) 
	throws NotBoundException, RemoteException, AccessException,
	       MalformedURLException, UnknownHostException, 
	       PortOccupiedException,
	       IOException, PortNotFound
    {
	// Look for service on specified port
	_node = (Node )Naming.lookup(nodeURL);
	_remoteSerialPort = _node.getRemoteSerialPort(portName.getBytes());
        System.out.println( _remoteSerialPort.getServerInetAddress().getHostAddress() +
                            ":" + _remoteSerialPort.getServerPort());
        
    }
    
    public static void main(String[] args) 
    {
	// Configure Log4J
	PropertyConfigurator.configure(System.getProperties());
	BasicConfigurator.configure();

	if (args.length != 2) 
        {
	    System.err.println("Usage: nodeURL portName");
	    return;
	}

        String nodeURL = NodeUtility.getNodeURL(args[0]);
        String portName = PortUtility.getPortName(args[1]);

	// Create socket factory; overcomes problems with RMI 'hostname'
	// property.
	try {
	    String host = NodeUtility.getHostName(nodeURL);
	    RMISocketFactory.setSocketFactory(new SiamSocketFactory(host));
	}
	catch (MalformedURLException e) {
	    System.err.println("Malformed URL \"" + nodeURL + "\": " + 
			       e.getMessage());
	}
	catch (IOException e) {
	    System.err.println("RMISocketFactory.setSocketFactory() failed");
	    System.err.println(e);
	}

	try 
        {
	    GetRemoteSerialPortParams app = new GetRemoteSerialPortParams(nodeURL, portName);
        }
	catch (Exception e) 
        {
	    System.err.println(e);
	    return;
	}
        
        return;
    }
}
