/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

import java.text.ParseException;
import java.io.Serializable;
import java.util.Vector;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.NotSupportedException;

/**
 * Parses DevicePacket packets. A PacketParser can be retrieved over the
 * network; hence it implements java.io.Serializable.
 */
public abstract class PacketParser implements Serializable {
    /** A registry name (belonging to the implementing or calling service class)
	may be associated with this parser. 
    */
    protected String _registryName="UNKNOWN";
    
    public PacketParser(){}
    public PacketParser(String registryName){
	super();
	_registryName=registryName;
    }

    /** Return array of fields parsed from DevicePacket contents. */
    abstract public PacketParser.Field[] parseFields(DevicePacket packet)
	throws NotSupportedException, ParseException;


    /** Get field with specified name. */
    static public Field getField(Field[] fields, String name) 
	throws NoDataException {
	for (int i = 0; i < fields.length; i++) {
	    try {
		if (name.equals(fields[i].getName())) {
		    return fields[i];
		}
	    } catch (Exception e) {
	    }
	}
	throw new NoDataException("Field " + name + " not found");
    }

    /** Each Record contains one or more Fields. */
    public class Field implements Serializable {

	/**
	 * Name of the field. Use byte[] instead of String, to save on
	 * class-retrieval bandwidth.
	 */
	protected String _name = null;

	/** Value of the field. */
	protected Object _value = null;

	/** Units on value (no units by default). */
	protected String _units = "";

	/** Create field with specified name, value, and units on value. */
	public Field(String name, Object value, String units) {
	    _name = name;
	    _value = value;
	    _units = units;
	}

	/** Create field with specified name and UNITLESS value. */
	public Field(String name, Object value) {
	    _name = name;
	    _value = value;
	    _units = ""; // No units
	}

	/** Return the field name. */
	public String getName() {
	    return _name;
	}

	/** Return the field value. */
	public Object getValue() {
	    return _value;
	}

	/** Get units on value. */
	public String getUnits() {
	    return _units;
	}
    }

    /** Each DevicePacket can be parsed into one or more Records. */
    public class Record implements Serializable {

	protected long _timestamp;

	protected long _deviceID;

	protected Vector _fields = new Vector();

	/**
	 * Create new record corresponding to the specified DevicePacket.
	 */
	public Record(DevicePacket packet) {
	    _deviceID = packet.sourceID();
	    _timestamp = packet.systemTime();
	}

	/** Get record timestamp. */
	public long getTimestamp() {
	    return _timestamp;
	}

	/** Get device ID. */
	public long getDeviceID() {
	    return _deviceID;
	}


	/** Add a field to the record.*/
	public void addField(Field field) {
	    _fields.addElement(field);
	}

	/** Get array of all fields in the record. */
	public Field[] getFields() {
	    Field[] fields = new Field[_fields.size()];
	    for (int i = 0; i < _fields.size(); i++) {
		fields[i] = (Field) _fields.elementAt(i);
	    }
	    return fields;
	}
    }
}
