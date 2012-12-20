/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Vector;
import org.mbari.siam.distributed.NodeEventCallback;
import org.mbari.siam.distributed.NodeEventListener;
import org.apache.log4j.Logger;

/**
   Remote NodeService invokes NodeEventCallbackService methods when 
   significant node events are detected; NodeEventCallbackService
   then notifies local NodeEventListeners.
 */
public class NodeEventCallbackService extends UnicastRemoteObject
    implements NodeEventCallback {

    static private Logger _logger = 
	Logger.getLogger(NodeEventCallbackService.class);

    private Vector _listeners = new Vector();

    public NodeEventCallbackService() 
	throws RemoteException {
	super();
    }
	
    /** Called by Node when a service is terminated. */
    public void serviceTerminated(long deviceID) throws RemoteException {
	// Notify listeners
	for (int i = 0; i < _listeners.size(); i++) {
	    NodeEventListener listener = 
		(NodeEventListener )_listeners.elementAt(i);

	    listener.serviceTerminated(deviceID);
	}
    }

    /** Called by Node when a service is started. */
    public void serviceStarted(long deviceID) throws RemoteException {
	// Notify listeners
	for (int i = 0; i < _listeners.size(); i++) {
	    NodeEventListener listener = 
		(NodeEventListener )_listeners.elementAt(i);

	    listener.serviceStarted(deviceID);
	}
    }

    /** Called by Node when a service changes state */
    public void serviceChanged(long deviceID) throws RemoteException {
	// Notify listeners
	_logger.info("NOT IMPLEMENTED");
    }


    /** Add a NodeEventListener, which will be notified on
     events of interest. */
    public void addListener(NodeEventListener listener) {
	_listeners.add(listener);
    }
}
