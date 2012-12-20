/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.jddac;

import java.util.Enumeration;
import net.java.jddac.common.meas.MeasAttr;
import net.java.jddac.common.meas.Measurement;
import net.java.jddac.common.meas.collection.Record;
import net.java.jddac.common.type.ArgArray;
import org.apache.log4j.Logger;

/**
 * Takes a ArgArray of Records and coallates by Measurement Name.
 */
public class CoallateByMeasurementNameFunction implements IFunction {
    
    private static final Logger log = Logger.getLogger(CoallateByMeasurementNameFunction.class);
    
    public CoallateByMeasurementNameFunction() {
    }
    
    /**
     * The
     * @param argArray <p>a ArgArray of JDDAC Records containing Measurements. An
     * values that are not <coe>Measurements</code> are ignored.
     * <pre>
     *   ArgArray {
     *      "RecordKey-1": Record {
     *      "RecordKey-2": Record
     *   }
     * </pre>
     *</p>
     *
     * @return <p>An ArgArray with the measurements coallated by MeasurementName. e.g.
     * <pre>
     *   ArgArray {
     *      "MeasurementName-1" : ArgArray {
     *         "RecordKey-1" : Measurement
     *         "RecordKey-2" : Measurement
     *      }
     *      "MeasurementName-2" : ArgArray {
     *         "RecordKey-1" : Measurement
     *         "RecordKey-2" : Measurement
     *      }
     *   }
     * </pre>
     * Where:
     *   MeasurementName = the common name that a bunch of Measurements
     * share. This name is used to coallate the Measurements
     *   RecordKey = The Key used to store the Record the Measurement was contained
     * in.</p>
     */
    public ArgArray execute(ArgArray argArray) {
        
        ArgArray coallatedMeasurements = new ArgArray();
        
        Enumeration keys = argArray.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            
            if (log.isDebugEnabled()) {
                log.debug("Coallating Measurements from Record '" + key + "'");
            }
            
            Object objRecord = argArray.get(key);
            if (objRecord instanceof Record) {
                Record record = (Record) objRecord;
                
                Enumeration names = record.keys();
                while (names.hasMoreElements()) {
                    String name = (String) names.nextElement();
                    Object objMeasurement = record.get(name);
                    if (objMeasurement instanceof Measurement) {
                        addMeasurement(coallatedMeasurements, name, objMeasurement, key);
                    }
                    
                }
            } else if (objRecord instanceof Measurement) {
                Measurement measurement = (Measurement) objRecord;
                addMeasurement(coallatedMeasurements, key, measurement, key);
            }
        }
        
        return coallatedMeasurements;
    }

    private String addMeasurement(final ArgArray coallatedMeasurements, String name, final Object objMeasurement, final String key) {
        Measurement measurement = (Measurement) objMeasurement;
        
        /*
         * If a Measurement does not contain a NAME then use the key
         * that was used to store this measurement in the record
         */
        String measName = (String) measurement.get(MeasAttr.NAME);
        name = (measName == null) ? name : measName;
        
        ArgArray value = (ArgArray) coallatedMeasurements.get(name);
        if (value == null) {
            value = new ArgArray();
            coallatedMeasurements.put(name, value);
        }
        // Store the Measurement using the name that the record was stored with
        value.put(key, measurement);
        return name;
    }
    
}