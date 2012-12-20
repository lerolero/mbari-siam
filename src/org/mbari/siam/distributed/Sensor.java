// Copyright 2001 MBARI
package org.mbari.siam.distributed;

import java.rmi.RemoteException;

/**
 * A Sensor is a Device that can acquire data.
 * 
 * @author Tom O'Reilly
 */
public interface Sensor extends Device {

	/** Mode; sensor should automatically acquire samples */
	final public int ACQUIRE_MODE = 1;

	/** Mode; Sensor should not automatically acquire samples */
	final public int STANDBY_MODE = 2;

	/** Get most recent DevicePacket. */
	public DevicePacket getCurrentData() throws RemoteException, Exception;

	/** Get DevicePacketStream for this Sensor. */
	public DevicePacketStream getPacketStream() throws RemoteException,
			Exception;

	/** Get latest Calibration. */
	public Calibration getCalibration() throws RemoteException, Exception;

	/** Get the SensorDataHandler. */
	public SensorDataHandler getDataHandler() throws RemoteException, Exception;

	/** Put sensor in "sleep" mode (stop taking data). */
	public void sleep() throws RemoteException, Exception;

	/** Bring sensor out of "sleep" mode (start taking data). */
	public void wakeup() throws RemoteException, Exception;
}