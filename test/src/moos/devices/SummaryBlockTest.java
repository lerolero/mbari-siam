package moos.devices;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;

import moos.deployed.BaseInstrumentService;
import moos.deployed.DeviceLog;
import moos.deployed.DeviceLogIterator;
import moos.jddac.SiamRecord;
import moos.jddac.xml.Coder;
import moos.jddac.xml.XmlCoder;

import net.java.jddac.common.meas.MeasAttr;
import net.java.jddac.common.type.ArgArray;
import net.java.jddac.common.type.TimeRepresentation;
import net.java.jddac.jmdi.fblock.FunctionBlock;

import org.apache.log4j.Logger;
import org.mbari.isi.interfaces.DevicePacket;
import org.mbari.isi.interfaces.DevicePacketParser;
import org.mbari.isi.interfaces.MetadataPacket;
import org.mbari.isi.interfaces.NotSupportedException;
import org.mbari.isi.interfaces.SensorDataPacket;
import org.mbari.isi.interfaces.SummaryPacket;
import org.mbari.jddac.AggregationBlock;
import org.mbari.jddac.NewArgArrayEvent;
import org.mbari.jddac.NewArgArrayListener;
import org.mbari.jddac.SampleCountFilter;
import org.mbari.jddac.StatsBlock;


/*
 * SummaryBlockTest.java
 *
 * Created on April 20, 2006, 9:22 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author brian
 */
public class SummaryBlockTest {
    
    private BaseInstrumentService instrumentService;
    private static final Logger log = Logger.getLogger(SummaryBlockTest.class);
    private final long deviceId;
    private String inputDirectory;
    private final StatsBlock statsBlock = new StatsBlock();
    private final Coder coder = new XmlCoder();
    
    private final DeviceLog deviceLog;
    
    /** Creates a new instance of SummaryBlockTest */
    public SummaryBlockTest(String className, long deviceId, String inputDirectory, String[] summaryVars) throws ClassNotFoundException, InstantiationException, IllegalAccessException, FileNotFoundException, IOException {
        if (className != null) {
            setInstrumentService((BaseInstrumentService) Class.forName(className).newInstance());
        }
        this.deviceId = deviceId;
        this.inputDirectory = inputDirectory;
        
        deviceLog = new DeviceLog(9999, inputDirectory);
        
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
                SummaryPacket packet = new SummaryPacket(getInstrumentService().getId());
                packet.setData(System.currentTimeMillis(), coder.encode(argArray).getBytes());
                packet.setRecordType(1000L);
                
                if (log.isDebugEnabled()) {
                	log.debug("Summary: \n" + new String(packet.getData()) );
                }
                
                //deviceLog.appendPacket(packet, true, true);
                
                
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
            
            byte[] deviceData = null;
            
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
     * @param instrumentConfiguration The configuration to update with the new metadata.
     * @throws Exception
     */
    protected void handleMetadataPacket(MetadataPacket metadataPacket)
    throws Exception {
        
    }
    
    public static void main(String[] args) throws Exception {
        
        String[] summaryVars = new String[args.length - 3];
        System.arraycopy(args, 3, summaryVars, 0, args.length - 3 );
        
        SummaryBlockTest test = new SummaryBlockTest(args[0], Long.parseLong(args[1]), args[2], summaryVars);
        test.run();
        System.exit(1);
    }


    public BaseInstrumentService getInstrumentService() {
        return instrumentService;
    }
    
    public void setInstrumentService(BaseInstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }
}

