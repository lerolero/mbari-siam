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


/** Remote Interface for performing closed loop control via remote methods implemented by the instrument service.
 */
/*
 $Id: $
 $Name: $
 $Revision: $
 */

public interface ControlProcessIF extends ProcessConfigIF{
	public static final int	CPIF_STATE_UNKNOWN=0;
	public static final int	CPIF_STATE_INSTANTIATED=1;
	public static final int	CPIF_STATE_STOPPED=2;
	public static final int	CPIF_STATE_RUNNING=3;

	public void startProcess()  throws Exception, RemoteException;
	public void stopProcess()  throws Exception, RemoteException;
	public void setOutput(int roleID, ControlOutputIF output) throws Exception,RemoteException;
	public ControlOutputIF getOutput(int outputID) throws RemoteException;
	public ControlOutputIF[] getOutputs() throws RemoteException;
	public ControlInputIF getInput(int inputID) throws RemoteException;
	public ControlInputIF[] getInputs() throws RemoteException;
	public void setParameter(String parameterName, String parameterValue) throws Exception, RemoteException;
	public int getState() throws RemoteException;
}
