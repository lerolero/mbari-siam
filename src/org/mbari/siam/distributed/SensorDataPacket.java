// Copyright 2001 MBARI
package org.mbari.siam.distributed;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

/**
 * SensorDataPacket contains "raw" bytes read from a sensor.
 * 
 * @author Tom O'Reilly
 */
public class SensorDataPacket extends DevicePacket implements Serializable {

	/** Serial version ID */
	private static final long serialVersionUID = 0L;

	private byte _dataBuffer[];

	public SensorDataPacket() {
	}

    /**
     * @param sourceID
     *            unique identifier of source sensor
     * @param maxBytes
     *            maximum bytes in "raw" data sample
     */	
    public SensorDataPacket(long sourceID, int maxBytes) {
		super(sourceID);

		if (maxBytes > 0) {
			_dataBuffer = new byte[maxBytes];
		}
	}

	/** Get "raw" bytes. */
	public byte[] dataBuffer() {
		return _dataBuffer;
	}

	public void setDataBuffer(byte[] buffer) {
		this._dataBuffer = buffer;
	}

	/** Return String representation. */
	public String toString() {
		byte[] buf = new byte[_dataBuffer.length];
		System.arraycopy(_dataBuffer, 0, buf, 0, _dataBuffer.length);
		convertToAscii(buf);

		return super.toString() + "nBytes=" + _dataBuffer.length + "\n"
				+ new String(buf, 0, buf.length);
	}
}
