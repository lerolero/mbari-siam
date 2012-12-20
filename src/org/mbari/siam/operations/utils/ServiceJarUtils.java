/****************************************************************************/
/* Copyright 2005 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.operations.utils;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.NumberFormatException;
import java.util.Vector;
import java.util.Properties;
import java.util.zip.ZipFile;
import org.apache.log4j.Logger;

import org.mbari.siam.distributed.MissingPropertyException;
import org.doomdark.uuid.UUID;

public class ServiceJarUtils
{
    private static Logger _log4j = Logger.getLogger(ServiceJarUtils.class);
    static public final String _UUID_TOKEN = "UUID";
    static public final String _SERVICE_NAME_TOKEN = "serviceName";
    static public final UUID _NULL_UUID = 
        new UUID("00000000-0000-0000-0000-000000000000");
    static public final String _serviceCodePath = 
        System.getProperty("siam_home") + File.separator + "service_code";

    /** determine if service code for the specified UUID is loaded */
    static public boolean isServiceLoaded(UUID uuid)
    {
        //form service jar path
        String jarPath = _serviceCodePath + File.separator + uuid + ".jar";

        File jarFile = new File(jarPath);

        return jarFile.exists();
    }

    /** return the path to a loaded service from the specified UUID */
    static public String getServicePath(UUID uuid)
    {
        //form service jar path
        String jarPath = _serviceCodePath + File.separator + uuid + ".jar";

        File jarFile = new File(jarPath);

        if ( jarFile.exists() )
            return jarPath;
        else
            return null;
    }
    
    /** create the service code direcotry if it does not exist */
    static public boolean creatServiceCodePath() {
        File serviceCodeDir = new File(_serviceCodePath);
	_log4j.debug("creatServiceCodePath() - path=" + _serviceCodePath);
        return serviceCodeDir.mkdir();
    }


    /** add the service code to the service code directory */
    static public void addServiceCode(File serviceFile, UUID uuid) 
	throws IOException {

	_log4j.debug("addServiceCode() - uuid=" + uuid);

        //create dest file name
        String dstPath = _serviceCodePath + File.separator + uuid + ".jar";

        //make sure you don't have a null UUID
        if ( uuid.equals(ServiceJarUtils._NULL_UUID) )
        {
            throw new IOException("jar file with null UUID cannot be added.");
        }
    
        // get input and output streams to the files
        FileInputStream fis = new FileInputStream(serviceFile);
        FileOutputStream fos = new FileOutputStream(dstPath);
        byte[] tempBuff = new byte[1024];
    
        //copy the file
        while ( fis.available() > 0 )
            fos.write(tempBuff, 0, fis.read(tempBuff));
    
        //close the file input streams
        fis.close();
        fos.close();
    }

    /** remove the service code from the 
     * service code directory for the specified UUID */
    static public boolean removeServiceCode(UUID uuid)
    {
        //create service code file
        File sf = new File(_serviceCodePath + File.separator + uuid + ".jar");

        //delete the service code
        return sf.delete();
    }

    
    /** return an array of strings containing the 
     * UUID and serviceName of all the loaded services */
    static public String[] listServiceCode() throws IOException
    {
        //get all files in service code directory
        File serviceCodeDir = new File(_serviceCodePath);

        //get the jar files in the service code directory
        File[] serviceCodeFiles = serviceCodeDir.listFiles();

        //if there is no serivce code return null
        if ( (serviceCodeFiles == null) || (serviceCodeFiles.length == 0) )
            return null;

        //create a vector for service description string (sds)
        Vector sds = new Vector();
        String serviceInfo = null;

        //get the service code info
        for (int i = 0; i < serviceCodeFiles.length; ++i)
        {
            if ( serviceCodeFiles[i].getName().endsWith(".jar") )
            {
                try
                {
                    //get the uuid
                    serviceInfo = 
                        getServiceCodeUUID(serviceCodeFiles[i]).toString();

                    //get the serviceName
                    serviceInfo = serviceInfo + " - " + 
                        getServiceCodeName(serviceCodeFiles[i]);
                }
                catch(Exception e)
                {
                    //if you get an exception then the Jar file was not a 
                    //valid service file
                    continue;
                }

                //store in a vector
                sds.add(serviceInfo);
            }
        }
        
        //there's got to be a better way to do this
        String[] serviceInfoArray = new String[sds.size()];
        for (int i = 0; i < sds.size(); ++i)
            serviceInfoArray[i] = (String)sds.get(i);
        
        return serviceInfoArray; 
    }

    
    /** get the intrument UUID from the service code file */
    static public UUID getServiceCodeUUID(File serviceFile) 
        throws IOException, MissingPropertyException, NumberFormatException
    {
        Properties props = new Properties();
        ZipFile zf = new ZipFile(serviceFile);
        InputStream jis = zf.getInputStream(zf.getEntry("service.properties"));
    
        //load the properties from the service.properties file in the jar file
        props.load(jis);
        jis.close();

        String uuidString = props.getProperty(_UUID_TOKEN);

        //if there is no UUID, that's a problem
        if ( uuidString == null )
            throw new MissingPropertyException("Jar file missing UUID"); 
    
        return new UUID(uuidString);
    }
    
    /** get the service name from the service code file */
    static public String getServiceCodeName(File serviceFile) 
        throws IOException, MissingPropertyException
    {
        Properties props = new Properties();
        ZipFile zf = new ZipFile(serviceFile);
        InputStream jis = zf.getInputStream(zf.getEntry("service.properties"));
    
        //load the properties from the service.properties file in the jar file
        props.load(jis);
        jis.close();

        String serviceName = props.getProperty(_SERVICE_NAME_TOKEN);

        //if there is no UUID, that's a problem
        if ( serviceName == null )
            throw new MissingPropertyException("Jar file missing serviceName"); 
    
        return serviceName;
    }
}

    
