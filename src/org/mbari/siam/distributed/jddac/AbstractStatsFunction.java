/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/*
 * AbstractStatsFunction.java
 *
 * Created on April 6, 2006, 1:21 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.mbari.siam.distributed.jddac;

import org.mbari.jddac.IFunction;

/**
 *
 * @author brian
 */
public abstract class AbstractStatsFunction implements IFunction {
    
    public final static String MEAN = "mean";
    public final static String STD_DEV = "stdDev";
    public final static String MIN_VALUE = "min";
    public final static String MIN_VALUE_TIME = "minTime";
    public final static String MAX_VALUE = "max";
    public final static String MAX_VALUE_TIME = "maxTime";
    public final static String NSAMPLES = "nSamples";
    public final static String START_TIME = "startTime";
    public final static String STOP_TIME = "stopTime";
    
}
