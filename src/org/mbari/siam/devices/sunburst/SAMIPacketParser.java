/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.sunburst;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.core.DeviceLog;
import org.mbari.siam.core.DeviceLogIterator;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.StringTokenizer;

/** Parses SIAM data packets for Sunburst SAMI-CO2.
    Calculates pCO2 from the packet data plus the calibration values
    that are passed in to the constructor.  Typically, those calibrations
    are included as attributes to the driver.
 */
/*
  $Id: SAMIPacketParser.java,v 1.5 2012/12/17 21:34:41 oreilly Exp $
  $Name: HEAD $
  $Revision $
*/

public class SAMIPacketParser extends PacketParser
{
    public final static String COUNTS = "counts";

    private static final long serialVersionUID=1L;

    /** Calibration values passed in to the constructor, used in calculating pCO2 */
    protected double _cal_T, _cal_a, _cal_b, _cal_c;

    public SAMIPacketParser(String registryName, double cal_T,
			    double cal_a, double cal_b, double cal_c)
    {
	super(registryName);
	_cal_T = cal_T;
	_cal_a = cal_a;
	_cal_b = cal_b;
	_cal_c = cal_c;
    }


    /** Return fields parsed from DevicePacket. */
    public PacketParser.Field[] parseFields(DevicePacket packet) 
	throws NotSupportedException, ParseException
    {
	if (!(packet instanceof SensorDataPacket))
	    throw new NotSupportedException("expecting SensorDataPacket");

	PacketParser.Field[] fields = new PacketParser.Field[19];
	String parseString =(new String(((SensorDataPacket)packet).dataBuffer())).trim();
	StringTokenizer st = new StringTokenizer(parseString, ",");

	int	i434, i620, i740;
	double	temperature, k434, k620;

	try
	{
	    fields[0] = new PacketParser.Field("name", _registryName, "string");
	    fields[1] = new PacketParser.Field("time", st.nextToken().trim(), "string");
	    fields[2] = new PacketParser.Field("date", st.nextToken().trim(), "string");
	    temperature = Double.parseDouble(st.nextToken().trim());
	    fields[3] = new PacketParser.Field("temperature", new Double(temperature), "deg C");
	    fields[4] = new PacketParser.Field("434dark", new Integer(st.nextToken().trim()), COUNTS);
	    fields[5] = new PacketParser.Field("620dark", new Integer(st.nextToken().trim()), COUNTS);
	    fields[6] = new PacketParser.Field("740dark", new Integer(st.nextToken().trim()), COUNTS);
	    i434 = Integer.parseInt(st.nextToken().trim());
	    fields[7] = new PacketParser.Field("434light", new Integer(i434), COUNTS);
	    i620 = Integer.parseInt(st.nextToken().trim());
	    fields[8] = new PacketParser.Field("620light", new Integer(i620), COUNTS);
	    i740 = Integer.parseInt(st.nextToken().trim());
	    fields[9] = new PacketParser.Field("740light", new Integer(i740), COUNTS);
	    fields[10] = new PacketParser.Field("StdDev1", new Integer(st.nextToken().trim()), COUNTS);
	    fields[11] = new PacketParser.Field("StdDev2", new Integer(st.nextToken().trim()), COUNTS);
	    fields[12] = new PacketParser.Field("StdDev3", new Integer(st.nextToken().trim()), COUNTS);
	    k434 = Double.parseDouble(st.nextToken().trim());
	    fields[13] = new PacketParser.Field("434blank", new Double(k434), COUNTS);
	    k620 = Double.parseDouble(st.nextToken().trim());
	    fields[14] = new PacketParser.Field("620blank", new Double(k620), COUNTS);
	    fields[15] = new PacketParser.Field("BattVoltage1", new Integer(st.nextToken().trim()), COUNTS);
	    fields[16] = new PacketParser.Field("BattVoltage2", new Integer(st.nextToken().trim()), COUNTS);
	    fields[17] = new PacketParser.Field("type", new Integer(st.nextToken().trim()), "none");

	    double a434, a620, r, v1, v2, rCO21, rCO22, tCoef, trCO2, pCO2, exp;
	    temperature /= 100.;
	    a434 = -1.0 * (log10(i434/(k434*i740)));
	    a620 = -1.0 * (log10(i620/(k620*i740)));
	    r = a620/a434;
	    v1 = r - 0.0043;
	    v2 = 2.136 - 0.2105*r;
	    rCO21 = -1.0 * (log10(v1/v2));
	    rCO22 = (temperature - _cal_T)*0.008 + rCO21;
	    tCoef = (-0.0075778) - (rCO22 * (-0.0012389)) - (rCO22*rCO22*(-0.00048757));
	    trCO2 = ((temperature - _cal_T) * (-1.0 * tCoef)) + rCO21;
	    exp = ((-1.0*_cal_b) + Math.sqrt((_cal_b*_cal_b) - (4.0*_cal_a*(_cal_c-trCO2)))) /
		   (2.0 * _cal_a);
	    pCO2 = Math.pow(10, exp);

	    fields[18] = new PacketParser.Field("pCO2", new Double(pCO2), "uatm");

	} catch (Exception e) {
	    throw new ParseException("Embedded Exception: " + e.getClass().getName() +
				     ":  " + e.getMessage(), 0);
	}

	return(fields);

    } /* parseFields() */


    /** log10 function here for Java 1.3 compatibility	*/
    protected double log10(double x)
    {
	return(Math.log(x)/Math.log(10));
    }


    /** main() is used to invoke this class as a standalone packet parser */
    public static void main(String[] args)
    {
	double	a=1.0, b=1.0, c=1.0, T=1.0;
	long	deviceID=0;
	String	dir = null;
	DeviceLog log=null;
	DeviceLogIterator iterator=null;

	if (args.length < 6) {
	    System.out.println("Usage: SAMIPacketParser a_cal b_cal c_cal T_cal deviceID directory");
	    System.exit(0);
	}

	try {
	    a = Double.parseDouble(args[0]);
	    b = Double.parseDouble(args[1]);
	    c = Double.parseDouble(args[2]);
	    T = Double.parseDouble(args[3]);
	    deviceID = Long.parseLong(args[4]);
	    dir = args[5];
	} catch (Exception e) {
	    System.err.println("Exception parsing command line: " + e);
	    System.exit(0);
	}

	SAMIPacketParser parser = new SAMIPacketParser("SAMI", T, a, b, c);
	
	try {
	    log = new DeviceLog(deviceID, dir);
	    iterator = new DeviceLogIterator(log);
	    System.out.println("Log contains " + log.nPackets() + " packets");
	} catch (Exception e) {
	    System.err.println("Exception opening device log for ID " + deviceID + 
			       " directory " + dir);
	    System.exit(0);
	}

	int nPackets = 0;
	while (iterator.hasNext())
	{
	    try {
		DevicePacket packet = (DevicePacket)(iterator.next());
		if (packet instanceof SensorDataPacket)
		{
		    System.out.println("\nPacket " + nPackets);
		    PacketParser.Field[] fields = parser.parseFields(packet);

		    for (int i = 0; i < fields.length; i++)
		    {
			System.out.println(fields[i].getName() + ": "
					   + fields[i].getValue() + " "
					   + fields[i].getUnits());
		    }

		    nPackets++;
		}
	    }
	    catch (Exception e) {
		System.err.println("Exception processing packet " + nPackets + ": " + e);
	    }
	}
	System.out.println("Processed " + nPackets + " packets");

    } /* main() */


} /* class SAMIPacketParser() */
