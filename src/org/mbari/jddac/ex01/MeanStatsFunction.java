/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/*
 * MeanStatsFunction.java
 *
 * Created on March 23, 2006, 4:08 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.mbari.jddac.ex01;

import org.mbari.jddac.AggregationBlock;
import org.mbari.jddac.IFunction;

import net.java.jddac.common.meas.MeasAttr;
import net.java.jddac.common.meas.Measurement;
import net.java.jddac.common.type.ArgArray;

/**
 * Function that calculates that central tendency statistics for MEasurements that are stored in an aggregation block
 * @author brian
 */
public class MeanStatsFunction implements IFunction {
    
    public final static String MEAN = "mean";
    public final static String STD_DEV = "stdDev";
    public final static String MIN_VALUE = "min";
    public final static String MIN_VALUE_TIME = "minTime";
    public final static String MAX_VALUE = "max";
    public final static String MAX_VALUE_TIME = "maxTime";
    public final static String NSAMPLES = "nSamples";
    public final static String START_TIME = "startTime";
    public final static String STOP_TIME = "stopTime";
    
    private final AggregationBlock aggregationBlock;
    
    /**
     * Creates a new instance of MeanStatsFunction. Since stats require several data points this Function is
     * associated with an AggregationBlock
     *
     * @param  aggregationBlock The AggregationBlock that the stats funciton operates on. A reference to the stats block
     *          is used to fetch the samples stored in the aggregationBlock
     */
    public MeanStatsFunction(AggregationBlock aggregationBlock) {
        if (aggregationBlock == null) {
            throw new IllegalArgumentException("The AggregationBlock argument can not be null");
        }
        this.aggregationBlock = aggregationBlock;
    }
    
    public ArgArray execute(ArgArray argArray) {
        return calc(aggregationBlock.getArgArrays());
    }
    
    private Measurement calc(ArgArray[] samples) {
        
        Measurement measurement = new Measurement();
        
        if (samples != null && samples.length > 0) {
            
            samples[0].get(MeasAttr.TIMESTAMP);

            double max = Double.MIN_VALUE;
            double min = Double.MAX_VALUE;
            double sum = 0;
            for (int i = 0; i < samples.length; i++) {
                Number value = (Number) samples[i].get(MeasAttr.VALUE);
                if (value != null) {
                    double x = value.doubleValue();
                    max = Math.max(max, x);
                    min = Math.min(min, x);
                    sum += x;
                }
            }
            double mean = sum / (double) samples.length;
            double stdDeviation = Math.sqrt(sum /( samples.length - 1));
            
            measurement.put(MEAN, new Double(mean));
            measurement.put(STD_DEV, new Double(stdDeviation));
            measurement.put(MIN_VALUE, new Double(min));
            measurement.put(MAX_VALUE, new Double(max));
            measurement.put(NSAMPLES, new Integer(samples.length));
        }
        
        return measurement;
        
    }
}
