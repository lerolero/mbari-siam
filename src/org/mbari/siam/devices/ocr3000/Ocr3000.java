/**
 * Copyright 2004 MBARI. MBARI Proprietary Information. All rights reserved.
 */

package org.mbari.siam.devices.ocr3000;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import java.rmi.RemoteException;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import org.mbari.siam.core.BaseInstrumentService;
import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.distributed.jddac.DeviceLogBlock;
import org.mbari.siam.distributed.jddac.DummyBlock;
import org.mbari.siam.distributed.jddac.FilterByKeyFunction;
import org.mbari.siam.distributed.jddac.InstrumentServiceBlock;
import org.mbari.siam.distributed.jddac.xml.MinimalXmlCoder;

import org.mbari.siam.utils.StopWatch;
import org.mbari.siam.utils.StreamUtils;
import net.java.jddac.common.meas.MeasAttr;
import net.java.jddac.common.meas.Measurement;
import net.java.jddac.common.meas.collection.Record;
import net.java.jddac.common.type.ArgArray;
import net.java.jddac.common.type.TypeAttr;

import org.apache.log4j.Logger;

import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DevicePacketParser;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.jddac.AggregationBlock;
import org.mbari.jddac.FilterByMeasurementNameFunction;
import org.mbari.jddac.FunctionFactory;
import org.mbari.jddac.SampleCountFilter;
import org.mbari.jddac.StatsBlock;
import org.mbari.util.NumberUtil;

/**
 * Class description
 * 
 * 
 * @version $Id: Ocr3000.java,v 1.4 2009/07/16 22:01:08 headley Exp $
 * @author Enter your name here...
 */
public class Ocr3000 extends PolledInstrumentService implements Instrument {

    static private Logger _logger = Logger.getLogger(Ocr3000.class);

    /** Default serial stop bits */
    static final int _STOP_BITS = SerialPort.STOPBITS_1;

    /**
     * The minimum amount of time that the OCR-3000 must be off to guarentee
     * that the shutters will open correctly
     */
    private static final int _SHUTTER_CLOSE_TIME = 15000;

    private static final int _SATURATED_FRAME = 2;

    /**
     * The maximum number of attempts the readFrame method will make when trying
     * to read a single frame
     */
    private static final int _READ_FRAME_RETRIES = 6;

    /** Default parity checking */
    static final int _PARITY = SerialPort.PARITY_NONE;

    /**
     * The minimum amount of time that the OCR-3000 must be on to guarentee that
     * the shutters close
     */
    private static final int _MINIMUM_ON_TIME = 30000;

    /**
     * Default amount of time to wait after power up before sampling. This can
     * be overridden in the service properties file by setting the "startDelay"
     * to the appropriate number of milliseconds
     */
    static final int _INSTRUMENT_START_DELAY = 10000;

    /** Default current limit for the instrument (millamps) */
    static final int _INSTRUMENT_CURRENT_LIMIT = 8000;

    /** Maximum time spent in milliseconds trying to read a single frame */
    private static final int _FRAME_READ_TIME_OUT = 5000;

    // data frame status values
    private static final int _FRAME_OK = 0;

    private static final int _DATA_FRAME_TERM_OFFSET = 309;

    private static final int _DATA_FRAME_LENGTH = 311;

    // various data frame offsets and lengths
    private static final int _DATA_FRAME_DATA_OFFSET = 14;

    private static final int _DATA_FRAME_DATA_LENGTH = 274;

    /** Default serial data bits */
    static final int _DATA_BITS = SerialPort.DATABITS_8;

    private static final int _CHECKSUM_FRAME_OFFSET = 308;

    private static final int _BAD_FRAME_TERM = 4;

    private static final int _BAD_FRAME_PREFIX = 3;

    private static final int _BAD_CHECKSUM = 1;

    private DevicePacketParser devicePacketParser;

    /**
     * Counts number of bad checksum encountered by readFrame() during on sample
     */
    private int _badCheckSumCount = 0;

    // Configurable OCR attributes
    Attributes _attributes = new Attributes(this);

    /**
     * Constructs ...
     * 
     * 
     * @throws RemoteException
     */
    public Ocr3000() throws RemoteException {
        // Configure the instrumentServiceBlock
        try {
            DevicePacketParser p = (DevicePacketParser) getDevicePacketParser();
            if (p != null) {
                OCRServiceBlock fblock = new OCRServiceBlock();
                fblock.setAcceptedVariableNames(p.getAcceptedVariableNames());
                fblock.deviceLogBlock.setCoder(new MinimalXmlCoder());
                setInstrumentServiceBlock(fblock);
            }
        }
        catch (Exception e) {
            setInstrumentServiceBlock(new DummyBlock());
        }
    }

    /** Return specifier for default sampling schedule. */
    protected ScheduleSpecifier createDefaultSampleSchedule() throws ScheduleParseException {

        // Sample every 60 seconds by default
        return new ScheduleSpecifier(60000);
    }

    /** calculate the checksum of a data frame */
    private boolean frameCheckSumValid(byte[] frameBuffer) {
        byte calcedCheckSum = 0;

        // the check sum is the negative value of the byte stored and index
        // _CHECKSUM_FRAME_OFFSET
        byte frameCheckSum = (byte) (-frameBuffer[_CHECKSUM_FRAME_OFFSET]);

        // calculate the check sum from the fram data
        for (int i = 0; i < _CHECKSUM_FRAME_OFFSET; i++) {
            calcedCheckSum += frameBuffer[i];
        }

        _logger.debug("frameCheckSumValid(...) reported: " + frameCheckSum);
        _logger.debug("frameCheckSumValid(...) calced  : " + calcedCheckSum);

        if (calcedCheckSum != frameCheckSum) {
            return false;
        }

        return true;
    }

    /** Sets communications power policy */
    protected PowerPolicy initCommunicationPowerPolicy() {
        return PowerPolicy.WHEN_SAMPLING;
    }

    /** Sets current limit */
    protected int initCurrentLimit() {
        return _INSTRUMENT_CURRENT_LIMIT;
    }

    /** Sets power policy */
    protected PowerPolicy initInstrumentPowerPolicy() {
        return PowerPolicy.WHEN_SAMPLING;
    }

    /** Set startup delay time */
    protected int initInstrumentStartDelay() {
        return _INSTRUMENT_START_DELAY;
    }

    /** Sets the maximum number of bytes in a instrument data sample */
    protected int initMaxSampleBytes() {

        // 64K byte max
        return 65536;
    }

    /** Sets the command prompt */
    protected byte[] initPromptString() {
        return "$ ".getBytes();
    }

    /** Sets the sample terminator */
    protected byte[] initSampleTerminator() {
        return "".getBytes();
    }

    /** Initialize the OCR-3000. */
    protected void initializeInstrument() throws InitializeException, Exception {

        /*
         * all this code does is make sure the shutter is fully closed after
         * initialization
         */

        int waitTime = _MINIMUM_ON_TIME - getInstrumentStartDelay();

        // wait for shutters to get fully charged so they can close
        if (waitTime > 0) {
            StopWatch.delay(waitTime);
        }

        // cut power to close shutters
        managePowerSleep();

        // wait for shutters to close
        StopWatch.delay(_SHUTTER_CLOSE_TIME);
    }

    // make an int from 2 bytes
    private int mkInt(byte[] bytes, int offset) {
        int result = 0;
        int shifter;

        shifter = bytes[offset] & 0x000000FF;
        result = (shifter << 8);
        result += bytes[offset + 1] & 0x000000FF;

        return result;
    }

    /*
     * Reads frame of data from the OCR-3000. If the frame checksum does not
     * match the readFrame will re-synch and attempte to read another frame for
     * up to _READ_FRAME_RETRIES
     */
    private int readFrame(byte[] frameBuffer) throws Exception {
        int i;
        int byteCount;

        StopWatch sw = new StopWatch(true);

        for (i = 0; i < _READ_FRAME_RETRIES; i++) {

            // clear out byte counter and start reading
            byteCount = 0;

            while (byteCount < _DATA_FRAME_LENGTH) {
                try {
                    if (_fromDevice.available() > 0) {
                        frameBuffer[byteCount] = (byte) _fromDevice.read();
                        ++byteCount;
                    }
                }
                catch (Exception e) {
                    _logger.error("readFrame() failed: " + e);

                    throw new Exception("readFrame() failed: " + e);
                }

                if (sw.read() > _FRAME_READ_TIME_OUT) {
                    _logger.error("readFrame() timed out");

                    throw new Exception("readFrame() timed out");
                }
            }

            // if the frame checksum checks out it's good
            if (frameCheckSumValid(frameBuffer)) {
                return byteCount;
            }

            // increment bad checksum count
            _badCheckSumCount++;

            // try to resync
            syncFrame();
        }

        throw new Exception("readFrame() failed to read a valid frame");
    }

    protected int readSample(byte[] sample) throws TimeoutException, IOException, Exception {
        int totalBytes = 0;
        int frameCount = 0;
        byte[] frameBuffer = new byte[512];
        StopWatch sw = new StopWatch();

        // flush the input
        _fromDevice.flush();

        // synchronize to the next frame
        syncFrame();

        // clear the bad checksum error counter
        _badCheckSumCount = 0;

        // kick off the reading of frames
        readFrame(frameBuffer);
        ++frameCount;

        // start the StopWatch to time the cpature
        sw.start();

        // capture frames for the sample time or until you run out of buffer
        while ((sw.read() < _attributes.sampleTime) && (totalBytes < getMaxSampleBytes())) {

            // if the frame is valid stuff it, otherwise take the
            // appropriate action
            switch (verifyFrame(frameBuffer)) {

            // if you get a bad frame prefix re-sync
            case _BAD_FRAME_PREFIX:
                _logger.warn("readSample(...) got _BAD_FRAME_PREFIX");
                syncFrame();

                break;

            // if you get a bad frame terminator re-sync
            case _BAD_FRAME_TERM:
                _logger.warn("readSample(...) got _BAD_FRAME_TERM");
                syncFrame();

                break;

            // if you get a saturated frame, drop it
            case _SATURATED_FRAME:
                _logger.warn("readSample(...) got _SATURATED_FRAME");

                break;

            default:

                // stuff the frame, it's good
                System.arraycopy(frameBuffer, 0, // source
                        sample, totalBytes, // destination
                        _DATA_FRAME_LENGTH); // length

                // keep track of the bytes copied
                totalBytes += _DATA_FRAME_LENGTH;
            }

            // read another frame
            _logger.debug("reading " + frameCount + " frame");
            readFrame(frameBuffer);
            ++frameCount;
        }

        // generate a bad checksum warning if you got bad checksums
        if (_badCheckSumCount > 0) {
            _logger.warn("readFrame() had " + _badCheckSumCount + " bad checksums");
        }

        // calculate the time remaining to charge the shutter caps
        int sleepTime = _MINIMUM_ON_TIME - getInstrumentStartDelay() - (int) sw.read();

        // wait for shutters to get fully charged so they can close
        if (sleepTime > 0) {
            StopWatch.delay(sleepTime);
        }

        // cut the power in readSample and wait for the shutters to close
        managePowerSleep();
        StopWatch.delay(_SHUTTER_CLOSE_TIME);

        /* return the number of bytes copied to the sample buffer */
        return totalBytes;
    }

    /** Request a sample from the instrument */
    protected void requestSample() throws IOException {

        // this method does nothing for the OCR-3000, but it must be
        // implemeted to for the service to compile
    }

    /** ********************************************************************* */

    /* data frame manipulation utils */

    /** ********************************************************************* */

    /** Synchronize on the next data frame */
    private int syncFrame() throws Exception {
        int bytesSkipped = 0;

        for (int i = 0;; ++i) {
            try {
                bytesSkipped = StreamUtils.skipUntil(_fromDevice, "\r\n".getBytes(), _FRAME_READ_TIME_OUT);

                break;
            }
            catch (Exception e) {
                if (i > 2) {
                    _logger.error("syncFrame() failed: " + e);

                    throw new Exception("syncFrame() failed: " + e);
                }
            }
        }

        return (bytesSkipped + 2);
    }

    /**
     * Self-test routine.
     * 
     * @return an integer
     */
    public int test() {
        return Device.OK;
    }

    /**
     * The verifyFrame method determines if a frame is valid by checking it's
     * prefix, terminator. The method also makes sure the frame does not contain
     * any saturated data values. The check sum is not tested as this is done by
     * the readFrame method
     */
    private int verifyFrame(byte[] frameBuffer) {
        _logger.debug("verifyFrame(...) INSTRUMENT    : " + new String(frameBuffer, 0, 10));
        _logger.debug("verifyFrame(...) FRAME TIMER   : " + new String(frameBuffer, 298, 10));
        _logger.debug("verifyFrame(...) FRAME COUNTER : " + (int) frameBuffer[297]);

        // check for _BAD_FRAME_PREFIX, first three chars should be "SAT"
        if ((frameBuffer[0] != 0x53) || (frameBuffer[1] != 0x41) || (frameBuffer[2] != 0x54)) {
            return _BAD_FRAME_PREFIX;
        }

        // check for _BAD_FRAME_TERM it should be <CR><LF>
        if ((frameBuffer[_DATA_FRAME_TERM_OFFSET] != 0x0D) || (frameBuffer[_DATA_FRAME_TERM_OFFSET + 1] != 0x0A)) {
            return _BAD_FRAME_PREFIX;
        }

        // calculate the ending index of the data portion of the frame
        int endIndex = _DATA_FRAME_DATA_OFFSET + _DATA_FRAME_DATA_LENGTH - 1;

        // check for _SATURATED_FRAME
        for (int i = _DATA_FRAME_DATA_OFFSET; i < endIndex; i += 2) {

            // if you get a sample greater than 65534 you're saturated
            if (mkInt(frameBuffer, i) > 0xFFFE) {
                _logger.warn("verifyFrame(...) _SATURATED_FRAME at offset " + i);

                return _SATURATED_FRAME;
            }
        }

        // you ran the gauntlet, congrats!
        return _FRAME_OK;
    }

    /** Get the attention of the instrument */
    private void getInstrumentAttention(int tries) throws Exception {
        for (int i = 0; i < tries; i++) {
            _fromDevice.flush();

            // send a CTRL-C to get the masters attention
            _toDevice.write(0x03);

            // send a <CR> to get the instrument prompt
            _toDevice.write("\r".getBytes());

            try {

                // wait for the prompt string
                StreamUtils.skipUntil(_fromDevice, getPromptString(), getSampleTimeout());

                return;
            }
            catch (Exception e) {
                _logger.warn("getInstrumentAttention() failed on try " + i + ": " + e);
            }
        }

        _logger.error("getInstrumentAttention() failed");

        throw new Exception("getInstrumentAttention() failed");
    }

    /**
     * Get the prompt of the instrument. The getInstrumentAttention(...) must be
     * called at least once before this call
     */
    private void getInstrumentPrompt() throws Exception {
        try {

            // wait for instrument prompt
            StopWatch.delay(500);

            // clear out input buffer
            _fromDevice.flush();

            // send a <CR> to get the instrument prompt
            _toDevice.write('\r');

            // wait for the prompt string
            StreamUtils.skipUntil(_fromDevice, getPromptString(), getSampleTimeout());
        }
        catch (Exception e) {
            String error = "getInstrumentPrompt() failed: " + e;
            _logger.error(error);

            throw new Exception(error);
        }
    }

    /**
     * Request the id using "id" and state of all settings using "show all" from
     * the instrument you are currently connected to. The
     * getInstrumentAttention(...) must be called at least once before this call
     */
    private String getInstrumentState() {
        String metaData;
        int bytesRead = 0;
        byte[] tmpBuff = new byte[1024];

        try {
            getInstrumentPrompt();
        }
        catch (Exception e) {
            return ("getInstrumentState() failed getting prompt: " + e);
        }

        try {
            _fromDevice.flush();

            // send the id command to the master
            _toDevice.write("id\r".getBytes());

            // get the response
            bytesRead = StreamUtils.readUntil(_fromDevice, tmpBuff, getPromptString(), 2000);
        }
        catch (Exception e) {
            return ("getInstrumentState() failed on \"id\": " + e);
        }

        // get id portion of string
        metaData = "ID:\r\n" + new String(tmpBuff, 0, bytesRead) + "\r\n";

        try {

            // send the id command to the master
            _toDevice.write("show all\r".getBytes());

            // get the response
            bytesRead = StreamUtils.readUntil(_fromDevice, tmpBuff, getPromptString(), 2000);
        }
        catch (Exception e) {
            return ("getInstrumentState() failed on \"show all\": " + e);
        }

        // get state portion of string
        metaData += "STATE:\r\n" + new String(tmpBuff, 0, bytesRead) + "\r\n";

        return metaData;
    }

    /** Return instrument metadata. */
    protected byte[] getInstrumentStateMetadata() {

        // create a timer to time time it takes to get instrument state
        StopWatch sw = new StopWatch(true);

        // an array for network address
        int[] addressList = null;

        // get the attention of the instrument
        try {
            getInstrumentAttention(4);
            _fromDevice.flush();
        }
        catch (Exception e) {
            String error = "getInstrumentStateMetadata(...) failed: " + e;

            return error.getBytes();
        }

        // get the remote address of the instruments
        try {
            addressList = getRemoteAdresses();
        }
        catch (Exception e) {
            _logger.warn("failed to get remote instrument addresses: " + e);
        }

        // get the instrument state for the master node
        String metaData = "MASTER NODE:\r\n";
        metaData += getInstrumentState();

        // get the state of the remote instruments if any were found
        if (addressList != null) {
            for (int i = 0; i < addressList.length; ++i) {
                _logger.debug("geting remote instrument " + addressList[i] + " state");

                metaData += "REMOTE NODE: " + addressList[i] + "\r\n";
                String cmd = "remote " + addressList[i] + "\r";

                try {

                    // switch to a remote node
                    _toDevice.write(cmd.getBytes());

                    // append metadata string
                    metaData += getInstrumentState();

                    // exit the remote instrument
                    _toDevice.write("exit\r".getBytes());
                }
                catch (Exception e) {
                    metaData += "failed to get get remote instrument state " + "of instrument " + addressList[i] + ": "
                            + e + "\r\n";
                }

                // wait for exit from the remote instrument
                StopWatch.delay(1000);
            }
        }

        // wait the necessary amount of time to make sure the shutters will
        // close
        int waitTime = _MINIMUM_ON_TIME - getInstrumentStartDelay() - (int) sw.read();

        // wait for shutters to get fully charged so they can close
        if (waitTime > 0) {
            StopWatch.delay(waitTime);
        }

        // cut power to close shutters
        managePowerSleep();

        // wait for shutters to close
        StopWatch.delay(_SHUTTER_CLOSE_TIME);

        return metaData.getBytes();
    }

    /**
     * Query the SatNET for remote instrument addresses. The
     * getInstrumentAttention(...) must be called at least once before this call
     */
    private int[] getRemoteAdresses() throws Exception {
        int[] addressList = new int[50];
        byte[] tmpBuff = new byte[80];
        int address = -1;
        int bytesRead = 0;
        int addressCount = 0;

        // fill the address list with invalid address
        Arrays.fill(addressList, -1);

        for (int i = 0; i < 5; i++) {
            try {
                _fromDevice.flush();

                // send the id command to the master
                _toDevice.write("ping all\r".getBytes());
            }
            catch (Exception e) {
                throw new Exception("failed to ping instruments: " + e);
            }

            // wait for the ping data to get arrive back
            StopWatch.delay(2000);

            // read out reponse a line at a time and parse for address
            while (_fromDevice.available() > 20) {
                try {

                    // grab a line
                    bytesRead = StreamUtils.readUntil(_fromDevice, tmpBuff, "\r\n".getBytes(), 2000);
                }
                catch (Exception e) {
                    _logger.warn("failed to read line after " + "\"ping all\" command");
                    bytesRead = 0;
                }

                // if you got a good line look for the address
                if (bytesRead > 10) {
                    try {
                        address = Integer.parseInt(new String(tmpBuff, 0, 3));
                    }
                    catch (Exception e) {
                        _logger.warn("failed to parseInt for line: " + new String(tmpBuff, 0, bytesRead));
                        address = -1;
                    }
                }

                // add a valid address (valid address are [001, 255]) is found
                // add it
                if ((address > 0) && (addressList.length > addressCount)) {
                    _logger.debug("found instrument at address " + address);

                    // search for the address
                    int index;
                    for (index = 0; index < addressCount; index++) {
                        if (addressList[index] == address) {
                            break;
                        }
                    }

                    // if the index matches the addressCount you found a new
                    // address, so add it to the list
                    if (index == addressCount) {
                        addressList[index] = address;
                        addressCount++;
                        _logger.debug("adding instrument address " + address + " to list at index " + index);
                    }
                }
            }
        }

        // create a new array just the right size
        int[] tmpIntArray = new int[addressCount];

        // copy over the addresses
        System.arraycopy(addressList, 0, // source
                tmpIntArray, 0, // destination
                addressCount); // length

        // sort the array
        Arrays.sort(tmpIntArray);

        return tmpIntArray;
    }

    public InstrumentServiceBlock getInstrumentServiceBlock() {
        return super.getInstrumentServiceBlock();
    }

    /**
     * Return parameters to use on serial port.
     * 
     * @return SerialPortParameters for this instrument's serial port
     * 
     * @throws UnsupportedCommOperationException
     */
    public SerialPortParameters getSerialPortParameters() throws UnsupportedCommOperationException {

        return new SerialPortParameters(_attributes.baud, _DATA_BITS, _PARITY, _STOP_BITS);
    }

    /**
     * Set the instruments clock.
     * 
     * @param time
     */
    public void setClock(long time) {
        // Do nothing
    }

    public org.mbari.siam.distributed.DevicePacketParser getDevicePacketParser()
            throws NotSupportedException {
        if (devicePacketParser == null) {
            if (_logger.isInfoEnabled()) {
                _logger.info("Creating a DevicePacketParser");
            }

            URL calUrl = getClass().getResource(
                    ((Attributes) getAttributes()).calibrationFile);
            //URL calUrl = getClass().getResource("/1516.CAL");
            if (calUrl != null) {
                try {
                    devicePacketParser = new DevicePacketParser(calUrl);
                }
                catch (Exception e) {
                    _logger.error("Failed to configure a DevicePacketParser",
                                    e);
                }
            }

        }
        return devicePacketParser;
    }

    /** Configurable OCR attributes */
    class Attributes extends InstrumentServiceAttributes {

        /**
         * Amount of time the data is collected from the OCR-3000 (millisec).
         */
        int sampleTime = 30000;

        /** Instrument baud rate */
        int baud = 19200;

        String calibrationFile = "";

        /** Constructor, with required InstrumentService argument */
        Attributes(DeviceServiceIF service) {
            super(service);
        }

        /**
         * Throw InvalidPropertyException if any invalid attribute values found
         * 
         * @throws InvalidPropertyException
         */
        public void checkValues() throws InvalidPropertyException {
            if (sampleTime <= 0) {
                throw new InvalidPropertyException(sampleTime + ": invalid sampleTime." + " Must be > 0");
            }
        }

        /**
         * Throw MissingPropertyException if specified attribute is mandatory.
         * 
         * @param attributeName
         * 
         * @throws MissingPropertyException
         */
        public void missingAttributeCallback(String attributeName) throws MissingPropertyException {
        }
    }

    /**
     * This class generates JDDAC records that are used by the
     * instrumentServiceBlock
     */
    public class DevicePacketParser extends org.mbari.siam.distributed.DevicePacketParser {

        private final DataStructure dataStructure = new DataStructure();

        private final Vector acceptedVariableNames = new Vector();

        DevicePacketParser(URL url) throws IOException {
            CalibrationInfo calInfo = new CalibrationInfo(url);
            dataStructure.setCalibrationInfo(calInfo);

            /*
             * Figure out the accepted fields. We're accepting every 5th OPTIC3
             * field.
             */
            SensorDefinition[] sd = calInfo.sensorDefinitions;
            Vector fields = new Vector();
            for (int i = 0; i < sd.length; i++) {
                if (sd[i].fitType.equalsIgnoreCase("OPTIC3")) {
                    fields.add(sd[i]);
                }
            }

            int n = 0;
            for (Iterator i = fields.iterator(); i.hasNext();) {
                n++;
                SensorDefinition s = (SensorDefinition) i.next();
                if (n % 5 == 0) {
                    acceptedVariableNames.add(s.name);
                }
            }
        }

        public Vector getAcceptedVariableNames() {
            return acceptedVariableNames;
        }
        
        public DataStructure getDataStructure() {
            return dataStructure;
        }

        protected void parseFields(DevicePacket packet) throws NotSupportedException, Exception {

            /*
             * Generate a JDDAC record for the first record in the packet.
             */
            int packetSize = dataStructure.getCalibrationInfo().getPacketSize();
            byte[] data = ((SensorDataPacket) packet).dataBuffer();
            byte[] buf = new byte[packetSize];
            // int numberOfRecords = data.length / packetSize;
            int src = 0;
            System.arraycopy(data, src, buf, 0, packetSize);
            dataStructure.setData(buf);

            Measurement[] m = dataStructure.getMeasurements();
            for (int j = 0; j < m.length; j++) {
                String name = (String) m[j].get(MeasAttr.NAME);
                if (acceptedVariableNames.contains(name)) {
                    _record.put(m[j].get(MeasAttr.NAME), m[j]);
                }
            }

        }

    }

    /**
     * A structure encapsulating access to engineering units for the
     * measurements in a single record.
     */
    public static class DataStructure {

        private byte[] data;

        private CalibrationInfo calibrationInfo;

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            this.data = data;
        }

        Measurement[] getMeasurements() {
            Measurement[] ma = new Measurement[calibrationInfo.getFieldCount()];
            for (int i = 0; i < ma.length; i++) {
                ma[i] = calibrationInfo.calibrate(data, i);
            }
            return ma;
        }

        public CalibrationInfo getCalibrationInfo() {
            return calibrationInfo;
        }

        public void setCalibrationInfo(CalibrationInfo calibrationInfo) {
            this.calibrationInfo = calibrationInfo;
        }

    }

    /**
     * Encapulates the calibration information contained in a single calibration
     * file.
     */
    public static class CalibrationInfo {
        /** The URL of the calibration file */
        final URL url;

        /**
         * The Sensor definitions that make up the calibration file. These are
         * stored in the order that they are read from the file
         */
        final SensorDefinition[] sensorDefinitions;

        /**
         * The offset in bytes from the start of the file for each field in the
         * data record each offset corresponds to the value represented by the
         * corresponding sensor definition.
         */
        final int[] fieldOffset;

        CalibrationInfo(URL url) throws IOException {
            this.url = url;

            /*
             * No error checking is being done...you'd better hope your calfile
             * is formatted correctly!
             */
            Vector sensorCals = new Vector();
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            while ((line = in.readLine()) != null) {

                // Skip comments and blank lines
                if (line.startsWith("#") || line.trim().length() == 0) {
                    continue;
                }

                // Parse the line into it's fields
                SensorDefinition sd = new SensorDefinition(line);
                // If it has coeffieicients grab them
                if (sd.calLines == 1) {
                    sd.addCoefficientLine(in.readLine());
                }
                sensorCals.add(sd);
            }
            in.close();
            SensorDefinition[] sdBuf = new SensorDefinition[sensorCals.size()];
            sensorDefinitions = (SensorDefinition[]) sensorCals.toArray(sdBuf);

            /*
             * Calculate offsets from the start of a data record. The offset is
             * the index in bytes to the start of a particular field in the data
             * buffer
             */
            fieldOffset = new int[sensorDefinitions.length];
            for (int i = 1; i < fieldOffset.length; i++) {
                fieldOffset[i] = fieldOffset[i - 1] + sensorDefinitions[i - 1].fieldLength;
            }
        }

        /**
         * @param data the byte array to convert
         * @param fieldIndex The index of the field in the data range, i.e
         *         first value, second value, etc,
         * @return A JDDAC {@link Measurement} containing the value in
         *         engineering units.
         */
        Measurement calibrate(byte[] data, int fieldIndex) {
            SensorDefinition sd = sensorDefinitions[fieldIndex];
            Measurement m = new Measurement();
            m.put(MeasAttr.NAME, sd.name);
            m.put("Satlantic Type", sd.type);
            m.put("Satlantic Id", sd.id);
            m.put(MeasAttr.UNITS, sd.units);
            m.put(MeasAttr.DATA_TYPE, getType(fieldIndex));
            m.put(MeasAttr.VALUE, applyFit(data, fieldIndex));
            return m;

        }

        /**
         * 
         * @param fieldIndex
         *            The index of the variable. Index is the 0 based count of
         *            the variable in the order it occurs in the calibration
         *            file.
         * @return The JDDAC {@link TypeAttr} for the variable
         */
        String getType(int fieldIndex) {
            SensorDefinition sd = sensorDefinitions[fieldIndex];
            String type = "Unknown";
            if (sd.dataType.equals("BU")) {
                type = TypeAttr.INTEGER64;
            }
            else if (sd.dataType.equals("BS")) {
                type = TypeAttr.INTEGER32;
            }
            else if (sd.dataType.equals("BF")) {
                type = TypeAttr.FLOAT32;
            }
            else if (sd.dataType.equals("BD")) {
                type = TypeAttr.FLOAT64;
            }
            else if (sd.dataType.equals("AI")) {
                type = TypeAttr.INTEGER32;
            }
            else if (sd.dataType.equals("AF")) {
                type = TypeAttr.FLOAT32;
            }
            else if (sd.dataType.equals("AS")) {
                type = TypeAttr.STRING;
            }
            return type;

        }

        /**
         * @param data
         *            The data bytes for a sample (entire record)
         * @param fieldIndex
         *            THe index fo the field to get the value of
         * @return The uncalibrated value of that field. THe value depends on
         *         the data-type specifed in the calibration file for that
         *         field.
         */
        Object getValue(byte[] data, int fieldIndex) {
            SensorDefinition sd = sensorDefinitions[fieldIndex];
            byte[] buf = new byte[sd.fieldLength];
            System.arraycopy(data, fieldOffset[fieldIndex], buf, 0, sd.fieldLength);

            Object value = null;
            if (sd.dataType.equals("BU")) {
                value = new Integer((int) NumberUtil.toLong(buf, false));
            }
            else if (sd.dataType.equals("BS")) {
                value = new Integer(NumberUtil.toInt(buf, false));
            }
            else if (sd.dataType.equals("BF")) {
                value = new Float(NumberUtil.toFloat(buf, false));
            }
            else if (sd.dataType.equals("BD")) {
                value = new Double(NumberUtil.toDouble(buf, false));
            }
            else if (sd.dataType.equals("AI")) {
                value = Integer.valueOf(new String(buf));
            }
            else if (sd.dataType.equals("AF")) {
                // value = Float.valueOf(new String(buf));
                value = new Float(0);
            }
            else if (sd.dataType.equals("AS")) {
                value = new String(buf);
            }
            return value;
        }

        /**
         * 
         * @param data
         *            A complete data record as provided by the instrument
         * @param fieldIndex
         *            THe index fo the field to get the value of
         * @return An Object representing the calibrated value for the field.
         */
        Object applyFit(byte[] data, int fieldIndex) {
            SensorDefinition sd = sensorDefinitions[fieldIndex];
            Object value = getValue(data, fieldIndex);
            if (sd.fitType.equals("NONE") || sd.fitType.equals("COUNT")) {
                // Do nothing
            }
            else if (sd.fitType.equals("POLYU")) {
                float[] c = sd.getCoefficients();
                double a = ((Number) value).doubleValue();
                double y = 0;
                for (int i = 0; i < c.length; i++) {
                    y += c[i] * Math.pow(a, i);
                }
                value = new Double(y);
            }
            else if (sd.fitType.equals("OPTIC3")) {
                float[] c = sd.getCoefficients();
                float a0 = c[0];
                float a1 = c[1];
                float Im = c[2];
                float CInt = c[3];
                float AInt = ((Integer) applyFit(data, 1)).floatValue();
                float y = Im * a1 * (((Number) value).floatValue() - a0) * (CInt / AInt);
                value = new Float(y);
            }
            return value;
        }

        /**
         * 
         * @return The number of fields in a data record
         */
        int getFieldCount() {
            return sensorDefinitions.length;
        }

        /**
         * @return The expected size of a data record in bytes.
         */
        int getPacketSize() {
            return fieldOffset[fieldOffset.length - 1] + sensorDefinitions[sensorDefinitions.length - 1].fieldLength;
        }

    }

    /**
     * A represents a single line and related
     * coefficeints from a HyperOCR calibration file.
     * 
     * @author brian
     * 
     */
    public static class SensorDefinition {

        /** A unique name for the variable */
        final String name;

        final String type;

        final String id;

        final String units;

        final int fieldLength;

        final String dataType;

        final int calLines;

        final String fitType;

        private float[] coefficients;

        SensorDefinition(String line) {
            StringTokenizer tok = new StringTokenizer(line);
            type = tok.nextToken();
            id = tok.nextToken();
            units = tok.nextToken();
            fieldLength = Integer.parseInt(tok.nextToken());
            dataType = tok.nextToken();
            calLines = Integer.parseInt(tok.nextToken());
            fitType = tok.nextToken();
            name = type + id;
        }

        void addCoefficientLine(String line) {
            StringTokenizer tok = new StringTokenizer(line);
            coefficients = new float[tok.countTokens()];
            int i = 0;
            while (tok.hasMoreTokens()) {
                coefficients[i] = Float.parseFloat(tok.nextToken());
                i++;
            }
        }

        public float[] getCoefficients() {
            return coefficients;
        }
    }

    /**
     * A specialized InstrumentService Block. This block takes the first sample
     * out of several samples that may be in a SensorDataPacket. That sample is
     * decimated by taking every nth wavelength.
     * 
     * @author brian
     * 
     */
    public class OCRServiceBlock extends InstrumentServiceBlock {

        public static final String OpIdFilter = "Filter By Measurement Name";

        final FilterByMeasurementNameFunction function = new FilterByMeasurementNameFunction();

        DeviceLogBlock deviceLogBlock;

        public OCRServiceBlock() {
            addFunction(FunctionFactory.createFunctionArg(OpIdFilter, OpIdFilter, function));
            addChild(getDeviceLogBlock());
        }

        public void processDevicePacket(DevicePacket packet) throws Exception {
            Record record = getInstrumentService().getDevicePacketParser().parse(packet);
            perform(OpIdFilter, record);
        }

        public void setInstrumentService(BaseInstrumentService instrumentService) {
            super.setInstrumentService(instrumentService);
            getDeviceLogBlock().setInstrumentService(instrumentService);
        }

        DeviceLogBlock getDeviceLogBlock() {
            if (deviceLogBlock == null) {
                deviceLogBlock = new DeviceLogBlock();
            }
            return deviceLogBlock;
        }

        public void setAcceptedVariableNames(Vector names) {
            function.getAllowedNames().clear();
            Enumeration e = names.elements();
            while (e.hasMoreElements()) {
                String name = (String) e.nextElement();
                function.getAllowedNames().add(name);
            }
        }

    }
}
