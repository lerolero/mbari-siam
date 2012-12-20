/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.NoDataException;

/**
 * DeviceLogIterator iterates over DevicePackets stored in a DeviceLog. NOTE:
 * This implementation is not thread-safe!
 * 
 * @author Tom O'Reilly
 */
public class DeviceLogIterator implements Iterator {

    static Logger _log4j = Logger.getLogger(DeviceLogIterator.class);

    private byte[] _syncByteBuf = new byte[DeviceLog.SYNC_PATTERN.length];

    DeviceLog _log = null;

    DeviceLogIndex.Entry _indexEntry = null;

    // Note that DeviceLog classes have been implemented with 'int'
    // instead of long, which may be too limiting. But we are stuck
    // with int for now...
    int _nPackets = 0; // Number of packets in log

    int _packetIndex = 0; // Current packet index

    long _startTime = 0L;

    /** Create iterator, passing in DeviceLog on which to iterate
	and specify a start time so that a binary search can be
	done (by getEntryIndex) to quickly find the first record.
    */
    public DeviceLogIterator(DeviceLog log, long start) {
	this(log);
	if(start>=0L){
	    try{
		Vector v=log._index.getEntries(start, 1);
		if(v.size()>0){
		    DeviceLogIndex.Entry entry=(DeviceLogIndex.Entry)v.elementAt(0);
		    _packetIndex=entry.getEntryIndex();
		    _startTime = start;
		}
	    }catch(NoDataException e){
		_log4j.error("DeviceLogIterator - no index entry for specified time ("+start+"):\n"+e);
	    }
	}
	
    }
    /** Create iterator, passing in DeviceLog on which to iterate. */
    public DeviceLogIterator(DeviceLog log) {
	_log = log;
	_indexEntry = new DeviceLogIndex.Entry();

	_nPackets = _log.nPackets();
	_packetIndex = 0;
    }

    /** Returns true if DeviceLog has more packets. */
    public boolean hasNext() {
	if (_packetIndex < _nPackets)
	    return true;
	else
	    return false;
    }

    /** Returns next DevicePacket from DeviceLog. */
    public Object next() throws NoSuchElementException {

	if (!hasNext()) {
	    throw new NoSuchElementException("no more packets");
	}

	// Load index entry (note that 'index' argument is 1-based!)
	try {
	    _log._index.getIndexEntry(_packetIndex + 1, _indexEntry);
	    _packetIndex++;
	} catch (NoDataException e) {
	    throw new NoSuchElementException(
					     "NoDataException from getIndexEntry()");
	}

	byte[] objectBytes = _log._data.readLogData(_indexEntry);
	if (objectBytes == null) {
	    throw new NoSuchElementException("null object bytes");
	}

	Object packet = null;
	try {
	    ByteArrayInputStream byteInput = 
		new ByteArrayInputStream(objectBytes);

	    int nBytes = 0;
	    _log4j.debug("get " + DeviceLog.SYNC_PATTERN.length + 
			 "bytes...");

	    byteInput.mark(DeviceLog.SYNC_PATTERN.length);

	    if ((nBytes = byteInput.read(_syncByteBuf, 0, 
				 DeviceLog.SYNC_PATTERN.length)) != 

		DeviceLog.SYNC_PATTERN.length) {
		_log4j.error("couldn't read sync; got " + 
			     nBytes + " bytes");
	    }
	    // Check for sync pattern bytes
	    boolean gotSync = true;
	    for (int i = 0; i < DeviceLog.SYNC_PATTERN.length; i++) {
		if (_syncByteBuf[i] != DeviceLog.SYNC_PATTERN[i]) {
		    gotSync = false;
		    break;
		}
	    }

	    if (!gotSync) {
		_log4j.warn("Sync not found.");
		byteInput.reset();
	    }

	    ObjectInputStream objectInput = 
		new ObjectInputStream(byteInput);

	    packet = objectInput.readObject();

	} catch (Exception e) {
	    _log4j.error(e);
	    throw new NoSuchElementException(e.getMessage());
	}

	if (packet == null) {
	    throw new NoSuchElementException("null packet object");
	}

	return packet;
    }

    /** Not supported. */
    public void remove() throws UnsupportedOperationException,
				IllegalStateException {

	throw new UnsupportedOperationException();
    }
}
