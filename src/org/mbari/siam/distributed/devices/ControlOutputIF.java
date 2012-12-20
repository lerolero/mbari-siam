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

import org.mbari.siam.foce.devices.controlLoop.OutputState;

/** Remote Interface for performing closed loop control via remote methods implemented by the instrument service.
 */
/*
 $Id: $
 $Name: $
 $Revision: $
 */

public interface ControlOutputIF extends Remote{
	public static final int DEFAULT_ID=0;
	public static final String DEFAULT_NAME="unknown";

	public static final int STATE_UNINITIALIZED =0;
	public static final int STATE_READY         =1;
	public static final int STATE_UPDATING      =4;
	
	public static final int STATUS_OK               =0x0;
	public static final int STATUS_TIMEOUT_EXPIRED  =0x1;
	public static final int STATUS_NOT_CONNECTED    =0x2;
	public static final int STATUS_CONNECT_ERROR    =0x3;
	public static final int STATUS_UPDATE_ERROR     =0x4;
	public static final int STATUS_DISCONNECT_ERROR =0x8;
	public static final int STATUS_ERROR            =0x10;
	public static final int STATUS_BAD_COMMAND      =0x20;
	public static final int STATUS_MASK_ALL         =0x3F;
	
	public void setOutputValue(double value) throws Exception, RemoteException;
	public void setOutputValue(int value) throws Exception, RemoteException;
	public void setOutputValue(boolean value) throws Exception, RemoteException;
	public void setOutputID(int id) throws RemoteException;
	public int getOutputID() throws RemoteException;	
	public Object getDevice() throws RemoteException;	
	public void setName(String name) throws RemoteException;
	public String name() throws RemoteException;
	public int getState() throws RemoteException;
	public int getStatus() throws RemoteException;
	public String stateString() throws RemoteException;
	public String statusString() throws RemoteException;
	public OutputState getOutputState() throws RemoteException;
	
}
