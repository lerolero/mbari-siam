/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed.jddac;

import java.io.Serializable;

/** Wrap a primitive integer into an Object, provide methods to modify 
 integer's value. */
public class MutableInteger extends Number implements Serializable {

    public int _value = 0;

    public MutableInteger(int value) {
	_value = value;
    }

    /** Set integer's value. */
    public void set(int value) {
	_value = value;
    }

    /** Return value as a byte. */
    public byte byteValue() {
	return (byte )_value;
    }

    /** Return value as a double. */
    public double doubleValue() {
	return (double )_value;
    }

    /** Return value as a float. */
    public float floatValue() {
	return (float )_value;
    }

    /** Return value as an int. */
    public int intValue() {
	return (int )_value;
    }

    /** Return value as a long. */
    public long longValue() {
	return (long )_value;
    }

    /** Return value as a short. */
    public short shortValue() {
        
	return (short )_value;
    }

    public String toString() {
	return Integer.toString(_value);
    }
}

