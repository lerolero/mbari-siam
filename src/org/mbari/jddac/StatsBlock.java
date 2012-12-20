/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/*
 * StatsBlock.java
 *
 * Created on April 10, 2006, 9:20 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.mbari.jddac;

import org.mbari.siam.distributed.jddac.FilterByKeyFunction;
import org.mbari.siam.distributed.jddac.MutableIntegerArrayFilter;
import org.mbari.siam.distributed.jddac.MutableIntegerArrayStatsFunction;
import org.mbari.siam.distributed.jddac.NumberFilter;
import org.mbari.siam.distributed.jddac.NumberStatsFunction;
import net.java.jddac.common.exception.OpException;
import net.java.jddac.common.type.ArgArray;
import net.java.jddac.jmdi.fblock.FunctionBlock;

import org.apache.log4j.Logger;

/**
 * Aggregates Records and/or Measurements for statistical analysis. Use as:
 * <pre>
 * StatsBlock b = new StatsBlock();
 * b.addFilter(someFilter);
 * b.addAcceptedVariableName("AVariableNameToProcess");
 * b.addChild(someBlockToHandleOutput);
 * 
 * // Add some records
 * b.addArgArray(someArgArray1);
 * b.addArgArray(someArgArray2);
 * b.doStats();
 * </pre>
 * 
 *You can add Child FunctionBLocks that listen for OpIdProcessRecord
 * 
 * 
 * @author brian
 */
public class StatsBlock extends AggregationBlock {
    
    private FilterByKeyFunction filterByKeyFunction = new FilterByKeyFunction();
    private RelayBlock drainBlock;
    private static final Logger log = Logger.getLogger(StatsBlock.class);
    
    public static final String OpIdDoStats = "doStats";
    public static final String OpIdProcessRecord = "processRecord";
    
    public StatsBlock() {
        init();
    }
    
    
    private void init() {
        
        /*
         * This function converts the internal array into an ArgArray that
         * can be relayed to other FunctionBlocks
         */
        IFunction f0 = new AggregateFunction(this);
        super.addFunction(FunctionFactory.createFunctionArg(OpIdDoStats,
                OpIdProcessRecord, f0));
        
        /*
         * This block Coallates the Measurements by Name then relays them on
         */
        RelayBlock coallateBlock = new RelayBlock();
        super.addChild(coallateBlock);
        IFunction f2 = new CoallateByMeasurementNameFunction();
        coallateBlock.addFunction(FunctionFactory.createFunctionArg(OpIdProcessRecord,
                OpIdProcessRecord, f2));
        
        /*
         * This block Filters out only the records we want and passes them on
         * to the next block
         */
        RelayBlock filterByKeyBlock = new RelayBlock();
        coallateBlock.addChild(filterByKeyBlock);
        filterByKeyBlock.addFunction(FunctionFactory.createFunctionArg(OpIdProcessRecord,
                OpIdProcessRecord, filterByKeyFunction));
        
        /*
         * This block converts the output of the filter block to a standard 
         * argArray. The argArray should arrive as follows:
         * ArgArray {
         * "name": ArgArray {
         *           "name1": Measurement
         *           "name2": Measurement
         *           "name3": Measurement
         *           ...
         *         }
         * }
         *
         * This block converts it to:
         * ArgArray {
         *   "name1": Measurement
         *   "name2": Measurement
         *   "name3": Measurement
         *   ...
         * }
         * 
         */
        RelayBlock scatterBlock = new ScatterBlock();
        filterByKeyBlock.addChild(scatterBlock);
        
        FilterBlock numberStatsBlock = new FilterBlock();
        scatterBlock.addChild(numberStatsBlock);
        numberStatsBlock.addFilter(new NumberFilter());
        IFunction f4 = new NumberStatsFunction();
        numberStatsBlock.addFunction(FunctionFactory.createFunctionArg(OpIdProcessRecord, OpIdProcessRecord, f4));
        
        
        FilterBlock miaStatsBlock = new FilterBlock();
        scatterBlock.addChild(miaStatsBlock);
        miaStatsBlock.addFilter(new MutableIntegerArrayFilter());
        IFunction f5 = new MutableIntegerArrayStatsFunction();
        miaStatsBlock.addFunction(FunctionFactory.createFunctionArg(OpIdProcessRecord, OpIdProcessRecord, f5));
       

        /*
         * This block gets handed the results
         */
        numberStatsBlock.addChild(getDrainBlock());
        miaStatsBlock.addChild(getDrainBlock());
    }
    
    
    RelayBlock getDrainBlock() {
        if (drainBlock == null) {
            drainBlock = new RelayBlock();
            drainBlock.addFunction(FunctionFactory.createFunctionArg(OpIdProcessRecord, OpIdProcessRecord, new RelayFunction()));
        }
        return drainBlock;
    }

    /**
     * Children added to this function will only recieve ArgArrays that result
     * from the statistical processing.
     * @see NumberStatsFunction
     * @see MutableIntegerArrayStatsFunction
     * @param child A CHild FunctionBlock to handle results. The child should
     *  process events with ID = OpIdProcessRecord
     */
    public boolean addChild(FunctionBlock child) {
        return getDrainBlock().addChild(child);
    }

    /**
     * You are not allowed to add functions to this block. Calling this method
     * does nothing.
     */
    public boolean addFunction(ArgArray argArray) {
        // Do nothing. You can not add functions to this block
        return false;
    }


    /**
     * Overridden method only adds logging. It does not change the super methods
     * implementation.
     */
    public ArgArray perform(String server_operation_id, ArgArray server_input_arguments) 
            throws Exception, OpException {
        
        if (OpIdDoStats.equals(server_operation_id) && log.isDebugEnabled()) {
            log.debug("Attempting to calculate statistic using " + size() + " samples.");
        }
        
        return super.perform(server_operation_id, server_input_arguments);
    }
    
    
    
    /**
     * Calling this method causes Stats to be executed on the samples that have
     * been collected
     */
    public void doStats() {
        try {
            perform(OpIdDoStats, new ArgArray());
        } 
        catch (Exception ex) {
            log.error("Failed to execute 'doStats'", ex);
        }
    }
    
    public void addAcceptedVariableName(String variableName) {
        filterByKeyFunction.getAllowedKeys().add(variableName);
    }
    
    public void removeAcceptedVariableName(String variableName) {
        filterByKeyFunction.getAllowedKeys().remove(variableName);
    }
    
    public String[] listAcceptedVariableNames() {
        return (String[]) filterByKeyFunction.getAllowedKeys().toArray(new String[1]);
    }
    
    
    
}
