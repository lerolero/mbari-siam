/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.linkBenchmark2.client;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.Socket;
import java.rmi.UnexpectedException;
import org.mbari.siam.tests.linkBenchmark2.interfaces.*;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DevicePacketSet;
import org.mbari.siam.distributed.NoDataException;
import org.apache.log4j.Logger;


/** 
    Benchmark2Test that uses RMI for communication with server.
*/

public class SocketBenchmarkProxy implements Benchmark2
{
    /** Log4j logger */
    protected static Logger _logger = Logger.getLogger(SocketBenchmarkProxy.class);

    // Wait at most 2 minutes for socket messages
    static final int SO_TIMEOUT = 120000;
    static final int SO_LINGER  = 10;

    /** Socket to target Node on which we get Node services */
    Socket	 _socket = null;
    ObjectInputStream _inStream = null;
    ObjectOutputStream _outStream = null;

    /**
       Connect to the Node Server. Return true on success, 
       false on error.
    */
    public void connectNode(String serverName, int port) throws Exception
    {
	_logger.debug("Looking for node server at " + serverName);

	if (_socket != null)
	    disconnectNode();

	try
	{
	    _socket = new Socket(serverName, port);
	    _logger.debug("Connected to node " + serverName);
	} catch (Exception e) {
	    _logger.error("Can't connect to host " + serverName, e);
	    _socket = null;
	    _inStream = null;
	    _outStream = null;
	    throw e;
	}

	try
	{
	    // Set a timeout so we don't hang forever
	    _socket.setSoTimeout(SO_TIMEOUT);
	    _socket.setSoLinger(true, SO_LINGER);
	    setupStreams();
	}	
	catch (Exception e) {
	    _logger.error("connectNode() - Exception: ", e);
	    throw e;
	}

	_logger.debug("connectNode() done");
    }

    /** Set up the ObjectxxxStreams */
    public void setupStreams() throws IOException, Exception
    {
	_outStream = new ObjectOutputStream(_socket.getOutputStream());
	_outStream.flush();
	_inStream = new ObjectInputStream(_socket.getInputStream());
    }


    /** Disconnect from the Node Server */
    public void disconnectNode() {
	_logger.debug("disconnectNode()");

	if (_socket != null)
	    try
	    {
		_outStream.writeObject("exit");
		_outStream.flush();
		_outStream.close();
		_socket.close();
	    } catch (Exception e) {
		_logger.error("Exception in socket close", e);
	    }

	_socket = null;
	_inStream = null;
	_outStream = null;
    }


    /** 
	Returns true if portal currently has "live" connection
	to remote node.
    */
    public boolean nodeConnected()
    {
	return(_socket != null);
    }


    /** Internal function to send string and return Object */
    protected Object getRemoteObject(String cmd) throws IOException
    {
	if (_socket == null)
	    throw new IOException("Not connected to remote node");

	try
	{
	    _outStream.writeObject(cmd);
	    _outStream.flush();
	    return(_inStream.readObject());
	} catch (Exception e) {
	    return(e);
	}
    }

    protected void checkReturn(Object obj, Class cl)
	throws UnexpectedException
    {
	if (obj instanceof Exception)
	    throw new UnexpectedException("Nested Exception is " + obj);
	if ((cl != null) && !cl.isInstance(obj))
	    throw new UnexpectedException("Unexpected returned class: "
					  + obj.getClass().toString());
    }

    protected Object getAndCheckRemoteObject(String cmd, Class cl)
	throws IOException, UnexpectedException
    {
	Object rtn = getRemoteObject(cmd);
	checkReturn(rtn, cl);
	return(rtn);
    }

    /** Internal function to send string and return Long */
    protected long getRemoteLong(String cmd)
	throws IOException, UnexpectedException
    {
	return(((Long)getAndCheckRemoteObject(cmd, Long.class)).longValue());
    }


    /** Sets DeviceLog for later getPackets(), returns total number of entries in DeviceLog */
    public int setDeviceLog(long sensorId, String directory)
	throws IOException, FileNotFoundException
    {
	Object rtn = getRemoteObject("setDeviceLog " + sensorId + " " + directory);
	if (rtn instanceof FileNotFoundException)
	    throw (FileNotFoundException)rtn;
	checkReturn(rtn, Integer.class);
	return(((Integer)rtn).intValue());
    }
  
    /** Uses FilteredDeviceLog for later getPackets(), returns total number of entries in DeviceLog */
    public int setFilteredDeviceLog(long sensorId, String directory)
	throws IOException, FileNotFoundException
    {
	Object rtn = getRemoteObject("setDeviceLog " + sensorId + " " + directory);
	if (rtn instanceof FileNotFoundException)
	    throw (FileNotFoundException)rtn;
	checkReturn(rtn, Integer.class);
	return(((Integer)rtn).intValue());
    }
  
    /** Gets DevicePacketSets from DeviceLog named in setDeviceLog() */
    public DevicePacketSet getPackets(long startKey, long endKey, int maxEntries)
	throws IOException, NoDataException
    {
	Object rtn = getRemoteObject("getPackets " +  startKey + " " + endKey + " " + maxEntries);
	if (rtn instanceof NoDataException)
	    throw (NoDataException)rtn;
	checkReturn(rtn, DevicePacketSet.class);
	return((DevicePacketSet)rtn);
    }

    /** Gets compressed DevicePacketSets from DeviceLog named in setDeviceLog() */
    public byte[] getCompressedPackets(long startKey, long endKey, int maxEntries)
	throws NoDataException, InvalidClassException, NotSerializableException, IOException
    {
	Object rtn = getRemoteObject("getCompressedPackets " +  startKey + " " + endKey + " " + maxEntries);
	if (rtn instanceof NoDataException)
	    throw (NoDataException)rtn;
	checkReturn(rtn, byte[].class);
	return((byte[])rtn);
    }
}
