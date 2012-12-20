package moos.devices.nortek;

import org.apache.log4j.Logger;
import moos.jddac.SiamRecord;

import java.net.URL;
import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author brian
 * @version $Id: LogParserTest.java,v 1.1 2008/11/04 22:15:20 bobh Exp $
 * @since Oct 12, 2006 1:49:55 PM PST
 */
public class LogParserTest extends TestCase {

    private static final Logger log = Logger.getLogger(LogParserTest.class);


    public void testReadDeviceLog() {
        final URL url = getClass().getResource("/aquadopp/nortek-mse/1474_0.dat");
        final File file = new File(url.getFile());
        final File dir = file.getParentFile();
        SiamRecord[] siamRecords = null;
        try {
            siamRecords = LogParser.readDeviceLog(1474, dir.getAbsolutePath());
        }
        catch (IOException e) {
            fail("Failed to read " + url.toExternalForm());
        }

        if (siamRecords == null || siamRecords.length == 0) {
            fail("No records were read from " + url.toExternalForm());
        }

        
    }

    public void testCreateSummaries() {
        final URL url = getClass().getResource("/aquadopp/nortek-mse/1474_0.dat");
        final File file = new File(url.getFile());
        final File dir = file.getParentFile();
        SiamRecord[] siamRecords = null;
        try {
            siamRecords = LogParser.readDeviceLog(1474, dir.getAbsolutePath());
        }
        catch (IOException e) {
            fail("Failed to read " + url.toExternalForm());
        }

        if (siamRecords == null || siamRecords.length == 0) {
            fail("No records were read from " + url.toExternalForm());
        }

        LogParser.createSummaries(siamRecords, 10);
    }

     public static Test suite() {
        return new TestSuite(LogParserTest.class);
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(LogParserTest.class);
    }

}
