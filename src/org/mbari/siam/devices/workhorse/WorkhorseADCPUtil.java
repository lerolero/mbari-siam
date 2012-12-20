/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.workhorse;

// import org.apache.commons.logging.Log;
// import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.mbari.util.NumberUtil;
import org.jboss.mq.selectors.parser;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;

/**
 * @author brian
 * @version $Id: WorkhorseADCPUtil.java,v 1.3 2012/12/17 21:34:50 oreilly Exp $
 * @since Oct 13, 2006 2:41:36 PM PST
 */
public class WorkhorseADCPUtil {

    static private Logger _log4j = Logger.getLogger(WorkhorseADCPUtil.class);
    

    public static void log(WorkhorseADCP.PD0DataStructure dataStructure) {

        StringBuffer sb = new StringBuffer();

        sb.append("ADCP data = \n" + NumberUtil.toHexString(dataStructure.getData()) + "\n");

        sb.append("---------- HEADER ----------\n");
        sb.append("Header length = " + dataStructure.getHeaderLength() + " bytes\n");
        int n = dataStructure.getNumberOfDataTypes();
        sb.append("Number of bytes in ensemble = "
                + dataStructure.getNumberOfBytesInEnsemble() + "\n");
        sb.append("Number of data types = " + n);
        sb.append("Offset for data type 1 = " + dataStructure.getOffsetForDataType(1)
        + " (Fixed Leader)\n");
        sb.append("Offset for data type 2 = " + dataStructure.getOffsetForDataType(2)
        + " (Variable Leader)\n");
        for (int i = 3; i <= n; i++) {
            sb.append("Offset for data type " + i + " = "
                    + dataStructure.getOffsetForDataType(i) + "\n");
        }

        sb.append("---------- FIXED LEADER ----------\n");
        sb.append("Number of beams = " + dataStructure.getNumberOfBeams() + "\n");
        sb.append("Number of cells = " + dataStructure.getNumberOfCells() + "\n");
        sb.append("Depth cell length = " + dataStructure.getDepthCellLength() + "\n");
        sb.append("Pings per ensemble = " + dataStructure.getPingsPerEnsemble() + "\n");

        sb.append("---------- VARIABLE LEADER ----------\n");
        sb.append("Speed of sound = " + dataStructure.getSpeedOfSound() + " [m/s]\n");
        sb.append("Depth of transducer = " + dataStructure.getDepthOfTransducer()
        + " [meters]\n");
        sb.append("Heading = " + dataStructure.getHeading() + " [degrees]\n");
        sb.append("Pitch = " + dataStructure.getPitch() + " [degrees]\n");
        sb.append("Roll = " + dataStructure.getRoll() + " [degrees]\n");
        sb.append("Salinity = " + dataStructure.getSalinity() + " [psu]\n");
        sb.append("Temperature = " + dataStructure.getTemperature() + " [C]\n");

        int nBeams = dataStructure.getNumberOfBeams();
        sb.append("---------- VELOCITY ----------\n");
        for (int i = 1; i <= nBeams; i++) {
            int[] v = dataStructure.getVelocity(i);
            StringBuffer sb2 = new StringBuffer("Velocity for beam #");
            sb2.append(i).append(" = [");
            for (int j = 0; j < v.length; j++) {
                sb2.append(v[j]).append(" ");
            }
            sb2.append("]\n");
            sb.append(sb2.toString());
        }

        sb.append("---------- CORRELATION MAGNITUDE ----------\n");
        for (int i = 1; i <= nBeams; i++) {
            int[] v = dataStructure.getCorrelationMagnitude(i);
            StringBuffer sb2 = new StringBuffer("Correlation Magnitude for beam #");
            sb2.append(i).append(" = [");
            for (int j = 0; j < v.length; j++) {
                sb2.append(v[j]).append(" ");
            }
            sb2.append("]\n");
            sb.append(sb2.toString());
        }

        sb.append("---------- ECHO INTENSITY ----------\n");
        for (int i = 1; i <= nBeams; i++) {
            float[] v = dataStructure.getEchoIntensity(i);
            StringBuffer sb2 = new StringBuffer("Echo Intensity for beam #");
            sb2.append(i).append(" = [");
            for (int j = 0; j < v.length; j++) {
                sb2.append(v[j]).append(" ");
            }
            sb2.append("]\n");
            sb.append(sb2.toString());
        }

        sb.append("---------- PERCENT GOOD ----------");
        for (int i = 1; i <= nBeams; i++) {
            int[] v = dataStructure.getPercentGood(i);
            StringBuffer sb2 = new StringBuffer("Percent Good for beam #");
            sb2.append(i).append(" = [");
            for (int j = 0; j < v.length; j++) {
                sb2.append(v[j]).append(" ");
            }
            sb2.append("]\n");
            sb.append(sb2.toString());
        }
        _log4j.info(sb.toString());
    }
}
