// Copyright 2003 MBARI
package org.mbari.siam.distributed;

import java.io.Serializable;

/**
 * A Port represents a node's instrument communication port. Each Port has a
 * name (e.g. "/dev/ttySX3"). Moreover, a Port may have an associated
 * DeviceService; this associated DeviceService is characterized by a deviceID
 * and a service mnemonic (e.g. "Seabird").
 * 
 * @author Tom O'Reilly
 */
public class Port implements Serializable {

	protected byte[] _portName = null;

	protected byte[] _serviceMnem = null;

	protected long _deviceID = 0;

	protected boolean _hasService = false;

	protected boolean _hasPowerSwitch = false;

	/** Create Port, with deviceID and mnemonic. */
	public Port(byte[] portName, long deviceID, byte[] serviceMnem) {
		_portName = new byte[portName.length];
		System.arraycopy(portName, 0, _portName, 0, portName.length);
		_deviceID = deviceID;
		_serviceMnem = new byte[serviceMnem.length];
		System.arraycopy(serviceMnem, 0, _serviceMnem, 0, serviceMnem.length);
		_hasService = true;
	}

	/** Create Port, with no service. */
	public Port(byte[] portName) {
		_portName = new byte[portName.length];
		System.arraycopy(portName, 0, _portName, 0, portName.length);
		_hasService = false;
	}

	/** Get name of port. */
	public byte[] getName() {
		return _portName;
	}

	/** Get port service Device ID. */
	public long getDeviceID() throws DeviceNotFound {
		if (!_hasService)
			throw new DeviceNotFound();
		return _deviceID;
	}

	/** Get port service mnemonic. */
	public byte[] getServiceMnemonic() throws DeviceNotFound {
		if (!_hasService)
			throw new DeviceNotFound();
		return _serviceMnem;
	}

	/** True if port has associated service. */
	public boolean hasService() {
		return _hasService;
	}

	/** True if port has associated power switch. */
	public boolean hasPowerSwitch() {
		return _hasPowerSwitch;
	}
}