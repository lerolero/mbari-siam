/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.server.RMISocketFactory;

import org.mbari.siam.distributed.portal.Portals;
import org.mbari.siam.utils.TimeoutServerSocket;

import org.apache.log4j.Logger;

/**
   Socket factory used on the node.
 */
public class NodeSocketFactory extends RMISocketFactory {

    static Logger _logger = Logger.getLogger(NodeSocketFactory.class);

    protected static final int DEFAULT_TIMEOUT = 20000;
    protected RMISocketFactory _defaultFactory;
    protected InetAddress _localHostAddr = null;
    protected int _readTimeout = DEFAULT_TIMEOUT;

    public NodeSocketFactory() {
	super();

	_defaultFactory = RMISocketFactory.getSocketFactory();

	// Get the address of the local host
	try {
	    _localHostAddr = InetAddress.getLocalHost();
	}
	catch (UnknownHostException e) {
	    _logger.error("NodeSocketFactory constructor", e);
	}
	catch (SecurityException e) {
	    _logger.error("NodeSocketFactory constructor", e);
	}
    }


    public ServerSocket createServerSocket(int port) 
	throws IOException {

	_logger.debug("NodeSocketFactory.createServerSocket() - port=" + port);
	_logger.debug("return new ServerSocket(port)...");
	return new TimeoutServerSocket(port, 3, 
				       Portals.DEFAULT_WIRELESS_SOCKET_TIMEOUT);
    }


    /**
       Create client-side socket.
       Note that some node components may talk to the node server or
       device servers via RMI. So we must ensure that these invocations
       work in absence of an enabled network interface.
     */
    public Socket createSocket(String serverHost, int port)
	throws IOException {

	try {
	    InetAddress serverAddr = InetAddress.getByName(serverHost);
	    if (_localHostAddr == null) {
		_logger.error("NodeSocketFactory.createSocket() - don't " + 
			      "know local host address!");
	    }
	    else if (serverAddr.equals(_localHostAddr)) {
		_logger.debug("NodeSocketFactory.createSocket() - connect to " +
			     " \"localhost\"");

//		_logger.debug("trace", new Exception("Called from"));
		serverHost = "localhost";
	    }
	}
	catch (UnknownHostException e) {
	    _logger.error("NodeSocketFactory.createSocket()", e);
	}

	_logger.debug("NodeSocketFactory.createSocket() - serverHost=" + 
		      serverHost);

	Socket socket = new Socket(serverHost, port);

	// Do stuff to the socket...
	try {
	    socket.setSoTimeout(_readTimeout);
	}
	catch (SocketException e) {
	    _logger.error("SocketException: ", e);
	}
	return socket;
    }
}
