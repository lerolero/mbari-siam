/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.linkBenchmark2.compression;

import java.io.ByteArrayOutputStream;

/**
ByteArrayOutputStream that can return count of valid bytes
@author Bob Herlien
*/

public class CountedByteArrayOutputStream extends ByteArrayOutputStream
{
    public int count()
    {
	return(count);
    }

} /* class CountedByteArrayOutputStream */
