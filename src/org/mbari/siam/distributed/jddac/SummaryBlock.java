/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed.jddac;

import org.mbari.siam.core.BaseInstrumentService;
import net.java.jddac.common.exception.OpException;
import net.java.jddac.common.meas.collection.Record;
import net.java.jddac.common.type.ArgArray;

import org.mbari.siam.distributed.DevicePacket;
import org.mbari.jddac.AggregationBlock;
import org.mbari.jddac.SampleCountFilter;
import org.mbari.jddac.StatsBlock;

/**
 * Created by IntelliJ IDEA.
 * User: brian
 * Date: Mar 29, 2006
 * Time: 1:04:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class SummaryBlock extends InstrumentServiceBlock {
    
    private StatsBlock statsBlock;
    private DeviceLogBlock deviceLogBlock;
    private SampleCountFilter sampleCountFilter;
    
    public SummaryBlock() {
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

    public void setInstrumentService(BaseInstrumentService instrumentService) {
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
    
    DeviceLogBlock getDeviceLogBlock() {
        if (deviceLogBlock == null) {
            deviceLogBlock = new DeviceLogBlock();
        }
        return deviceLogBlock;
    }

    
  
    
    
}
