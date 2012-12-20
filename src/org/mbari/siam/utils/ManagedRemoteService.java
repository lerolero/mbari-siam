/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils;

import java.util.Vector;
import java.util.Iterator;
import java.rmi.Remote;
import java.rmi.Naming;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.RemoteService;
import org.mbari.siam.operations.portal.PortalSocketFactory;
import org.mbari.siam.utils.NodeProbe;


/** ManagedRemoteService creates and manages an RMI SIAM 'RemoteService' proxy 
    associated with the specified name, and periodically checks that 
    the service is still available. */
public class ManagedRemoteService {

    static protected Logger _log4j = 
	Logger.getLogger(ManagedRemoteService.class);

    protected static final int REFRESH_INTERVAL_MSEC = 45000;
    protected long _lastRefreshTime = 0;
    protected RemoteService _service;
    protected String _serviceURL;
    protected String _serviceMnemonic;
    protected Vector _statusListeners = new Vector();
    protected InetAddress _serviceAddr;
    protected NodeProbe _nodeProbe = new NodeProbe();

    protected final static int PROBE_TIMEOUT_MSEC = 30000;
    protected final static int RMIREGISTRY_PORT = 1099;

    protected Object _lock = new Object();

    /** Flag indicates status of last connection attempt */
    private boolean _lastConnectAttemptOK = false;


    public ManagedRemoteService(String serviceURL, 
				String serviceMnemonic,
				int pingIntervalMsec) throws Exception {

	// Create socket factory; overcomes problems with RMI 'hostname'
	// property.
	String hostName = getHostName(serviceURL);

	_serviceAddr = InetAddress.getByName(hostName);

	_service = null;
	_serviceURL = new String(serviceURL);
	_serviceMnemonic = new String(serviceMnemonic);

	// Start service.ping() timer
	_log4j.debug("Constructor - create actionListener");
	TimerTask timerListener = new TimerTask() {

		public void run() {

		    _log4j.debug("actionPerformed()...");
		    synchronized (_lock) {
			try {
			    // Probe registry on remote host
			    if (_nodeProbe.probe(_serviceAddr, RMIREGISTRY_PORT, 
						 PROBE_TIMEOUT_MSEC)) {

				// RMI registry is alive on remote port
				_log4j.debug("node probe returned true");
				if (_service == null) {
				    // Try to connect
				    connect();
				}
			    }
			    else {
				_log4j.debug("node probe returned false");
				_service = null;
				return;
			    }
			}
			catch (Exception e) {
			    _log4j.error("Exception from node probe: " + e);
			}

			if (_service == null) {
			    _log4j.debug("ping timer callback - service is null");
			    return;
			}

			_log4j.debug("Try to ping service...");

			try {
			    _service.ping();
			    _log4j.debug("ping() successful");
			}
			catch (Exception e) {
			    // Can't ping service - assume it's disconnected
			    _log4j.debug("ping() FAILED");
			    _service = null;
			}
		    }
		}
	    };

	_log4j.debug("Constructor - create timer, interval=" + 
		     pingIntervalMsec + " msec");

	Timer timer = new Timer();
	timer.schedule(timerListener, 0, pingIntervalMsec);

	_log4j.debug("Done with constructor");
    }	


    /** Set service reference to 'null', so that next access attempt
	will result in reconnection */
    synchronized public void resetConnection() {
	_service = null;
    }

    /** Notify listeners of connection status */
    protected void notifyListeners(boolean connected) {
	int i = 0;
	Iterator iterator = _statusListeners.iterator();
	while (iterator.hasNext()) {
	    _log4j.debug("notify listener #" + i);
	    StatusListener listener = (StatusListener )iterator.next();
	    if (connected) {
		listener.connectSucceeded(_serviceMnemonic);
	    }
	    else {
		listener.connectFailed(_serviceMnemonic);
	    }
	}
	_log4j.debug("done notifying listeners");
    }

    /** Reconnect service */
    public RemoteService connect() 
	throws Exception {

	_log4j.debug("connect() - get _lock");
	synchronized (_lock) {
	    // Get proxy via Naming service
	    try {
		// Probe registry on remote host
		if (!_nodeProbe.probe(_serviceAddr, RMIREGISTRY_PORT, 
				      PROBE_TIMEOUT_MSEC)) {

		    _log4j.debug("RMI registry not found");
		    throw new Exception("RMI registry not found at " + 
					_serviceAddr);
		}

		_log4j.debug("Get " + _serviceURL + " proxy");

		_service = (RemoteService )Naming.lookup(_serviceURL);
		if (_service == null) {
		    _log4j.error("connect() - _service stub is NULL!");
		    throw new Exception("connect() - service stub is NULL!");
		}

		_log4j.debug("connect(): _lastConnectAttempOK=" + 
			     _lastConnectAttemptOK);

		if (!_lastConnectAttemptOK) {

		    _log4j.debug("set lastConnetAttemptOK = true");
		    _lastConnectAttemptOK = true;

		    // Notify listeners of connection
		    notifyListeners(true);
		}
		return _service;

	    }
	    catch (Exception e) {
		_log4j.debug("Got exception trying to retrieve remote service: "
			     + e, e);

		_service = null;

		if (_lastConnectAttemptOK) {
		    // Notify listeners of failed connection
		    notifyListeners(false);
		    _log4j.debug("set lastConnetAttemptOK = false");
		    _lastConnectAttemptOK = false;
		}
		throw new Exception(_serviceURL + " service not available");
	    }
	}
    }

    /** Return true if connected to remote service */
    public boolean connected() {
	synchronized (_lock) {
	    if (_service != null) {
		return true;
	    }
	    else {
		return false;
	    }
	}
    }

    /** Get service proxy */
    public RemoteService getProxy() 
	throws Exception {

	/* ***
	_log4j.warn("getProxy() always returns connect()");
	return connect();

	*** */

	long now = System.currentTimeMillis();

	if (now - _lastRefreshTime < REFRESH_INTERVAL_MSEC) {
	    _log4j.debug("getProxy() - no refresh yet");
	    if (_service == null) {
		_log4j.debug("getProxy() - null proxy, return connect()");
		return connect();
	    }
	    else {
		return _service;
	    }
	}
	else {
	    _log4j.debug("getProxy() - refresh RMI service stub");
	    _lastRefreshTime = now;
	    return connect();	// TEST TEST TEST
	}
    }


    /** Add a status listener */
    synchronized public void addListener(StatusListener listener) {
	_statusListeners.add(listener);
    }


    /** Return the 'hostname' portion of the remote service's URL. Note that 
	this is a TOTAL hack, since Java's URL class does not support
	"rmi" as a valid protocol!!! */
    public static String getHostName(String nodeURL) {

	// NOTE: This method only works if the node's url starts with "rmi"!
	// Build a temporary valid URL by replacing "rmi" with "ftp"
	StringBuffer buf = new StringBuffer(nodeURL);
	buf.replace(0, 3, "ftp");
	try {
	    URL url = new URL(new String(buf));
	    return url.getHost();
	}
	catch (MalformedURLException e) {
	    System.err.println("MalformedURLException on \"" + 
			       new String(buf) + "\": " + e.getMessage());
	}
	return "dummy";
    }


    /** Interface for components that wish to respond to connection status */
    public interface StatusListener {
	/** Have connection to service */
	public void connectSucceeded(String serviceURL);

	/** No connection to service */
	public void connectFailed(String serviceURL);
    }

}
