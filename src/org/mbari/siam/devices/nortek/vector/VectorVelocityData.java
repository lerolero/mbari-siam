/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.nortek.vector;

import org.apache.log4j.Logger;
import org.mbari.util.NumberUtil;

import java.io.Serializable;

/**
 * Parser for Nortek Vector Velocity message, ID = 0x10
 */

/** Vector Velocity data */
public class VectorVelocityData extends DataStructure implements Serializable {
    
    private static final Logger log = Logger.getLogger(VectorVelocityData.class);
    private static final long serialVersionUID=1L;


    public VectorVelocityData() {
    }

    /** Sequential ensemble number */
    public short ensemble() {
        return getByte(3);
    }

    /** Pressure (mm) */
    public int pressure() {
	return(((int)getShort(6) & 0xffff) | ((int)getByte(4) << 16));
    }

    /** Velocity X or East, 0.1 or 0.01 cm/s, see VectorSystemData.velocityMult()  */
    public short velocityX() {
        return getShort(10);
    }

    /** Velocity Y or North, 0.1 or 0.01 cm/s, see VectorSystemData.velocityMult()  */
    public short velocityY() {
        return getShort(12);
    }

    /** Velocity Z or Up, 0.1 or 0.01 cm/s, see VectorSystemData.velocityMult()  */
    public short velocityZ() {
        return getShort(14);
    }

    /** Return String representation */
    public String toString() {
        StringBuffer output = new StringBuffer(super.toString());
        output.append("\nensemble=").append(ensemble());
        output.append("; velocityX=").append(velocityX());
        output.append("; velocityY=").append(velocityY());
        output.append("; velocityZ=").append(velocityZ());
        output.append("\n");

        return output.toString();
    }

}
