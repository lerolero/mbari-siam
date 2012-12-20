/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/

package org.mbari.siam.operations.utils;

public class PuckCommandSpec_versionOneDotZero extends PuckCommandSpec {

    public static final String VERSION = "moos.operations.utils.PuckCommandSpec_versionOneDotZero";

    /**
     * This class should only be instantiated by the PuckCommandSpec superclass
     * in order to maintain the singleton pattern
     *
     */
    protected PuckCommandSpec_versionOneDotZero(){
    }

    public String setBaudRateCommand() {
        return "SB";
    }

    public String verifyBaudRateCommand() {
        return "VB";
    }

    public String readFromMemoryCommand() {
        return "RM";
    }

    public String writeToMemoryCommand() {
        return "WM";
    }

    public String flushMemoryCommand() {
        return "FL";
    }

    public String eraseMemoryCommand() {
        return "ER";
    }

    public String getAddressCommand() {
        return "GA";
    }

    public String putPuckInInstrumentModeCommand() {
        return "SM";
    }

    public String setAddressOfMemoryPointerCommand() {
        return "SA";
    }

    public String getSizeOfMemoryCommand() {
        return "SZ";
    }

    public String queryPuckTypeCommand() {
        throw new UnsupportedOperationException(
            "The query puck type command is not supported in PUCK spec 1.0");
    }

    public String getFirmwareVersionCommand() {
        return "VR";
    }
}
