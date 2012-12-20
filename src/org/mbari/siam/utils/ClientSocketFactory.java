/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils;

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
ClientSocketFactory creates client sockets that timeout more
cleanly than the default RMI stuff. SIAM service clients may find this
class to be useful.
 */
public class ClientSocketFactory extends RMISocketFactory
{
    static private Logger _logger = 
	Logger.getLogger(ClientSocketFactory.class);

    protected RMISocketFactory _defaultFactory;
    protected int _connectTimeout = Portals.DEFAULT_WIRELESS_SOCKET_TIMEOUT;
    protected int _readTimeout = Portals.DEFAULT_WIRELESS_SOCKET_TIMEOUT;
    protected Socket _socket;

    /** Create the socket factory, specifying the inet name of the remote
     node server. */
    public ClientSocketFactory(int connectTimeout, int readTimeout) {
	super();
	_connectTimeout = connectTimeout;
	_readTimeout = readTimeout;

	try {
	    _logger.info("NodeGUISocketFactory: local host = " +
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

	_logger.debug("NodeGUISocketFactory.createServerPort() - port=" + 
			   port);

	_logger.debug("return new ServerSocket(port)...");
	return new ServerSocket(port);
    }

    /** Create a client socket; socket gets a timeout setting. */
    public Socket createSocket(String host, int port)
	throws IOException {

	_logger.debug("NodeGUISocketFactory.createSocket() - host=" + 
		      host + ", port=" + port);

	_logger.debug("Create new Socket(" + host + "," + port + ")...");

	_socket = null;

	SocketCreator thread = new SocketCreator(host, port);
        thread.start();

	try {
	    thread.join(_connectTimeout);
	}
	catch (InterruptedException e) {
	}

	if (_socket != null) {
	    _logger.debug("got new socket!");

	    // Do stuff to the socket...
	    try {
		_socket.setSoTimeout(_readTimeout);
	    }
	    catch (SocketException e) {
		_logger.error("SocketException: " + e);
	    }

	    return _socket;
	}
	else {
	    throw new IOException("Socket creation timed out");
	}
    }


    class SocketCreator extends Thread {
	String _host;
	int _port;

	SocketCreator(String host, int port) {
	    _host = host;
	    _port = port;
	}

	public void run() {
	    try {
		_socket = new Socket(_host, _port);
	    }
	    catch (UnknownHostException e) {
	    }
	    catch (IOException e) {
	    }
	}
    }

}
