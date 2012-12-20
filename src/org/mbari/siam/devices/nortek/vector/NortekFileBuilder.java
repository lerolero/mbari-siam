/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.nortek.vector;

import org.mbari.siam.core.DeviceLog;
import org.mbari.siam.core.DeviceLogIterator;
import org.mbari.siam.utils.PacketUtilities;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.MetadataPacket;
import org.mbari.siam.distributed.SensorDataPacket;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Build a Nortek profiler data file, readable by AquaPro processing
 * software, from SIAM packet inputs.
 */
public class NortekFileBuilder {

    /**
     * log4j logger
     */
    static Logger _log4j = Logger.getLogger(NortekFileBuilder.class);

    byte[] _buf = new byte[1024];

    public void run(long deviceID, String inputDirectory, String outputFile)
            throws FileNotFoundException, IOException {

        boolean gotConfiguration = false;

        /*
         * The log containing packets that we're reading.
         */
        DeviceLog deviceLog = new DeviceLog(deviceID, inputDirectory);

        /*
         * The stream we're writing to
         */
        FileOutputStream outputStream = new FileOutputStream(outputFile);

        /*
         * Counter for the number of packets written to disk
         */
        int nSaved = 0;  
        int nIgnored = 0;

        /*
         * Iterate through all SIAM packets in input log. The file we're creating
         * needs to start with the binary configuration information.
         */
        DeviceLogIterator iterator = new DeviceLogIterator(deviceLog);
        while (iterator.hasNext()) {
            DevicePacket packet = (DevicePacket) (iterator.next());

            byte[] deviceData = null;

            
            if (packet instanceof SensorDataPacket) {
                /*
                 * For sensor data packets we write the entire contents of the 
                 * dataBuffer to the Norktek file.
                 */
                deviceData = ((SensorDataPacket) packet).dataBuffer();
                
                //System.out.println("SensorDataPacket: " + new String(deviceData));
            }
            else if (packet instanceof MetadataPacket) {
                /*
                 * For Metadata packets we ONLY want the binary device metadata
                 */
                MetadataPacket metadata = (MetadataPacket) packet;
                try {
                    deviceData = PacketUtilities.getDeviceMetadata(metadata);
                    gotConfiguration = true;
                    System.out.println("MetadataPacket: " + new String(deviceData));
                }
                catch (Exception e) {
                    _log4j.error("Couldn't get device metadata", e);
                    continue;
                }

                
                /*
                 * Debugging output.
                 */
                if (_log4j.isDebugEnabled()) {
                    ByteArrayInputStream byteInput =
                            new ByteArrayInputStream(deviceData);

                    /*
                     * This data should consist of three configuration
                     * data structures (hardware, header, instrument
                     * config structures).
                     */
                    for (int i = 0; i < 3; i++) {
                        try {
                            int nBytes =
                                    DataStructure.read(byteInput, _buf, 5000);
                            _log4j.debug("read() returned " + nBytes + " bytes");
                            int id = DataStructure.id(_buf);
                            _log4j.debug("Config data struct id=0x" +
                                    Integer.toHexString(id));
                        }
                        catch (Exception e) {
                            _log4j.error("Error retrieving config: " + e);
                        }
                    }
                    byteInput.close();
                }

                
            }
            else {
                // Only process SensorDataPacket and MetadataPacket types
                continue;
            }

            // Save to Nortek file only if we've already encountered a
            // Nortek configuration packet in the input stream.
            if (gotConfiguration) {
                outputStream.write(deviceData);
                nSaved++;
            }
            else {
                _log4j.debug("No configuration packet yet - skipping...");
                nIgnored++;
            }

        }

        outputStream.flush();
        outputStream.close();
        _log4j.debug("Saved " + nSaved + " packets. Ignored " + nIgnored + " packets");
    }


    public static void main(String[] args) {

        // Configure Log4J
        PropertyConfigurator.configure(System.getProperties());
        BasicConfigurator.configure();

        if (args.length != 3) {
            _log4j.error("usage: deviceID, logDirectory, outputName");
            return;
        }

        NortekFileBuilder builder = new NortekFileBuilder();

        try {
            builder.run(Long.parseLong(args[0]), args[1], args[2]);
        }
        catch (Exception e) {
            _log4j.error(e);
        }
    }

}

