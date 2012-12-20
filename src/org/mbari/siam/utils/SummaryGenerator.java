/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils;

import org.mbari.siam.core.BaseInstrumentService;
import org.mbari.siam.core.DeviceLog;
import org.mbari.siam.core.DeviceLogIterator;
import org.mbari.siam.distributed.jddac.SiamRecord;
import org.mbari.siam.distributed.jddac.xml.Coder;
import org.mbari.siam.distributed.jddac.xml.XmlCoder;
import net.java.jddac.common.meas.MeasAttr;
import net.java.jddac.common.type.ArgArray;
import net.java.jddac.common.type.TimeRepresentation;
import net.java.jddac.jmdi.fblock.FunctionBlock;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.*;
import org.mbari.jddac.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;




/**
 * Generates summaries from a Packet log. This class does not work with the Aquadopp data.
 *
 * Use As:
 * <code>
 * SummaryGenerator sg = new SummaryGenerator(className, deviceId, inputDirectory, summaryVars);
 * sg.run();  // Output XML is echoed to the console.
 * </code>
 *
 * 
 * @author brian
 * @version $Id: SummaryGenerator.java,v 1.3 2012/12/17 21:40:54 oreilly Exp $
 */
public class SummaryGenerator {
    
    private BaseInstrumentService instrumentService;
    private static final Logger log = Logger.getLogger(SummaryGenerator.class);
    private final long deviceId;
    private String inputDirectory;
    private final StatsBlock statsBlock = new StatsBlock();
    private final Coder coder = new XmlCoder();
    
    private final DeviceLog deviceLog;
    
    /** Creates a new instance of SummaryBlockTest */
    public SummaryGenerator(String className, long deviceId, String inputDirectory, String[] summaryVars) throws ClassNotFoundException, InstantiationException, IllegalAccessException, FileNotFoundException, IOException {
        if (className != null) {
            setInstrumentService((BaseInstrumentService) Class.forName(className).newInstance());
        }
        this.deviceId = deviceId;
        this.inputDirectory = inputDirectory;
        
        deviceLog = new DeviceLog(deviceId, inputDirectory);
        
        final SampleCountFilter filter = new SampleCountFilter(statsBlock, 5);
        statsBlock.addFilter(filter);
        setAcceptedVariableNames(summaryVars);
        
        statsBlock.addNewArgArrayListener(new NewArgArrayListener() {
            public void processEvent(NewArgArrayEvent event) {
                if (statsBlock.size() == filter.getCount()) {
                    log.debug("executing stats and clearing data");
                    statsBlock.doStats();
                    statsBlock.clear();
                }
            }
        });
        
        statsBlock.addChild(new FunctionBlock() {
            public ArgArray perform(String opId, ArgArray argArray) {
                /*
                 * Make sure the record has a timestamp. If it doesn' add one.
                 */
                Object time = argArray.get(MeasAttr.TIMESTAMP);
                if (time == null) {
                    argArray.put(MeasAttr.TIMESTAMP, new TimeRepresentation());
                }
                
                if (log.isDebugEnabled()) {
                    log.debug("Creating a SummaryPacket");
                }
                
                /*
                 * Encode the data as XML
                 */
                String data = coder.encode(argArray);
                SummaryPacket packet = new SummaryPacket(getInstrumentService().getId());
                packet.setData(System.currentTimeMillis(), data.getBytes());
                packet.setRecordType(1000L);
                
                if (log.isDebugEnabled()) {
                    log.debug("Summary: \n" + data );
                }
                
                //deviceLog.appendPacket(packet, true, true);
                
                /*
                 * Echo output to screen
                 */
                System.out.println(data);
                
                return argArray;
            }
            
        });
    }
    
    public void setAcceptedVariableNames(String[] names) {
        String[] oldNames = statsBlock.listAcceptedVariableNames();
        if (oldNames != null) {
            for (int i = 0; i < oldNames.length; i++) {
                statsBlock.removeAcceptedVariableName(oldNames[i]);
            }
        }
        
        for (int i = 0; i < names.length; i++) {
            log.info("Adding variable name: '" + names[i] + "'");
            statsBlock.addAcceptedVariableName(names[i]);
        }
    }
    
    
    /*
    public SummaryBlockTest(long deviceID) throws MalformedURLException {
        this.deviceID = deviceID;
        url = new URL(new StringBuffer().append("http://new-ssds.mbari.org:8080/servlet/GetOriginalDataServlet?deviceID=").append(deviceID).append("&numHoursBack=24&recordDelimiter=[END]&outputAs=binary").toString());
    }
     **/
    
    
    public void run() throws Exception {
        
        SiamRecord[] siamRecords = readDeviceLog();
        
        SiamRecord siamRecord = null;
        for (int i = 0; i < siamRecords.length; i++) {
            
            siamRecord = siamRecords[i];
            
            // TODO run the siam record through the function chain
            if (log.isDebugEnabled()) {
                log.debug(siamRecord.toString());
            }
            
            statsBlock.perform(AggregationBlock.OpIdAddArgArray, siamRecord);
        }
        
        deviceLog.close();
        // Feed it into AquadoppInstrumentBlock
        
        // Summaryize the results
    }
    
    
    
    /**
     * Reads A device log and returns the contents as an array of SiamRecords
     */
    public SiamRecord[] readDeviceLog()
    throws FileNotFoundException, IOException, NotSupportedException {
        
        boolean gotConfiguration = true;
        byte[] buf = new byte[1024];
        int nSaved = 0;
        Vector records = new Vector();
        
        // To correctly parse the Aquadopp data you need to know the instruments configuration (nCells and nBeams)
        DevicePacketParser parser = getInstrumentService().getDevicePacketParser();
        
        // Open input SIAM log
        DeviceLog input = new DeviceLog(deviceId, inputDirectory);
        
        // Iterate through SIAM packets in input log
        DeviceLogIterator iterator = new DeviceLogIterator(input);
        while (iterator.hasNext()) {
            DevicePacket packet = (DevicePacket) (iterator.next());
            
            log.debug("Found Packet: " + packet);
            
            if (packet instanceof MetadataPacket) {
                MetadataPacket metadata = (MetadataPacket) packet;
                try {
                    handleMetadataPacket(metadata);
                    gotConfiguration = true;
                } catch (Exception e) {
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
                    } catch (Exception e) {
                        log.debug("Failed to correctly handle a SensorDataPacket", e);
                    }
                    nSaved++;
                }
            } else {
                log.debug("No configuration packet yet - skipping...");
            }
            
        }
        
        log.debug("Found " + nSaved + " packets");
        
        /*
         * Convert packets to an Array
         */
        SiamRecord[] siamRecords = new SiamRecord[records.size()];
        records.toArray(siamRecords);
        return siamRecords;
    }
    
    private static SiamRecord handleSensorDataPacket(SensorDataPacket sensorDataPacket, DevicePacketParser devicePacketParser)
    throws Exception {
        return devicePacketParser.parse(sensorDataPacket);
    }
    
    /**
     * Process a metadata packet an updates the instrument configuration object with the new metadata.
     * @param metadataPacket The metadata to process
     * @exception Exception
     */
    protected void handleMetadataPacket(MetadataPacket metadataPacket)
    throws Exception {
        
    }
    
    public static void main(String[] args) {
        args = new String[] {"moos.devices.nortek.Aquadopp", 
                "1474",
                "test/resources/aquadopp",
                "amplitude-0",
                "amplitude-1",
                "amplitude-2",
                "amplitude-3",
                "velocity-0",
                "velocity-1",
                "velocity-2",
                "velocity-3",
                "voltage"};
        
        int exitCode = 1;
        try {
            String[] summaryVars = new String[args.length - 3];
            System.arraycopy(args, 3, summaryVars, 0, args.length - 3 );
            
            SummaryGenerator test = new SummaryGenerator(args[0], Long.parseLong(args[1]), args[2], summaryVars);
            test.run();
        } catch (Exception e) {
            log.error("Failed to run main method", e);
            System.out.println("Use as: moos.utils.SummaryGenerator [Service Class] [SIAM ID] [Log Directory] [Summary Variables ...]");
            exitCode = 0;
        }
        System.exit(exitCode);
    }
    
    
    public BaseInstrumentService getInstrumentService() {
        return instrumentService;
    }
    
    public void setInstrumentService(BaseInstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }
}

