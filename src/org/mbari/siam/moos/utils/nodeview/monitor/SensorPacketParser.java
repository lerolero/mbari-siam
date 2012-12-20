/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.moos.utils.nodeview.monitor;

import java.util.Vector;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.SensorDataPacket;

/** Parses SIAM data packets */
public class SensorPacketParser extends PacketParser {
    public static final int DATA_BUFFER = DEVICE_PACKET_FIELDS+0;
    public static final int SENSOR_PACKET_FIELDS = DEVICE_PACKET_FIELDS+1;
    SensorDataPacket _sensorPacket = null;

    public SensorPacketParser(){
	super();
	_fields.setSize(SENSOR_PACKET_FIELDS);
    }

    public SensorPacketParser(SensorDataPacket packet){
	super();
	_fields.setSize(SENSOR_PACKET_FIELDS);
	parse(packet);
    }

    public Vector parse(Object data){
	_fields=super.parse(data);
	_sensorPacket = (SensorDataPacket)data;	
	// This may not work out so well, but it will do for now
	_fields.setElementAt(new String(_sensorPacket.dataBuffer()),DATA_BUFFER);
	return _fields;
    }

    public Object  get(int field){
	System.out.println("SensorPacketParser get("+field+")");
	if( field < SENSOR_PACKET_FIELDS){
	    System.out.println("SensorPacketParser: field["+field+"]="+_fields.elementAt(field));
	    return _fields.elementAt(field);
	}else{
	    System.out.println("SensorPacketParser: field out of range "+field);
	    return null;
	}
    }

    public SensorDataPacket getSensorPacket(){
	return _sensorPacket;
    }

}
