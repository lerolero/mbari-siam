/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils;

import org.mbari.siam.devices.nortek.Aquadopp;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.MetadataPacket;

import java.io.ByteArrayInputStream;

public class PacketUtilities {
    
    private static final Logger logger = Logger.getLogger(PacketUtilities.class);
    
    static public byte[] getDeviceMetadata(byte[] metadata) throws Exception {
        

        byte[] deviceMetadata = null;
        ByteArrayInputStream inputStream = new ByteArrayInputStream(metadata);

        
        /*
         * Move inputStream pointer past the XML tag to the first byte of
         * the metadata spit out by the Aquadopp
         */
        try {
            StreamUtils.skipUntil(inputStream,
                    (MetadataPacket.DEVICE_INFO_TAG + "\n").getBytes(),
                    2000);
        } catch (Exception e) {
            inputStream.close();
            throw new Exception(MetadataPacket.DEVICE_INFO_TAG + " not found");
        }
        
        /*
         * Found start of device metadata; now read all the data until the 
         * closing XML tag is found
         */
        try {
            int nBytes =
                    StreamUtils.readUntil(inputStream,
                    metadata,
                    MetadataPacket.DEVICE_INFO_CLOSE_TAG.getBytes(),
                    2000);
            
            inputStream.close();
            deviceMetadata = new byte[nBytes];
            System.arraycopy(metadata, 0, deviceMetadata, 0, nBytes);
            logger.debug("Found device metadata: " + new String(deviceMetadata));
        } catch (Exception e) {
            inputStream.close();
            throw new Exception(MetadataPacket.DEVICE_INFO_CLOSE_TAG +
                    " not found");
        }
        
        /*
         * For Aquadopp data we need to filterout the ASCII tags, which we 
         * added, from the binary metadata
         */
        try {
            //byte[] copyMetadata = new byte[deviceMetadata.length];
            //System.arraycopy(deviceMetadata, 0, copyMetadata, 0, deviceMetadata.length);
            inputStream = new ByteArrayInputStream(deviceMetadata);
            int nBytes = StreamUtils.readUntil(inputStream, deviceMetadata, Aquadopp.ASCII_CONFIG_TAG.getBytes(), 2000);
            inputStream.close();
            
            if (nBytes < deviceMetadata.length - 1) {
                byte[] copyMetadata = new byte[nBytes];
                System.arraycopy(deviceMetadata, 0, copyMetadata, 0, nBytes);
                deviceMetadata = copyMetadata;
            }
            
        }
        catch (Exception e) {
            logger.info("An error ocurred while searching for " + Aquadopp.ASCII_CONFIG_TAG, e);
        }
        
        return deviceMetadata;
    }
    
    static public byte[] getDeviceMetadata(MetadataPacket packet) throws Exception {
        return getDeviceMetadata(packet.getBytes());
    }
    
}
