/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.workhorse;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.SensorDataPacket;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.StringTokenizer;
/*
  $Id: WorkhorsePacketParser.java,v 1.9 2012/12/17 21:34:51 oreilly Exp $
  $Name: HEAD $
  $Revision: 1.9 $
*/

public class WorkhorsePacketParser extends PacketParser{

//    static private Logger _logger = Logger.getLogger(WorkhorsePacketParser.class);

    public static String REGISTRY_NAME_KEY="registryName";
    public static String VX_KEY="velocityX";
    public static String VY_KEY="velocityY";
    public static String VZ_KEY="velocityZ";

    private static final long serialVersionUID=1L;

    protected WorkhorseADCP.PD0DataStructure _pd0Struct = new WorkhorseADCP.PD0DataStructure();

	/** No-arg constructor needed to instantiate via class loader (e.g. by logView) */
	public WorkhorsePacketParser(){
		super();
    }
	
    public WorkhorsePacketParser(String registryName){
	super(registryName);
    }

    /** Parse the ADCP data into a Velocity object	*/
    public PacketParser.Field[] parseFields(DevicePacket packet)
	throws NotSupportedException, ParseException
    {

	if (!(packet instanceof SensorDataPacket)) {
	    throw new NotSupportedException("expecting SensorDataPacket");
	}

	PacketParser.Field[] fields = new PacketParser.Field[4];

	double x, y, z;
	int[] vel;

	byte[] data = ((SensorDataPacket)packet).dataBuffer();

	if ((data[0] != WorkhorseADCP.PD0DataStructure.ID_HEADER) || (data[1] != WorkhorseADCP.PD0DataStructure.ID_DATA_SOURCE))
	    throw new NotSupportedException("Packet not a recognized Workhorse format");

	_pd0Struct.setData(data);

	/* ADCP velocities are integer mm/s.  They are converted here
	   to a double type in cm/s
	*/
	vel = _pd0Struct.getVelocity(1);
	x = 0.1 * vel[0];

	vel = _pd0Struct.getVelocity(2);
	y = 0.1 * vel[0];

	vel = _pd0Struct.getVelocity(3);
	z = 0.1 * vel[0];

	fields[0] = new PacketParser.Field(REGISTRY_NAME_KEY, this._registryName, "string");
	fields[1] = new PacketParser.Field(VX_KEY, new Double(x), "cm/s");
	fields[2] = new PacketParser.Field(VY_KEY, new Double(y), "cm/s");
	fields[3] = new PacketParser.Field(VZ_KEY, new Double(z), "cm/s");
	return(fields);
    }

}
