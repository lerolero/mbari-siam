/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed.jddac;

import java.io.Serializable;

/** This class holds an array of primitive int values; these values can
 be modified. */
public class MutableIntegerArray implements Serializable {

    private int[] _values;

    public MutableIntegerArray(int[] values) {
	_values = new int[values.length];
        System.arraycopy(values, 0, this._values, 0, values.length);
    }


    /** Set value of specified element. */
    public void set(int index, int value) {
	_values[index] = value;
    }


    /** Get value of specified element. */
    public int get(int index) {
	return _values[index];
    }
    
    public int size() {
        return _values.length;
    }
    
    public int[] getValues() {
        return _values;
    }


    /** Return String representation. */
    public String toString() {

	StringBuffer buf = new StringBuffer("");
	for (int i = 0; i < _values.length; i++) {
	    buf.append(Integer.toString(_values[i]));
	    buf.append(" ");
	}

	return buf.toString();
    }

}
