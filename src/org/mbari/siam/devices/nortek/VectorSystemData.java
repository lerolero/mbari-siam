/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.nortek;

import org.apache.log4j.Logger;
import org.mbari.util.NumberUtil;

import java.io.Serializable;

/**
 * Parser for Nortek Vector System Data message, ID = 0x11
 */

/** Vector System data */
public class VectorSystemData extends DataStructure implements Serializable {
    
    private static final Logger log = Logger.getLogger(VectorSystemData.class);
    private static final long serialVersionUID=1L;

    public VectorSystemData() {
    }

    /** Voltage (0.1 volt) */
    public short voltage() {
        return getShort(10);
    }

    /** Sound speed (0.1 m/sec) */
    public short soundSpeed() {
        return getShort(12);
    }

    /** Heading (0.1 deg) */
    public short heading() {
        return getShort(14);
    }

    /** Pitch (0.1 deg) */
    public short pitch() {
        return getShort(16);
    }

    /** Roll (0.1 deg) */
    public short roll() {
        return getShort(18);
    }

    /** Temperature (0.01 deg) */
    public short temperature() {
        return getShort(20);
    }

    /** Status		*/
    /* Bit 7&6: Power level (00 high, 01, 10, 11 low) */
    /* Bit 5&4: Wakeup state (00 bad power, 01 power applied, 10 break, 11 RTC alarm)*/
    /* Bit 3:   Roll (0 OK, 1 out of range)     */
    /* Bit 2:   Pitch (0 OK, 1 out of range)	*/
    /* Bit 1:   Scaling (0 = 1 mm/s, 1 = 0.1 mm/s) */
    /* Bit 0:   Orientation (0 = up, 1 = down)	*/
    public short status() {
        return getByte(23);
    }

    /* Velocity multiplier, cm/s		*/
    public double velocityMult() {
	if ((status() & 0x2) != 0)
	    return(0.01);
	return(0.1);
    }

    /** Error		*/
    /* Bit 7-5: Unused				*/
    /* Bit 4:   Flash (0=OK, 1=error)		*/
    /* Bit 3:   Tag bit (0=OK, 1=error)		*/
    /* Bit 2:   Sensor data (0=OK, 1=error)	*/
    /* Bit 1:   Measurement data (0=OK, 1=error)*/
    /* Bit 0:   Compass (0=OK, 1=error)		*/
    public short error() {
        return getByte(22);
    }

    /** Return String representation */
    public String toString() {
        StringBuffer output = new StringBuffer(super.toString());
        output.append("\nstatus=").append(status());
        output.append("; error=").append(error());
        output.append("\nvoltage=").append(voltage());
        output.append("; soundSpeed=").append(soundSpeed());
        output.append("; temperature=").append(temperature());
	output.append("\nheading=").append(heading());
	output.append("; pitch=").append(pitch());
	output.append("; roll=").append(roll());
        output.append("\n");

        return output.toString();
    }

}
