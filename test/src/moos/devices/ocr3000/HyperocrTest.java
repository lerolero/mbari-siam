package moos.devices.ocr3000;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;

import moos.deployed.BaseInstrumentService;
import moos.deployed.DeviceLog;
import moos.deployed.DeviceLogIterator;
import moos.devices.SummaryBlockTest;
import moos.devices.ocr3000.Ocr3000;
import moos.jddac.SiamRecord;
import moos.jddac.xml.Coder;
import moos.jddac.xml.MinimalXmlCoder;
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

public class HyperocrTest {

    private BaseInstrumentService instrumentService;

    private static final Logger log = Logger.getLogger(SummaryBlockTest.class);

    private final long deviceId;

    private String inputDirectory;

    private final Ocr3000.OCRServiceBlock fblock;


    /** Creates a new instance of SummaryBlockTest */
    public HyperocrTest(long deviceId, String inputDirectory)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, FileNotFoundException,
            IOException {
        
        setInstrumentService(new Ocr3000());
        this.deviceId = deviceId;
        this.inputDirectory = inputDirectory;
        
        
        fblock = (Ocr3000.OCRServiceBlock) getInstrumentService().getInstrumentServiceBlock();


        fblock.addChild(new FunctionBlock() {
            
            Coder coder = new MinimalXmlCoder();
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
                
                log.info(coder.encode(argArray));


                return argArray;
            }

        });
    }

 
    /*
     * public SummaryBlockTest(long deviceID) throws MalformedURLException {
     * this.deviceID = deviceID; url = new URL(new
     * StringBuffer().append("http://new-ssds.mbari.org:8080/servlet/GetOriginalDataServlet?deviceID=").append(deviceID).append("&numHoursBack=24&recordDelimiter=[END]&outputAs=binary").toString()); }
     */

    public void run() throws Exception {

        SiamRecord[] siamRecords = readDeviceLog();

        SiamRecord siamRecord = null;
        for (int i = 0; i < siamRecords.length; i++) {

            siamRecord = siamRecords[i];

            // TODO run the siam record through the function chain
            if (log.isDebugEnabled()) {
                log.debug(siamRecord.toString());
            }

            fblock.perform(fblock.OpIdFilter, siamRecord);
        }

        // Feed it into AquadoppInstrumentBlock

        // Summaryize the results
    }

    /**
     * Reads A device log and returns the contents as an array of SiamRecords
     */
    public SiamRecord[] readDeviceLog() throws FileNotFoundException, IOException, NotSupportedException {

        boolean gotConfiguration = true;
        byte[] buf = new byte[1024];
        int nSaved = 0;
        Vector records = new Vector();

        // To correctly parse the Aquadopp data you need to know the instruments
        // configuration (nCells and nBeams)
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
                    nSaved++;
                }
            }
            else {
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

    private static SiamRecord handleSensorDataPacket(SensorDataPacket sensorDataPacket,
            DevicePacketParser devicePacketParser) throws Exception {
        return devicePacketParser.parse(sensorDataPacket);
    }

    /**
     * Process a metadata packet an updates the instrument configuration object
     * with the new metadata.
     * 
     * @param metadataPacket
     *            The metadata to process
     * @param instrumentConfiguration
     *            The configuration to update with the new metadata.
     * @throws Exception
     */
    protected void handleMetadataPacket(MetadataPacket metadataPacket) throws Exception {

    }

    public static void main(String[] args) throws Exception {

        HyperocrTest test = new HyperocrTest(Long.parseLong(args[0]), args[1]);
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
