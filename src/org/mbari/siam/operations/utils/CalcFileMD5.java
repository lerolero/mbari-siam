/****************************************************************************/
/* Copyright 2005 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.operations.utils;

import java.io.FileInputStream;
import java.io.IOException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.Ostermiller.util.MD5;

public class CalcFileMD5
{
    public static void main(String[] args) 
    {
        if ( args.length < 1 )
        {
            System.out.println("usage: java CalcFileMD5 [file]");
            return;
        }

        CalcFileMD5 app = new CalcFileMD5();
        app.execute(args);
    }
    
    public void execute(String[] args)
    {
        
        //MD5 byte array and payloadSize vars
        byte[] MD5Bytes = null;
        int payloadSize = 0;

        //calculate file MD5 and size
        try
        {

            FileInputStream jis = new FileInputStream(args[0]);
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.reset();
            byte[] jarFileBytes = new byte[1024];
            int bytesRead = 0;            

            //upate MD5 and count the bytes
            while ( jis.available() > 0 )
            {
                bytesRead = jis.read(jarFileBytes);
                payloadSize += bytesRead; 
                md.update(jarFileBytes, 0, bytesRead);
            }
            
            jis.close();
            
            //the actual MD5 calc'n
            MD5Bytes = md.digest();
            
            //dispaly file MD5
            System.out.println("payload size : " + payloadSize);
            System.out.println("MessageDigest MD5 : 0x" + 
                               bytesToHexString(MD5Bytes));
            System.out.println("Ostermiller MD5   : 0x" + 
                               MD5.getHashString(new FileInputStream(args[0])));
        }
        catch(IOException e)
        {
            System.out.println("Failure while computing payload MD5: " + e);
            return;
        }
        catch(NoSuchAlgorithmException e)
        {
            System.out.println("Failed to get MD5 hash algorithm");
            return;
        }
    }

    private String bytesToHexString(byte[] b)
    {
        int hex;
        String hexChars = "0123456789abcdef";
        StringBuffer byteString = new StringBuffer();
        
        for (int i = 0; i < b.length; ++i) 
        {
            hex = b[i] & 0xFF;
            byteString.append(hexChars.charAt(hex >> 4));
            byteString.append(hexChars.charAt(hex & 0x0f));
        }

        return byteString.toString();
    }
}


