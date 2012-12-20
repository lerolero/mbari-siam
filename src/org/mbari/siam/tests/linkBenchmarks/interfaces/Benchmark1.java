/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.linkBenchmarks.interfaces;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Benchmark1 extends Remote {

  void emptyTest() throws RemoteException;

  long primitiveTest1(short a, double b, long retvalue) throws RemoteException;

  Struct1 structTest1(byte fillValue) throws RemoteException;

}
