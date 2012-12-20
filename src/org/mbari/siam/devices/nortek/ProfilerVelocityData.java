/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.nortek;

import org.mbari.util.NumberUtil;

import java.io.Serializable;


/**
 * Profiler velocity data
 */
public class ProfilerVelocityData extends DataStructure implements Serializable {

    private int cellCount = 0;

    private int beamCount = 0;

    /**
     * Offset in Bytes (0-base indexing) of the start of the velocity measurements.
     * This value is fixed
     */
    public static final int START_VELOCITY_INDEX = 30;

    /**
     * Offset in Bytes (0-base indexing) of the start of the amplitude measurements.
     * This value changes with the number of beams and cells
     */
    private int startAmplitudeIndex;

    public ProfilerVelocityData() {
    }

    public int getCellCount() {
        return cellCount;
    }

    public void setCellCount(int cellCount) {
        this.cellCount = cellCount;
        startAmplitudeIndex = START_VELOCITY_INDEX + 2 * cellCount * getBeamCount();
    }

    public int getBeamCount() {
        return beamCount;
    }

    public void setBeamCount(int beamCount) {
        this.beamCount = beamCount;
    }

    public short size() {
        return NumberUtil.toShort(new byte[]{_dataBytes[2], _dataBytes[3]}, true);
    }

    /**
     * Voltage (0.1 volt)
     */
    public int voltage() {
        return NumberUtil.toUnsignedInt(_dataBytes[14], _dataBytes[15]);
    }

    /**
     * Sound speed (0.1 m/sec)
     */
    public int soundSpeed() {
        return NumberUtil.toUnsignedInt(_dataBytes[16], _dataBytes[17]);
    }

    /**
     * Heading (0.1 deg)
     */
    public short heading() {
        return NumberUtil.toShort(new byte[]{_dataBytes[18], _dataBytes[19]}, true);
    }

    /**
     * Pitch (0.1 deg)
     */
    public short pitch() {
        return NumberUtil.toShort(new byte[]{_dataBytes[20], _dataBytes[21]}, true);
    }

    /**
     * Roll (0.1 deg)
     */
    public short roll() {
        return NumberUtil.toShort(new byte[]{_dataBytes[22], _dataBytes[23]}, true);
    }

    /**
     * Temperature (0.01 deg)
     */
    public short temperature() {
        return NumberUtil.toShort(new byte[]{_dataBytes[28], _dataBytes[29]}, true);
    }

    /**
     * Set the number of cells and beams.
     */
    public void setParameters(int nCells, int nBeams) {
        cellCount = nCells;
        beamCount = nBeams;
    }


    /**
     * Velocity (mm/sec).
     */
    public short velocity(int beam, int cell) {
        int idx = START_VELOCITY_INDEX + 2 * beam * cellCount + 2 * cell;
        return NumberUtil.toShort(new byte[]{_dataBytes[idx], _dataBytes[idx + 1]}, true);
    }


    /**
     * Amplitude (counts)
     */
    public short amplitude(int beam, int cell) {
        return _dataBytes[startAmplitudeIndex + beam * cellCount + cell];
    }


    /**
     * Return String representation
     */
    public String toString() {
        StringBuffer output = new StringBuffer(super.toString() + "\n");
        output.append("voltage=" + voltage());
        output.append("; soundSpeed=" + soundSpeed());
        output.append("; temperature=" + temperature());
        output.append("\n\n");

        if (cellCount <= 0) {
            output.append("Unknown number of cells\n");
        }
        else {
            for (int beam = 0; beam < beamCount; beam++) {
                for (int cell = 0; cell < cellCount; cell++) {
                    output.append("beam=" + beam + ", cell=" + cell +
                            "; velocity=" + velocity(beam, cell) +
                            "; amplitude=" + amplitude(beam, cell) +
                            "\n");
                }
            }
        }

        return new String(output);
    }
}