/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/

package org.mbari.siam.test.org.mbari.isi.interfaces;

import java.util.Properties;

import junit.framework.TestCase;

import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.PropertyException;
import org.mbari.siam.distributed.ServiceAttributes;

public class TestServiceAttributes extends TestCase {

    public final void testGetHelp() {
        ServiceAttributes attributes = InstrumentServiceAttributes
            .getAttributes();
        String expectedResponse = "Valid properties:\n"
            + "  currentLimitMa - int\n"
            + "  maxSampleTries - int\n"
            + "  startDelayMsec - int\n"
            + "  sampleTimeoutMsec - long\n"
            + "  sampleSchedule - mnemonic: \"[interval (sec)]\", \"[cron-style string]\"\n"
            + "  powerPolicy - mnemonic: \"NEVER\", \"SAMPLING\", \"ALWAYS\"\n"
            + "  commPowerPolicy - mnemonic: \"NEVER\", \"SAMPLING\", \"ALWAYS\"\n"
            + "  timeSynch - boolean\n" + "  serviceName - String\n"
            + "  isiID - long\n" + "  UUID - java.lang.String\n"
            + "  instrumentName - java.lang.String\n";

        String response = attributes.getHelp();
        assertEquals(expectedResponse, response);
    }

    public final void testToPropertyStrings() {
        ServiceAttributes attributes = InstrumentServiceAttributes
            .getAttributes();
        String expectedResponse = "frameworkVersion=\n" + "serviceStatus=2\n"
            + "sampleTimeoutMsec=1000\n" + "maxSampleTries=3\n" + "isiID=0\n"
            + "currentLimitMa=12000\n" + "powerPolicy=NEVER\n"
            + "startDelayMsec=0\n" + "instrumentName=UNKNOWN\n"
            + "serviceName=\n" + "commPowerPolicy=SAMPLING\n"
            + "timeSynch=false\n" + "parentID=0\n"
            + "UUID=00000000-0000-0000-0000-000000000000\n";
        String response = ServiceAttributes.toPropertyStrings(attributes
            .toProperties());
        assertEquals(expectedResponse, response);
    }

    /*
     * Class under test for Properties toProperties()
     */
    public final void testToProperties() {
        ServiceAttributes attributes = InstrumentServiceAttributes
            .getAttributes();
        Properties properties = attributes.toProperties();
        assertNotNull(properties);
    }

    /*
     * Class under test for Properties toProperties(String[])
     */
    public final void testToPropertiesStringArray() {
        ServiceAttributes attributes = InstrumentServiceAttributes
            .getAttributes();
        try {
            Properties properties = attributes.toProperties(new String[]{
                "isiID", "powerPolicy", "serviceName"});
            assertEquals(properties.size(), 3);
        } catch (InvalidPropertyException e) {
            e.printStackTrace();
            fail("No exception should be thrown here, all properties are legal");
        }
        try {
            attributes
                .toProperties(new String[]{"isiID", "powerPolicy", "FOO"});
            fail("FOO is not a legal exception, this code should not be reached");
        } catch (InvalidPropertyException e) {
            // good, an exception should be thrown in this case because of "FOO"
        }
    }

    public final void testFromProperties() {
        ServiceAttributes attributes = InstrumentServiceAttributes
            .getAttributes();
        try {
            Properties properties = attributes.toProperties(new String[]{"isiID",
                "powerPolicy", "serviceName"});
            properties.setProperty("isiID", "999");
            properties.setProperty("serviceName", "TestServiceName");
            attributes.fromProperties(properties, true);
            properties = attributes.toProperties();
            assertEquals(properties.getProperty("isiID"), "999");
            assertEquals(properties.getProperty("serviceName"), "TestServiceName");
        } catch (InvalidPropertyException e) {
            e.printStackTrace();
            fail("No exception expected here");
        } catch (PropertyException e) {
            e.printStackTrace();
            fail("No exception expected here");
        }
    }
}
