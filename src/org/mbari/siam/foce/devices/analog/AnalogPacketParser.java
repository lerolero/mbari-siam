/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.analog;

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

/** Parses SIAM data packets */
/*
  $Id: AnalogPacketParser.java,v 1.4 2012/12/17 21:36:28 oreilly Exp $
  $Name: HEAD $
  $Revision: 1.4 $
  @author Bob Herlien
*/

public class AnalogPacketParser extends PacketParser implements Serializable
{
    static private Logger _log4j = Logger.getLogger(AnalogPacketParser.class);
    private static final long serialVersionUID=1L;

    protected int	_numChans;
    protected String[]	_parseNames, _parseUnits;

    public AnalogPacketParser(int numChans, String registryName,
			    String[] parseNames, String[] parseUnits)
    {
	super(registryName);

	_numChans = numChans;
	_parseNames = parseNames;
	_parseUnits = parseUnits;
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
	PacketParser.Field[] fields = new PacketParser.Field[_numChans+1];

	try {
	    fields[0]=new PacketParser.Field("name", _registryName, "string");

	    for (int i = 0; i < _numChans; i++)
	    {
		fields[i+1] = new PacketParser.Field(_parseNames[i],
						     new Double(Double.parseDouble(st.nextToken())),
						     _parseUnits[i]);
	    }
	}catch (Exception e){
	    _log4j.error("Can't parse Analog record: " + dataString);
	    throw new ParseException(e.toString(), 0);
	}

	return fields;
    }
}
