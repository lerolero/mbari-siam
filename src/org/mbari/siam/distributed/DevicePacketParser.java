/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

import org.mbari.siam.distributed.jddac.SiamRecord;
import org.mbari.siam.distributed.jddac.SiamMeasurement;
import org.mbari.siam.distributed.jddac.MutableInteger;
import org.mbari.siam.distributed.jddac.MutableIntegerArray;
import org.mbari.siam.distributed.jddac.MutableFloatArray;
import org.mbari.siam.distributed.jddac.MutableDoubleArray;
import net.java.jddac.common.meas.MeasAttr;
import net.java.jddac.common.type.ArgArray;
import net.java.jddac.common.type.TypeAttr;
import org.apache.log4j.Logger;

/**
 * A DevicePacketParser generates JDDAC Records from SIAM DevicePackets.
 */
abstract public class DevicePacketParser {

    /** log4j logger */
    static Logger _log4j = Logger.getLogger(DevicePacketParser.class);

    protected SiamRecord _record = null;

    /**
     * Array to hold measurment attributes/values
     */
    private Object[][] _measAttr = new String[4][2];

    /**
     * Parse DevicePacket, return Record containing Measurements.
     */
    public final SiamRecord parse(DevicePacket packet)
            throws NotSupportedException, Exception {

        _record = new SiamRecord(packet.systemTime());

        // Call subclass-implemented parse method
        parseFields(packet);

        return _record;
    }

    /**
     * Parse raw data from DevicePacket, fill in _record appropriately.
     */
    abstract protected void parseFields(DevicePacket packet)
            throws NotSupportedException, Exception;


    /**
     * Add an integer-valued Measurement to this object's Record,
     * and specify the initial integer value.
     */
    protected void addMeasurement(String name,
                                  String description,
                                  String units,
                                  int value) {
        setAttributes(name, description, units, TypeAttr.INTEGER32);

        SiamMeasurement measurement =
                new SiamMeasurement(name, new ArgArray(_measAttr));

        // Set the value
        MutableInteger valueObject = new MutableInteger(value);

        measurement.put(MeasAttr.VALUE, valueObject);

        // Add measurement to the record
        _record.put(name, measurement);
    }

    /**
     * Add a {@link Number}-valued Measurement to this object's Record
     * 
     * @param name The name of the measurement
     * @param description Descriptive info about the measurement
     * @param units The measurment units
     * @param value The value of the measurement
     */
    protected void addMeasurement(String name, String description, String units, Number value) {
        


        // Figure out the number type
        String type = null;
        if (value instanceof Byte) {
            type = TypeAttr.INTEGER8;
        }
        else if (value instanceof Short) {
            type = TypeAttr.INTEGER16;
        }
        else if (value instanceof Integer) {
            type = TypeAttr.INTEGER32;
        }
        else if (value instanceof Long) {
            type = TypeAttr.INTEGER64;
        }
        else if (value instanceof Float) {
            type = TypeAttr.FLOAT32;
        }
        else if (value instanceof Double) {
            type = TypeAttr.FLOAT64;
        }
        
        setAttributes(name, description, units, type);

        SiamMeasurement measurement = 
	    new SiamMeasurement(name, new ArgArray(_measAttr));

        measurement.put(MeasAttr.VALUE, value);

        _record.put(name, measurement);
    }



    /**
     * Set integer value of specified measurement. Throws exception if
     * specified measurement does not have integer value.
     */
    public void setMeasurement(String name, int value)
            throws Exception {
        SiamMeasurement measurement = (SiamMeasurement) _record.get(name);

        if (measurement == null) {
            throw new Exception("Measurement named \"" + name +
                    "\" not found in record");
        }

        Object object = null;

        if ((object = measurement.get(MeasAttr.VALUE)) == null) {
            throw new Exception("Attribute " + MeasAttr.VALUE +
                    " not found for " + name);
        }

        if (object instanceof MutableInteger) {
            MutableInteger mutableInteger =
                    (MutableInteger) object;

            mutableInteger.set(value);

            // Set the value
            measurement.put(MeasAttr.VALUE, mutableInteger);
        }
        else {
            _log4j.error("Oops - value is of type " +
                    object.getClass().getName());

            throw new Exception(name + " does not contain an integer");
        }
    }


    /**
     * Add a Measurement which contains an array of integer values to
     * this DevicePacketParser's Record.
     */
    protected void addArrayMeasurement(String name,
                                       String description,
                                       String units,
                                       int[] values) {

        setAttributes(name, description, units, TypeAttr.INTEGER32);

        SiamMeasurement measurement =
                new SiamMeasurement(name, new ArgArray(_measAttr));

        // Set the values
        measurement.put(MeasAttr.VALUE, new MutableIntegerArray(values));

        // Add measurement to the record
        _record.put(name, measurement);
    }

    /**
     * Add a Measurement which contains an array of float values to
     * this DevicePacketParser's Record.
     */
    protected void addArrayMeasurement(String name,
                                       String description,
                                       String units,
                                       float[] values) {

        setAttributes(name, description, units, TypeAttr.FLOAT32);

        SiamMeasurement measurement =
                new SiamMeasurement(name, new ArgArray(_measAttr));

        // Set the values
        measurement.put(MeasAttr.VALUE, new MutableFloatArray(values));

        // Add measurement to the record
        _record.put(name, measurement);
    }

    /**
     * Add a Measurement which contains an array of float values to
     * this DevicePacketParser's Record.
     */
    protected void addArrayMeasurement(String name,
                                       String description,
                                       String units,
                                       double[] values) {

        setAttributes(name, description, units, TypeAttr.FLOAT64);

        SiamMeasurement measurement =
                new SiamMeasurement(name, new ArgArray(_measAttr));

        // Set the values
        measurement.put(MeasAttr.VALUE, new MutableDoubleArray(values));

        // Add measurement to the record
        _record.put(name, measurement);
    }


    /**
     * Set integer array values of specified measurement. Throws exception if
     * specified measurement is not an integer array.
     */
    public void setArrayMeasurement(String name, int index, int value)
            throws Exception {
        SiamMeasurement measurement = (SiamMeasurement) _record.get(name);

        if (measurement == null) {
            throw new Exception("Measurement named \"" + name +
                    "\" not found in record");
        }

        Object object = null;

        if ((object = measurement.get(MeasAttr.VALUE)) == null) {
            throw new Exception("Attribute " + MeasAttr.VALUE +
                    " not found for " + name);
        }

        if (object instanceof MutableIntegerArray) {

            MutableIntegerArray array = (MutableIntegerArray) object;

            // Set the array element's value
            array.set(index, value);
            measurement.put(MeasAttr.VALUE, array);
        }
        else {
            _log4j.error("Oops - value is of type " +
                    object.getClass().getName());

            throw new Exception(name + " does not contain an integer array");
        }
    }


    /**
     * Set the timestamp on the record.
     */
    protected void setRecordTimestamp(long timestamp) {
    }

    /**
     * Set basic measurement attribute strings.
     */
    protected final void setAttributes(String name, String description,
                                       String units, String dataType) {

        _measAttr[0][0] = MeasAttr.NAME;
        _measAttr[0][1] = name;
        _measAttr[1][0] = MeasAttr.DESCRIPTION;
        _measAttr[1][1] = description;
        _measAttr[2][0] = MeasAttr.UNITS;
        _measAttr[2][1] = units;
        _measAttr[3][0] = MeasAttr.DATA_TYPE;
        _measAttr[3][1] = dataType;
    }
}

