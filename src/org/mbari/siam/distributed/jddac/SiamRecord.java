/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed.jddac;

import net.java.jddac.common.meas.MeasAttr;
import net.java.jddac.common.meas.collection.Record;
import net.java.jddac.common.type.TimeRepresentation;

import java.io.Serializable;

/**
   SiamRecord extends JDDAC Record. SiamRecord is instantiated with a time-tag.
 */
public class SiamRecord 
    extends Record 
    implements Serializable {

    /**
     * This key is used to locate a SiamRecord when it's stored in an ArgArray
     */
    public static final String KEY = "SiamRecord";

    /** Create record. */
    public SiamRecord() {
	this(0);
    }

    /** Create record, specify time stamp. */
    public SiamRecord(long timeStamp) {
	super();
	put(MeasAttr.TIMESTAMP, new TimeRepresentation(timeStamp));

    }


    /** Set record's time-stamp. */
    public void setTimeStamp(long timeStamp) {
	put(MeasAttr.TIMESTAMP, new TimeRepresentation(timeStamp));
    }

    /** Get record's time-stamp. */
    public long getTimeStamp() {
        return ((TimeRepresentation) get(MeasAttr.TIMESTAMP)).getTime();
    }
}
