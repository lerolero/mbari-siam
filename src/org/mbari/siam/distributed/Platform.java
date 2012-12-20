// Copyright 2001 MBARI
package org.mbari.siam.distributed;

import java.rmi.RemoteException;

/**
 * A Platform contains and manages a collection of Devices.
 * 
 * @author Tom O'Reilly
 */
public interface Platform extends Device {

	/**
	 * Get location of specified device.
	 */
	public Location getLocation(Device device) throws Exception,
			RemoteException;

	/**
	 * Get DevicePacketStream, which provides DevicePackets from all Sensors on
	 * this platform.
	 */
	public DevicePacketStream getPacketStream() throws Exception,
			RemoteException;

	/**
	 * Get DeviceGeometry for this platform.
	 */
	public DeviceGeometry getDeviceGeometry() throws Exception, RemoteException;

	/**
	 * Get the EventManager for this platform.
	 */
	public EventManager getEventManager() throws Exception, RemoteException;

	/**
	 * Get DevicePacket objects, from specified sensor, within specified time
	 * window.
	 */
	public DevicePacket[] getDevicePackets(long sensorID, long startTime,
			long endTime) throws RemoteException, DeviceNotFound,
			NoDataException, TooMuchDataException;
}