/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

/** Various attributes and classes associated with ShoreMessaging */
public class ShoreMessagingHelper {

    /** Prefix marks "standard" header */
    public static final String MESSAGE_PREFIX = "||#";

    /** Name of the service */
    static public final String SERVICE_NAME = "ShoreMessaging";

    /** Multiple messages in one downlink are separated by MSG_DELIMITER */
    public static final byte[] MSG_DELIMITER = "|||".getBytes();

    public static class ConnectFailed extends Exception {

	public ConnectFailed() {
	    super();
	}

	public ConnectFailed(String msg) {
	    super(msg);
	}
    }

    public static class ModemInUse extends ConnectFailed {
    }

    public static class MessageTooBig extends Exception {

	public MessageTooBig(String msg) {
	    super(msg);
	}
    }

    /** Message to be downlinked */
    public static class Message {

	protected byte[] _messageBytes;
	boolean _solitaryDownlink = false;

	public Message(byte[] messageBytes) {

	    _messageBytes = new byte[messageBytes.length];

	    System.arraycopy(messageBytes, 0,
			     _messageBytes, 0, messageBytes.length);
	}

	/** Return message bytes */
	public byte[] getBytes() {
	    return _messageBytes;
	}

	/** Enable/disable solitary downlink for this message */
	public void solitaryDownlink(boolean value) {
	    _solitaryDownlink = value;
	}

	/** Return true if this message to be downlinked by itself (i.e.
	    not aggregated with other messages) */
	public boolean solitaryDownlink() {
	    return _solitaryDownlink;
	}
    }

    /** Bind service to localhost */
    public static void bind(ShoreMessaging service) throws Exception {
	// Start rmiregistry
	try {
	    LocateRegistry.createRegistry(1099);
	}
	catch (RemoteException e) {
	    // Already running on port 1099?
	    ;
	}

	// Bind to localhost, so bind() succeeds in absence of
	// network connection.
	String url = "rmi://localhost/" + SERVICE_NAME;

	Naming.bind(url, service);
    }

    /** Prepend "standard" header to message */
    public static byte[] prependHeader(byte[] msgBody, long sourceID,
				       long epochMsec) {

	// Header timestamp gets converted to epoch MSEC
	String header = new String(MESSAGE_PREFIX + sourceID + "," + 
				   epochMsec/1000 + ", ");

	byte[] fullMsg = new byte[header.length() + msgBody.length];

	// Concatenate header with messsage bytes
	System.arraycopy(header.getBytes(), 0, 
			 fullMsg, 0,
			 header.length());

	System.arraycopy(msgBody, 0, 
			 fullMsg, header.length(), 
			 msgBody.length);

	return fullMsg;
    }

    /** Return service proxy from specified host */
    public static ShoreMessaging getProxy(String serviceHost) 
	throws Exception {

	String url = 
	    "rmi://" + serviceHost + "/" + SERVICE_NAME;

	System.out.println("Get service at " + url);
	return (ShoreMessaging )Naming.lookup(url);
    }
}

