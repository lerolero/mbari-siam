/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.linkBenchmark2.interfaces;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.DevicePacketSet;
import org.mbari.siam.core.DeviceLog;


public interface Benchmark2 extends Remote {

  public static final String RMI_SERVER_NAME = "/benchmark2";
  public static final int SOCKET_TCP_PORT = 5605;

  /** Sets DeviceLog for later getPackets(), returns total number of entries in DeviceLog */
  int setDeviceLog(long sensorId, String directory)
      throws IOException, FileNotFoundException;
  
  /** Uses FilteredDeviceLog for later getPackets(), returns total number of entries in FilteredDeviceLog */
  int setFilteredDeviceLog(long sensorId, String directory)
      throws IOException, FileNotFoundException;
  
  /** Gets DevicePacketSets from DeviceLog named in setDeviceLog() */
  DevicePacketSet getPackets(long startKey, long endKey, int maxEntries) 
      throws NoDataException, IOException;

  /** Gets compressed DevicePacketSets from DeviceLog named in setDeviceLog() */
  byte[] getCompressedPackets(long startKey, long endKey, int maxEntries) 
      throws NoDataException, InvalidClassException, NotSerializableException, IOException, RemoteException;

}
