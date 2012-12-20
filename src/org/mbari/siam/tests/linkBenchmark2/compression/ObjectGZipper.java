/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.linkBenchmark2.compression;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;

import org.apache.log4j.Logger;

/**
ObjectGZipper compresses an object, generating a byte[] array
@author Bob Herlien
*/
public class ObjectGZipper
{
    static private Logger _logger = Logger.getLogger(ObjectGZipper.class);
    protected CountedByteArrayOutputStream _byteStream =null;

    public ObjectGZipper()
    {
	_byteStream = new CountedByteArrayOutputStream();
    }

    public byte[] compress(Object obj)
	throws InvalidClassException, NotSerializableException, IOException
    {
	_byteStream.reset();

	GZIPOutputStream outStream = new GZIPOutputStream(_byteStream);
	ObjectOutputStream objStream = new ObjectOutputStream(outStream);

	objStream.writeObject(obj);
	objStream.close();
	outStream.close();

	int count = _byteStream.count();

	_logger.debug("GZipper: count = " + count + ", size = " + _byteStream.size());
	return(_byteStream.toByteArray());
    }

} /* class ObjectGZipper */
