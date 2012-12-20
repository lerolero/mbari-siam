/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed.jddac;

import org.mbari.siam.core.BaseInstrumentService;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.jddac.RelayBlock;

import java.io.Serializable;

/**
 * The InstrumentServiceBlock provides functions that operate on a
 */
public abstract class InstrumentServiceBlock extends RelayBlock implements Serializable {

    /**
     * This OpId is used by the BaseInstrumentService to start a process on a sensor data packet
     */
    public static final String OpIdProcessDevicePacket = "processDevicePacket";

    /**
     * This is the key used to locate the SensorDataPacket inside an argArray
     */
    public static final String KEY_SENSORDATAPACKET = "SensorDataPacket";

    private BaseInstrumentService instrumentService;

    public InstrumentServiceBlock() {
    }

    public BaseInstrumentService getInstrumentService() {
        return instrumentService;
    }

    public void setInstrumentService(BaseInstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    public abstract void processDevicePacket(DevicePacket packet) throws Exception;
    
    

}
