// Copyright MBARI 2004
package org.mbari.siam.devices.nobska;

import java.rmi.RemoteException;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.RangeException;

/** 
    Interface to extend services of Nobska MAVS service
    @author Tom O'Reilly
*/


public interface NobskaMAVS_IF extends Instrument 
{

	/** Set sampling interval (millisec). */
	public void setSampleInterval(int msec) throws RemoteException;
 
}
