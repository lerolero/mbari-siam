/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/*
 * MutableDoubleArray.java
 *
 * Created on April 6, 2006, 2:54 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.mbari.siam.distributed.jddac;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * @author brian
 */
public class MutableFloatArray implements Serializable {

    private static final NumberFormat numberFormat = new DecimalFormat("#.##");
    
    private NumberFormat customFormat;

    private float[] values;

    /**
     * Creates a new instance of MutableDoubleArray
     */
    public MutableFloatArray() {
    }


    public MutableFloatArray(float[] values) {
        this.values = new float[values.length];
        System.arraycopy(values, 0, this.values, 0, values.length);
    }


    /**
     * Set value of specified element.
     */
    public void set(int index, float value) {
        this.values[index] = value;
    }


    /**
     * Get value of specified element.
     */
    public double get(int index) {
        return this.values[index];
    }

    public int size() {
        return this.values.length;
    }

    public float[] getValues() {
        return this.values;
    }


    /**
     * Return String representation.
     */
    public String toString() {

        StringBuffer buf = new StringBuffer("");
        for (int i = 0; i < this.values.length; i++) {
            if (numberFormat == null) {
                buf.append(Float.toString(this.values[i]));
            }
            else {
                buf.append(numberFormat.format(values[i]));
            }
            buf.append(" ");

        }

        return buf.toString();
    }

//    public NumberFormat getNumberFormat() {
//        if (cu)
//        return numberFormat;
//    }
//
//    public void setNumberFormat(NumberFormat format) {
//        this.customFormat = format;
//    }
}
