/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.moos.utils.nodeview.monitor;

import java.util.Vector;
import java.util.StringTokenizer;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.SensorDataPacket;

/** Parses SIAM data packets */
public class CompassPacketParser extends SensorPacketParser {
    public static final int HEADING = SENSOR_PACKET_FIELDS+0;
    public static final int COMPASS_PACKET_FIELDS = SENSOR_PACKET_FIELDS+1;

    public CompassPacketParser(){
	super();
	_fields.setSize(COMPASS_PACKET_FIELDS);
    }

    public CompassPacketParser(SensorDataPacket packet){
	super();
	_fields.setSize(COMPASS_PACKET_FIELDS);
	parse(packet);
    }

    public Vector parse(Object data){
	_sensorPacket = (SensorDataPacket)data;	
	_fields=super.parse(data);
	String foo = (String)get(DATA_BUFFER);
	StringTokenizer st = new StringTokenizer(foo.trim(),",");
	String subToken=null;
	if (st.hasMoreTokens()){
	    subToken = st.nextToken().trim();
	    System.out.println("token 1 (NMEA type)="+subToken);
	}
	if (st.hasMoreTokens()){
	    subToken = st.nextToken().trim();
	    System.out.println("token 2(heading)="+subToken);
	    int index=0;
	    for(index=0;index<subToken.length();index++)
		if(Character.isDigit(subToken.charAt(index)))
		    break;
	    _fields.setElementAt(getDouble(subToken.substring(index),0),HEADING);
	}
	return _fields;
    }

    public Object  get(int field){
	//System.out.println("CompassPacketParser fields: "+_fields.size()+" {"+_fields+"} getting field:"+field);
	if( field < COMPASS_PACKET_FIELDS)
	    return _fields.elementAt(field);
	else
	    return null;
    }

    public Object get(String fieldName){

	String field = fieldName.trim().toUpperCase();
	if(field.equals("HEADING"))
	    return(_fields.elementAt(HEADING));

	return super.get(fieldName);
    }

}
