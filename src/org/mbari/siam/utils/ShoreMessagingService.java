/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils;

import java.util.Vector;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.ShoreMessaging;
import org.mbari.siam.distributed.ShoreMessagingHelper;
import org.mbari.siam.utils.Queue;
import org.mbari.siam.core.SiamTimer;
import org.mbari.siam.core.SiamTimerTask;

/**
   Base class for implementations of ShoreMessaging; performs functions not
   specific to particular modem device. Subclasses should implement methods
   of ShoreMessaging that are device-specific, e.g. connectModem().
   ShoreMessagingService queues out-going messages for downlink.
   The downlink queue is populated by service clients and consumed by 
   logic in a subclass' connect() method.
 */
public abstract class ShoreMessagingService 
    extends UnicastRemoteObject
    implements ShoreMessaging {

    protected static Logger _log4j = 
	Logger.getLogger(ShoreMessagingService.class);

    static final int SHUTDOWN_DELAY_MSEC = 3000;

    /** Downlink messages to be sent to modem device */
    protected Queue _downlinkMsgQ = new Queue();

    /** Constructor */
    public ShoreMessagingService() throws RemoteException {
	super();
    }

    /** Queue a message for downlink; returns current count of 
	messages in downlink queue. */
    public synchronized int queueDownlinkMessage(byte[] messageBytes)
	throws ShoreMessagingHelper.MessageTooBig, Exception {

	if (messageBytes.length > maxDownlinkMsgBytes()) {
	    throw new ShoreMessagingHelper.MessageTooBig("Message bytes (" 
						   + messageBytes.length + 
						   ") exceeds maximum (" + 
						   maxDownlinkMsgBytes() + 
						   ")");
	}

	_log4j.debug("Queue message: " + new String(messageBytes));

	ShoreMessagingHelper.Message message = 
	    new ShoreMessagingHelper.Message(messageBytes);

	_downlinkMsgQ.pushBack(message);
	return _downlinkMsgQ.size();
    }

    /** Get count of messages in downlink queue */
    public int nQueuedDownlinkMsgs() {
	return _downlinkMsgQ.size();
    }


    /** Just return; service is alive */
    public void ping() {
	return;
    }


    /** Initiate service shutdown */
    public synchronized void shutdown() {
	_log4j.debug("shutdown()");
	new SiamTimer().schedule(new ShutdownTask(), 1);
    }


    protected class ShutdownTask extends SiamTimerTask {

	public void run() {

	    _log4j.debug("ShutdownTask().run()");

	    try {
		Thread.sleep(SHUTDOWN_DELAY_MSEC);
	    }
	    catch (Exception e) {
	    }

	    System.exit(1);
	}
    }
}
