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
 *
 * @author brian
 */
public class MutableDoubleArray implements Serializable {
    private static final NumberFormat numberFormat = new DecimalFormat("#.##");

    private double[] values;

    /** Creates a new instance of MutableDoubleArray */
    public MutableDoubleArray() {}

    /**
     * Constructs ...
     *
     *
     * @param values
     */
    public MutableDoubleArray(double[] values) {
        this.values = new double[values.length];
        System.arraycopy(values, 0, this.values, 0, values.length);
    }

    /**
     * Method description
     *
     *
     * @return size of array
     */
    public int size() {
        return this.values.length;
    }

    /**
     * Return String representation.
     *
     * @return string representation
     */
    public String toString() {
        StringBuffer buf = new StringBuffer("");

        for (int i = 0; i < this.values.length; i++) {
            if (numberFormat == null) {
                buf.append(Double.toString(this.values[i]));
            }
            else {
                buf.append(numberFormat.format(values[i]));
            }

            buf.append(" ");
        }

        return buf.toString();
    }

    //~--- get methods --------------------------------------------------------

    /**
     * Get value of specified element.
     *
     * @param index
     *
     * @return specified value
     */
    public double get(int index) {
        return this.values[index];
    }

    /**
     * Method description
     *
     *
     * @return  values member variable
     */
    public double[] getValues() {
        return this.values;
    }

    //~--- set methods --------------------------------------------------------

    /**
     * Set value of specified element.
     *
     * @param index
     * @param value
     */
    public void set(int index, double value) {
        this.values[index] = value;
    }

//  public NumberFormat getNumberFormat() {
//      return numberFormat;
//  }
//
//  public void setNumberFormat(NumberFormat format) {
//      this.numberFormat = format;
//  }
}
