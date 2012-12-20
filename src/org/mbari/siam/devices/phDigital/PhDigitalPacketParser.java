/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.phDigital;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.SensorDataPacket;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.StringTokenizer;

/** Parses SIAM data packets */
/*
  $Id: PhDigitalPacketParser.java,v 1.8 2012/12/17 21:34:17 oreilly Exp $
  $Name: HEAD $
  $Revision: 1.8 $
*/

public class PhDigitalPacketParser extends PacketParser {

    static private Logger _log4j = Logger.getLogger(PhDigitalPacketParser.class);

    public static String REGISTRY_NAME_KEY="name";
    public static String VOLTAGE_KEY="voltage";
    public static String PH_KEY="pH";
    public static String TEMPERATURE_KEY="temperature";
    public static String CORRECTION_KEY="correction";

    private static final long serialVersionUID=1L;

	/** No-arg constructor needed to instantiate via class loader (e.g. by logView) */
	public PhDigitalPacketParser(){
		super();
    }
	
    public PhDigitalPacketParser(String registryName){
	super(registryName);
    }

    /** Return fields parsed from DevicePacket. */
    public PacketParser.Field[] parseFields(DevicePacket packet) 
	throws NotSupportedException, ParseException {

	if (!(packet instanceof SensorDataPacket)) {
	    throw new NotSupportedException("expecting SensorDataPacket");
	}

	String dataString = new String(((SensorDataPacket)packet).dataBuffer());

        // remove first (non-parseable) byte, the '*', before tokenizing 
        StringTokenizer st = 
            new StringTokenizer(dataString.substring(1,dataString.length()-1));

	PacketParser.Field[] fields = new PacketParser.Field[5];
	try{
	    fields[0]=new PacketParser.Field("name",this._registryName,"string");
	    // data format: voltage, pH, correction, temp
	    fields[1] = new PacketParser.Field(VOLTAGE_KEY,new Double(st.nextToken()),"mV");
	    fields[2] = new PacketParser.Field(PH_KEY,new Double(st.nextToken()),"pH units");
	    fields[3] = new PacketParser.Field(TEMPERATURE_KEY,new Double(st.nextToken()),"deg K");
	    fields[4] = new PacketParser.Field(CORRECTION_KEY,new Double(st.nextToken()),"pH units");
	    
	}catch (Exception e){
	    _log4j.error("Can't parse PhDigital record: " + dataString);
	    throw new ParseException(e.toString(),0);
	}

	return fields;
    }

}
