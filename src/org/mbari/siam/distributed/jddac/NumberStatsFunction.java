/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/*
 * NumberStatsFunction.java
 *
 * Created on April 6, 2006, 12:55 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.mbari.siam.distributed.jddac;

import java.util.Enumeration;
import java.util.Vector;
import net.java.jddac.common.meas.MeasAttr;
import net.java.jddac.common.meas.Measurement;
import net.java.jddac.common.type.ArgArray;
import org.apache.log4j.Logger;
import org.mbari.jddac.IFunction;

/**
 *
 * @author brian
 */
public class NumberStatsFunction extends AbstractStatsFunction {
    
    private static final Logger log = Logger.getLogger(NumberStatsFunction.class);
    
    /**
     * Creates a new instance of NumberStatsFunction
     */
    public NumberStatsFunction() {
    }
    
    /**
     * @param argArray <p>An ArgArray of Measurmentts to calculate stats on The
     * ArgArray should be like:
     * <pre>
     * ArgArray {
     *   "name-1": Measurement {
     *      MeasAttr.VALUE: MutableInteger
     *    }
     *   "name-2": Measurement {
     *      MeasAttr.VALUE: MutableInteger
     *    }
     *    ...
     * }
     * </pre>
     * Each measurement should, at a minimum, contain a value that is a MutableInteger
     * </p>
     */
    public ArgArray execute(ArgArray argArray) {
        
	log.debug("Executing on " + argArray);

        Measurement measurement = new Measurement();
        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;
        double sum = 0;
        int count = 0;
        
        Vector values = new Vector();
        
        Enumeration keys = argArray.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            Object obj = argArray.get(key);
            
	    log.debug("Found " + obj);
            
            /*
             * Ignore any values in the ArgArray that are not Measurements.
             */
            if (obj instanceof  Measurement) {
                Measurement m = (Measurement) obj;
                
		log.debug("Found " + m);
                
                /*
                 * If it's the first measurement grab all the non-value, non-timestamp
                 * fields and add to our measurement.
                 */
                if (count == 0) {
                    Enumeration fields = m.keys();
                    while (fields.hasMoreElements()) {
                        String field = (String) fields.nextElement();
                        if (!field.equals(MeasAttr.TIMESTAMP) && !field.equals(MeasAttr.VALUE)) {
                            measurement.put(field, m.get(field));
                        }
                    }
                }
                
                /*
                 * If the value is not null then perform statistics on it.
                 */
                Number value = (Number) m.get(MeasAttr.VALUE);
                if (value != null) {
                    values.add(value);
                    log.debug("Found a value");
                    double x = value.doubleValue();
                    max = Math.max(max, x);
                    min = Math.min(min, x);
                    sum += x;
                    count++;
                }
            }
        }
        
        double mean = sum / (double) count;
        Enumeration data = values.elements();
        double sig = 0;
        while (data.hasMoreElements()) {
            Number n = (Number) data.nextElement();
            sig += Math.pow(n.doubleValue() - mean, 2);
        }
        double stdDeviation = Math.sqrt(sig / (count - 1));
        
        
        /*
         * Store the stats in our measurement
         */
        measurement.put(MEAN, new Double(mean));
        measurement.put(STD_DEV, new Double(stdDeviation));
        measurement.put(MIN_VALUE, new Double(min));
        measurement.put(MAX_VALUE, new Double(max));
        measurement.put(NSAMPLES, new Integer(count));
        
        return measurement;
    }
    
    
}
