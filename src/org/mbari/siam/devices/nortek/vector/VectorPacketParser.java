/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.nortek.vector;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.SensorDataPacket;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.StringTokenizer;

/** Parses SIAM data packets for Nortek Vector ADV.
    The Vector is a streaming instrument that sends 
    System data packets at 1 Hz and Velocity data packets
    at another (higher) rate (2-64 Hz). The System data
    packets contain a velocity multiplier that is used
    to scale the velocity readings, so they provide 
    context for the velocity data packets.

    To accommodate this, the VectorPacketParser (this class)
    takes both kinds of packets, and uses the system data
    packets to set its internal copy of the velocity 
    multilplier. It may also be initialized with a multiplier
    value, or it may be set using setMult().

    When a VectorPacketParser is obtained via Vector.getParser(), the latest
    value of velocity multiplier is passed to the VectorPacketParser 
    by the Vector instrument service.

    The typical use pattern is to get a single VectorPacketParser
    and feed it a stream of packets as they come in, thus ensuring
    that the correct multiplier is set when a velocity data packet 
    arrives.
 */
/*
  $Id: VectorPacketParser.java,v 1.2 2012/12/17 21:34:10 oreilly Exp $
  $Name: HEAD $
  $Revision: 1.2 $
*/

public class VectorPacketParser extends PacketParser {

    static private Logger _log4j = Logger.getLogger(VectorPacketParser.class);

    public static String REGISTRY_NAME_KEY="name";
    public static String TYPE_KEY="type";
    public static String VX_KEY="velocityX";
    public static String VY_KEY="velocityY";
    public static String VZ_KEY="velocityZ";
    public static String ENSEMBLE_KEY="ensemble";
    public static String PRESSURE_KEY="pressure";
    public static String VMULT_KEY="velocityMultiplier";
    public static String VOLTAGE_KEY="voltage";
    public static String SOUND_SPEED_KEY="soundSpeed";
    public static String HEADING_KEY="heading";
    public static String PITCH_KEY="pitch";
    public static String ROLL_KEY="roll";
    public static String TEMPERATURE_KEY="temperature";
    public static String STATUS_KEY="status";
    public static String ERROR_KEY="error";
    public static String TYPE_VELOCITY_KEY="velocityData";
    public static String TYPE_SYSTEM_KEY="systemData";
    public static String TYPE_AVG_VEL_KEY="averagedVelocityData";
    public static String AVG_VX_KEY="avgVelocityX";
    public static String AVG_VY_KEY="avgVelocityY";
    public static String AVG_VZ_KEY="avgVelocityZ";
    public static String AVG_POINTS="averagedDataPoints";

    private static final long serialVersionUID=1L;

    protected VectorSystemData _systemData = new VectorSystemData();
    protected VectorVelocityData _velocityData = new VectorVelocityData();
    protected double _velocityMultiplier=0.01;
    protected String _vUnits="0.01 cm/sec";
    public VectorPacketParser(String registryName, double mult){
	super(registryName);
	setMult(mult);
    }

    public void setMult(double mult){
	_velocityMultiplier=mult;
    }

    /** Return fields parsed from DevicePacket. */
    public PacketParser.Field[] parseFields(DevicePacket packet) 
	throws NotSupportedException, ParseException {

	if (!(packet instanceof SensorDataPacket)) {
	    throw new NotSupportedException("expecting SensorDataPacket");
	}

	PacketParser.Field[] fields;

	byte[] dataBytes=((SensorDataPacket)packet).dataBuffer();
	PacketParser.Field field0 = new PacketParser.Field("name",this._registryName,"string");

	if (packet.getRecordType() == org.mbari.siam.devices.nortek.vector.Vector.RECORDTYPE_AVG_VEL) {
	    fields = new PacketParser.Field[6];
	    fields[0] = field0;
	    StringTokenizer st = new StringTokenizer(new String(dataBytes));
	    fields[1] = new PacketParser.Field(TYPE_KEY,TYPE_AVG_VEL_KEY,"string");
	    fields[2] = new PacketParser.Field(AVG_VX_KEY,new Double(Double.parseDouble(st.nextToken())));
	    fields[3] = new PacketParser.Field(AVG_VY_KEY,new Double(Double.parseDouble(st.nextToken())));
	    fields[4] = new PacketParser.Field(AVG_VZ_KEY,new Double(Double.parseDouble(st.nextToken())));
	    fields[5] = new PacketParser.Field(AVG_POINTS,new Integer(Integer.parseInt(st.nextToken())));
	    return(fields);
	}
	else if (DataStructure.isVectorVelocityData(dataBytes)) {
	    fields = new PacketParser.Field[7];
	    fields[0] = field0;
	    _velocityData.setBytes(dataBytes);
	    double x = _velocityMultiplier * _velocityData.velocityX();
	    double y = _velocityMultiplier * _velocityData.velocityY();
	    double z = _velocityMultiplier * _velocityData.velocityZ();
	    // data format: Vx,Vy,
	    fields[1] = new PacketParser.Field(TYPE_KEY,TYPE_VELOCITY_KEY,"string");
	    fields[2] = new PacketParser.Field(VX_KEY,new Double(x),_vUnits);
	    fields[3] = new PacketParser.Field(VY_KEY,new Double(y),_vUnits);
	    fields[4] = new PacketParser.Field(VZ_KEY,new Double(z),_vUnits);
	    fields[5] = new PacketParser.Field(ENSEMBLE_KEY,new Integer(_velocityData.ensemble()),"number");
	    fields[6] = new PacketParser.Field(PRESSURE_KEY,new Integer(_velocityData.pressure()),"mm");
	    return(fields);
	}
	else if(DataStructure.id(dataBytes)==Vector.VECTOR_SYSTEM_DATA){
	    fields = new PacketParser.Field[11];
	    fields[0] = field0;
	    _systemData.setBytes(dataBytes);
	    // set this parser's multiplier using the value 
	    // contained in the packet
	    setMult(_systemData.velocityMult());
	    // set units here since it will be less of a hit to 
	    // do it at 1 Hz and need not be checked for each velocity packet
	    if(_velocityMultiplier==0.01){
		_vUnits="0.01 cm/sec";
	    }else{
		_vUnits="0.1 cm/sec";
	    }
	    fields[1] = new PacketParser.Field(TYPE_KEY,TYPE_SYSTEM_KEY,"string");
	    fields[2] = new PacketParser.Field(VMULT_KEY,new Double(_velocityMultiplier),"double");
	    fields[3] = new PacketParser.Field(VOLTAGE_KEY,new Integer(_systemData.voltage()),"0.1 Volts");
	    fields[4] = new PacketParser.Field(SOUND_SPEED_KEY,new Integer(_systemData.soundSpeed()),"0.1 m/s");
	    fields[5] = new PacketParser.Field(HEADING_KEY,new Integer(_systemData.heading()),"0.1 deg");
	    fields[6] = new PacketParser.Field(PITCH_KEY,new Integer(_systemData.pitch()),"0.1 deg");
	    fields[7] = new PacketParser.Field(ROLL_KEY,new Integer(_systemData.roll()),"0.1 deg");
	    fields[8] = new PacketParser.Field(TEMPERATURE_KEY,new Integer(_systemData.temperature()),"0.01 dec C");
	    fields[9] = new PacketParser.Field(STATUS_KEY,new Integer(_systemData.status()),"short bit field");
	    fields[10] = new PacketParser.Field(ERROR_KEY,new Integer(_systemData.error()),"short bit field");
	    return(fields);
	}else
	    throw new ParseException("Not a Parseable Packet type",0);	    
    }

}
