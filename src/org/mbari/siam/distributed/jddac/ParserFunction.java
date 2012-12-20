/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed.jddac;

import org.mbari.siam.distributed.jddac.SiamRecord;
import net.java.jddac.common.type.ArgArray;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.DevicePacketParser;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.jddac.IFunction;

/**
 * Created by IntelliJ IDEA.
 * User: brian
 * Date: Mar 30, 2006
 * Time: 1:56:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class ParserFunction implements IFunction {

    private DevicePacketParser devicePacketParser;
    private static final Logger log = Logger.getLogger(ParserFunction.class);


    /**
     * Executing this funciton will parse a SensorDataPacket contained in an argArray
     *
     * @param argArray The argArray should contain a SensorDataPacket where the key is
     *      InstrumentServiceBlock.KEY_SENSORDATAPACKET
     * @return A SiamRecord containing the parsed Measurements. If the parse failed it will be empty.
     */
    public ArgArray execute(ArgArray argArray) {

        // Pull the SensorDataPacket out of the argArray and parse it.
        SensorDataPacket sensorDataPacket = (SensorDataPacket) argArray.get(InstrumentServiceBlock.KEY_SENSORDATAPACKET);
        ArgArray out = null;
        boolean parsed = false;
        if (sensorDataPacket != null && devicePacketParser != null) {
            try {
                out = devicePacketParser.parse(sensorDataPacket);
                parsed = true;
            }
            catch (Exception e) {
                log.warn("Failed to parse a SensorDataPacket");
            }
        }

        // If parsing does not occur we'll return an empty record
        if (!parsed) {
	    log.debug("Did not parse data from " + argArray + 
		      ". Returning an empty ArgArray");

            out = new SiamRecord();
        }

        return out;
    }

    public DevicePacketParser getDevicePacketParser() {
        return devicePacketParser;
    }

    public void setDevicePacketParser(DevicePacketParser devicePacketParser) {
        this.devicePacketParser = devicePacketParser;
    }

}
