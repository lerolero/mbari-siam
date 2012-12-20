/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.moos.utils.chart.sampler;

import java.util.Vector;
import org.mbari.siam.distributed.DevicePacket;

/** Parses SIAM data packets */
public class PacketParser implements Parser {
    public static final int TIMESTAMP        = 0;
    public static final int SOURCE_ID        = 1;
    public static final int SEQUENCE_NO      = 2;
    public static final int STATUS_VERSION   = 3;
    public static final int DEVICE_PACKET_FIELDS    = 4;

    DevicePacket _packet = null;
    Vector _fields = new Vector(DEVICE_PACKET_FIELDS);

    public Vector parse(Object data){
	_packet = (DevicePacket)data;
	_fields.setElementAt(new Long(_packet.systemTime()),TIMESTAMP);
	_fields.setElementAt(new Long(_packet.sourceID()),SOURCE_ID);
	_fields.setElementAt(new Long(_packet.sequenceNo()),SEQUENCE_NO);
	_fields.setElementAt(new Long(_packet.metadataRef()),STATUS_VERSION);
	return _fields;
    }

    public Vector  getAll(){return _fields;}

    public Object  get(int field){
	if( field < DEVICE_PACKET_FIELDS)
	    return _fields.elementAt(field);
	else
	    return null;
    }

    public Object get(String fieldName){
	String field = fieldName.trim().toUpperCase();
	if(field.equals("TIMESTAMP"))
	    return(_fields.elementAt(TIMESTAMP));
	if(field.equals("SOURCE_ID"))
	    return(_fields.elementAt(SOURCE_ID));
	if(field.equals("SEQUENCE_NO"))
	    return(_fields.elementAt(SEQUENCE_NO));
	if(field.equals("STATUS_VERSION"))
	    return(_fields.elementAt(STATUS_VERSION));

	return null;
    }

    public DevicePacket getPacket(){
	return _packet;
    }
}
