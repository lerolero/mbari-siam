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

import org.mbari.siam.distributed.PacketParser;

/** Remote Interface for performing closed loop control via remote methods implemented by the instrument service.
 */
/*
 $Id: $
 $Name: $
 $Revision: $
 */

public interface CO2SubsystemMonitorIF extends Remote{
	/** Fan state constant ON  */
	public static final int FAN_CONTROL_ON=0;
	/** Fan state constant OFF */
	public static final int FAN_CONTROL_OFF=1;

	/** Set the CO2 cooling fan control bit. valid state values 
	 are FAN_CONTROL_ON, FAN_CONTROL_OFF
	 */
	public void setFanControl(int state) throws Exception,RemoteException;

	/** get the CO2 Subsystem monitor state */
	public PacketParser.Field[] getMonitorState() throws Exception,RemoteException;
}