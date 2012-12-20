/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/*
 * SampleCountFilter.java
 *
 * Created on March 24, 2006, 11:22 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.mbari.jddac;

import net.java.jddac.common.type.ArgArray;
import org.apache.log4j.Logger;

import java.util.Vector;

/**
 * This filter limits the number of samples to 'count'. If an additional sample
 * is added it's added to the end of the list and the oldest sample is removed.
 * Use as:
 * <pre>
 * AggregationBlock fblock = new AggregationBlock();
 * fblock.addFilter(new SampleCountFilter(fblock, 10)); // Hold onto 10 samples
 * </pre>
 *
 *
 * @author brian
 */
public class SampleCountFilter implements  IFilter {
    
    private final AggregationBlock aggregationBlock;
    
    private int count;
    
    private static final Logger log = Logger.getLogger(SampleCountFilter.class);
    
    /** Creates a new instance of SampleCountFilter */
    public SampleCountFilter(AggregationBlock aggregationBlock) {
        this(aggregationBlock, 1);
    }
    
    public SampleCountFilter(AggregationBlock aggregationBlock, int count) {
        if (aggregationBlock == null) {
            throw new IllegalArgumentException("The AggregationBlock argument can not be null");
        }
        this.aggregationBlock = aggregationBlock;
        this.setCount(count);
    }

    public boolean accept(ArgArray argArray) {
        boolean ok = false;
        adjustSamples();
        if (aggregationBlock.size() + 1 <= count) {
            ok = true;
        }
        return ok;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        if (count < 1) {
            log.warn("An attempt was made to set the count property to " + 
                    count + ". This is not allowed. Setting the count to 1");
            count = 1;
        }
        this.count = count;
        adjustSamples();
        
    }
    
    /**
     * This gets the Vector containing the samples in the AggregationBlock and
     * pulls out the earliest samples until the right number is reached.
     */
    private void adjustSamples() {
        Vector samples = aggregationBlock.getSamples();
        synchronized (samples) {
            int n = samples.size();
            while (n >= getCount()) {
                if (log.isDebugEnabled()) {
                    log.debug("Removing " + samples.elementAt(0) + " from " +
                            aggregationBlock);
                }
                samples.removeElementAt(0);
                n = samples.size();
            }
        }
    }
    
    
}
