// Copyright 2001 MBARI
package org.mbari.siam.distributed;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import net.java.jddac.common.meas.MeasAttr;
import net.java.jddac.common.type.ArgArray;
import net.java.jddac.common.type.TimeRepresentation;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.jddac.SiamMeasurement;
import org.mbari.siam.distributed.jddac.SiamRecord;

/**
 * MeasurementPacket contains a SiamRecord, which is composed of a series of 
 * SiamMeasurement objects. Note that SiamMeasurement and SiamRecord are
 * extensions of JDDAC Measurement and JDDAC Record, respectively.
 * NOTE: This is currently implemented as an extension of DeviceMessagePacket,
 * and contains a String representation of the SiamRecord, since there 
 * seems to be a problem in serializing the JDDAC stuff(?)
 * 
 * @author Tom O'Reilly
 */
public class MeasurementPacket 
    extends DeviceMessagePacket 
    implements Serializable {

    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(MeasurementPacket.class);

    /** Serial version ID */
    private static final long serialVersionUID = 0L;

    public MeasurementPacket(long sourceID, ArgArray argArray) {
	super(sourceID);
        
        /*
         * See if the ArgArray contains a TIMESTAMP
         */
        Object timestamp = argArray.get(MeasAttr.TIMESTAMP);
        long time = 0;
        if (timestamp instanceof TimeRepresentation) {
            time = ((TimeRepresentation) timestamp).getTime();
        }
        else {
            time = System.currentTimeMillis();
        }
	setMessage(time, argArray.toString().getBytes());
    }
}
