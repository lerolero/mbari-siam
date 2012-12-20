/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.utils;


public abstract class PuckCommandSpec {

    private static String PUCK_SPECIFICATION_VERSION = PuckCommandSpec_versionOneDotTwo.VERSION;
    private static PuckCommandSpec _this;

    //we're only initializing this class once, so if the puck spec changes,
    //all existing references to the spec that have been stored in local variables
    //(to avoid multiple calls to PuckCommandSpec.getInstance) will be
    //invalid. In essence, this singleton is not designed for the spec to be
    //changed at run time.
    public synchronized static PuckCommandSpec getInstance(){
        if(_this == null){
            try {
                Class puckSpecClass = PuckCommandSpec.class.getClassLoader().loadClass(PUCK_SPECIFICATION_VERSION);
                _this = (PuckCommandSpec)puckSpecClass.newInstance();
            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InstantiationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return _this;
    }
    
    /**
     * in spec v1.2 command is: PUCKSB
     * @return the PUCK command to set PUCK baud rate
     */
    public abstract String setBaudRateCommand();
    
    /**
     * in spec v1.2 command is: PUCKVB
     * @return the PUCK command to Verify baud rate support 
     */
    public abstract String verifyBaudRateCommand();
    
    /**
     * in spec v1.2 command is: PUCKRM
     * @return the PUCK command to Read from PUCK memory
     */
    public abstract String readFromMemoryCommand();
    
    /**
     * in spec v1.2 command is: PUCKWM
     * @return the PUCK command to Write to PUCK memory
     */
    public abstract String writeToMemoryCommand();
    
    /**
     * in spec v1.2 command is: PUCKFM
     * in spec v1.0 command is: FL ???? In 1.0, description says command is to "End PUCK write session"
     * @return the PUCK command to Make sure any buffered bytes are written to PUCK memory
     */
    public abstract String flushMemoryCommand();
    
    /**
     * in spec v1.2 command is: PUCKEM
     * in spec v1.0 command is: ER
     * @return the PUCK command to Erase PUCK memory
     */
    public abstract String eraseMemoryCommand();
    
    /**
     * in spec v1.2 command is: PUCKGA
     * @return the PUCK command to Get address of PUCK internal memory pointer 
     */
    public abstract String getAddressCommand();
    
    /**
     * in spec v1.2 command is: PUCKIM
     * in spec v1.0 command is: SM
     * @return the PUCK command to Put PUCK into instrument mode
     */
    public abstract String putPuckInInstrumentModeCommand();
    
    /**
     * in spec v1.2 command is: PUCKSA
     * @return the PUCK command to Set address of PUCK internal memory pointer
     */
    public abstract String setAddressOfMemoryPointerCommand();
    
    /**
     * in spec v1.2 command is: PUCKSZ
     * @return the PUCK command to Get the size of the PUCK memory
     */
    public abstract String getSizeOfMemoryCommand();
    
    /**
     * in spec v1.2 command is: PUCKTY
     * in spec v1.0 command DOES NOT EXIST
     * @return the PUCK command to Query PUCK type
     */
    public abstract String queryPuckTypeCommand();
    
    /**
     * in spec v1.2 command is: PUCKVR
     * @return the PUCK command to Get PUCK firmware version string
     */
    public abstract String getFirmwareVersionCommand();

    
    public final static String getPUCK_SPECIFICATION_VERSION() {
        return PUCK_SPECIFICATION_VERSION;
    }

    
    public final static void setPUCK_SPECIFICATION_VERSION(
        String puck_specification_version) {
        if(_this != null){
            throw new RuntimeException("The puck version can not be " +
                    "changed after a call to PuckCommandSpec.getInstance " +
                    "has been made.");
        }
        PUCK_SPECIFICATION_VERSION = puck_specification_version;
    }
}
