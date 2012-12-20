/****************************************************************************/
/* Copyright 2005 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

import java.util.Properties;
import org.mbari.puck.Puck;
import org.apache.log4j.Logger;
import org.doomdark.uuid.UUID;
import com.Ostermiller.util.MD5;

public class ServiceSandBox {
	private static Logger _log4j = Logger.getLogger(ServiceSandBox.class);

    private String _sandBoxPath = null;
    public static final String JAR_NAME = "service.jar";
    public static final String DATASHEET_NAME = "datasheet.bin";
    public static final String PROPERTIES_NAME = "service.properties";
    
    /** create a Sandbox object */
    public ServiceSandBox(String path) throws IOException
    {
        _sandBoxPath = path;

        //if the directory dos not exist, make it
        File f = new File(_sandBoxPath);

        if ( !f.exists() )
            if ( !f.mkdirs() )
                throw new IOException("failed to create directory " + 
                                      _sandBoxPath);
    }

    /** Get MD5 of Jar file. */
    public byte[] getJarMD5() throws IOException
    {
    	FileInputStream input = 
    		new FileInputStream(new File(getJarPath()));
    	
        byte[] hash = MD5.getHash(input);
        input.close();
        return hash;
    }
    
    /** Get the path to the sand box. */
    public String getPath()
    {
        return _sandBoxPath;
    }
    
    /** get the name of the jar file including path info */
    public String getJarPath()
    {
        return (_sandBoxPath + File.separator + JAR_NAME);
    }

    /** get the name of the ids file including path info */
    public String getDatasheetPath()
    {
        return (_sandBoxPath + File.separator + DATASHEET_NAME);
    }
    
    /** get the name of the ids file including path info */
    public String getPropertiesPath()
    {
        return (_sandBoxPath + File.separator + PROPERTIES_NAME);
    }

    /** Store PUCK datasheet */
    void storeDatasheet(Puck.Datasheet datasheet) throws IOException {
        FileOutputStream output = null;
        
        try
        {
            output = new FileOutputStream(getDatasheetPath());
        }
        catch(FileNotFoundException e)
        {
            throw new IOException("caught FileNotFoundException " + 
                                  "while creating datasheet FileOutputStream: " + e);
        }
            
        output.write(datasheet.getBytes());

        //close the file output streams
        output.close();
    }


    public UUID getInstrumentUUID() throws IOException
    {
        Puck.Datasheet datasheet = getDatasheet();
        
        return datasheet.getUUID();
    }

    public Properties getServiceProperties() throws IOException
    {
        FileInputStream input = null;

        //create the file input stream
        try
        {
            input = new FileInputStream(_sandBoxPath + 
                                      File.separator + PROPERTIES_NAME);
        }
        catch (FileNotFoundException e)
        {
            throw new IOException("caught FileNotFoundException while " +
                                  "opening properties input stream: " + e);
        }
        
        Properties props = new Properties();

        props.load(input);
        input.close();
        
        return props;
    }


    public void saveServiceProperties(Properties properties) throws IOException
    {
        FileOutputStream output = null;

        //create the file output stream
        try
        {
            output = new FileOutputStream(_sandBoxPath + 
					  File.separator + PROPERTIES_NAME);
        }
        catch (FileNotFoundException e)
        {
            throw new IOException("caught FileNotFoundException while " +
                                  "opening properties output stream: " + e);
        }
        
        Properties props = new Properties();

	
        properties.store(output, "Persisted");
        output.close();

    }


    public void storeFile(File src, boolean overwrite) throws IOException, 
        FileNotFoundException
    {
        //create the destination file
        File dest = new File(_sandBoxPath + File.separator + src.getName());
        
        //check if the dest file exists
        if ( !overwrite )
            throw new IOException("destination file already exists");
        
        // get input and output streams to the files
        FileInputStream input = new FileInputStream(src);
        FileOutputStream output = new FileOutputStream(dest);
        byte[] tempBuff = new byte[1024];

        //copy the file
        while ( input.available() > 0)
            output.write(tempBuff, 0, input.read(tempBuff));

        //close the file input streams
        input.close();
        output.close();
    }

    public String[] listFiles() throws IOException
    {
        File dir = new File(_sandBoxPath);

        if ( dir.isDirectory() )
            return dir.list();
        else
            throw new IOException("directory '" + _sandBoxPath + "' not valid");
    }
    
    public void deleteFile(String file) throws IOException
    {
        File f = new File(_sandBoxPath + File.separator + file);

        if ( f.exists() )
        {
        	_log4j.debug("deleteFile() - delete " + f);
        	
            try
            {
                if ( !f.delete() ) {
                	_log4j.error("deleteFile() - couldn't delete 'sandbox' file " + f);
                    throw new IOException("failed to delete 'sandbox' file " 
                    		+ f);
                }
            }
            catch (SecurityException e)
            {
                throw new IOException("SecurityException while deleting " + 
                                      f + ": " + e);
            }
        }
    }
    
    /** delete all of the files in the service sand box */
    public void deleteAllFiles() throws IOException 
    {
        File dir = new File(_sandBoxPath);
        
        if ( !dir.isDirectory() )
            throw new IOException("directory '" + _sandBoxPath + "' not valid");
        
        String[] fileList = dir.list();        
        
        for (int i = 0; i < fileList.length; i++)
            deleteFile(fileList[i]);
    }



    /** Retrieve the stored PUCK datasheet */
    Puck.Datasheet getDatasheet() throws IOException {

        FileInputStream input = null;

        //create the file input stream
        try {
            input = new FileInputStream(_sandBoxPath + 
                                      File.separator + DATASHEET_NAME);
        }
        catch (FileNotFoundException e) {
            throw new IOException("caught FileNotFoundException while " +
                                  "opening datasheet input stream: " + e);
        }

        byte[] bytes = new byte[Puck.Datasheet._SIZE];
        
        // read the datasheet bytes
        if ( input.read(bytes) != bytes.length) {
            input.close();
            throw new IOException("failed to read instrument datasheet bytes");
        }

        input.close();

        // return the instrument datasheet
        return new Puck.Datasheet(bytes);
    }

}


