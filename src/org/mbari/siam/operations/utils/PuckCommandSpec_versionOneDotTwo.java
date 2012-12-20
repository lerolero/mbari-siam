/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.utils;


public class PuckCommandSpec_versionOneDotTwo extends PuckCommandSpec {
    public static final String VERSION = "moos.operations.utils.PuckCommandSpec_versionOneDotTwo";
    /**
     * This class should only be instantiated by the PuckCommandSpec superclass
     * in order to maintain the singleton pattern
     *
     */
    protected PuckCommandSpec_versionOneDotTwo(){
    }

    public String setBaudRateCommand() {
        return "PUCKSB";
    }

    public String verifyBaudRateCommand() {
        return "PUCKVB";
    }

    public String readFromMemoryCommand() {
        return "PUCKRM";
    }

    public String writeToMemoryCommand() {
        return "PUCKWM";
    }

    public String flushMemoryCommand() {
        return "PUCKFM";
    }

    public String eraseMemoryCommand() {
        return "PUCKEM";
    }

    public String getAddressCommand() {
        return "PUCKGA";
    }

    public String putPuckInInstrumentModeCommand() {
        return "PUCKIM";
    }

    public String setAddressOfMemoryPointerCommand() {
        return "PUCKSA";
    }

    public String getSizeOfMemoryCommand() {
        return "PUCKSZ";
    }

    public String queryPuckTypeCommand() {
        return "PUCKTY";
    }

    public String getFirmwareVersionCommand() {
        return "PUCKVR";
    }
}