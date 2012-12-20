package moos.devices.nortek;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Test class used for verifying the Aquadopp Profiler Velocity Data
 *
 * @author brian
 * @version $Id: AquadoppParserTest.java,v 1.1 2008/11/04 22:15:20 bobh Exp $
 * @since Oct 12, 2006 10:24:07 AM PST
 */
public class AquadoppParserTest extends TestCase {

    private static final Log log = LogFactory.getLog(AquadoppParserTest.class);

    public AquadoppParserTest(String arg) {
        super(arg);
    }

    public void testHardWareConfiguration() {
        URL url = getClass().getResource("/aquadopp/singleHardwareConfigurationDatum.binary");
        byte[] bytes = new byte[1476];
        try {
            InputStream inputStream = url.openStream();
            inputStream.read(bytes);
        }
        catch (IOException e) {
            fail("Unable to read " + url.toExternalForm() + ". Reason: " + e.getLocalizedMessage());
        }
        HardwareConfiguration hardwareConfiguration = new HardwareConfiguration();
        hardwareConfiguration.setBytes(bytes);


    }

    public void testInstrumentConfiguration() {
        URL url = getClass().getResource("/aquadopp/singleInstrumentConfigurationDatum.binary");
        byte[] bytes = new byte[1024];
        try {
            InputStream inputStream = url.openStream();
            inputStream.read(bytes);
        }
        catch (IOException e) {
            fail("Unable to read " + url.toExternalForm() + ". Reason: " + e.getLocalizedMessage());
        }
        InstrumentConfiguration instrumentConfiguration = new InstrumentConfiguration();
        instrumentConfiguration.setBytes(bytes);
    }

    public void testProfilerVelocityData() {
        URL url = getClass().getResource("/aquadopp/singleHRProfileDatum.binary");
        File file = new File(url.getFile());
        byte[] bytes = new byte[(int) file.length()];
        try {
            InputStream inputStream = new FileInputStream(file);
            inputStream.read(bytes);
            inputStream.close();
        }
        catch (IOException e) {
            fail("Unable to read " + url.toExternalForm());
        }

        HRProfilerData profilerData = new HRProfilerData();
        profilerData.setBytes(bytes);
        profilerData._nBeams = 3;
        profilerData._nCells = 15;
        if (log.isInfoEnabled()) {
            log.info(profilerData.toString());
        }

    }



    public static Test suite() {
        return new TestSuite(AquadoppParserTest.class);
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(AquadoppParserTest.class);
    }




}
