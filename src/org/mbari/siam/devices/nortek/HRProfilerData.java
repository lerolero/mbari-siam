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
 *
 *
 * <p>Here's the data structure from the SamplesHR.cpp provided by Nortek:
 * <pre>
//////////////////////////////////////////////////////////////////////////////
// HR Aquadopp profile data (32 + f(nBeams) bytes)
typedef struct {
   unsigned char  cSync;         // sync = 0xa5                     0
   unsigned char  cId;           // identification = 0x2a           1
   unsigned short hSize;         // size of structure (words)       2
   PdClock        clock;         // date and time                   4
   unsigned short hMilliseconds; // milliseconds                   10
   short          hError;        // error code                     12
   unsigned short hBattery;      // battery voltage (0.1 V)        14
   unsigned short hSoundSpeed;   // speed of sound (0.1 m/s)       16
   short          hHeading;      // compass heading (0.1 deg)      18
   short          hPitch;        // compass pitch (0.1 deg)        20
   short          hRoll;         // compass roll (0.1 deg)         22
   union {                                                         24
      struct {
         char     cFill;
         char     cStatus;       // status
      } Status;
      struct {
         unsigned char cMSB;     // pressure MSB
         char     cFill;
         unsigned short hLSW;    // pressure LSW
      } Pressure;                // (mm)
   } u1;
   short          hTemperature;  // temperature (0.01 deg C)       28
   unsigned short hAnaIn1;       // analog input 1                 30
   unsigned short hAnaIn2;       // analog input 2                 32
   short          hSpare[10];                                      34
   // actual size of the following = nBeams*nBins*3 + 2
   short          hVel[PD_MAX_BEAMS][PD_MAX_BINS]; // short hVel[nBeams][nCells];   // velocity 54
   unsigned char  cAmp[PD_MAX_BEAMS][PD_MAX_BINS]; // char  cAmp[nBeams][nCells];   // amplitude
   unsigned char  cCorr[PD_MAX_BEAMS][PD_MAX_BINS];// char  cCorr[nBeams][nCells];  // correlation  (0-100)
                                                   // char  cFill           // if nCells % 2 != 0
   short          hChecksum;     // checksum
} PdHrProf;
 *</pre></p>
 *
 */

/** Profiler velocity data */
public class HRProfilerData extends DataStructure implements Serializable {
    
    private static final Logger log = Logger.getLogger(HRProfilerData.class);

    int _nCells = 0;
    int _nBeams = 3;

    static final int START_VELOCITY_INDEX = 54;

    public HRProfilerData() {
    }

    /** Voltage (0.1 volt) */
    public short voltage() {
        return getShort(14);
    }

    /** Sound speed (0.1 m/sec) */
    public short soundSpeed() {
        return getShort(16);
    }

    /** Heading (0.1 deg) */
    public short heading() {
        return getShort(18);
    }

    /** Pitch (0.1 deg) */
    public short pitch() {
        return getShort(20);
    }

    /** Roll (0.1 deg) */
    public short roll() {
        return getShort(22);
    }

    /** Temperature (0.01 deg) */
    public short temperature() {
        return getShort(28);
    }

    /** Set the number of cells and beams. */
    public void setParameters(int nCells, int nBeams) {
        _nCells = nCells;
        _nBeams = nBeams;
    }


    /** Velocity (mm/sec). */
    public int velocity(int beam, int cell) {
        int idx = START_VELOCITY_INDEX + 2 * beam * _nCells + 2 * cell;

        int value = NumberUtil.toShort(new byte[] {_dataBytes[idx], _dataBytes[idx + 1]}, true);
	log.debug("beam = " + beam + ", cell = " + cell + 
		  ", index = " + idx + ", value = " + value);

        return value;

    }

    /** Amplitude (counts) */
    public int amplitude(int beam, int cell) {
        int startIndex = START_VELOCITY_INDEX + 2 * _nCells * _nBeams;
        int idx = startIndex + beam * _nCells + cell;
        return NumberUtil.toUnsignedInt(_dataBytes[idx]);
    }


    /** Return String representation */
    public String toString() {
        StringBuffer output = new StringBuffer(super.toString());
        output.append("\nvoltage=").append(voltage());
        output.append("; soundSpeed=").append(soundSpeed());
        output.append("; temperature=").append(temperature());
        output.append("\n\n");

        if (_nCells <= 0) {
            output.append("Unknown number of cells\n");
        }
        else {
            for (int beam = 0; beam < _nBeams; beam++) {
                for (int cell = 0; cell < _nCells; cell++) {
                    output.append("beam=").append(beam).append(", cell=").append(cell);
                    output.append("; velocity=").append(velocity(beam, cell));
                    output.append("; amplitude=").append(amplitude(beam, cell)).append("\n");
                }
            }
        }

        return output.toString();
    }

}
