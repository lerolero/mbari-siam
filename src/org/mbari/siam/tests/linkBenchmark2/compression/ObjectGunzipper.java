/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.linkBenchmark2.compression;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.DataFormatException;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.io.StreamCorruptedException;
import java.io.InvalidClassException;
import java.io.OptionalDataException;

import org.apache.log4j.Logger;

/**
ObjectGunzipper uncompresses an object, generating the original Object
@author Bob Herlien
*/
public class ObjectGunzipper
{
    static private Logger _logger = Logger.getLogger(ObjectGunzipper.class);

    public Object decompress(byte[] compObj)
	throws StreamCorruptedException, SecurityException, NullPointerException, IOException,
	       ClassNotFoundException, InvalidClassException, OptionalDataException, DataFormatException
    {
	ByteArrayInputStream byteStream = new ByteArrayInputStream(compObj);
	GZIPInputStream      gzStream   = new GZIPInputStream(byteStream);
	ObjectInputStream    objStream  = new ObjectInputStream(gzStream);

	return(objStream.readObject());
    }

} /* class ObjectGunzipper */
