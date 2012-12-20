/****************************************************************************/
/* Copyright 2004 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.operations.utils;

import java.io.IOException;
import java.net.Socket;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.UnexpectedException;
import java.lang.reflect.Array;
import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Port;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DevicePacketSet;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.MetadataPacket;
import org.mbari.siam.distributed.DeviceMessagePacket;
import org.mbari.siam.distributed.DevicePacketStream;
import org.mbari.siam.distributed.DevicePacketOutputStream;
import org.mbari.siam.distributed.DevicePacketServerThread;
import org.mbari.siam.distributed.Authentication;
import org.mbari.siam.distributed.AuthenticationException;
import org.mbari.siam.distributed.InvalidDataException;
import org.mbari.siam.distributed.Location;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.TooMuchDataException;
import org.mbari.siam.operations.utils.NodeProxy;
import org.mbari.siam.distributed.leasing.LeaseRefused;
import org.mbari.siam.distributed.portal.Portals;
import org.apache.log4j.Logger;


/** 
    Uses Socket I/O instead of RMI to talk to Node.
    Requires NodeServer on Node side to act as its peer.
    @author Bob Herlien
*/

public class NodeProxy
{
    static final int NODE_TCP_PORT = 5503;
    // Wait at most 2 minutes for socket messages
    static final int SO_TIMEOUT = 120000;
    static final int SO_LINGER  = 10;

    static private Logger _logger = Logger.getLogger(NodeProxy.class);

    /** Socket to target Node on which we get Node services */
    InetAddress	 _inAddr = null;
    Socket	 _socket = null;
    ObjectInputStream _inStream = null;
    ObjectOutputStream _outStream = null;


    /**
       Connect to the Node Server. Return true on success, 
       false on error.
    */
    public boolean connectNode(InetAddress inAddr)
    {
	_logger.debug("Looking for node server at " + inAddr.getHostName());

	if (_socket != null)
	    disconnectNode();

	try
	{
	    _socket = new Socket(inAddr, Portals.nodeTCPPort());
	    _logger.debug("Connected to node " + inAddr);
	} catch (Exception e) {
	    _logger.error("Can't connect to host " + inAddr, e);
	    _socket = null;
	    _inStream = null;
	    _outStream = null;
	    return(false);
	}

	try
	{
	    // Set a timeout so we don't hang forever
	    _socket.setSoTimeout(SO_TIMEOUT);
	    _socket.setSoLinger(true, SO_LINGER);

	    _outStream = new ObjectOutputStream(_socket.getOutputStream());
	    _outStream.flush();
	    _inStream = new ObjectInputStream(_socket.getInputStream());
	}	
	catch (Exception e) {
	    _logger.error("connectNode() - Exception: ", e);
	}

	_logger.debug("connectNode() done");
	return(true);
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


    /** Return InetAddress of device service host. */
    public InetAddress host() throws IOException
    {
	if (_socket == null)
	    throw new IOException("Not connected to remote node");
	return(_socket.getInetAddress());
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


    /** Name of Node service class. */
    public byte[] getName() throws IOException, UnexpectedException
    {
	Object rtn = getAndCheckRemoteObject("getName", String.class);
	return(((String)rtn).getBytes());
    }

    /** Unique identifier for Node instance */
    public long getId() throws IOException, UnexpectedException
    {
	return(getRemoteLong("getId"));
    }


    /** Get array of Node's Port objects. */
    public Port[] getPorts() throws IOException, UnexpectedException
    {
	return((Port[])getAndCheckRemoteObject("getPorts", Port[].class));
    }

    
    /**
       Get DevicePacket objects, from specified sensor, within
       specified time window.
    */
    public DevicePacketSet getDevicePackets(long sensorID, 
					    long startTime, long endTime)
	throws IOException, UnexpectedException, DeviceNotFound,
	       NoDataException
    {
	Object rtn = getRemoteObject("getDevicePackets " + sensorID +
				     " " + startTime + " " + endTime);
	if (rtn instanceof DeviceNotFound)
	    throw (DeviceNotFound)rtn;
	if (rtn instanceof NoDataException)
	    throw (NoDataException)rtn;
	checkReturn(rtn, DevicePacketSet.class);
	return((DevicePacketSet)rtn);
    }


    /** Request a lease of the Node's comms medium
	@param leaseMillisec lease period in milliseconds 
	@return leaseID for use with renewLease(), terminateLease().
	Will always be >= 1.
    */
    public int establishLease(long leaseMillisec) throws IOException,
               UnexpectedException, LeaseRefused
    {
	Object rtn = getRemoteObject("establishLease " + leaseMillisec);
	if (rtn instanceof LeaseRefused)
	    throw (LeaseRefused)rtn;
	checkReturn(rtn, Integer.class);
	return(((Integer)rtn).intValue());
    }

    /** Renew a lease with the Node's comms medium
	@param leaseID lease ID returned by establishLease()
	@param leaseMillisec lease period in milliseconds
    */
    public void renewLease(int leaseID, long leaseMillisec) 
	throws IOException, UnexpectedException, LeaseRefused
    {
	Object rtn = getRemoteObject("renewLease " + leaseID + 
				     " " + leaseMillisec);
	if (rtn instanceof LeaseRefused)
	    throw (LeaseRefused)rtn;
	checkReturn(rtn, Object.class);
    }

    /** Terminate the session with the communications link.
	@param leaseID lease ID returned by establishLease()
    */
    public void terminateLease(int leaseID)
	throws IOException, UnexpectedException
    {
	Object rtn = getRemoteObject("terminateLease " + leaseID);
	checkReturn(rtn, Object.class);
    }
}
