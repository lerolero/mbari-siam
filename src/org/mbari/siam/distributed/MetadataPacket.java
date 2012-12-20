// Copyright 2002 MBARI
package org.mbari.siam.distributed;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Iterator;
import java.lang.ClassNotFoundException;
import org.mbari.siam.distributed.Exportable;

/**
 * MetadataPacket contains information relating to "state" of the generating
 * device.
 * 
 * @author Tom O'Reilly
 */
public class MetadataPacket extends DevicePacket implements Serializable {

	/** Serial version ID */
	private static final long serialVersionUID = 0L;
	
	/** Tags delimiting service attributes in payload. */
	public final static String SERVICE_ATTR_TAG = "<SRVC_MD>";
	public final static String SERVICE_ATTR_CLOSE_TAG = "</SRVC_MD>";

	/** Tags delimiting device status in payload. */
	public final static String DEVICE_INFO_TAG = "<DEV_MD>";
	public final static String DEVICE_INFO_CLOSE_TAG = "</DEV_MD>";

	/** Tags delimiting instrument document in payload. */
	public final static String INSTRUMENT_DOC_TAG = "<DOC_MD>";
	public final static String INSTRUMENT_DOC_CLOSE_TAG = "</DOC_MD>";

	/** Tags delimiting service properties document in payload. */
	public final static String SERVICE_PROP_TAG = "<SVP_MD>";
	public final static String SERVICE_PROP_CLOSE_TAG = "</SVP_MD>";


	/**
	 * Indicates that packet was generated autonomously (i.e. not from any
	 * external command.
	 */
	public static final int AUTO_GENERATED = -1;

	/** Metadata */
	private byte _bytes[];

	/** Indication of what generated the MetadataPacket */
	private byte _cause[];

	public MetadataPacket() {
	}

	/**
	 * @param sourceID
	 *            unique identifier of source device
	 * @param cause
	 *            reason packet was generated
	 * @param bytes
	 *            data buffer
	 */
	public MetadataPacket(long sourceID, byte cause[], byte bytes[]) {
		super(sourceID);

		_bytes = new byte[bytes.length];

		System.arraycopy(bytes, 0, _bytes, 0, bytes.length);

		_cause = new byte[cause.length];
		for (int i = 0; i < _cause.length; i++) {
			_cause[i] = cause[i];
		}
	}

	/** Indication of what generated the MetadataPacket */
	public byte[] cause() {
		return _cause;
	}

	/** Get status bytes. */
	public byte[] getBytes() {
		return _bytes;
	}

	/** Return String representation. */
	public String toString() {

		// print all of the super class fields
		String retval = super.toString();
		retval += (" cause=" + new String(_cause) + "\n");
		retval += new String(_bytes);
		return retval;
	}
}

