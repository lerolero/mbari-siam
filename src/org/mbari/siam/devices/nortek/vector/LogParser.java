/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.nortek.vector;

import org.mbari.siam.core.DeviceLog;
import org.mbari.siam.core.DeviceLogIterator;
import org.mbari.siam.distributed.jddac.SiamRecord;
import org.mbari.siam.distributed.jddac.xml.Coder;
import org.mbari.siam.distributed.jddac.xml.XmlCoder;
import org.mbari.siam.utils.PacketUtilities;
import net.java.jddac.common.meas.MeasAttr;
import net.java.jddac.common.meas.collection.Record;
import net.java.jddac.common.type.ArgArray;
import net.java.jddac.common.type.TimeRepresentation;
import net.java.jddac.jmdi.fblock.FunctionBlock;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DevicePacketParser;
import org.mbari.siam.distributed.MetadataPacket;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.jddac.*;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * @author brian
 * @version $Id: LogParser.java,v 1.2 2012/12/17 21:34:06 oreilly Exp $
 * @since Oct 12, 2006 1:51:31 PM PST
 */
public class LogParser {

    private static final Logger log = Logger.getLogger(LogParser.class);

    private LogParser() {
        // No instantiation
    }

    public static ArgArray[] createSummaries(SiamRecord[] siamRecords, int sampleCount) {

        /*
         * Setup a stats block to generate summaries
         */
        final StatsBlock statsBlock = new StatsBlock();
        final List summaries = new ArrayList();
        final SampleCountFilter filter = new SampleCountFilter(statsBlock, sampleCount);
        final Coder coder = new XmlCoder();
        statsBlock.addFilter(filter);
        statsBlock.addAcceptedVariableName(moos.devices.nortek.DevicePacketParser.AMPLITUDE + "-0");
        statsBlock.addAcceptedVariableName(moos.devices.nortek.DevicePacketParser.AMPLITUDE + "-1");
        statsBlock.addAcceptedVariableName(moos.devices.nortek.DevicePacketParser.AMPLITUDE + "-2");
        statsBlock.addAcceptedVariableName(moos.devices.nortek.DevicePacketParser.AMPLITUDE + "-3");
        statsBlock.addAcceptedVariableName(moos.devices.nortek.DevicePacketParser.AVG_INTERVAL);
        statsBlock.addAcceptedVariableName(moos.devices.nortek.DevicePacketParser.HEADING);
        statsBlock.addAcceptedVariableName(moos.devices.nortek.DevicePacketParser.N_BEAMS);
        statsBlock.addAcceptedVariableName(moos.devices.nortek.DevicePacketParser.N_CELLS);
        statsBlock.addAcceptedVariableName(moos.devices.nortek.DevicePacketParser.PITCH);
        statsBlock.addAcceptedVariableName(moos.devices.nortek.DevicePacketParser.ROLL);
        statsBlock.addAcceptedVariableName(moos.devices.nortek.DevicePacketParser.SOUND_SPEED);
        statsBlock.addAcceptedVariableName(moos.devices.nortek.DevicePacketParser.SYSTEM_TIME);
        statsBlock.addAcceptedVariableName(moos.devices.nortek.DevicePacketParser.TEMPERATURE);
        statsBlock.addAcceptedVariableName(moos.devices.nortek.DevicePacketParser.VELOCITY + "-0");
        statsBlock.addAcceptedVariableName(moos.devices.nortek.DevicePacketParser.VELOCITY + "-1");
        statsBlock.addAcceptedVariableName(moos.devices.nortek.DevicePacketParser.VELOCITY + "-2");
        statsBlock.addAcceptedVariableName(moos.devices.nortek.DevicePacketParser.VELOCITY + "-3");
        statsBlock.addAcceptedVariableName(moos.devices.nortek.DevicePacketParser.VELOCITY + "-X");
        statsBlock.addAcceptedVariableName(moos.devices.nortek.DevicePacketParser.VELOCITY + "-Y");
        statsBlock.addAcceptedVariableName(moos.devices.nortek.DevicePacketParser.VELOCITY + "-Z");
        statsBlock.addAcceptedVariableName(moos.devices.nortek.DevicePacketParser.VOLTAGE);



        /*
         * This listener listens for when an new ArgArray is added to the stats block. When
         * one is found it will create statistics and clear all samples out of the summarizer
         * if the sample count reaches the threshold provided as an argument
         */
        statsBlock.addNewArgArrayListener(new NewArgArrayListener() {
            public void processEvent(NewArgArrayEvent event) {
                if (statsBlock.size() == filter.getCount()) {
                    log.debug("executing stats and clearing data");
                    statsBlock.doStats();
                    statsBlock.clear();
                }
            }
        });

        /*
         * This functionBlock listens for any stats coming out of the stats block and adds
         * them to a List
         */
        statsBlock.addChild(new FunctionBlock() {
            public ArgArray perform(String opId, ArgArray argArray) {
                /*
                 * Make sure the record has a timestamp. If it doesn' add one.
                 */
                Object time = argArray.get(MeasAttr.TIMESTAMP);
                if (time == null) {
                    argArray.put(MeasAttr.TIMESTAMP, new TimeRepresentation());
                }
		log.debug(coder.encode(argArray));
                summaries.add(argArray);
                return argArray;
            }
        });

        /*
         * Loop through all the records an process them with the statsblock
         */
        Record siamRecord = null;
        for (int i = 0; i < siamRecords.length; i++) {
            siamRecord = siamRecords[i];
            try {
                statsBlock.perform(AggregationBlock.OpIdAddArgArray, siamRecord);
            }
            catch (Exception e) {
                log.error("Failed to handle " + siamRecord);
            }
        }

        statsBlock.destroy();

        /*
        * Convert packets to an Array
        */
        ArgArray[] argArrays = new ArgArray[summaries.size()];
        summaries.toArray(argArrays);
        return argArrays;

    }


    /**
     * Reads A device log and returns the contents as an array of SiamRecords
     */
    public static final SiamRecord[] readDeviceLog(long deviceID, String inputDirectory)
            throws FileNotFoundException, IOException {

        boolean gotConfiguration = false;
        byte[] buf = new byte[1024];
        int processedCount = 0;
        int totalCount = 0;
        int configCount = 0;
        Vector records = new Vector();

        // To correctly parse the Aquadopp data you need to know the instruments configuration (nCells and nBeams)
        InstrumentConfiguration config = new InstrumentConfiguration();
        org.mbari.isi.interfaces.DevicePacketParser parser = new moos.devices.nortek.DevicePacketParser(config);

        // Open input SIAM log
        DeviceLog input = new DeviceLog(deviceID, inputDirectory);

        // Iterate through SIAM packets in input log
        DeviceLogIterator iterator = new DeviceLogIterator(input);
        while (iterator.hasNext()) {
            DevicePacket packet = (DevicePacket) (iterator.next());
            totalCount++;
            if (packet instanceof MetadataPacket) {
                MetadataPacket metadata = (MetadataPacket) packet;
                try {
                    handleMetadataPacket(metadata, config);
                    configCount++;
                    gotConfiguration = true;
                }
                catch (Exception e) {
                    log.warn("Couldn't get device metadata", e);
                    continue;
                }
            }

            // Save to Nortek file only if we've already encountered a
            // Nortek configuration packet in the input stream.
            if (gotConfiguration) {
                if (packet instanceof SensorDataPacket) {
                    try {
                        records.addElement(handleSensorDataPacket((SensorDataPacket) packet, parser));
                    }
                    catch (Exception e) {
                        log.debug("Failed to correctly handle a SensorDataPacket", e);
                    }
                    processedCount++;
                }
            }
            else {
                log.debug("No configuration packet yet - skipping...");
            }

        }

        int total = processedCount + configCount;
        log.debug("Found " + processedCount + " record packets");
        log.debug("Found " + configCount + " configuration packets");
        log.debug("Found " + total + " valid packets (of " + totalCount + ")");

        /*
        * Convert packets to an Array
        */
        SiamRecord[] siamRecords = new SiamRecord[records.size()];
        records.toArray(siamRecords);
        return siamRecords;
    }

    private static SiamRecord handleSensorDataPacket(SensorDataPacket sensorDataPacket, DevicePacketParser devicePacketParser)
            throws Exception {
        SiamRecord record = devicePacketParser.parse(sensorDataPacket);
        record.put(moos.devices.nortek.DevicePacketParser.SYSTEM_TIME, sensorDataPacket.systemTime());
        return devicePacketParser.parse(sensorDataPacket);
    }

    /**
     * Process a metadata packet an updates the instrument configuration object with the new metadata.
     * @param metadataPacket The metadata to process
     * @param instrumentConfiguration The configuration to update with the new metadata.
     * @throws Exception
     */
    private static void handleMetadataPacket(MetadataPacket metadataPacket, InstrumentConfiguration instrumentConfiguration)
            throws Exception {

        byte[] deviceData = PacketUtilities.getDeviceMetadata(metadataPacket);
        byte[] buf = new byte[1024];

        ByteArrayInputStream byteInput = new ByteArrayInputStream(deviceData);

        // This data should consist of three configuration
        // data structures (hardware, header, instrument
        // config structures).
        for (int i = 0; i < 3; i++) {
            try {
                int nBytes = DataStructure.read(byteInput, buf, 5000);
                log.debug("read() returned " + nBytes + " bytes");
                int id = DataStructure.id(buf);
                log.debug("Config data struct id=0x" + Integer.toHexString(id));
                if (DataStructure.isInstrumentConfiguration(buf)) {
                    log.debug("Found InstrumentConfiguration");
                    instrumentConfiguration.setBytes(buf);
                    log.debug("Aquadopp has " + instrumentConfiguration.nBeams() + " beams with " +
                            instrumentConfiguration.nCells());
                }
            }
            catch (Exception e) {
                log.warn("Error retrieving config", e);
            }
        }

        byteInput.close();
    }


}
