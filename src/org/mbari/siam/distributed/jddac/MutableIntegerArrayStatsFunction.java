/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed.jddac;

import java.util.Enumeration;
import java.util.Vector;
import net.java.jddac.common.meas.MeasAttr;
import net.java.jddac.common.meas.Measurement;
import net.java.jddac.common.type.ArgArray;
import org.apache.log4j.Logger;
/*
 * MutableIntegerArrayStatsFunction.java
 *
 * Created on April 6, 2006, 12:58 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author brian
 */
public class MutableIntegerArrayStatsFunction extends  AbstractStatsFunction {
    
    // CVS revision 
    private static String _versionID = "$Revision: 1.3 $";

    private static final Logger log = Logger.getLogger(MutableIntegerArrayStatsFunction.class);
    
    /** Creates a new instance of MutableIntegerArrayStatsFunction */
    public MutableIntegerArrayStatsFunction() {
    }
    
    public ArgArray execute(ArgArray argArray) {
        Measurement measurement = new Measurement();
        int[] max =  new int[1];
        int[] min = new int[1];
        int[] sum = new int[1];
        int count = 0;
        int arraySize = 0;
        
        Vector data = new Vector();
        
	log.debug("Running stats on " + argArray);
        
        Enumeration keys = argArray.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            Object obj = argArray.get(key);
            
            /*
             * Ignore any values in the ArgArray that are not Measurements.
             */
            if (obj instanceof  Measurement) {
                Measurement m = (Measurement) obj;
                
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
                Object value = m.get(MeasAttr.VALUE);
                if (value instanceof MutableIntegerArray) {
                    MutableIntegerArray array = (MutableIntegerArray) value;
                    int[] values = array.getValues();
                    
                    
                    /*
                     * On the first Measurment, initialize values
                     */
                    if (count == 0) {
                        arraySize = array.size();
                        max = new int[arraySize];
                        System.arraycopy(values, 0, max, 0, arraySize);
                        min = new int[arraySize];
                        System.arraycopy(values, 0, min, 0, arraySize);
                        sum = new int[arraySize];
                        //System.arraycopy(values, 0, sum, 0, arraySize);
                    }
                    
                    /*
                     * We can only do stats on arrays that are the same size.
                     * Stats are column wise (i.e. by index, valueA[1] is
                     * compared with valueB[1]
                     */
                    if (array.size() == arraySize) {
                        data.add(values);
                        for (int i = 0; i < arraySize; i++) {
                            max[i] = Math.max(max[i], values[i]);
                            min[i] = Math.min(min[i], values[i]);
                            sum[i] += values[i];
                        }
                        count++;
                        
                    } else {
                        if (log.isInfoEnabled()) {
                            log.info("The MutableIntegerArray contains " + array.size() +
                                    " elements. Expected " + arraySize  + " elements");
                        }
                    }
                }
            }
        }
        
        double[] mean = new double[arraySize];
        for (int i = 0; i < arraySize; i++) {
            mean[i] = (double) sum[i] / (double) count;
        }
        
        Enumeration v = data.elements();
        double[] sig = new double[arraySize];
        while (v.hasMoreElements()) {
            int[] datum = (int[]) v.nextElement();
            for (int i = 0; i < arraySize; i++) {
                sig[i] += Math.pow((double) datum[i] - mean[i], 2);
            }
        }
        
        double stdDeviation[] = new double[arraySize];
        for (int i = 0; i < arraySize; i++) {
            stdDeviation[i] = Math.sqrt(sig[i] / (count - 1));
        }
        
        
        
        /*
         * Store the stats in our measurement
         */
        measurement.put(MEAN, new MutableDoubleArray(mean));
        measurement.put(STD_DEV, new MutableDoubleArray(stdDeviation));
        measurement.put(MIN_VALUE, new MutableIntegerArray(min));
        measurement.put(MAX_VALUE, new MutableIntegerArray(max));
        measurement.put(NSAMPLES, new Integer(count));
        
        return measurement;
    }
    
    
}
