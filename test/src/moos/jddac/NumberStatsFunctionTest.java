/*
 * NumberStatsFunctionTest.java
 * JUnit based test
 *
 * Created on April 10, 2006, 9:06 PM
 */

package moos.jddac;

import junit.framework.*;
import junit.textui.TestRunner;
import net.java.jddac.common.meas.MeasAttr;
import net.java.jddac.common.meas.Measurement;
import net.java.jddac.common.type.ArgArray;

/**
 *
 * @author brian
 */
public class NumberStatsFunctionTest extends TestCase {
    
    public NumberStatsFunctionTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(NumberStatsFunctionTest.class);
        
        return suite;
    }

    /**
     * Test of execute method, of class moos.jddac.NumberStatsFunction.
     */
    public void testExecute() { 
        final double tolerance = 0.0000001;
        final double[] data = {2.02619566744940, 12.35038462759501, 28.46082740563152, 
                0.34514552313232, 4.86118086849325, 7.09678264960956, 
                6.95526099315214, 21.13273677178368, 9.52657737394861, 
                6.95849937163718, 0.53458744601627,  26.13749867975503,  
                15.57837513007814,  32.61351024615826,  16.30980195863984,
                14.65273137046272,  29.61774962385136,  18.38033737068103,   
                7.09265751776356,  23.52481139660010};
        final double expectedMean = 14.20778259962195;
        final double expectedMax = 32.61351024615826;
        final double expectedMin = 0.34514552313232;
        final double expectedStd = 10.07069127423970;

        final ArgArray argArray = new ArgArray();
        for (int i = 0; i < data.length; i++) {
            final Measurement m = new Measurement();
            m.put(MeasAttr.NAME, "snotvolume");
            m.put(MeasAttr.UNITS, "boogers");
            m.put(MeasAttr.VALUE, new Double(data[i]));
            argArray.put(String.valueOf(i), m);
        }

        NumberStatsFunction instance = new NumberStatsFunction();
        
        ArgArray result = instance.execute(argArray);
        final double mean = ((Double) result.get(NumberStatsFunction.MEAN)).doubleValue();
        final double max = ((Double) result.get(NumberStatsFunction.MAX_VALUE)).doubleValue();
        final double min = ((Double) result.get(NumberStatsFunction.MIN_VALUE)).doubleValue();
        final double std = ((Double) result.get(NumberStatsFunction.STD_DEV)).doubleValue();
        final int count = ((Integer) result.get(NumberStatsFunction.NSAMPLES)).intValue();
        
        double dMean = Math.abs(mean - expectedMean);
        double dMax = Math.abs(max - expectedMax);
        double dMin = Math.abs(min - expectedMin);
        double dStd = Math.abs(std - expectedStd);
        int dCount = Math.abs(count - data.length);
        
        assertTrue("Mean is not within tolerance of expected value", dMean <= tolerance);
        assertTrue("Max is not within tolerance of expected value", dMax <= tolerance);
        assertTrue("Min is not within tolerance of expected value", dMin <= tolerance);
        assertTrue("Std is not within tolerance of expected value", dStd <= tolerance);
        assertTrue("Count is not expected value", dCount == 0);


    }
    
    public static void main(String[] args) {
        TestRunner.run(suite());
    }
}
