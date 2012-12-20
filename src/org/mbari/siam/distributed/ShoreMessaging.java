/****************************************************************************/
/* Copyright 2008 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.distributed;

import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * Service interface for shore messaging facility.
 * 
 * @author Tom O'Reilly
 */
public interface ShoreMessaging extends RemoteService {

    /** Return maximum bytes allowed in a downlink message */
    public int maxDownlinkMsgBytes() 
	throws RemoteException;

    /** Queue a message for downlink; returns current number of messages
     in queue. */
    public int queueDownlinkMessage(byte[] message)
	throws ShoreMessagingHelper.MessageTooBig, 
	       Exception, RemoteException;

    /** Return number of messages currently in downlink queue */
    public int nQueuedDownlinkMsgs() 
	throws RemoteException;


    /** Connect modem to shore, attempt to downlink queued messages */
    public void connect(int timeoutSec) 
	throws ShoreMessagingHelper.ConnectFailed, RemoteException;


    /** Shutdown service */
    public void shutdown() throws RemoteException;

}
