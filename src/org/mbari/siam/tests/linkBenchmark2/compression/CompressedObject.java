/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.linkBenchmark2.compression;

public class CompressedObject
{
    /** byte array representation of the compresed object */
    protected byte[] _object;
    protected int    _uncompressedLen,_compressedLen;

    public CompressedObject(byte[] compressedObject, int uncompressedLen, int compressedLen)
    {
	_object = compressedObject;
	_uncompressedLen = uncompressedLen;
	_compressedLen = compressedLen;
    }

    public CompressedObject(byte[] compressedObject, int uncompressedLen)
    {
	_object = compressedObject;
	_uncompressedLen = uncompressedLen;
	_compressedLen = _object.length;
    }

    public byte[] getCompressedBytes()
    {
	return(_object);
    }

    public int getCompressedObjectLen()
    {
	return(_compressedLen);
    }

    public int getUncompressedObjectLen()
    {
	return(_uncompressedLen);
    }

} /* class CompressedObject */
