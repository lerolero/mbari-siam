// Copyright 2001 MBARI
package org.mbari.siam.distributed;

import java.io.Serializable;

/**
 * SensorStatusPacket contains information relating to "state" of the generating
 * sensor.
 * 
 * @author Tom O'Reilly
 * @deprecated Replaced by MetadataPacket
 */
public class SensorStatusPacket extends DevicePacket {

	/**
	 * Indicates that packet was generated autonomously (i.e. not from any
	 * external command.
	 */
	public static final int AUTO_GENERATED = -1;

	/** Description of status. */
	private byte _statusBytes[];

	/** Indication of what generated the SensorStatusPacket */
	private byte _cause[];

	/**
	 * @param sourceID
	 *            unique identifier of source sensor
	 * @param cause
	 *            reason for generating packet
	 * @param statusBytes
	 *            status data buffer
	 */
	public SensorStatusPacket(long sourceID, byte cause[], byte statusBytes[]) {
		super(sourceID);

		_statusBytes = new byte[statusBytes.length];
		for (int i = 0; i < _statusBytes.length; i++) {
			_statusBytes[i] = statusBytes[i];
		}

		_cause = new byte[cause.length];
		for (int i = 0; i < _cause.length; i++) {
			_cause[i] = cause[i];
		}
	}

	/** Indication of what generated the SensorStatusPacket */
	public byte[] cause() {
		return _cause;
	}

	/** Get status bytes. */
	public byte[] getStatusBytes() {
		return _statusBytes;
	}
}

