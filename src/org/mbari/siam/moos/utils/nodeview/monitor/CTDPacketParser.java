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
public class CTDPacketParser extends SensorPacketParser {
    public static final int TEMPERATURE = SENSOR_PACKET_FIELDS+0;
    public static final int PRESSURE = SENSOR_PACKET_FIELDS+1;
    public static final int CONDUCTIVITY = SENSOR_PACKET_FIELDS+2;
    public static final int CTD_PACKET_FIELDS = SENSOR_PACKET_FIELDS+3;

    public CTDPacketParser(){
	super();
	_fields.setSize(CTD_PACKET_FIELDS);
    }

    public CTDPacketParser(SensorDataPacket packet){
	super();
	_fields.setSize(CTD_PACKET_FIELDS);
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
	    System.out.println("token 1 (temp)="+subToken);
	    int index=0;
	    for(index=0;index<subToken.length();index++)
		if(Character.isDigit(subToken.charAt(index)))
		    break;
	    _fields.setElementAt(getDouble(subToken.substring(index),0),TEMPERATURE);
	}
	if (st.hasMoreTokens()){
	    subToken = st.nextToken().trim();
	    System.out.println("token 2(cond)="+subToken);
	    int index=0;
	    for(index=0;index<subToken.length();index++)
		if(Character.isDigit(subToken.charAt(index)))
		    break;
	    _fields.setElementAt(getDouble(subToken.substring(index),0),CONDUCTIVITY);
	}
	_fields.setElementAt(new Double(0),PRESSURE);

	return _fields;
    }

    public Object  get(int field){
	//System.out.println("CTDPacketParser fields: "+_fields.size()+" {"+_fields+"} getting field:"+field);
	if( field < CTD_PACKET_FIELDS)
	    return _fields.elementAt(field);
	else
	    return null;
    }

    public Object get(String fieldName){

	String field = fieldName.trim().toUpperCase();
	if(field.equals("TEMPERATURE")){
	    System.out.println("found "+field+" = "+_fields.elementAt(TEMPERATURE));
	    return(_fields.elementAt(TEMPERATURE));
	}
	if(field.equals("PRESSURE"))
	    return(_fields.elementAt(PRESSURE));
	if(field.equals("CONDUCTIVITY"))
	    return(_fields.elementAt(CONDUCTIVITY));

	return super.get(fieldName);
    }

}
