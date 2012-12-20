/****************************************************************************/
/* Copyright 2003 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.distributed;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * NodeService uses this interface to invoke remote callbacks when significant
 * local events are detected.
 */
public interface NodeEventCallback extends Remote {

	/** Called by Node when a service is terminated. */
	public void serviceTerminated(long deviceID) throws RemoteException;

	/** Called by Node when a service is started. */
	public void serviceStarted(long deviceID) throws RemoteException;

	/** Called by Node when a service changes state. */
	public void serviceChanged(long deviceID) throws RemoteException;
}

