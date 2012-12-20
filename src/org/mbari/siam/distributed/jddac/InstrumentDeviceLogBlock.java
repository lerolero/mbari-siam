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
import net.java.jddac.common.exception.OpException;
import net.java.jddac.common.type.ArgArray;
import net.java.jddac.jmdi.fblock.FunctionBlock;
import org.mbari.siam.distributed.SummaryPacket;

/**
 * 
 * <p><!-- Insert Description --></p>
 *
 * @author Brian Schlining
 * @version $Id: InstrumentDeviceLogBlock.java,v 1.2 2012/12/17 21:35:27 oreilly Exp $
 * @deprecated Use DeviceLogBlock instead
 */
public class InstrumentDeviceLogBlock extends FunctionBlock {

   private InstrumentService instrumentService;
    
    /** Creates a new instance of DeviceLogBlock */
    public InstrumentDeviceLogBlock() {
    }
    
    public InstrumentDeviceLogBlock(InstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    public InstrumentService getInstrumentService() {
        return instrumentService;
    }

    public void setInstrumentService(InstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    public ArgArray perform(String server_operation_id, ArgArray server_input_arguments) 
            throws Exception, OpException {
        
        SummaryPacket packet = new SummaryPacket(instrumentService.getId());
        packet.setData(System.currentTimeMillis(), server_input_arguments.toString().getBytes());
        instrumentService.logPacket(packet);
        
        return server_input_arguments;
    }
    

    
}
