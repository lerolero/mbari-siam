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
import java.util.Iterator;

/** Remote Interface for reading the state of the control loop via remote methods implemented by the instrument service.
	Clients like the GUI may get the implementing service and cast to ControlStateIF to read control process signals and configuration parameters.
 */
/*
 $Id: $
 $Name: $
 $Revision: $
 */

public interface ProcessStateIF extends Remote{
	//////////////////////////////
	/* Loop Response Parameters */
	//////////////////////////////
	
	/** Get signal value.
		Signals represent intermediate calculated values in the control loop processing chain,
		i.e., the "lines" in control loop block diagram that connect functional blocks.
	 */
	public Number getSignal(int signalID) throws Exception,RemoteException;
		
	/** get current value of a control system parameter */
	public Number getParameter(int paramID) throws Exception,RemoteException;
	
	/** get ID of a control system parameter by name */
	public int parameterID(String parameterName) throws Exception,RemoteException;
	
	/** get name of a control system parameter by ID */
	public String parameterName(int parameterID) throws Exception,RemoteException;
	
	/** get ID of a control system signal */
	public int signalID(String signalName) throws Exception,RemoteException;
	
	/** return set of parameter names that may be used to get parameter values
	 KeySet/Iterator are not exportable, so we deal in String arrays */
	public String[] parameterNames() throws RemoteException;

	/** return set of signal names that may be used to get signal values
	 KeySet/Iterator are not exportable, so we deal in String arrays */
	public String[] signalNames() throws RemoteException;
	
	/** return a signal name for the given signal ID */
	public String signalName(int signalID) throws RemoteException;	
	
	/** return a signal name for the given signal ID */
	public String filterInputName(int inputID) throws RemoteException;
}
