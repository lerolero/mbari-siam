/****************************************************************************/
/* Copyright 2003 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.utils;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

/** Various file manipulation utilities. */
public class FileUtils {

    /** log4j logger */
    static Logger _log4j = Logger.getLogger(FileUtils.class);

    public static boolean compareFile(String file1, String file2)
        throws FileNotFoundException, IOException 
    {

        BufferedInputStream bin1 = 
            new BufferedInputStream(new FileInputStream(file1));
        BufferedInputStream bin2 = 
            new BufferedInputStream(new FileInputStream(file2));

        boolean match = true;

        try
        {
            int i;
            for(;;)
            {
                if ( (i = bin1.read()) != bin2.read() )
                {
                    match = false;
                    break;
                }
                
                if ( i == -1 )
                    break;
            }
        }
        catch(IOException e)
        {
            throw e;
        }
        finally
        {
            if ( bin1 != null)
                bin1.close();
                
            if ( bin2 != null)
                bin2.close();
        }

	
        return match;
    }
    
    /** Copy specified file to specified destination.
     */
    public static void copyFile(String sourceName, String destinationName) 
	throws FileNotFoundException, IOException {


	FileInputStream  in = new FileInputStream(sourceName);
	copyFile(in, destinationName);
    }


    /** Copy specified file to specified destination.
     */
    public static void copyFile(InputStream in, String destinationName) 
	throws FileNotFoundException, IOException {

        long start = System.currentTimeMillis();

	BufferedInputStream  bin = new BufferedInputStream( in );
	FileOutputStream out = new FileOutputStream( destinationName );
	BufferedOutputStream bout = new BufferedOutputStream( out );

	int i;
	while ( (i=bin.read()) != -1) {
	    bout.write(i);
	}
	bout.close();
	long stop = System.currentTimeMillis();
	_log4j.debug("Took "+(stop-start) + " millisecs");
	_log4j.debug("File copied");
    }
}
