/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed.jddac;

import java.io.Serializable;
import net.java.jddac.common.type.ArgArray;
import net.java.jddac.common.meas.Measurement;
import net.java.jddac.common.meas.MeasAttr;

/** 
    SiamMeasurement extends JDDAC Measurement. SiamMeasurement is 
    instanitated with a name. */
public class SiamMeasurement 
    extends Measurement 
    implements Serializable {


    // Every SIAM measurment must be created with a name.
    public SiamMeasurement(String name) {
        put(MeasAttr.NAME, name);
    }

    /** Throws exception if MeasAttr.Name is not specified in 
	input ArgArray. */
    public SiamMeasurement(String name, ArgArray argArray) {
	super(argArray);
        put(MeasAttr.NAME, name);
    }

    /** Throws exception if MeasAttr.Name is not specified in 
	input ArgArray. */
    public SiamMeasurement(String name, SiamMeasurement measurement) {
        super(measurement);
        put(MeasAttr.NAME, name);
    }


    /** Return measurement value; return null if no value yet. */
    Object getValue() {
	return get(MeasAttr.VALUE);
    }

    /** Get measurement name */
    public String getName() {
	return (String) get(MeasAttr.NAME);
    }


    /** Return measurment units; return "" if no units specified. */
    public String getUnits() {
	return (String )get(MeasAttr.UNITS);
    }
}
