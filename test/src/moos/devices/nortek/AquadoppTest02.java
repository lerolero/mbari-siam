/*
 * AquadoppTest02.java
 *
 * Created on April 10, 2006, 10:20 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package moos.devices.nortek;

import moos.jddac.xml.Coder;
import moos.jddac.xml.XmlCoder;
import net.java.jddac.common.meas.MeasAttr;
import net.java.jddac.common.meas.collection.Record;
import net.java.jddac.common.type.ArgArray;
import net.java.jddac.common.type.TimeRepresentation;
import net.java.jddac.jmdi.fblock.FunctionBlock;

import org.apache.log4j.Logger;
import org.mbari.isi.interfaces.SummaryPacket;
import org.mbari.jddac.AggregationBlock;
import org.mbari.jddac.NewArgArrayEvent;
import org.mbari.jddac.NewArgArrayListener;
import org.mbari.jddac.SampleCountFilter;
import org.mbari.jddac.StatsBlock;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author brian
 */
public class AquadoppTest02 extends AquadoppTest01 {

    private static final Logger log = Logger.getLogger(AquadoppTest02.class);

    private final StatsBlock statsBlock = new StatsBlock();
    
    private final Coder coder = new XmlCoder();

    /**
     * Creates a new instance of AquadoppTest02
     */
    public AquadoppTest02(long deviceID, String inputDirectory, final File summaryFile) throws FileNotFoundException {
        super(deviceID, inputDirectory);

        final OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(summaryFile));
        try {
            outputStream.write("<?xml version=\"1.0\" ?>".getBytes());
        }
        catch (IOException e) {
            log.error("Cant write to file", e);
        }

        final SampleCountFilter filter = new SampleCountFilter(statsBlock, 5);
        statsBlock.addFilter(filter);
        statsBlock.addAcceptedVariableName("amplitude-0");
        statsBlock.addAcceptedVariableName("amplitude-1");
        statsBlock.addAcceptedVariableName("amplitude-2");
        statsBlock.addAcceptedVariableName("amplitude-3");
        statsBlock.addAcceptedVariableName("velocity-0");
        statsBlock.addAcceptedVariableName("velocity-1");
        statsBlock.addAcceptedVariableName("velocity-2");
        statsBlock.addAcceptedVariableName("velocity-3");
        statsBlock.addAcceptedVariableName("voltage");
        statsBlock.addAcceptedVariableName(DevicePacketParser.SYSTEM_TIME);

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

                /*
                * Encode the data as XML
                */
                log.debug("Encoding " + argArray);
                SummaryPacket packet = new SummaryPacket(1500);
                byte[] data = coder.encode(argArray).getBytes();
                packet.setData(System.currentTimeMillis(), data);
                packet.setRecordType(1000L);
                log.debug("Summary: " + new String(data));
                try {
                    outputStream.write(data);
                }
                catch (IOException e) {
                    log.error("Failed to write to " + summaryFile.getAbsolutePath(), e);
                }
                return argArray;
            }

        });
    }


    public void run() throws Exception {

        log.info("Starting Test");

        Record[] siamRecords = readDeviceLog();

        log.debug("Found " + siamRecords.length + " records");

        Record siamRecord = null;
        for (int i = 0; i < siamRecords.length; i++) {

            siamRecord = siamRecords[i];

            // TODO run the siam record through the function chain
            if (log.isDebugEnabled()) {
                log.debug("Adding a record");
            }

            statsBlock.perform(AggregationBlock.OpIdAddArgArray, siamRecord);
        }
    }

    public static void main(String[] args) throws Exception {

        //log.debug("Calling AquadoppTest01(" + Long.parseLong(args[0]) + "," + args[1] + ")");

        //AquadoppTest02 test = new AquadoppTest02(Long.parseLong(args[0]), args[1]);
        //AquadoppTest02 test = new AquadoppTest02(1474L, "C:/Documents and Settings/brian/workspace/siam/test/resources/aquadopp");
        AquadoppTest02 test = new AquadoppTest02(1474L, "/Users/brian/workspace/siam/test/resources/aquadopp/nortek-mse", 
                new File("/Users/brian/workspace/siam/test/aquadoppSummary1474.xml"));

        test.run();
        System.exit(0);
    }

}
