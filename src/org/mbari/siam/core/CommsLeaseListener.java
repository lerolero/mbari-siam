/****************************************************************************/
/* Copyright 2003 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.core;

import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.leasing.LeaseListener;
import org.mbari.siam.distributed.leasing.LeaseRefused;
import org.mbari.siam.distributed.leasing.LeaseManager;
import org.mbari.siam.distributed.SleepRollCallListener;
import org.mbari.siam.utils.SyncProcessRunner;
import org.mbari.siam.utils.ThreadUtility;

import org.apache.log4j.Logger;

/**
   CommsLeaseListener responds to the Node's LeaseManager leasing events,
   and in response carries out several tasks to manage the relationship with
   the node's "parent" node. E.g. in the case of the primary surface node, 
   CommsLeaseListener applies power to the radio that enables the communications 
   link to its parent, which in this case is the shore portal. 

   The class assumes there may be a device whose power is turned 
   on/off by invoking scripts (specified in the node properties).

   CommsLeaseListener has delays built in to account for the time
   it takes to establish the communication link, and to allow the application
   protocol to get to the other side before tearing down the link.

   When it gets establishCallback() and power is not already on, it
   turns on RF power, delays for CommsManager.radioAcquireTime, and 
   then notifies the parent via Node.notifyParentLinkConnected().

   Conversely, when it gets terminateCallback() or expiredCallback()
   and the remaining connections == 0 and power is already on, it
   calls Node.notifyParentLinkDisconnecting(), delays for
   CommsManager.protocolWaitTime and then turns off RF power.

   2/7/2006 k headley
   When a subnode is configured with a secondary wireless link to shore,
   it should only bring up the ppp link, and not notify its parent (or
   the portal) that the link is up. To that end, a new property
   (CommsManager.notifyParent) has been added to siamPort.cfg to indicate
   that the CommsLeaseListener should not contact its parent node as
   part of establishing and releasing communications. If CommsManager.notifyParent
   is set to false, the ppp link will be established at the specified
   interval, but no telemetry will take place.

   @author Bob Herlien - derived from CommsManager.java
*/

public class CommsLeaseListener implements LeaseListener {
    protected final static long PROTOCOL_WAIT_TIME = 20*1000;

    protected NodeProperties _nodeProperties = null;
    protected NodeService _nodeService = null;
    protected Object _lock = null;
    protected CommsSleepRollCallListener _listener = null;
    protected long _nextEstablishTime = LeaseManager.UNKNOWN_TIME;
    protected boolean _commsManagerEnabled = true;
    protected long _protocolWaitTime = PROTOCOL_WAIT_TIME;
    protected long _processWaitTime = 0;
    protected String _commsOnCommand = null;
    protected String _commsOffCommand = null;
    protected String _commsStatusCommand = null;
    protected SyncProcessRunner _processRunner = null;
    protected long onTime = 0;


    /** By convention, connection and disconnection commands should return value of 0 if
	successful, and non-zero in case of error. The scripts should return PORT_IN_USE (255) if
	the port is in use by another process. */
    protected final static int PORT_IN_USE = 255;

    /** By convention, "connection status" command should return 0 if link IS connected, else 
	return 1 if link is NOT connected. */
    protected final static int COMMS_DISCONNECTED = 1;
    protected final static int COMMS_CONNECTED = 0;

    // This variable indicates when a lease is being established; okToSleep() will tell sleep manager
    // NOT to sleep while a lease is being established.
    protected boolean _establishingLease = false;

    protected final static String DFLT_COMMS_STATUS_COMMAND = "linkStatus ppp";


    private static Logger _log4j = Logger.getLogger(CommsLeaseListener.class);

    CommsLeaseListener(NodeService nodeService) {
	_nodeService = nodeService;
	_lock = new Object();
	_processRunner = new SyncProcessRunner();
	_nodeProperties = NodeManager.getInstance().getNodeProperties();

	if (_commsManagerEnabled) {
	    _listener = new CommsSleepRollCallListener();
	    SleepManager.getInstance().addSleepRollCallListener(_listener);
	}

	_log4j.info("CommsLeaseListener() complete");

    } /* CommsLeaseListener() */


    /** This method must be called by creator (not constructor) to initialize
	parameters. */
    public void initialize() {

	_log4j.debug("initialize()");

	_commsOnCommand = 
	    _nodeProperties.getProperty("CommsManager.parentLinkOn");

	_commsOffCommand = 
	    _nodeProperties.getProperty("CommsManager.parentLinkOff");

	_commsStatusCommand = 
	    _nodeProperties.getProperty("CommsManager.parentLinkStatus", 
					DFLT_COMMS_STATUS_COMMAND);

	_protocolWaitTime = 
	    _nodeProperties.getLongProperty("CommsManager.protocolWaitTime", 
					    PROTOCOL_WAIT_TIME);

	_processWaitTime = 
	    _nodeProperties.getLongProperty("CommsManager.processWaitTime",
					    0);


	String enblString = 
	    _nodeProperties.getProperty("CommsManager.enabled");

	if (enblString != null){
	    _commsManagerEnabled = enblString.equalsIgnoreCase("true");
	}
    }



    /** 
	Called when a lease is being established. If this is the first lease, inform the 
	parent of the connection. If link to parent is not connected, 
	turn on comms power and inform parent.
    */
    public void leaseEstablishCallback(int leaseID, long leasePeriod,
				       long nextEstablishTime, int numConnections) 
	throws LeaseRefused
    {
	// This state value is checked by the sleep management thread.
	_establishingLease = true;

	int retVal = 0;

	_log4j.debug("leaseEstablishCallback, LeaseID " + leaseID + 
		     ", nConn=" + numConnections);

	if (nextEstablishTime != LeaseManager.UNKNOWN_TIME) {
	    _nextEstablishTime = nextEstablishTime;
	}

	if (!_commsManagerEnabled) {
	    _log4j.info("leaseEstablishCallback() - Comms manager not enabled");
	    _establishingLease = false;
	    return;
	}

	/* 
	   Check to see if link is connected. Note that a non-SIAM process may turn on the link before
	   any leases are established. Likewise, a non-SIAM process may turn off the link outside
	   of the lease-expired/lease-terminated mechanisms. 
	*/
	if (commLinkConnected()) {
	    // Link is already connected 
	    if (numConnections == 1) {
		// Notify parent of connection
		_nodeService.notifyParentLinkConnected();
	    }
	    _log4j.debug("leaseEstablishCallback() - link already connected");
	    _establishingLease = false;
	    return;
	}


	synchronized (this)  {

	    int maxTries = 25;
	    try {
		maxTries = 
		    NodeManager.getInstance().getNodeProperties().maxReassertCommLinkTries();
	    }
	    catch (InvalidPropertyException e) {
		_log4j.error(e.getMessage() + ": using default value of 25");
		maxTries = 25;
	    }

	    boolean connected = false;
	    _log4j.debug("commsOnCommand: " + _commsOnCommand);
	    _log4j.debug("commsOffCommand: " + _commsOffCommand);


	    if ((_commsOnCommand != null) && (_commsOnCommand.length() > 0)) {

		for (int i = 0; i < maxTries; i++) {

		    try	{
			_log4j.info("Calling SyncProcessRunner(\"" 
				    + _commsOnCommand + "\"); attempt #" + i);
			_processRunner.exec(_commsOnCommand);
			retVal = _processRunner.waitFor(_processWaitTime);
			_log4j.debug("SyncProcessRunner complete");

			String output=_processRunner.getOutputString();
			if(output!=null) {
			    _log4j.info(output);
			}

			onTime = System.currentTimeMillis();

			if (retVal != 0) {
			    _log4j.error(_commsOnCommand + 
					 " leaseEstablishCallback(): " + 
					 "connect attempt #" + i + 
					 " failed, return code = " + 
					 retVal);

			    if (retVal == PORT_IN_USE) {
				// Likely that some other process is using
				// the comm port - abort here (i.e. don't
				// interfere further with the port or its power), and 
				// assume that 
				_establishingLease = false;
				if (!commLinkConnected()) {
				    throw new LeaseRefused("Abort (Comm port in use?)");
				}
				else {
				    return;
				}
			    }
			}
			else {
			    connected = true;
			    _log4j.debug("leaseEstablishCallback() - Established connection");
			    break;
			}

		    } catch (Exception e) {
			if (e instanceof LeaseRefused) {
			    // Propagate LeaseRefused
			    _establishingLease = false;
			    throw (LeaseRefused )e;
			}		    

			_log4j.error("Exception in leaseEstablishCallback: "
				     +e, e);
		    }

		}
	    }
	    else {
		_log4j.debug("leaseEstablishCallback(" + leaseID + ") - no commsOnScript");
		_establishingLease = false;
		return;
	    }

	    if (connected) {
		try {
		    _nodeService.notifyParentLinkConnected();
		} 
		catch (Exception e) {
		    _log4j.error("Exception in leaseEstablishCallback: "+e, e);
		}
	    }
	    else {
		_establishingLease = false;
		throw new LeaseRefused("Unable to establish connection after " + 
			     maxTries + " attempts");

	    }

	}
	_log4j.debug("leaseEstablishCallback(" + leaseID + ") done");
	_establishingLease = false;

    }


    /**
       Called when lease is being terminated.  If RF power is on,
       inform Parent that power is about to be turned off.
       This callback implementation must execute in a timely manner.
       @param leaseID lease ID of session being terminated
       @param nextEstablishTime next estimated lease establishment time 
       @param numConnections Total number of connections on the comms
       media, after this one has been terminated
    */
    public void leaseTerminatedCallback(int leaseID, long nextEstablishTime,
					int numConnections)
    {
	_log4j.debug("leaseTerminatedCallback, LeaseID " + leaseID + 
		     ", nConn=" + numConnections);

	if (nextEstablishTime != LeaseManager.UNKNOWN_TIME)
	    _nextEstablishTime = nextEstablishTime;

	if ((numConnections <= 0) && _commsManagerEnabled) {

	    try {
		switchCommsOff();
	    }
	    catch (Exception e) {
		_log4j.error("leaseEstablishCallback() - got exception from switchCommsOff(): " + e);
	    }
	}

	_log4j.debug("leaseTerminatedCallback("+leaseID+") done");
    }

    /**
       Called when lease expires.
       This callback implementation must execute in a timely manner.
       @param leaseID lease ID of session being established
       @param nextEstablishTime next estimated lease establishment time 
       @param numConnections Total number of connections on the comms
       media, after this one has expired
    */
    public void leaseExpiredCallback(int leaseID, long nextEstablishTime,
				     int numConnections) {

	LeaseManager.Lessee lessee = 
	    _nodeService._leaseManager.getLessee(leaseID);

	String clientNote = "???";
	if (lessee != null) {
	    clientNote = lessee._clientNote;
	}

	_log4j.debug("leaseExpiredCallback, LeaseID " + leaseID + 
		     "(clientNote: " + clientNote + "), nConn=" + 
		     numConnections);

	if (nextEstablishTime != LeaseManager.UNKNOWN_TIME)
	    _nextEstablishTime = nextEstablishTime;

	if ((numConnections <= 0) && _commsManagerEnabled) {

	    try {
		switchCommsOff();
	    }
	    catch (Exception e) {
		_log4j.error("leaseEstablishCallback() - got exception from switchCommsOff(): " + e);
	    }
	}
	_log4j.debug("leaseExpiredCallback("+leaseID+") done");
    }


    /**
       Called when lease is being renewed. Not used by this implementation
    */
    public void leaseRenewalCallback(int leaseID, long leasePeriod,
				     int numConnections) throws LeaseRefused
    {
    }



    class CommsSleepRollCallListener implements SleepRollCallListener {

        public long okToSleep()
	{
	    if (_establishingLease) {
		// Another thread is trying to bring up comms system - don't sleep now
		return 0;
	    }

	    long retVal = SleepRollCallListener.NO_TIME_SPECIFIED;
	    long estTime;

	    synchronized(_lock)	{
		int nLeases = _nodeService._leaseManager.nLessees();
		if (nLeases > 0) {
		    retVal = 0;
		}
		else if (_nextEstablishTime != LeaseManager.UNKNOWN_TIME) {
		    estTime = _nextEstablishTime - System.currentTimeMillis();
		    if (estTime >= 0) {
			retVal = estTime;
		    }
		}
	    }

	    return(retVal);
	}
    }



    /** Invoked before link is disconnected. */
    protected void disconnectingCallback(long nextConnectMsec) throws Exception {
	_log4j.debug("Sending notifyParentLinkDisconnecting()");

	_nodeService.notifyParentLinkDisconnecting(nextConnectMsec);
    }


    /** Shut down comms link, turn off power. */
    public synchronized void switchCommsOff() throws Exception {

	int retVal = 0;

	if (_commsOffCommand == null) {
	    return;
	}

	Thread.sleep(_protocolWaitTime);

	try	{
	    _log4j.info("SyncProcessRunner.exec(\"" 
			+ _commsOffCommand + "\"), connect time = "
			+ (System.currentTimeMillis()-onTime)/1000);
	    _processRunner.exec(_commsOffCommand);
	    retVal = _processRunner.waitFor(_processWaitTime);
	    _log4j.debug("SyncProcessRunner complete");

	    String output=_processRunner.getOutputString();

	    if(output!=null) {
		_log4j.info(output);
	    }

	    if (retVal != 0) {
		_log4j.error(_commsOffCommand+" returns "+retVal);
	    }

	} 
	catch (Exception e) {
	    _log4j.error("switchCommsOff(): caught Exception: " + e, e);
	}
	catch (Throwable e) {
	    _log4j.error("switchCommsOff(): caught Throwable: " + e, e);
	}
    }


    /** Return true if comms link is connected. */
    public boolean commLinkConnected() {

	if (_commsStatusCommand == null) {
	    // No status command - assume it is connected.
	    _log4j.warn("commLinkConnected() - no comms status command defined - assume cnonected");
	    return true;
	}

	try {
	    _log4j.debug("commLinkConnected() - run " + _commsStatusCommand);
	    _processRunner.exec(_commsStatusCommand);
	    int retVal = _processRunner.waitFor(_processWaitTime);
	    _log4j.debug("commLinkConnected(): " + retVal);
	    if (retVal == COMMS_CONNECTED) {
		return true;
	    }
	    else {
		return false;
	    }
	}
	catch (Exception e) {
	    _log4j.error("commLinkConnected(): caught Exception " + e, e);
	    return false;
	}

    }
}


