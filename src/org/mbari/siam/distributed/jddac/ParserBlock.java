/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed.jddac;

import java.io.Serializable;
import org.mbari.siam.core.BaseInstrumentService;
import net.java.jddac.common.type.ArgArray;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DevicePacketParser;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.jddac.FunctionFactory;

/**
 * This InstrumentServiceBlock includes a ParserFunction by default. This
 * function will parse a SensorDataPacket to a SiamRecord and pass the SiamRecord
 * on down the function chain. RelayBlocks that want to process the SiamRecord 
 * should listen for <code>OpIdRelaySiamRecord</code>
 */
public class ParserBlock extends InstrumentServiceBlock implements Serializable {
    
    public static final String OpIdRelaySiamRecord = "relaySiamRecord";
        
    private final ParserFunction parserFunction = new ParserFunction();
    
    private static final Logger log = Logger.getLogger(ParserBlock.class);
    
    private final ArgArray sampleArray = new ArgArray();


    public ParserBlock() {
        this(null);
    }
    
    public ParserBlock(BaseInstrumentService instrumentService) {
        addFunction(FunctionFactory.createFunctionArg(OpIdProcessDevicePacket, 
                OpIdRelaySiamRecord, parserFunction));
        setInstrumentService(instrumentService);
    }

    /**
     * Setting the instrumentSerivce also sets the devicePacketParser used to
     * parse the SensorDataPackets.
     */
    public void setInstrumentService(BaseInstrumentService instrumentService) {
        super.setInstrumentService(instrumentService);
        if (instrumentService != null) {
            PacketParser packetParser = null;
            try {
                parserFunction.setDevicePacketParser((DevicePacketParser) instrumentService.getDevicePacketParser());
            }
            catch (NotSupportedException e) {
		log.debug("The operation 'getParser' is not supported by " + 
			  instrumentService);
            }
            catch (Exception e) {
		log.debug("Failed to set parser", e);
            }
            
        }
    }

    public void processDevicePacket(DevicePacket packet) throws Exception {
        if (packet instanceof SensorDataPacket) {
            sampleArray.put(InstrumentServiceBlock.KEY_SENSORDATAPACKET, packet);
            perform(OpIdProcessDevicePacket, sampleArray);
        }
    }
}
