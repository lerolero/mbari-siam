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

import org.mbari.siam.distributed.SensorDataPacket;

/** ControlResponseIF defines the interface to different types of control loop responses.
	A ControlResponseIF uses ControlProcessIF objects to get control process state and 
	manipulate control process inputs and outputs.
 */
/*
 $Id: $
 $Name: $
 $Revision: $
 */

public interface ControlResponseIF{
	
	
	/** initialize control loop */
	public void initialize() throws RemoteException;
	/** reset control loop */
	public void reset() throws RemoteException;
	/** adjust inputs and outputs based on current control process state */
	public float update() throws Exception, RemoteException;
	
	/** Return a sample buffer with current process data. */
	public StringBuffer getSampleBuffer()throws Exception, RemoteException;
	
	/** Return a sample buffer with current process data. */
	public SensorDataPacket getSamplePacket()throws Exception, RemoteException;
		
}
