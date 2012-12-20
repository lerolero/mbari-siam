/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.linkBenchmark2.server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.UnknownServiceException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;
import org.mbari.siam.tests.linkBenchmark2.interfaces.*;
import org.mbari.siam.tests.linkBenchmark2.compression.*;
import org.mbari.siam.core.DeviceLog;
import org.mbari.siam.core.FilteredDeviceLog;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DevicePacketSet;
import org.mbari.siam.distributed.NoDataException;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;

/**
BenchmarkServer is the server side of the DeviceLog benchmark test
using sockets rather than RMI.  It fields requests from SocketBenchmarkProxy
on the client side.
@author Bob Herlien
*/
public class BenchmarkServer extends Thread
{
    static private Logger _logger = Logger.getLogger(BenchmarkServer.class);

    // Wait at most 2 minutes for socket messages
    static final int SO_TIMEOUT = 120000;
    static final int SO_LINGER  = 10;

    /** DeviceLog that we're going to get data from */
    protected DeviceLog _log = null;

    /** Socket on which we listen for connections from client */
    ServerSocket _srvSocket = null;

    public BenchmarkServer(int port) throws IOException
    {
	super();
	PropertyConfigurator.configure(System.getProperties());
	PatternLayout layout = new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");
	BasicConfigurator.configure(new ConsoleAppender(layout));

	// Create server socket to listen for messages from node
	_srvSocket = new ServerSocket(port);
        System.out.println("Done with constructor.");
    }

    protected String className()
    {
	return "BenchmarkServer";
    }

    /**
       Wait for and accept new client connection.
    */
    public void run()
    {
	_logger.info(className() + " started.");

	while (true)
	{
	    try
	    {
		new BenchmarkWorker(_srvSocket.accept()).start();
	    }
	    catch (IOException e) {
		_logger.error("ServerSocket.accept() failed: " + e);
	    }
	}
    }


    /** Process one command object, return a response object
     */
    Object cmdResponse(String cmdline) throws Exception
    {

	StringTokenizer st = new StringTokenizer(cmdline);
	String cmdKeyword = st.nextToken();

	if (cmdKeyword.equals("exit"))
	    return null;

	if (cmdKeyword.equals("setDeviceLog"))
	{
	    long sensorId = Long.parseLong(st.nextToken());
	    _log = new DeviceLog(sensorId, st.nextToken());
	    return(new Integer(_log.nPackets()));
	}
	if (cmdKeyword.equals("setFilteredDeviceLog"))
	{
	    long sensorId = Long.parseLong(st.nextToken());
	    _log = new FilteredDeviceLog(sensorId, st.nextToken());
	    return(new Integer(_log.nPackets()));
	}
	else if (cmdKeyword.equals("getPackets"))
	{
	    long startKey = Long.parseLong(st.nextToken());
	    long endKey = Long.parseLong(st.nextToken());
	    int maxEntries = Integer.parseInt(st.nextToken());
	    if (_log == null)
		return(new NoDataException("No log opened"));
	    else
		return(_log.getPackets(startKey, endKey, maxEntries));
	}
	else if (cmdKeyword.equals("getCompressedPackets"))
	{
	    long startKey = Long.parseLong(st.nextToken());
	    long endKey = Long.parseLong(st.nextToken());
	    int maxEntries = Integer.parseInt(st.nextToken());

	    if (_log == null)
		return(new NoDataException("No log opened"));
	    else
	    {
		ObjectGZipper deflater = new ObjectGZipper();
		return(deflater.compress(_log.getPackets(startKey, endKey, maxEntries)));
	    }
	}
	else
	{
	    return(new UnknownServiceException("Unknown cmd: " + cmdKeyword));
	}
    } /* cmdReply() */


    /** BenchmarkWorker runs in thread launched when a connection 
	has been accepted.
    */
    class BenchmarkWorker extends Thread
    {
	Socket _socket = null;
	ObjectInputStream _inStream = null;
	ObjectOutputStream _outStream = null;

	public BenchmarkWorker(Socket s)
	{
	    _socket = s;
	}

	public void run()
        {
	    _logger.debug("Connected to node " + _socket.getInetAddress());

	    try
	    {
		// Set a timeout so we don't hang forever
		// _socket.setSoTimeout(SO_TIMEOUT);
		_socket.setSoLinger(true, SO_LINGER);

		_outStream = new ObjectOutputStream(_socket.getOutputStream());
		_outStream.flush();
		_inStream = new ObjectInputStream(_socket.getInputStream());

		// Service the NodeProxy requests
		serveSession();

	    } catch (IOException e) {
		_logger.error("Exception in BenchmarkServer:", e);
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
	    Object response;

	    while(true)
	    {
		try
		{
		    String cmdline = (String)(_inStream.readObject());
		    _logger.debug("Benchmarkserver got cmdline: " + cmdline);
		    response = cmdResponse(cmdline);
		    if (response == null)
			return;
		    _outStream.writeObject(response);

		    // Flush any output
		    _outStream.flush();
		} catch (Exception e) {
		    if (e instanceof IOException)
			throw (IOException)e;
		    else
		    {
			_logger.debug("serveSession: sending caught exception to peer: "
				      +  e.toString());
			_outStream.writeObject(e);
			_outStream.flush();
		    }
		}
	    }
	} /* servSession() */

    } /* class BenchmarkWorker */

    public static void main(String args[])
    {
	try {
	    BenchmarkServer _server = new BenchmarkServer(Benchmark2.SOCKET_TCP_PORT);

	    _server.run();
	} catch (Exception e) {
	    _logger.error("Error in starting BenchmarkServer: " + e);
	    e.printStackTrace();
	}
    }

} /* class BenchmarkServer */
