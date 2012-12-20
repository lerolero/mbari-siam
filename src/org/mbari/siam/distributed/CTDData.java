/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

import java.io.Serializable;

/**
 * Represents data from one CTD sample
 */

/** CTDData */
public class CTDData implements Temperature, Serializable
{
    public double	_conductivity, _temperature, _pressure, _oxygen;
    public boolean	_hasPressure, _hasOxygen;

    /** No-argument constructor, initializes values to zero */
    public CTDData()
    {
	_conductivity = _temperature = _pressure = _oxygen = 0.0;
	_hasPressure = _hasOxygen = false;
    }

    /** Constructor for conductivity and temperature only. */
    public CTDData(double conductivity, double temperature)
    {
	_conductivity = conductivity;
	_temperature = temperature;
	_hasPressure = false;
	_hasOxygen = false;
    }

    public CTDData(double conductivity, double temperature, double pressure)
    {
	this(conductivity, temperature);
	_pressure = pressure;
	_hasPressure = true;
    }
    
    public CTDData(double conductivity, double temperature, double pressure, double oxygen)
    {
	this(conductivity, temperature, pressure);
	_oxygen = oxygen;
	_hasOxygen = true;
    }
		   
    /** Return conductivity */
    public double getConductivity()
    {
        return(_conductivity);
    }

    /** Return temperature */
    public double getTemperature()
    {
        return(_temperature);
    }

    /** Return pressure */
    public double getPressure() throws NoDataException
    {
	if (!_hasPressure)
	    throw new NoDataException();
        return(_pressure);
    }

    /** Return oxygen	*/
    public double getOxygen() throws NoDataException
    {
	if (!_hasOxygen)
	    throw new NoDataException();
        return(_oxygen);
    }

    /** Return String representation */
    public String toString()
    {
	StringBuffer sb = new StringBuffer("Cond=");
	sb.append(_conductivity);
	sb.append(", Temp=");
	sb.append(_temperature);
	if (_hasPressure)
	{
	    sb.append(", Pressure=");
	    sb.append(_pressure);
	}
	if (_hasOxygen)
	{
	    sb.append(", Oxygen=");
	    sb.append(_oxygen);
	}
	return(sb.toString());
    }

}