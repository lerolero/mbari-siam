/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.moos.utils.chart.sampler;

import java.util.Vector;
import java.util.StringTokenizer;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.SensorDataPacket;

/** Parses SIAM data packets */
public class EnvironmentalPacketParser extends SensorPacketParser {
    public static final int TEMPERATURE = SENSOR_PACKET_FIELDS+0;
    public static final int PRESSURE = SENSOR_PACKET_FIELDS+1;
    public static final int HUMIDITY = SENSOR_PACKET_FIELDS+2;
    public static final int GFLO = SENSOR_PACKET_FIELDS+3;
    public static final int GFHI = SENSOR_PACKET_FIELDS+4;
    public static final int ENVIRONMENTAL_PACKET_FIELDS = SENSOR_PACKET_FIELDS+5;

    public EnvironmentalPacketParser(){
	super();
	_fields.setSize(ENVIRONMENTAL_PACKET_FIELDS);
    }

    public EnvironmentalPacketParser(SensorDataPacket packet){
	super();
	_fields.setSize(ENVIRONMENTAL_PACKET_FIELDS);
	parse(packet);
    }

    public Double getDouble(String token, int start){
	return new Double(Double.parseDouble(token.substring(start)));
    }

    public Vector parse(Object data){
	_sensorPacket = (SensorDataPacket)data;	
	_fields=super.parse(data);
	String foo = (String)get(DATA_BUFFER);
	StringTokenizer st = new StringTokenizer(foo.trim(),",");
	while(st.hasMoreTokens()){
	    String token = st.nextToken().trim();
	    //System.out.println("parsing "+token);
	    if(token.toUpperCase().startsWith("T"))
		_fields.setElementAt(getDouble(token,1),TEMPERATURE);
	    if(token.toUpperCase().startsWith("P"))
		_fields.setElementAt(getDouble(token,1),PRESSURE);
	    if(token.toUpperCase().startsWith("H"))
		_fields.setElementAt(getDouble(token,1),HUMIDITY);
	    if(token.toUpperCase().startsWith("GFLO"))
		_fields.setElementAt(getDouble(token,4),GFLO);
	    if(token.toUpperCase().startsWith("GFHI"))
		_fields.setElementAt(getDouble(token,4),GFHI);
	}
	return _fields;
    }

    public Object  get(int field){
	//System.out.println("fields: "+_fields.size()+" {"+_fields+"} getting field:"+field);
	if( field < ENVIRONMENTAL_PACKET_FIELDS)
	    return _fields.elementAt(field);
	else
	    return null;
    }

    public Object get(String fieldName){

	String field = fieldName.trim().toUpperCase();
	if(field.equals("TEMPERATURE")){
	    //System.out.println("found "+field+" = "+_fields.elementAt(TEMPERATURE));
	    return(_fields.elementAt(TEMPERATURE));
	}
	if(field.equals("PRESSURE"))
	    return(_fields.elementAt(PRESSURE));
	if(field.equals("HUMIDITY"))
	    return(_fields.elementAt(HUMIDITY));
	if(field.equals("GFLO"))
	    return(_fields.elementAt(GFLO));
	if(field.equals("GFHI"))
	    return(_fields.elementAt(GFHI));

	return super.get(fieldName);
    }

}
