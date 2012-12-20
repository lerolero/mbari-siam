/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.SyncFailedException;

import org.apache.log4j.Logger;

/* $Id: DeviceLogData.java,v 1.5 2012/12/17 21:32:41 oreilly Exp $ */

/**
 This class implements the serialized class storage and retrieval functions 
 needed by the <code>DeviceLog</code> class. 
 @see DeviceLog
 @author Tim Meese
 */
public class DeviceLogData {
	
        /** Log4j logger */
        static Logger _logger = Logger.getLogger(DeviceLog.class);
	
	protected RandomAccessFile _dataFile = null;
	protected long _deviceId;
	protected int _segment;
	protected long _currentDataFileExtent = 0;
	
	/** default constructor
	 @param deviceId ISI Device ID 
	 @param segmentNumber segment number 
	 */
	protected DeviceLogData(long deviceId, int segmentNumber, 
			     String directory) 
	    throws FileNotFoundException, IOException {
	    this(deviceId,segmentNumber,"",directory);
	}
	protected DeviceLogData(long deviceId, int segmentNumber, 
			     String suffix, String directory) 
	    throws FileNotFoundException, IOException {
		_deviceId = deviceId;
		_segment=segmentNumber;
		String dataFileName = getDataFileName(segmentNumber, 
						      suffix, directory);
		
		// try to open a generic file for existence check
		
		File openAttempt = new File(dataFileName);
		
		if (openAttempt.exists() == true) {
			
			_dataFile = new RandomAccessFile(dataFileName, "rw");
			
			// restore file state
			_currentDataFileExtent = _dataFile.length();
			
			// since we always do a seek to the current extent, this is 
			// all that is necessary here
			
		}
		else {
			_dataFile = new RandomAccessFile(dataFileName, "rw");
		}
	}
	
	private String getDataFileName(int segmentNum, String directory) {
	    return getDataFileName(segmentNum,"",directory);
	}
	private String getDataFileName(int segmentNum, String suffix, String directory) {
		return new String(directory + File.separator + 
				_deviceId + "_" + segmentNum +suffix+ ".dat");
	}
	
	/** this routine appends a byte array of serialized class data to the
	 data log according to the information contained in the 
	 <code>DeviceLogIndex.Entry</code> that is passed in
	 @param indexEntry index entry used to write data
	 @see DeviceLogIndex.Entry
	 */
	protected synchronized void appendLogData(byte[] serializedData, 
			DeviceLogIndex.Entry indexEntry) {
		long seekOffset = _currentDataFileExtent;
		int dataLength;
		FileDescriptor fd;
		
		if ((indexEntry == null) || (serializedData == null)) {
			_logger.error("DeviceLogData:appendLogData: null idx entry");
			return;
		}
		
		dataLength = serializedData.length;
		
		// update DeviceLogIndex.Entry with current offset, length
		
		indexEntry._dataOffset = seekOffset;
		indexEntry._dataSize = dataLength;
		
		try {
			_dataFile.seek(seekOffset);
			_dataFile.write(serializedData);
			
		}
		catch (IOException ioe) {
			_logger.error("appendLogData: seek/write exception");
		}
		
		try {
			fd = _dataFile.getFD();
			try {
				fd.sync();
			}
			catch (SyncFailedException sfe) {
				_logger.error("appendLogData: fd sync failed");
			}
		}
		catch (IOException ioe) {
			_logger.error("appendLogData: seek/write exception");
		}
		
		_currentDataFileExtent += dataLength;
	}
	
	/** this routine reads a byte array of serialized class data from the
	 data log according to the information contained in the 
	 <code>DeviceLogIndex.Entry</code> that is passed in
	 @param indexEntry index entry used to read data
	 @see DeviceLogIndex.Entry
	 */
	protected synchronized byte[] readLogData(DeviceLogIndex.Entry indexEntry) {
		byte[] result = null;
		int bytesRead = 0;
		
		if (indexEntry == null) {
			_logger.error("DeviceLogData.readLogData: null index entry");
			return result;
		}
		
		if (indexEntry._dataOffset >= _currentDataFileExtent) {
			_logger.error("DeviceLogData.readLogData: data offset error for device " + 
				      _deviceId + 
				      " (offset req:"+indexEntry._dataOffset+
				      " offset max:"+_currentDataFileExtent+
				      " diff:"+(indexEntry._dataOffset-_currentDataFileExtent)+")");
			return result;
		}
		
		try {
			_dataFile.seek(indexEntry._dataOffset);
		}
		catch (IOException ioe) {
			_logger.error("readLogData: data file seek exception");
		}
		
		result = new byte[indexEntry._dataSize];
		
		try {
			bytesRead = _dataFile.read(result);
		}
		catch (IOException ioe) {
			_logger.error("readLogData: data file seek exception");
		}
		
		if (bytesRead != indexEntry._dataSize) {
			_logger.error("readLogData: data size mismatch");
			_logger.error("  bytes read from file: " + bytesRead);
			_logger.error("  dataSize in index entry: " + 
					indexEntry._dataSize);
			
			// TODO throw an exception
		}
		
		return result;
	}
	
	/** Close the data file. This DeviceLogData instance is no longer usable
	 after close() has been called. */
	protected void close() throws IOException {
		_dataFile.close();
	}

    protected void seek(long pos) throws IOException{
	_dataFile.seek(pos);
    }
    protected long getLength()throws IOException{
	return _dataFile.length();
    }
    protected long getFilePointer()throws IOException{
	return _dataFile.getFilePointer();
    }
    protected int readBytes(byte[] buf, int offset, int bytes)throws IOException{
	int ptr=offset;
	int bytesRead=0;
	for(bytesRead=0;bytesRead<bytes;bytesRead++){
	    buf[ptr++]=_dataFile.readByte();
	}
	return bytesRead;
    }
    protected FileInputStream getFileInputStream()throws IOException{
	return new FileInputStream(_dataFile.getFD());
    }

}






