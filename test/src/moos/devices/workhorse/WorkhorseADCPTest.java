package moos.devices.workhorse;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;

import java.net.URL;
import java.io.*;

/**
 * @author brian
 * @version $Id: WorkhorseADCPTest.java,v 1.1 2008/11/04 22:15:20 bobh Exp $
 * @since Oct 13, 2006 2:12:31 PM PST
 */
public class WorkhorseADCPTest extends TestCase {

    private static final Logger log = Logger.getLogger(WorkhorseADCPTest.class);

    public WorkhorseADCPTest(String arg) {
        super(arg);
    }

    public void testPD0DataFormat() {
        final URL url = getClass().getResource("/workhorse/singleDatum.binary");
        final File file = new File(url.getFile());
        byte[] data = new byte[(int) file.length()];

        try {
            InputStream inputStream = new FileInputStream(file);
            inputStream.read(data);
            inputStream.close();
        }
        catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        WorkhorseADCP.PD0DataStructure d = new WorkhorseADCP.PD0DataStructure();
        d.setData(data);
        int binOneDistance = d.getBinOneDistance();
        int depthCellLength = d.getDepthCellLength();
        float depthOfTransducer = d.getDepthOfTransducer();
        int errorVelocityMaximum = d.getErrorVelocityMaximum();
        int headerLength = d.getHeaderLength();

        int numberOfBeams = d.getNumberOfBeams();
        for (int i = 1; i <= numberOfBeams; i++) {
            int[] velocity = d.getVelocity(i);
            int[] correlation = d.getCorrelationMagnitude(i);
            float[] echoIntensity = d.getEchoIntensity(i);
        }

        if (log.isInfoEnabled()) {
            WorkhorseADCPUtil.log(d);
        }

    }

    public void testMultiRecords() {
        final URL url = getClass().getResource("/workhorse/adcp_1294.binary");
        final File file = new File(url.getFile());
        byte[] data = new byte[(int) file.length()];

         try {
            InputStream inputStream = new FileInputStream(file);
            inputStream.read(data);
            inputStream.close();
        }
        catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        byte[] datum = new byte[1347];
        WorkhorseADCP.PD0DataStructure dataStructure = new WorkhorseADCP.PD0DataStructure();
        boolean ok = true;
        while (ok) {
            inputStream.mark(1350);
            int i = inputStream.read();
            if (i == 127) {
                i = inputStream.read();
                if (i == 127) {
                    inputStream.reset();
                    try {
                        inputStream.read(datum);
                    }
                    catch (IOException e) {
                        break;
                    }
                    dataStructure.setData(datum);
                    WorkhorseADCPUtil.log(dataStructure);
                }
            }

            ok = i > -1;
        }
    }

    public static Test suite() {
        return new TestSuite(WorkhorseADCPTest.class);
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(WorkhorseADCPTest.class);
    }
}
