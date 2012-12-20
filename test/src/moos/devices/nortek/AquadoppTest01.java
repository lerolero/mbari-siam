package moos.devices.nortek;

import moos.deployed.DeviceLog;
import moos.deployed.DeviceLogIterator;
import moos.jddac.SiamRecord;
import moos.utils.PacketUtilities;
import org.apache.log4j.Logger;
import org.mbari.isi.interfaces.DevicePacket;
import org.mbari.isi.interfaces.DevicePacketParser;
import org.mbari.isi.interfaces.MetadataPacket;
import org.mbari.isi.interfaces.SensorDataPacket;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import net.java.jddac.common.meas.MeasAttr;

/**
 * Created by IntelliJ IDEA.
 * User: brian
 * Date: Apr 3, 2006
 * Time: 10:44:48 AM
 * To change this template use File | Settings | File Templates.
 */
public class AquadoppTest01 {

    private static final Logger log = Logger.getLogger(AquadoppTest01.class);
    private final long deviceID;
    private String inputDirectory;

    private URL url;

    public AquadoppTest01(long deviceID, String inputDirectory) {
        this.deviceID = deviceID;
        this.inputDirectory = inputDirectory;
    }

    public AquadoppTest01(long deviceID) throws MalformedURLException {
        this.deviceID = deviceID;
        url = new URL(new StringBuffer().append("http://new-ssds.mbari.org:8080/servlet/GetOriginalDataServlet?deviceID=").append(deviceID).append("&numHoursBack=24&recordDelimiter=[END]&outputAs=binary").toString());
    }


    public void run() throws Exception {

        SiamRecord[] siamRecords = readDeviceLog(deviceID, inputDirectory);

        SiamRecord siamRecord = null;
        for (int i = 0; i < siamRecords.length; i++) {

            siamRecord = siamRecords[i];

            // TODO run the siam record through the function chain
            if (log.isDebugEnabled()) {
                log.debug(siamRecord.toString());
            }
        }

        // Feed it into AquadoppInstrumentBlock

        // Summaryize the results
    }
    
    public SiamRecord[] readDeviceLog() throws FileNotFoundException, IOException {
        return readDeviceLog(deviceID, inputDirectory);
    }


    /**
     * Reads A device log and returns the contents as an array of SiamRecords
     */
    public static final SiamRecord[] readDeviceLog(long deviceID, String inputDirectory)
            throws FileNotFoundException, IOException {

        boolean gotConfiguration = false;
        byte[] buf = new byte[1024];
        int nSaved = 0;
        Vector records = new Vector();

        // To correctly parse the Aquadopp data you need to know the instruments configuration (nCells and nBeams)
        InstrumentConfiguration config = new InstrumentConfiguration();
        DevicePacketParser parser = new moos.devices.nortek.DevicePacketParser(config);

        // Open input SIAM log
        DeviceLog input = new DeviceLog(deviceID, inputDirectory);

        // Iterate through SIAM packets in input log
        DeviceLogIterator iterator = new DeviceLogIterator(input);
        while (iterator.hasNext()) {
            DevicePacket packet = (DevicePacket) (iterator.next());

            log.debug("Found Packet: " + packet);

            byte[] deviceData = null;

            if (packet instanceof MetadataPacket) {
                MetadataPacket metadata = (MetadataPacket) packet;
                try {
                    handleMetadataPacket(metadata, config);
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

    private static SiamRecord handleSensorDataPacket(SensorDataPacket sensorDataPacket, DevicePacketParser devicePacketParser)
            throws Exception {
        SiamRecord record = devicePacketParser.parse(sensorDataPacket);
        record.put("system-time", sensorDataPacket.systemTime());
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
                int nBytes =
                        DataStructure.read(byteInput, buf, 5000);
                log.debug("read() returned " + nBytes + " bytes");
                int id = DataStructure.id(buf);
                log.debug("Config data struct id=0x" +
                        Integer.toHexString(id));
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

    public static void main(String[] args) throws Exception {

        //AquadoppTest01 test = new AquadoppTest01(Long.parseLong(args[0]), args[1]);
        //AquadoppTest01 test = new AquadoppTest01(1474L, "C:/Documents and Settings/brian/workspace/siam/test/resources/aquadopp");
        AquadoppTest01 test = new AquadoppTest01(1474L, "/Users/brian/workspace/siam/test/resources/aquadopp");

        test.run();
    }
}
