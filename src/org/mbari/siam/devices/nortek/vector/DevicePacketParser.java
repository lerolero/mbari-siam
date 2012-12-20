/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.nortek.vector;

import org.mbari.siam.utils.PacketUtilities;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.MetadataPacket;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.SensorDataPacket;

import java.io.Serializable;
import java.text.ParseException;



/**
 *
 * Parse raw data into JDDAC Records.
 *
 * Usage:
 * <pre>
 *  // We have an InstrumentConfiguration object already instantiated. We need this
 *  // confguration object in order to correctly parse data packets. The configuratio
 *  // object tells us the number of beams and cells that each data packet represents.
 *  DevicePacketParser parser = new DevicePacketParser()
 *  parser.setInstrumentConfiguration(instrumentConfiguration);
 *  ArgArray siamRecord = parser.parse(sensorDataPacket);  // Actually an SiamRecord which is a subclass of ArgArray
 * </pre>
 *
 * */
public class DevicePacketParser extends org.mbari.siam.distributed.DevicePacketParser
        implements Serializable {
    
    static final String VELOCITY = "velocity";
    static final String AMPLITUDE = "amplitude";
    static final String ENSEMBLE = "ensemble";
    
    static final String AVG_INTERVAL = "avgInterval";
    static final String N_CELLS = "nCells";
    static final String N_BEAMS = "nBeams";
    static final String VOLTAGE = "voltage";
    static final String SOUND_SPEED = "soundSpeed";
    static final String HEADING = "heading";
    static final String PITCH = "pitch";
    static final String ROLL = "roll";
    static final String TEMPERATURE = "temperature";
    static final String SYSTEM_TIME = "systemtime";
    
    private static final Logger log = Logger.getLogger(DevicePacketParser.class);
    private InstrumentConfiguration instrumentConfiguration;
    
    public DevicePacketParser() {
    }

    public DevicePacketParser(InstrumentConfiguration instrumentConfiguration) {
        this.instrumentConfiguration = instrumentConfiguration;
    }
    
    /** 
     * Convert a <code>DataStructure</code> to a JDDAC Record containing 
     * JDDAC Measurements
     */
    void convertDataStructure(DataStructure data) throws Exception {
        if (data instanceof InstrumentConfiguration) {
            log.debug("Parsing instrument configuration");
            convertInstrumentConfiguration((InstrumentConfiguration ) data);
        } 
        else if (data instanceof ProfilerVelocityData) {
            log.debug("Parsing profiler velocity");
            convertProfilerVelocityData((ProfilerVelocityData) data);
        }
        else if (data instanceof  HRProfilerData) {
            log.debug("Parsing HR profiler");
            convertHRProfilerData((HRProfilerData) data);
        }
        else if (data instanceof  VectorVelocityData) {
            log.debug("Parsing VectorVelocity");
            convertVectorVelocityData((VectorVelocityData) data);
        }
        else if (data instanceof  VectorSystemData) {
            log.debug("Parsing VectorSystem");
            convertVectorSystemData((VectorSystemData) data);
        }
    }
    
    /** 
     * Convert a <code>InstrumentConfiguration</code> to a JDDAC Record containing 
     * JDDAC Measurements
     */
    private void convertInstrumentConfiguration(InstrumentConfiguration config) {

        addMeasurement(AVG_INTERVAL, "averaging interval", "sec",
                config.avgInterval());
        
        addMeasurement(N_CELLS, "# of cells", "cells",
                config.nCells());

        addMeasurement(N_BEAMS, "# of beams", "beams",
                config.nBeams());
    }

    /** 
     * Convert a <code>ProfilerVelocityData</code> to a JDDAC Record containing 
     * JDDAC Measurements
     */
    private void convertProfilerVelocityData(ProfilerVelocityData velocityData) throws IllegalStateException {

        if (instrumentConfiguration == null) {
            throw new IllegalStateException("An InstrumentConfiguration must be supplied in order to parse ProfilerVelocityData");
        }
        
        addMeasurement(VOLTAGE, "voltage", "0.1 volt",
                velocityData.voltage());
        
        addMeasurement(SOUND_SPEED, "sound speed", "0.1 m/sec",
                velocityData.soundSpeed());
        
        
        addMeasurement(HEADING, "heading", "0.1 deg",
                velocityData.heading());
        
        addMeasurement(PITCH, "pitch", "0.1 deg",
                velocityData.pitch());
        
        addMeasurement(ROLL, "roll", "0.1 deg",
                velocityData.roll());
        
        addMeasurement(TEMPERATURE, "temperature", "0.01 degC",
                velocityData.temperature());
        

        int nBeams = instrumentConfiguration.nBeams();
        int nCells = instrumentConfiguration.nCells();
        velocityData.setBeamCount(nBeams);
        velocityData.setCellCount(nCells);
        for (int beam = 0; beam < nBeams; beam++) {

            int[] velocity = new int[nCells];
            int[] amplitude = new int[nCells];
            
            for (int cell = 0; cell < nCells; cell++) {
                velocity[cell] = velocityData.velocity(beam, cell);
                amplitude[cell] = velocityData.amplitude(beam, cell);
            }
            
            addArrayMeasurement(VELOCITY + "-" + beam,
                    "water velocities, beam " + beam,
                    "0.001 m/sec",
                    velocity);
            
            
            addArrayMeasurement(AMPLITUDE + "-" + beam,
                    "amplitudes, beam " + beam,
                    "counts",
                    amplitude);
            
        }
    }
    
    /** 
     * Convert a <code>HRProfilerData</code> to a JDDAC Record containing 
     * JDDAC Measurements
     */
    private void convertHRProfilerData(HRProfilerData profilerData) throws IllegalStateException {

        if (instrumentConfiguration == null) {
            throw new IllegalStateException("An InstrumentConfiguration must be supplied in order to parse ProfilerVelocityData");
        }


        addMeasurement(VOLTAGE, "voltage", "0.1 volt",
                profilerData.voltage());
        
        addMeasurement(SOUND_SPEED, "sound speed", "0.1 m/sec",
                profilerData.soundSpeed());
        
        
        addMeasurement(HEADING, "heading", "0.1 deg",
                profilerData.heading());
        
        addMeasurement(PITCH, "pitch", "0.1 deg",
                profilerData.pitch());
        
        addMeasurement(ROLL, "roll", "0.1 deg",
                profilerData.roll());
        
        addMeasurement(TEMPERATURE, "temperature", "0.01 degC",
                profilerData.temperature());
        
        int nBeams = instrumentConfiguration.nBeams();
        int nCells = instrumentConfiguration.nCells();
        profilerData._nBeams = nBeams;
        profilerData._nCells = nCells;
        
        for (int beam = 0; beam < nBeams; beam++) {
            
            final int[] velocity = new int[nCells];
            final int[] amplitude = new int[nCells];
        
            for (int cell = 0; cell < nCells; cell++) {
                velocity[cell] = profilerData.velocity(beam, cell);
                amplitude[cell] = profilerData.amplitude(beam, cell);
            }
            
            addArrayMeasurement(VELOCITY + "-" + beam,
                    "water velocities, beam " + beam,
                    "0.001 m/sec",
                    velocity);
            
            
            addArrayMeasurement(AMPLITUDE + "-" + beam,
                    "amplitudes, beam " + beam,
                    "counts",
                    amplitude);
            
        }
    }
    
    
    /** 
     * Convert a <code>VectorVelocityData</code> to a JDDAC Record containing 
     * JDDAC Measurements
     */
    private void convertVectorVelocityData(VectorVelocityData velocityData)
	throws IllegalStateException {

        addMeasurement(ENSEMBLE, "ensemble", "count",
                velocityData.ensemble());
        
        addMeasurement(VELOCITY + "-X", "velocity, X or East", "0.001 m/sec",
                velocityData.velocityX());
        
        
        addMeasurement(VELOCITY + "-Y", "velocity, Y or North", "0.001 m/sec",
                velocityData.velocityY());
        
        
        addMeasurement(VELOCITY + "-Z", "velocity, Z or UP", "0.001 m/sec",
                velocityData.velocityZ());
        
    }
    
    
    /** 
     * Convert a <code>VectorSystemData</code> to a JDDAC Record containing 
     * JDDAC Measurements
     */
    private void convertVectorSystemData(VectorSystemData systemData)
	throws IllegalStateException {

        addMeasurement(VOLTAGE, "Battery voltage", "0.1 volt",
		systemData.voltage());
        
        addMeasurement(SOUND_SPEED, "sound speed", "0.1 m/sec",
                systemData.soundSpeed());
        
        addMeasurement(HEADING, "heading", "0.1 deg",
                systemData.heading());
        
        addMeasurement(PITCH, "pitch", "0.1 deg",
                systemData.pitch());
        
        addMeasurement(ROLL, "roll", "0.1 deg",
                systemData.roll());
        
        addMeasurement(TEMPERATURE, "temperature", "0.01 degC",
                systemData.temperature());
        
    }
    
    
    /** Parse raw data from DevicePacket, fill in _record appropriately. */
    protected void parseFields(DevicePacket packet) throws Exception {
        
        // Clear the output record, since we don't yet know what type
        // of data the packet contains
        _record.clear();
        
        byte[] dataBytes = null;
        
        if (packet instanceof SensorDataPacket) {
            dataBytes = ((SensorDataPacket) packet).dataBuffer();
        } 
        else if (packet instanceof MetadataPacket) {
            // Try to get device metadata from payload
            try {
                dataBytes = PacketUtilities.getDeviceMetadata((MetadataPacket) packet);
            } catch (Exception e) {
                throw new ParseException("parseFields: device metadata not found", 0);
            }
        } 
        else {
            String err = "expecting SensorDataPacket or MetadataPacket";
            throw new NotSupportedException(err);
        }
        
        DataStructure data = null;
        
        int id = DataStructure.id(dataBytes);
        log.debug("parseFields() - id=0x" + Integer.toHexString(id));
        
        switch(id) {
            case Aquadopp.INSTRUMENT_CONFIGURATION:
		log.debug("Found instrument configuration");
                data = new InstrumentConfiguration();
                
                break;
            case Aquadopp.PROFILER_VELOCITY_DATA:
		log.debug("Found Profiler Velocity Data");
                data = new ProfilerVelocityData();
                
                break;
            case Aquadopp.HR_PROFILER_DATA:
		log.debug("Found HR Profiler Data");
                data = new HRProfilerData();
                
                break;
	    case Vector.VECTOR_VELOCITY_DATA:
		log.debug("Found Vector Velocity Data");
                data = new VectorVelocityData();
                
                break;
	    case Vector.VECTOR_SYSTEM_DATA:
		log.debug("Found Vector System Data");
                data = new VectorSystemData();
                
                break;
            default:
                
                throw new NotSupportedException("Nortek record type 0x" + Integer.toHexString(id)  + " not supported");
        }
        
        data.setBytes(dataBytes);
        
        convertDataStructure(data);
        addMeasurement(SYSTEM_TIME, "Sample time", "epic seconds", new Long(packet.systemTime() / 1000));

    }

    /**
     * Gets the InstrumentConfiguration object used for parsing Velocity data
     * @return The surrently set InstrumentConfiguration
     */
    public InstrumentConfiguration getInstrumentConfiguration() {
        return instrumentConfiguration;
    }

    /**
     * Set the InstrumentConfiguration object used for parsing Velocity data. This object is required
     * in order to correctly resolve the number of beams and cells in the data.
     * @param instrumentConfiguration The congiguration used for parsing the beam data.
     */
    public void setInstrumentConfiguration(InstrumentConfiguration instrumentConfiguration) {
        this.instrumentConfiguration = instrumentConfiguration;
    }
}
