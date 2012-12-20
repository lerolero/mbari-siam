// Copyright 2001 MBARI
package org.mbari.siam.distributed;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

/**
 * SummaryPacket contains a "summary" of multiple data samples.
 * 
 * @author Tom O'Reilly
 */
public class SummaryPacket extends DevicePacket implements Serializable {

	/** Serial version ID */
	private static final long serialVersionUID = 0L;

	/** Message text */
	private byte _data[];

	public SummaryPacket() {
	}

	/**
	 * @param sourceID
	 *            unique identifier of source device.
	 */
	public SummaryPacket(long sourceID) {
		super(sourceID);
	}

	/**
	 * Set message content.
	 * 
	 * @param time
	 *            message time-tag (millisec since epoch)
	 * @param data
	 *            text bytes
	 */
	public void setData(long time, byte[] data) {
		_data = new byte[data.length];
		System.arraycopy(data, 0, _data, 0, data.length);
		setSystemTime(time);
	}

	/** Get data bytes. */
	public byte[] getData() {
		return _data;
	}

	/** Return String representation. */
	public String toString() {
		return super.toString() + new String(_data);
	}
}

