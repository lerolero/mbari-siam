/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed.devices;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.Serializable;
import java.io.IOException;


/** Remote Interface for reading the state of the control loop via remote methods implemented by the instrument service.
	Clients like the GUI may get the implementing service and cast to ControlStateIF to read control process signals and configuration parameters.
 */
/*
 $Id: $
 $Name: $
 $Revision: $
 */

public interface ControlStateIF extends Remote{
	//////////////////////////////
	/* Loop Response Parameters */
	//////////////////////////////
	
	/** Get signal value.
		Signals represent intermediate calculated values in the control loop processing chain,
		i.e., the "lines" in control loop block diagram that connect functional blocks.
	 */
	public Number getSignal(int signalID) throws Exception,RemoteException;
	
	/** get current (instantaneous) state of all loop signals and configuration parameters. 
		This will return an object that can be accessed through the ControlStateIF interface
		to get values of control loop configuration and signal state.
	 */
	public ControlStateIF getState() throws RemoteException;
	
	/** get current value of a control system parameter */
	public Number getParameter(int paramID) throws Exception,RemoteException;
	
}
