package moos.devices.workhorse;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class WorkhorseTest01 {
    
    private static final Logger log = Logger.getLogger(WorkhorseTest01.class);
    
    public static String dumpBytes(byte[] bs) {
        StringBuffer ret = new StringBuffer(bs.length);
        for (int i = 0; i < bs.length; i++) {
            String hex = Integer.toHexString(0x0100 + (bs[i] & 0x00FF)).substring(1);
            ret.append(hex.length() < 2 ? "0" : "").append(hex);
        }
        return ret.toString();
    }
    
    public static String formatBytes(byte[] bs) {
        StringBuffer ret = new StringBuffer(bs.length);
        int n = 0;
        for (int i = 0; i < bs.length; i++) {
            String hex = Integer.toHexString(0x0100 + (bs[i] & 0x00FF))
            .substring(1);
            ret.append(hex.length() < 2 ? "0" : "").append(hex);
            if (n == 1) {
                ret.append(" ");
                n = -1;
            }
            n++;
        }
        return ret.toString();
    }
    
    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        File dataSource = new File(args[0]);
        byte[] buf = new byte[(int) dataSource.length()];
        InputStream in = new FileInputStream(dataSource);
        in.read(buf);
        
        log.info("ADCP data = \n" + formatBytes(buf));
        
        WorkhorseADCP.PD0DataStructure parser = new WorkhorseADCP.PD0DataStructure();
        parser.setData(buf);
        
        log.info("---------- HEADER ----------");
        log.info("Header length = " + parser.getHeaderLength() + " bytes");
        int n = parser.getNumberOfDataTypes();
        log.info("Number of bytes in ensemble = "
                + parser.getNumberOfBytesInEnsemble());
        log.info("Number of data types = " + n);
        log.info("Offset for data type 1 = " + parser.getOffsetForDataType(1)
        + " (Fixed Leader)");
        log.info("Offset for data type 2 = " + parser.getOffsetForDataType(2)
        + " (Variable Leader)");
        for (int i = 3; i <= n; i++) {
            log.info("Offset for data type " + i + " = "
                    + parser.getOffsetForDataType(i));
        }
        
        log.info("---------- FIXED LEADER ----------");
        log.info("Number of beams = " + parser.getNumberOfBeams());
        log.info("Number of cells = " + parser.getNumberOfCells());
        log.info("Depth cell length = " + parser.getDepthCellLength());
        log.info("Pings per ensemble = " + parser.getPingsPerEnsemble());
        
        log.info("---------- VARIABLE LEADER ----------");
        log.info("Speed of sound = " + parser.getSpeedOfSound() + " [m/s]");
        log.info("Depth of transducer = " + parser.getDepthOfTransducer()
        + " [meters]");
        log.info("Heading = " + parser.getHeading() + " [degrees]");
        log.info("Pitch = " + parser.getPitch() + " [degrees]");
        log.info("Roll = " + parser.getRoll() + " [degrees]");
        log.info("Salinity = " + parser.getSalinity() + " [psu]");
        log.info("Temperature = " + parser.getTemperature() + " [C]");
        
        int nBeams = parser.getNumberOfBeams();
        log.info("---------- VELOCITY ----------");
        for (int i = 1; i <= nBeams; i++) {
            int[] v = parser.getVelocity(i);
            StringBuffer sb = new StringBuffer("Velocity for beam #");
            sb.append(i).append(" = [");
            for (int j = 0; j < v.length; j++) {
                sb.append(v[j]).append(" ");
            }
            sb.append("]");
            log.info(sb.toString());
        }
        
        log.info("---------- CORRELATION MAGNITUDE ----------");
        for (int i = 1; i <= nBeams; i++) {
            int[] v = parser.getCorrelationMagnitude(i);
            StringBuffer sb = new StringBuffer("Correlation Magnitude for beam #");
            sb.append(i).append(" = [");
            for (int j = 0; j < v.length; j++) {
                sb.append(v[j]).append(" ");
            }
            sb.append("]");
            log.info(sb.toString());
        }
        
        log.info("---------- ECHO INTENSITY ----------");
        for (int i = 1; i <= nBeams; i++) {
            float[] v = parser.getEchoIntensity(i);
            StringBuffer sb = new StringBuffer("Echo Intensity for beam #");
            sb.append(i).append(" = [");
            for (int j = 0; j < v.length; j++) {
                sb.append(v[j]).append(" ");
            }
            sb.append("]");
            log.info(sb.toString());
        }
        
        log.info("---------- PERCENT GOOD ----------");
        for (int i = 1; i <= nBeams; i++) {
            int[] v = parser.getPercentGood(i);
            StringBuffer sb = new StringBuffer("Percent Good for beam #");
            sb.append(i).append(" = [");
            for (int j = 0; j < v.length; j++) {
                sb.append(v[j]).append(" ");
            }
            sb.append("]");
            log.info(sb.toString());
        }
        
    }
    
}
