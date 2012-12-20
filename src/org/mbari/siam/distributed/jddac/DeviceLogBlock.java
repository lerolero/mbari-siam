/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/*
 * DeviceLogBlock.java
 *
 * Created on April 11, 2006, 1:56 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.mbari.siam.distributed.jddac;

import org.mbari.siam.core.BaseInstrumentService;
import org.mbari.siam.distributed.jddac.xml.Coder;
import org.mbari.siam.distributed.jddac.xml.XmlCoder;

import net.java.jddac.common.exception.OpException;
import net.java.jddac.common.meas.MeasAttr;
import net.java.jddac.common.type.ArgArray;
import net.java.jddac.common.type.TimeRepresentation;
import net.java.jddac.jmdi.fblock.FunctionBlock;
import org.apache.log4j.Logger;

import org.mbari.siam.distributed.SummaryPacket;

/**
 * For loging summary packets
 *
 * @author brian
 */
public class DeviceLogBlock extends FunctionBlock {
    
    private BaseInstrumentService instrumentService;
    
    private Coder coder = new XmlCoder();
    
    private static final Logger log = Logger.getLogger(DeviceLogBlock.class);
    
    
    /** Creates a new instance of DeviceLogBlock */
    public DeviceLogBlock() {
    }
    
    public DeviceLogBlock(BaseInstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    public BaseInstrumentService getInstrumentService() {
        return instrumentService;
    }

    public void setInstrumentService(BaseInstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    public ArgArray perform(String server_operation_id, ArgArray server_input_arguments) 
            throws Exception, OpException {
        
        /*
         * Make sure the record has a timestamp. If it doesn' add one.
         */
        Object time = server_input_arguments.get(MeasAttr.TIMESTAMP);
        if (time == null) {
            server_input_arguments.put(MeasAttr.TIMESTAMP, new TimeRepresentation());
        }
        
        if (log.isDebugEnabled()) {
            log.debug("Creating a SummaryPacket");
        }
        
        /*
         * Encode the data as XML
         */
        SummaryPacket packet = new SummaryPacket(instrumentService.getId());
        packet.setData(System.currentTimeMillis(), coder.encode(server_input_arguments).getBytes());
        packet.setRecordType(1000L);
        instrumentService.logPacket(packet);
        
        return server_input_arguments;
    }

    public void setCoder(Coder coder) {
        this.coder = coder;
    }

    public Coder getCoder() {
        return coder;
    }
    

    
}
