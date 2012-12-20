// MBARI copyright 2003 
package org.mbari.siam.distributed;

import java.io.Serializable;

/**
 * The PowerSwitch class contains information about power switches.
 * 
 * @author Tom O'Reilly
 */
public class PowerSwitch implements Serializable {

	protected byte[] _switchName = null;

	/** ISI ID of device which is powered by switch. */
	protected long _switchedDeviceID = 0;

	/**
	 * Create PowerSwitch object with specified name and switched device ID.
	 * 
	 * @param switchName
	 *            Name of switch object
	 * @param switchedDeviceID
	 *            ISI device ID of associated instrument; 0 if instrument
	 *            service not running.
	 */
	public PowerSwitch(byte[] switchName, long switchedDeviceID) {
		_switchName = new byte[switchName.length];
		System.arraycopy(switchName, 0, _switchName, 0, switchName.length);
		_switchedDeviceID = switchedDeviceID;
	}

	/** Return name of switch. */
	public byte[] getName() {
		return _switchName;
	}

	/** Return ISI ID of associated device. */
	public long getSwitchedDeviceID() throws DeviceNotFound {

		if (_switchedDeviceID == 0) {
			// No device service associated with this switch
			throw new DeviceNotFound();
		}

		return _switchedDeviceID;
	}
}
