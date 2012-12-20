/****************************************************************************/
/* Copyright 2005 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.operations.utils;

import java.io.File;
import java.lang.NumberFormatException;

import org.doomdark.uuid.UUID;

public class RemoveServiceJar
{

    public static void main(String[] args) 
    {
        if ( args.length < 1)
        {
            System.out.println("usage: java RemoveService [uuid]");
            System.exit(1);
        }
        
        RemoveServiceJar app = new RemoveServiceJar();
        
        try
        {
            app.execute(args[0]);
        }
        catch(Exception e)
        {
            System.err.println(e);
            e.printStackTrace();
        }
    }

    public void execute(String serviceUUID) throws Exception
    {
        //create service_code dir if it's not there
        ServiceJarUtils.creatServiceCodePath();

        UUID uuid = null;
        
        try
        {
            uuid = new UUID(serviceUUID);
        }
        catch (NumberFormatException e)
        {
            System.out.println("UUID '" + serviceUUID + 
                               "' is not a valid UUID: " + e);
            return;
        }

        if ( !ServiceJarUtils.isServiceLoaded(uuid) )
        {
            System.out.println("Could not find service with UUID: " + uuid);
            return;
        }

        if ( ServiceJarUtils.removeServiceCode(uuid) )
            System.out.println("Service with UUID '" + uuid + "' removed");
        else
            System.out.println("Removal of service with UUID '" + 
                               uuid + "' failed");
    }
}

    