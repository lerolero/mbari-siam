/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

import java.io.Serializable;

/**
 * Temperature is an interface for any data type that can return a temperature
 */

/** Temperature */
public interface Temperature
{
    /** Return temperature in degrees Celsius */
    public double getTemperature();
}