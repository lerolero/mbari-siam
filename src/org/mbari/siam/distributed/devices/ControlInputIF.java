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

import org.mbari.siam.utils.FilterInput;
import org.mbari.siam.utils.RangeValidator;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.foce.devices.controlLoop.InputState;

/** Remote Interface for performing closed loop control via remote methods implemented by the instrument service.
 */
/*
 $Id: $
 $Name: $
 $Revision: $
 */

public interface ControlInputIF extends Remote{
	public static final int FIELD_TYPE_DOUBLE  =0;
	public static final int FIELD_TYPE_LONG    =1;
	public static final int FIELD_TYPE_INT     =2;
	public static final int FIELD_TYPE_BOOLEAN =3;
	public static final int FIELD_TYPE_FLOAT   =4;
	public static final int FIELD_TYPE_SHORT   =5;
	public static final int FIELD_TYPE_BYTE    =6;
	
	public static final int STATE_INSTANTIATED  =0;
	public static final int STATE_INITIALIZED   =1;
	public static final int STATE_CONNECTING    =2;
	public static final int STATE_WAITING       =3;
	public static final int STATE_UPDATING      =4;
	public static final int STATE_DISCONNECTING =5;
	
	public static final int STATUS_OK               =0x0;
	public static final int STATUS_TIMEOUT_EXPIRED  =0x1;
	public static final int STATUS_NOT_CONNECTED    =0x2;
	public static final int STATUS_CONNECT_ERROR    =0x3;
	public static final int STATUS_UPDATE_ERROR     =0x4;
	public static final int STATUS_DISCONNECT_ERROR =0x8;
	public static final int STATUS_ERROR            =0x10;
	public static final int STATUS_MASK_ALL         =0x1F;
	
	public void initialize() throws RemoteException;
	public void connect() throws Exception, RemoteException;
	public void disconnect() throws Exception, RemoteException;
	public FilterInput getFilterInput() throws RemoteException;
	public void setFilterInput(FilterInput input) throws RemoteException;
	public void setInputID(int id) throws RemoteException;
	public int getInputID() throws RemoteException;	
	public Number getInputValue() throws RemoteException;
	public void setService(Instrument service) throws RemoteException;
	public Instrument getService() throws RemoteException;
	public void setLastUpdateTime(long time_msec) throws RemoteException;	
	public void setUpdateTimeout(long timeoutMsec) throws RemoteException;
	public long timeSinceLastUpdate() throws RemoteException;
	public boolean updateTimeoutExpired() throws RemoteException;
	public void setValidator(RangeValidator validator) throws RemoteException;
	public RangeValidator getValidator() throws RemoteException;
	public InputState getInputState() throws RemoteException;
	
	public long getUpdateTimeout() throws RemoteException;
	public int getState() throws RemoteException;
	public int getStatus() throws RemoteException;
	public String stateString() throws RemoteException;
	public String statusString() throws RemoteException;
	 
}
