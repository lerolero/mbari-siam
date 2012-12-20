/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;

/**
   TimeoutServerSocket overrides ServerSocket's accept() 
   method; it sets soTimeout on the returned socket.

   @author Tom O'Reilly
*/
public class TimeoutServerSocket extends ServerSocket
{
    private static int _defaultTimeout = 60000;
    private  int _timeout = _defaultTimeout;

    /** Constructor with timeout */
    public TimeoutServerSocket(int port, int backlog, int timeout)
	throws IOException {
	super(port, backlog);
        _timeout = timeout;
    }
    
    public TimeoutServerSocket(int port)
	throws IOException {
	super(port);
        _timeout = _defaultTimeout;
    }

    public TimeoutServerSocket(int port, int backlog)
	throws IOException {

	super(port, backlog);
    }

    public TimeoutServerSocket(int port, int backlog, InetAddress bindAddr)
	throws IOException {
	super(port, backlog, bindAddr);
    }

    /** Set default timeout.  This is the timeout that applies to
     * the Socket that's returned via accept(), not the ServerSocket
     * itself
     */
    public static void setDefaultTimeout(int timeout)
    {
        _defaultTimeout = timeout;
    }
    
    
    /** Accept a connection and set timeout on its socket. */
    public Socket accept() 
	throws IOException {

	Socket s = super.accept();
	s.setSoTimeout(_timeout);
	return(s);
    }
}

