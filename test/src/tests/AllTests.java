package tests;

import junit.framework.TestSuite;
import junit.framework.Test;
import moos.devices.nortek.AquadoppParserTest;
import moos.devices.nortek.LogParserTest;
import moos.devices.workhorse.WorkhorseADCPTest;
import moos.jddac.NumberStatsFunctionTest;
import moos.jddac.MutableIntegerArrayStatsFunctionTest;

/**
 * @author brian
 * @version $Id: AllTests.java,v 1.1 2008/11/04 22:15:20 bobh Exp $
 * @since Oct 12, 2006 1:08:28 PM PST
 */
public class AllTests {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(AllTests.suite());
        System.exit(0);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for test");
        //$JUnit-BEGIN$
        //suite.addTest(AquadoppParserTest.suite());
        //suite.addTest(LogParserTest.suite());
        //suite.addTest(MutableIntegerArrayStatsFunctionTest.suite());
        //suite.addTest(NumberStatsFunctionTest.suite());
        suite.addTest(WorkhorseADCPTest.suite());
        //$JUnit-END$
        return suite;
    }
}
