/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

public class PortConfiguration {

	protected long _deviceID;

	protected byte[] _portName;

	public PortConfiguration() {
		_deviceID = 0;
		_portName = null;
	}

	public void set(long deviceID, byte[] portName) {
		_deviceID = deviceID;
		_portName = new byte[portName.length];
		System.arraycopy(portName, 0, _portName, 0, portName.length);
	}

	public long getDeviceId() {
		return _deviceID;
	}

	public byte[] getPortName() {
		return _portName;
	}
}