/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.moos.utils.chart.sampler;

import java.util.Vector;
import org.mbari.siam.distributed.DevicePacket;

/** Creates Parsers for SIAM data packets
    Not especially manageable/scalable...just an interrim
    solution.
 */
public class ParserFactory {
    public static final int ISI_ENVIRONMENTAL = 1217;
    public static final int ISI_SEABIRD37SM   = 1105;
    public static final int ISI_GARMIN25HVS   = 1216;
    public static final int ISI_SHMOO_00 = 1020;
    public static final int ISI_SHMOO_01 = 1021;
    public static final int ISI_SHMOO_02 = 1022;
    public static final int ISI_SHMOO_03 = 1023;
    public static final int ISI_SHMOO_04 = 1024;
    public static final int ISI_SHMOO_05 = 1025;
    public static final int ISI_KVHC100  = 1103;

    private static ParserFactory _theFactory=null;

    public static ParserFactory getInstance(){
	if(_theFactory==null)
	    _theFactory=new ParserFactory();
	return _theFactory;
    }

    public PacketParser getParser(DevicePacket packet){
	return getParser((int)packet.sourceID());
    }

    public PacketParser getParser(long isiID){
	switch((int)isiID){
	case ISI_ENVIRONMENTAL:
	    return new EnvironmentalPacketParser();
	default:
	    return null;
	}
    }

}
