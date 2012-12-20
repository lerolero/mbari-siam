/****************************************************************************/
/* Copyright 2004 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.core;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

import org.mbari.siam.utils.StreamUtils;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.TimeoutException;

/**
CpuLease is responsible for keeping the CPU
awake as requested by external processes.

@author Bob Herlien
*/

public class CpuLease extends Thread
{
    private static Logger _log4j = Logger.getLogger(CpuLease.class);
    public static final int CPULEASE_TCP_PORT = 5505;

    // Wait at most 5 seconds for socket messages
    static final int SO_TIMEOUT = 5000;
    static final int SO_LINGER  = 2;

    /** Server Socket on which we listen for messages */
    ServerSocket _srvSocket = null;

    /** The NodeService singleton */
    NodeService _nodeService = null;


    CpuLease(NodeService nodeService)
    {
	_nodeService = nodeService;
	try
	{
	    _srvSocket = new ServerSocket(CPULEASE_TCP_PORT);
	} catch (IOException e) {
	    _log4j.error("Can't create ServerSocket; exiting", e);
	    return;
	}

	_log4j.info("CpuLease() constructor");

    } /* CpuLeaseSleepRollcallListener() */


    /**
       Wait for and accept new client connection
    */
    public void run()
    {
	_log4j.info("CpuLease started");
	while (true)
	{
	    try
	    {
		new CpuLeaseWorker(_srvSocket.accept()).start();
	    }
	    catch (IOException e) {
		_log4j.error("ServerSocket.accept() failed: " + e);
	    }
	}
    }


    /** CpuLeaseWorker runs in thread launched when a connection 
	has been accepted.
    */
    class CpuLeaseWorker extends Thread
    {
	Socket _socket = null;

	public CpuLeaseWorker(Socket s)
	{
	    _socket = s;
	}

	public void run()
        {
	    _log4j.debug("Connected to node " + _socket.getInetAddress());

	    try
	    {
		// Set a timeout so we don't hang forever
		_socket.setSoTimeout(SO_TIMEOUT);

		// Service the request
		serveRequest(_socket);

	    } catch (Exception e) {
		_log4j.error(e);
	    }

	    // Whether successful or not, try to close the socket
	    try
	    {
		_socket.close();
	    } catch (Exception e) {
		_log4j.error("Exception in socket close", e);
	    }

	    _socket = null;
	}

	void serveRequest(Socket s) throws TimeoutException, Exception,
	   NullPointerException, IOException, NumberFormatException
	{
	    byte[] inbuf = new byte[256];
	    byte[] term  = "\n".getBytes();
	    int	   rqstId;
	    long   when, howLong;
	    int nBytes = 0;

	    if ((nBytes = StreamUtils.readUntil(s.getInputStream(), inbuf,
				      term, SO_TIMEOUT)) <= 0) {
		_log4j.error("serveRequest() - got only " + nBytes + " bytes");
		return;
	    }

	    _log4j.debug("serveRequest(): got " + new String(inbuf));

	    StringTokenizer tok = new StringTokenizer(new String(inbuf));
	    if (!tok.nextToken().equalsIgnoreCase("awake")) {
		_log4j.warn("serveRequest() - unrecognized request: " + 
			     new String(inbuf));
		return;
	    }

	    rqstId = Integer.parseInt(tok.nextToken());
	    when   = Long.parseLong(tok.nextToken());
	    howLong = Long.parseLong(tok.nextToken());
	    _nodeService.cpuLease(rqstId, when, howLong);
	    _log4j.debug("cpuLease(" + rqstId + "," + when + ","
			  + howLong + ")");
	}
    }

} /* CpuLease */
