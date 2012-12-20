package org.mbari.siam.distributed.devices;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.Serializable;
import java.io.IOException;

import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.NoDataException;

/** Remote Interface for operating the FOCE thrusters via remote methods implemented by the instrument service.
 */
/*
 $Id: $
 $Name:  $
 $Revision: $
 */

public interface ElmoThrusterIF extends ElmoIF{
	
	////////////////////////////////////////
	//         Turn Sensor Control       //
	////////////////////////////////////////
	
	/** Enable or disable the turns sensor */
	public void setTurnsSensorEnable(boolean value) throws RemoteException;
	/** Return number of times the turns sensor has been triggered since the last reset */	
	public long getTSTriggerCount() throws RemoteException;
	/** Return the elapsed time since last turns sensor trigger (msec) */	
	public long getTSElapsedMsec() throws RemoteException;
	/** Return the turns sensor state */	
	public int getTSState() throws RemoteException;
	/** return a mnemonic for turns sensor state */
	public String getTSStateName() throws RemoteException;
	/** return thruster sample string */
	public String getThrusterSampleMessage() throws TimeoutException,IOException, Exception, RemoteException;
	
}
