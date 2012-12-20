/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.linkBenchmarks.interfaces;
import java.io.Serializable;

public class Struct1 implements Serializable {

    public short x;
    public long y;
    public double z;

    public byte array[] = new byte[1000];

}
