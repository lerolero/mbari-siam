/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.nortek.vector;

import java.io.Serializable;


/** Configuration data. */
public class InstrumentConfiguration extends DataStructure implements Serializable
{
    public InstrumentConfiguration() {
    }

    /** Averaging interval.		*/
    /*  Units are seconds for Aquadopp	*/
    /*  For Vector, returns 512/sampleRate*/
    public int avgInterval() {
        return getShort(16);
    }

    /** Sample frequency for Vector	*/
    /*  Equal to 512/avgInterval()	*/
    public int sampleFrequency() {
        return(512/avgInterval());
    }

    /** Coordinate System (0=ENU, 1=XYZ, 2=BEAM) */
    public String coordSystem() {
        switch(getShort(32))
	{
	  case 0:
	      return("ENU");

	  case 1:
	      return("XYZ");

	  case 2:
	      return("BEAM");

	  default:
	      return("Unknown");

	}
    }

    /** Number of cells (bins). */
    public int nCells() {
        return getShort(34);
    }

    /** Number of beams */
    public int nBeams() {
        return getShort(18);
    }

    /** Measurement interval (sec) */
    public int measurementInterval() {
        return getShort(38);
    }

    /** Return String representation */
    public String toString() {
        String output = super.toString() + "\n";
        output += "avgInterval=" + avgInterval() +
                "; nCells=" + nCells() + "; nBeams: " + nBeams() +
                "; measurementInterval: " + measurementInterval() +
	        "; coordinateSystem: " + coordSystem() +
	        "; sampleFrequency: " + sampleFrequency();

        return output;
    }
}
