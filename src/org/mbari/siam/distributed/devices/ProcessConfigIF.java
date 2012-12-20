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


/** Extends ControlStateIF to provide methods for changing the ControlLoop state.
    Remote Interface for performing closed loop control via remote methods implemented by the instrument service.
	Clients like the GUI may get the implementing service and cast to ProcessConfigIF and using 
    methods from ProcessConfigIF and ProcessStateIF to manipulate the control process.
 */
/*
 $Id: $
 $Name: $
 $Revision: $
 */

public interface ProcessConfigIF extends ProcessStateIF{
	
	
	/** set a control system parameter */
	public void setParameter(int parameterID, Number parameterValue) throws Exception,RemoteException;
	
	/** set a control system parameter */
	public void setParameter(String parameterName, String parameterValue) throws Exception,RemoteException;
	
}
