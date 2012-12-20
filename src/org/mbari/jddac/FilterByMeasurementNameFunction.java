/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.jddac;

import java.util.Enumeration;
import java.util.Vector;

import net.java.jddac.common.meas.MeasAttr;
import net.java.jddac.common.meas.Measurement;
import net.java.jddac.common.meas.collection.Record;
import net.java.jddac.common.type.ArgArray;

public class FilterByMeasurementNameFunction implements IFunction {

    /**
     * Vector<String>
     */
    private final Vector allowedNames = new Vector();

    /**
     * Filters out any Measurements in the record that do not match the list
     * of supplied names. All non-measurement fields are passed on to.
     */
    public ArgArray execute(ArgArray argArray) {


        /*
         * If it's a record we filter for only the measurement names we want.
         * If it's not a record we just relay the argArray on.
         */
        if (argArray instanceof Record) {
            ArgArray record = new Record();
            Enumeration e = argArray.keys();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                Object value = argArray.get(key);

                /*
                 * Only allow specified Measurements
                 */
                if (value instanceof Measurement) {
                    Measurement m = (Measurement) value;
                    if (allowedNames.contains(m.get(MeasAttr.NAME))) {
                        record.put(key, value);
                    }
                }
                else {
                    record.put(key, value);
                }
            }
            argArray = record;
        }

        return argArray;
    }

    /**
     * A Vector that stores the strings of the allowed Measurement names (found by
     * looking for measurments in the supplied record, then looking to see if the
     * MeasAttr.NAME field of measurement is contained in this Vector of Strings
     * @return Vector of allowed names
     */
    public Vector getAllowedNames() {
        return allowedNames;
    }



}
