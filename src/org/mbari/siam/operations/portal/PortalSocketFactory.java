/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.portal;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.Socket;
import java.net.SocketException;
import java.net.ServerSocket;
import java.rmi.server.RMISocketFactory;
import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.mbari.siam.distributed.portal.Portals;

/**
PortalSocketFactory creates client sockets that timeout more
cleanly than the default RMI stuff
 */
public class PortalSocketFactory extends RMISocketFactory
{
    static private Logger _logger = 
	Logger.getLogger(PortalSocketFactory.class);

    private int _hashCode = "PortalSocketFactory".hashCode();
    protected RMISocketFactory _defaultFactory;
    protected int _readTimeout = Portals.DEFAULT_WIRELESS_SOCKET_TIMEOUT;

    /** Create the socket factory, specifying the inet name of the remote
     node server. */
    public PortalSocketFactory(int readTimeout) {
	super();
	_readTimeout = readTimeout;

	try {
	    _logger.info("PortalSocketFactory: local host = " +
			 InetAddress.getLocalHost());
	}
	catch (UnknownHostException e) {
	    _logger.error("UnknownHostException: " + e);
	}

	_defaultFactory = RMISocketFactory.getSocketFactory();
    }

    /** Create a server socket; just returns a plain vanilla 
	java.net.ServerSocket. */
    public ServerSocket createServerSocket(int port) 
	throws IOException {

	_logger.debug("PortalSocketFactory.createServerPort() - port=" + 
			   port);

	ServerSocket srvsocket = new ServerSocket(port);
	_logger.debug("PortalSocketFactory returned new ServerSocket(port)...");
	return(srvsocket);
    }

    /** Create a client socket; socket gets a timeout setting. */
    public Socket createSocket(String host, int port)
	throws IOException {

	_logger.debug("PortalSocketFactory.createSocket() - host=" + 
		      host + ", port=" + port);

	_logger.debug("return new Socket(" + host + "," + port + ")...");

	Socket socket = new Socket(host, port);

	// Do stuff to the socket...
	try {
	    socket.setSoTimeout(_readTimeout);
	}
	catch (SocketException e) {
	    _logger.error("SocketException: " + e);
	}

	_logger.debug("PortalSocketFactory returned new Socket("
		      + host + "," + port + ")");
	return socket;
    }

    public boolean equals(Object obj)
    {
	if (obj instanceof PortalSocketFactory)
	    return(true);
	return(false);
    }

    public int hashCode()
    {
	return(_hashCode);
    }

}
