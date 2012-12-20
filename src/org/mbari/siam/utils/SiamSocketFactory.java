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
import java.net.ServerSocket;
import java.rmi.server.RMISocketFactory;
import org.apache.log4j.Logger;


public class SiamSocketFactory extends RMISocketFactory {

    static Logger _logger = Logger.getLogger(SiamSocketFactory.class);

    protected RMISocketFactory _defaultFactory;
    protected String _serverHost = null;

    public SiamSocketFactory(String serverHost) {
	super();
	_serverHost = serverHost;

	_defaultFactory = RMISocketFactory.getSocketFactory();
    }


    public ServerSocket createServerSocket(int port) 
	throws IOException {

	_logger.debug("SiamSocketFactory.createServerPort() - port=" + port);
	_logger.debug("return new ServerSocket(port)...");
	return new ServerSocket(port);
    }

    public Socket createSocket(String host, int port)
	throws IOException {


	_logger.debug("return new Socket(host, port)...");
	Socket socket = new Socket(_serverHost, port);
	_logger.debug("SiamSocketFactory.createSocket() - host=" + 
		      host + ", port=" + port + ", server host=" + 
		      _serverHost + ", soTimeout=" + socket.getSoTimeout());

	return socket;
    }
}
