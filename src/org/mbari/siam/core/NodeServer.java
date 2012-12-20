/****************************************************************************/
/* Copyright 2004 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.core;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownServiceException;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

/**
NodeServer is one half of an RMI replacement using Sockets.
It receives method requests from org.mbari.siam.operations.utils.NodeProxy,
and sends responses back.
@author Bob Herlien
*/
public class NodeServer extends Thread
{
    static private Logger _logger = Logger.getLogger(NodeServer.class);
    static final int NODE_TCP_PORT = 5503;

    // Wait at most 2 minutes for socket messages
    static final int SO_TIMEOUT = 120000;
    static final int SO_LINGER  = 10;

    /** Reference to node's NodeService */
    NodeService _nodeService = null;

    /** Socket on which we listen for messages from node that we're
	connected */
    ServerSocket _srvSocket = null;

    /**
       Construct NodeServer for specified node.
    */
    public NodeServer(NodeService nodeService) throws IOException
    {
	// Save reference to NodeService
	_nodeService = nodeService;
	// Create server socket to listen for messages from node
	_srvSocket = new ServerSocket(NODE_TCP_PORT);
    }

    /**
       Wait for and accept new client connection; add new client to list
       of clients.
    */
    public void run()
    {
	_logger.info("NodeServer started");
	while (true)
	{
	    try
	    {
		new NodeWorker(_srvSocket.accept()).start();
	    }
	    catch (IOException e) {
		_logger.error("ServerSocket.accept() failed: " + e);
	    }
	}
    }


    /** NodeWorker runs in thread launched when a connection 
	has been accepted.
    */
    class NodeWorker extends Thread
    {
	Socket _socket = null;
	ObjectInputStream _inStream = null;
	ObjectOutputStream _outStream = null;

	public NodeWorker(Socket s)
	{
	    _socket = s;
	}

	public void run()
        {
	    _logger.debug("Connected to node " + _socket.getInetAddress());

	    try
	    {
		// Set a timeout so we don't hang forever
		_socket.setSoTimeout(SO_TIMEOUT);
		_socket.setSoLinger(true, SO_LINGER);

		_outStream = new ObjectOutputStream(_socket.getOutputStream());
		_outStream.flush();
		_inStream = new ObjectInputStream(_socket.getInputStream());

		// Service the NodeProxy requests
		serveSession();

	    } catch (IOException e) {
		_logger.error("Exception in NodeServer:", e);
	    }

	    try
	    {
		_outStream.flush();
		_outStream.close();
	    } catch (Exception e) {
		_logger.error("Exception in outStream close", e);
	    }

	    // Whether successful or not, try to close the socket
	    try
	    {
		_socket.close();
	    } catch (Exception e) {
		_logger.error("Exception in socket close", e);
	    }

	    _socket = null;
	    _inStream = null;
	    _outStream = null;
	}

	/** Method to service commands sent on socket
	 */
	void serveSession() throws IOException
	{
	    while(true)
	    {
		try
		{
		    String cmdline = (String)(_inStream.readObject());
		    StringTokenizer st = new StringTokenizer(cmdline);
		    String cmdKeyword = st.nextToken();
		    _logger.debug("Nodeserver got cmdline: " + cmdline);

		    if (cmdKeyword.equals("exit"))
			return;
		    if (cmdKeyword.equals("getName"))
			_outStream.writeObject(
				       new String(_nodeService.getName()));
		    else if (cmdKeyword.equals("getId"))
			_outStream.writeObject(new Long(_nodeService.getId()));
		    else if (cmdKeyword.equals("getPorts"))
			_outStream.writeObject(_nodeService.getPorts());
		    else if (cmdKeyword.equals("getDevicePackets"))
		    {
			long sensorId = Long.parseLong(st.nextToken());
			long startTime = Long.parseLong(st.nextToken());
			long endTime = Long.parseLong(st.nextToken());
			_outStream.writeObject(
			   _nodeService.getDevicePackets(sensorId,
							 startTime, endTime));
		    }
		    else if (cmdKeyword.equals("establishLease"))
		    {
			long ms = Long.parseLong(st.nextToken());
			_outStream.writeObject(
			    new Integer(_nodeService.establishLease(ms, "NodeServer".getBytes())));
		    }
		    else if (cmdKeyword.equals("renewLease"))
		    {
			int leaseId = Integer.parseInt(st.nextToken());
			long ms = Long.parseLong(st.nextToken());
			_nodeService.renewLease(leaseId, ms);
			//Write dummy object to let NodeProxy know we did it
			_outStream.writeObject(new Integer(0));
		    }
		    else if (cmdKeyword.equals("terminateLease"))
		    {
			int leaseId = Integer.parseInt(st.nextToken());
			_nodeService.terminateLease(leaseId);
			//Write dummy object to let NodeProxy know we did it
			_outStream.writeObject(new Integer(0));
		    }
		    else
		    {
			_outStream.writeObject(new UnknownServiceException(
					       "Unknown cmd: " + cmdKeyword));
		    }
		    // Flush any output
		    _outStream.flush();
		} catch (Exception e) {
		    if (e instanceof IOException)
			throw (IOException)e;
		    else
		    {
			_logger.debug(
			  "serveSession: sending caught exception to peer: "
			  +  e.toString());
			_outStream.writeObject(e);
			_outStream.flush();
		    }
		}
	    }
	} /* servSession() */
    } /* class NodeWorker */

} /* class NodeServer */
