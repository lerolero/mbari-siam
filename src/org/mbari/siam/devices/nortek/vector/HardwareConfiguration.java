/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.nortek.vector;

import java.io.Serializable;


/**
 * Hardware configuration data.
 */
public class HardwareConfiguration
        extends DataStructure
        implements Serializable {

    static final int SERIALNO_BYTES = 14;
    byte[] _serialNo = new byte[SERIALNO_BYTES];

    static final int FWVERSION_BYTES = 4;
    byte[] _firmwareVersion = new byte[FWVERSION_BYTES];

    public HardwareConfiguration() {
    }

    /**
     * Averaging interval, in seconds.
     */
    public byte[] serialNo() {
        System.arraycopy(_dataBytes, 4, _serialNo, 0, SERIALNO_BYTES);
        return _serialNo;
    }

    /**
     * Measurement interval (sec)
     */
    public short boardConfiguration() {
        return getShort(18);
    }

    public short boardFrequency() {
        return getShort(20);
    }

    public short picVersion() {
        return getShort(22);
    }

    public short hwVersion() {
        return getShort(24);
    }

    /**
     * Return recorder size in bytes
     */
    public int recorderSize() {
        return getShort(26) * 65536;
    }

    public byte[] fwVersion() {
        System.arraycopy(_dataBytes, 42, _firmwareVersion, 0, FWVERSION_BYTES);
        return _firmwareVersion;
    }


    /**
     * Return String representation
     */
    public String toString() {

        short boardConfig = boardConfiguration();
        boolean hasRecorder = false;
        boolean hasCompass = false;
        boolean hasNewCompass = false;

        if ((boardConfig & 01) > 0) {
            hasRecorder = true;
        }
        if ((boardConfig & 02) > 0) {
            hasCompass = true;
        }
        if ((boardConfig & 04) > 0) {
            hasNewCompass = true;
        }

        String output = super.toString() + "\n";
        output += "serialNo=" + new String(serialNo()) +
                "; boardConfig=0x" + Integer.toHexString(boardConfig) +
                "; hasRecorder=" + hasRecorder + "; hasCompass=" + hasCompass +
                "; hasNewCompass=" + hasNewCompass +
                "; boardFreq=" + boardFrequency() + "; picVers=" + picVersion() +
                "; hwVers=" + hwVersion() +
                "; fwVers=" + new String(fwVersion()) +
                "; recorderSize=" + recorderSize() / 1024 / 1024 + " MBytes";


        return output;
    }
}
