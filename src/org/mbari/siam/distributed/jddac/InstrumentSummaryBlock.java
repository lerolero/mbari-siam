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
import net.java.jddac.common.meas.collection.Record;
import net.java.jddac.common.type.ArgArray;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.jddac.AggregationBlock;
import org.mbari.jddac.SampleCountFilter;
import org.mbari.jddac.StatsBlock;

/**
 * 
 * <p><!-- Insert Description --></p>
 *
 * @author Brian Schlining
 * @version $Id: InstrumentSummaryBlock.java,v 1.2 2012/12/17 21:35:29 oreilly Exp $
 * @deprecated Used SummaryBlock instead
 */
public class InstrumentSummaryBlock extends InstrumentBlock {


    
    private StatsBlock statsBlock;
    private InstrumentDeviceLogBlock deviceLogBlock;
    private SampleCountFilter sampleCountFilter;
    
    public InstrumentSummaryBlock() {
        super();
        // Initialize the stats block
        getStatsBlock();
    }
    
    public void doSummary() {
        getStatsBlock().doStats();
    }
    

    public ArgArray perform(String server_operation_id, ArgArray server_input_arguments) 
            throws Exception, OpException {
        // Overridden. Only method to be called should be processDevicePacket
        return null;
    }
    
    /**
     * @return The number of samples that can be stored in the summaryBlock
     */
    public int getSampleCount() {
        return sampleCountFilter.getCount();
    }
    
    /**
     * @param count The maximum number of samples that the SummaryBlock will hold.
     * If samples beyond this value are added then the oldest samples are removed
     * from the summary block and the new sample is added.
     */
    public void setSampleCount(int count) {
        sampleCountFilter.setCount(count);
    }
    
    public void addVariableName(String variableName) {
        getStatsBlock().addAcceptedVariableName(variableName);
    }
    
    public void removeVariableName(String variableName) {
        getStatsBlock().removeAcceptedVariableName(variableName);
    }
    
    
    public void processDevicePacket(DevicePacket packet) throws Exception {
        Record record = getInstrumentService().getDevicePacketParser().parse(packet);
        getStatsBlock().perform(AggregationBlock.OpIdAddArgArray, record);
    }

    public void setInstrumentService(InstrumentService instrumentService) {
        super.setInstrumentService(instrumentService);
        getDeviceLogBlock().setInstrumentService(instrumentService);
    }
    
    
    public StatsBlock getStatsBlock() {
        if (statsBlock == null) {
            statsBlock = new StatsBlock();
            sampleCountFilter = new SampleCountFilter(statsBlock, 1);
            statsBlock.addFilter(sampleCountFilter);
            statsBlock.addChild(getDeviceLogBlock());
        }
        return statsBlock;
    }
    
    InstrumentDeviceLogBlock getDeviceLogBlock() {
        if (deviceLogBlock == null) {
            deviceLogBlock = new InstrumentDeviceLogBlock();
        }
        return deviceLogBlock;
    }

    
  
    
    
}
