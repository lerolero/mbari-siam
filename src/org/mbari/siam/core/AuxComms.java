/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.leasing.LeaseManager;


/**
The AuxComms subsystem implements an "auxillary" ppp link.
*/
public class AuxComms {

    private static Logger _log4j = Logger.getLogger(AuxComms.class);
    protected LeaseManager _leaseManager = null;
    protected long _leaseInterval = 0;
    protected long _leaseRenewalInterval = 0;

    public AuxComms(NodeService service, NodeProperties properties) {

	_leaseManager = new LeaseManager();

	_leaseInterval = 
	    properties.getLongProperty("NodeService.auxLeaseInterval", 
					    NodeService.LEASE_DURATION);

	_leaseRenewalInterval = 
	    properties.getLongProperty("NodeService.auxLeaseRenewalInterval",
					NodeService.LEASE_RENEWAL_INTERVAL);

	Listener listener = new Listener(service);
	listener.initialize();
	_leaseManager.addListener(listener);

	// Register timer task that periodically establishes a ppp link
	SiamTimer commsTimer = new SiamTimer();
	CommsSchedulerTask schedulerTask = 
	    new CommsSchedulerTask(_leaseManager,
				   _leaseInterval, _leaseRenewalInterval);

	commsTimer.schedule(schedulerTask, 0, _leaseRenewalInterval);
    }


    /** Get reference to aux lease manager. */
    public LeaseManager getLeaseManager() {
	return _leaseManager;
    }

    /** Maintains auxillary ppp link. */
    public class Listener extends CommsLeaseListener {

	public Listener(NodeService service) {
	    super(service);
	}

	/** Initialize properties */
	public void initialize() {

	    _log4j.debug("initialize()");

	    _commsOnCommand = 
		_nodeProperties.getProperty("CommsManager.auxLinkOn");

	    _commsOffCommand = 
		_nodeProperties.getProperty("CommsManager.auxLinkOff");

	    _protocolWaitTime = 
		_nodeProperties.getLongProperty("CommsManager.auxProtocolWaitTime",
						PROTOCOL_WAIT_TIME);

	    _processWaitTime = 
		_nodeProperties.getLongProperty("CommsManager.auxProcessWaitTime",
						0);
	    String enblString = 
		_nodeProperties.getProperty("CommsManager.auxEnabled");

	    if (enblString != null){
		_commsManagerEnabled = enblString.equalsIgnoreCase("true");
	    }
	}


	/** Invoked after link is established. */
	protected void connectedCallback() throws Exception {
	    _log4j.debug("connectedCallback()");
	}


	/** Invoked before link is disconnected. */
	protected void disconnectingCallback() throws Exception {
	    _log4j.debug("disconnectingCallback()");
	}
    }
}
