/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.cpuTemp;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.SensorDataPacket;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.StringTokenizer;

/** Parses packets from CPUTempService */
/*
  $Id: CPUTempPacketParser.java,v 1.3 2012/12/17 21:37:16 oreilly Exp $
  $Name: HEAD $
  $Revision $
  @author Bob Herlien
*/

public class CPUTempPacketParser extends PacketParser implements Serializable
{
    static private Logger _log4j = Logger.getLogger(CPUTempPacketParser.class);
    private static final long serialVersionUID=1L;

    public CPUTempPacketParser(String registryName)
    {
	super(registryName);
    }

    /** Return fields parsed from DevicePacket. */
    public PacketParser.Field[] parseFields(DevicePacket packet) 
	throws NotSupportedException, ParseException
    {
	if (!(packet instanceof SensorDataPacket)) {
	    throw new NotSupportedException("expecting SensorDataPacket");
	}

	String dataString = new String(((SensorDataPacket)packet).dataBuffer());
	StringTokenizer st = new StringTokenizer(dataString);
	PacketParser.Field[] fields = new PacketParser.Field[3];

	try {
	    fields[0] = new PacketParser.Field("name", _registryName, "string");

	    fields[1] = new PacketParser.Field("CPU_Temperature",
					       new Integer(st.nextToken()),
					       "degrees C");

	    fields[2] = new PacketParser.Field("Ambient_Temperature",
					       new Integer(st.nextToken()),
					       "degrees C");

	}catch (Exception e){
	    _log4j.error("Can't parse Digital record: " + dataString);
	    throw new ParseException(e.toString(), 0);
	}

	return fields;
    }
}
