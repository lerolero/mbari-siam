/****************************************************************************/
/* Copyright 2003 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.distributed;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * NodeEventListener gets notified by NodeEventCallback service when signficant
 * event occurs on Node.
 */
public interface NodeEventListener {

	/** Called when a service is terminated. */
	public void serviceTerminated(long deviceID);

	/** Called when a service is started. */
	public void serviceStarted(long deviceID);

	/** Called when a service changes state. */
	public void serviceChanged(long deviceID);
}