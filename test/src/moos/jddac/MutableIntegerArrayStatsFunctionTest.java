/*
 * MutableIntegerArrayStatsFunctionTest.java
 * JUnit based test
 *
 * Created on June 15, 2006, 4:01 PM
 */

package moos.jddac;

import junit.framework.*;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;
import junit.textui.TestRunner;
import net.java.jddac.common.meas.MeasAttr;
import net.java.jddac.common.meas.Measurement;
import net.java.jddac.common.type.ArgArray;
import org.apache.log4j.Logger;

/**
 *
 * @author brian
 */
public class MutableIntegerArrayStatsFunctionTest extends TestCase {
    
    public MutableIntegerArrayStatsFunctionTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(MutableIntegerArrayStatsFunctionTest.class);
        
        return suite;
    }

    /**
     * Test of execute method, of class moos.jddac.MutableIntegerArrayStatsFunction.
     */
    public void testExecute() {
        final double tolerance = 0.0000001;
        final int[] data = {50, 90, 82, 64, 82, 66, 34, 29, 34, 53, 73, 31, 84,
                57, 37, 70, 55, 44, 69, 62};
        final double expectedMean = 58.30000000000000;
        final int expectedMax = 90;
        final int expectedMin = 29;
        final double expectedStd = 19.07630108689561;

        final ArgArray argArray = new ArgArray();
        for (int i = 0; i < data.length; i++) {
            final Measurement m = new Measurement();
            m.put(MeasAttr.NAME, "snotvolume");
            m.put(MeasAttr.UNITS, "boogers");
            m.put(MeasAttr.VALUE, new MutableIntegerArray(new int[] {data[i], data[i]}));
            argArray.put(String.valueOf(i), m);
        } 

        MutableIntegerArrayStatsFunction instance = new MutableIntegerArrayStatsFunction();
        
        ArgArray result = instance.execute(argArray);
        final double mean = ((MutableDoubleArray) result.get(NumberStatsFunction.MEAN)).get(0);
        final int max = ((MutableIntegerArray) result.get(NumberStatsFunction.MAX_VALUE)).get(0);
        final int min = ((MutableIntegerArray) result.get(NumberStatsFunction.MIN_VALUE)).get(0);
        final double std = ((MutableDoubleArray) result.get(NumberStatsFunction.STD_DEV)).get(0);
        final int count = ((Integer) result.get(NumberStatsFunction.NSAMPLES)).intValue();
        
        double dMean = Math.abs(mean - expectedMean);
        double dMax = Math.abs(max - expectedMax);
        double dMin = Math.abs(min - expectedMin);
        double dStd = Math.abs(std - expectedStd);
        int dCount = Math.abs(count - data.length);
        
        assertTrue("Mean is not within tolerance of expected value. Expected " +
                expectedMean + ", Found + " + mean, dMean <= tolerance);
        assertTrue("Max is not within tolerance of expected value. Expected " +
                expectedMax + ", Found + " + max, dMax <= tolerance);
        assertTrue("Min is not within tolerance of expected value. Expected " +
                expectedMin + ", Found + " + min, dMin <= tolerance);
        assertTrue("Std is not within tolerance of expected value. Expected " +
                expectedStd + ", Found + " + std, dStd <= tolerance);
        assertTrue("Count is not expected value. Expected " +
                data.length + ", Found + " + count, dCount == 0);

    }
    
    public static void main(String[] args) {
        TestRunner.run(suite());
    }
    
}
