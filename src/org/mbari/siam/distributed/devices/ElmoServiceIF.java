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

/** Remote Interface for operating the Elmo motor controllers via remote methods implemented by the instrument service.
 */
/*
 $Id: $
 $Name: $
 $Revision: $
 */

public interface ElmoServiceIF extends Remote{
	
	/** convert shaft RPM to motor counts/sec 
	 using current value of countsPerRevolution and gearRatio 
	 attributes
	 */
	public int rpm2counts(double shaftRPM) throws RemoteException;
				
}
