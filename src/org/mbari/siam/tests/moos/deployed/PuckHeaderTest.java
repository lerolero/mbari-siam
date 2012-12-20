/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/*
 * Created on Oct 6, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.mbari.siam.tests.moos.deployed;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import junit.framework.TestCase;
import org.mbari.siam.core.PuckHeader;

import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.ServiceAttributes;

/**
 * @author oreilly
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class PuckHeaderTest extends TestCase {
 
    /** Verify correct property parsing. */
    public void testProperties() {
        final String FILENAME = "service.properties";

        InstrumentServiceAttributes attributes = 
        	InstrumentServiceAttributes.getAttributes();
        
        attributes.isiID = 9999;
        attributes.currentLimitMa = 500;
        attributes.serviceName = "the Shmoo".getBytes();

        Properties properties = 
        	attributes.toConfigurableProperties();
        
        try {
            OutputStream output = new FileOutputStream(FILENAME);
            properties.store(output, "Test");
            output.close();
        } catch (IOException e) {
            fail("Can't open output properties file");
        }

        Properties properties2 = new Properties();

        try {
            InputStream input = new FileInputStream(FILENAME);
            properties2.load(input);
            input.close();
        } catch (IOException e) {
            fail("Can't open input properties file");
        }
        PuckHeader puckHeader = new PuckHeader();
        puckHeader.setServiceProperties(properties2);

        System.out.println("Header: isiID=" + 
                puckHeader.getIsiId() + 
                ", current limit=" +
                puckHeader.getCurrentLimit() + 
                ", service name=" + 
                puckHeader.getServiceName());
        
        assertTrue(puckHeader.getIsiId() == attributes.isiID);
        assertTrue(puckHeader.getCurrentLimit() == attributes.currentLimitMa);
        assertTrue(puckHeader.getServiceName().equals(
                new String(attributes.serviceName)));
        
    }
  
 }