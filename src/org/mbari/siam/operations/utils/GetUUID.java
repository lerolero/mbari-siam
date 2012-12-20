/****************************************************************************/
/* Copyright 2003 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.operations.utils;

import org.mbari.siam.utils.StopWatch;

import org.doomdark.uuid.EthernetAddress;
import org.doomdark.uuid.UUIDGenerator;
import org.doomdark.uuid.UUID;

import java.lang.Process;
import java.lang.Runtime;
import java.lang.String;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.util.Properties;


public class GetUUID 
{
    private static final String _BOGUS_ETH0_ADDRESS = "00:00:00:00:00:00";
    
    public void execute() throws Exception
    {
        String eth0Address = null;
        UUID uuid = null;

        
        Properties sysProperties = System.getProperties();
        String binLoc = sysProperties.getProperty("siam.bin");
        if (binLoc == null)
        {
            System.out.println("siam.bin property not set");
            return;
        }
        
        binLoc = binLoc.trim();
        
        Process proc = Runtime.getRuntime().exec(binLoc + 
                                                 File.separator + 
                                                 "ethaddr");
        
        BufferedReader br = 
            new BufferedReader(new InputStreamReader(proc.getInputStream()));
        
        eth0Address = br.readLine();
        
        UUIDGenerator uuidGen = UUIDGenerator.getInstance();
        
        StopWatch uuidGenTimer = new StopWatch();
        
        
        if ( eth0Address.startsWith(_BOGUS_ETH0_ADDRESS) )
            uuid = uuidGen.generateTimeBasedUUID();
        else
            uuid = uuidGen.generateTimeBasedUUID(new EthernetAddress(eth0Address));
/*        
//        First UUID takes some time to generate because of the secure random
//        number generator creation.  After the first UUID has been generated
//        the subsequent UUIDs are created quickly.
        
        {
            for (int i = 0; i < 5; ++i)
            {
                uuidGenTimer.clear();
                uuidGenTimer.start();
                uuid = uuidGen.generateTimeBasedUUID(new EthernetAddress(eth0Address));
                uuidGenTimer.stop();
                System.out.println("UUID " + i + " took " + 
                                   uuidGenTimer.read() + " ms");
            }
        }
*/        

        System.out.println("UUID         : " + uuid);
        System.out.println("eth0 address : " + eth0Address);
    }

    public static void main(String[] args) 
    {
        GetUUID app = new GetUUID();
        
        try
        {
            app.execute();
        }
        catch(Exception e)
        {
            System.out.println(e);
            e.printStackTrace();
        }
        
        System.exit(0);
    }
}
