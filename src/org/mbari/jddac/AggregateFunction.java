/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.jddac;

import net.java.jddac.common.type.ArgArray;
import org.apache.log4j.Logger;

/**
 * This Function is applied to an aggregation block. It converts the Samples
 * that are stored internally from an ArgArray[] to an ArgArray like:
 * <pre>
 * ArgArray {
 *   "0": argArray[0]
 *   "1": argArray[1]
 *   "2": argArray[2]
 *   ...
 * }
 * </pre>
 * This allows the records to be passed on to other FunctionBlocks that expect
 * an argArray
 */
public class AggregateFunction implements IFunction {
    
    final AggregationBlock aggregationBlock;
    
    private static final Logger log = Logger.getLogger(AggregateFunction.class);
    
    public AggregateFunction(AggregationBlock aggregationBlock) {
        this.aggregationBlock = aggregationBlock;
    }
    
    public ArgArray execute(ArgArray argArray) {
        if (log.isDebugEnabled()) {
            log.debug("Aggregating samples from " + aggregationBlock);
        }
        ArgArray[] samples = aggregationBlock.getArgArrays();
        ArgArray out = new ArgArray();
        for (int i = 0; i < samples.length; i++) {
            out.put(String.valueOf(i), samples[i]);
        }
        return out;
    }
    
}