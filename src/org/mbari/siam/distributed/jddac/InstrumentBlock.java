/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/*
 * Created on Apr 14, 2006
 * 
 * The Monterey Bay Aquarium Research Institute (MBARI) provides this
 * documentation and code 'as is', with no warranty, express or
 * implied, of its quality or consistency. It is provided without support and
 * without obligation on the part of MBARI to assist in its use, correction,
 * modification, or enhancement. This information should not be published or
 * distributed to third parties without specific written permission from MBARI
 */
package org.mbari.siam.distributed.jddac;

import org.mbari.siam.core.InstrumentService;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.jddac.RelayBlock;

/**
 * 
 * <p><!-- Insert Description --></p>
 *
 * @author Brian Schlining
 * @version $Id: InstrumentBlock.java,v 1.2 2012/12/17 21:35:26 oreilly Exp $
 * @deprecated Used InstrumentServiceBlock instead
 * @see InstrumentServiceBlock
 */
public abstract class InstrumentBlock extends RelayBlock {
    
    /**
     * This OpId is used by the BaseInstrumentService to start a process on a sensor data packet
     */
    public static final String OpIdProcessDevicePacket = "processDevicePacket";

    /**
     * This is the key used to locate the SensorDataPacket inside an argArray
     */
    public static final String KEY_SENSORDATAPACKET = "SensorDataPacket";

    private InstrumentService instrumentService;

    public InstrumentBlock() {
        super();
        // TODO Auto-generated constructor stub
    }
    
    public InstrumentService getInstrumentService() {
        return instrumentService;
    }

    public void setInstrumentService(InstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    public abstract void processDevicePacket(DevicePacket packet) throws Exception;
}
