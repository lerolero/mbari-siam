// Copyright 2001 MBARI
package org.mbari.siam.distributed;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

/**
 * DeviceMessagePacket contains a text message from a device.
 * 
 * @author Tom O'Reilly
 */
public class DeviceMessagePacket extends DevicePacket implements Serializable {

	/** Serial version ID */
	private static final long serialVersionUID = 0L;

	/** Message text */
	private byte _message[];

	public DeviceMessagePacket() {
	}

	/**
	 * @param sourceID
	 *             unique identifier of source device.
	 */
	public DeviceMessagePacket(long sourceID) {
		super(sourceID);
	}

	/**
	 * Set message content.
	 * 
	 * @param time
	 *            message time-tag (millisec since epoch)
	 * @param message
	 *            text bytes
	 */
	public void setMessage(long time, byte[] message) {
		_message = new byte[message.length];
		System.arraycopy(message, 0, _message, 0, message.length);
		setSystemTime(time);
	}

	/** Get message bytes. */
	public byte[] getMessage() {
		return _message;
	}

	/** Return String representation. */
	public String toString() {
		return super.toString() + new String(_message);
	}
}

