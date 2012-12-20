/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.StreamCorruptedException;
import java.io.RandomAccessFile;

import java.util.Vector;
import java.util.NoSuchElementException;
import java.text.DateFormat;
import java.util.Date;

import org.mbari.siam.utils.StopWatch;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.DeviceMessagePacket;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DevicePacketSet;
import org.mbari.siam.distributed.MetadataPacket;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.MeasurementPacket;
import org.mbari.siam.distributed.DeviceMessagePacket;
import org.mbari.siam.distributed.SummaryPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.PowerPort;
import org.mbari.siam.distributed.RangeException;

/* $Id: DeviceLog.java,v 1.9 2012/12/17 21:32:40 oreilly Exp $ */

/**
   <code>DeviceLog</code> is the top level class implementation of an indexed
   record retrieval class. It utilizes two subordinate classes, 
   <code>DeviceLogData</code> and <code>DeviceLogIndex</code> to implement 
   indexed random access record storage. 
 
   @see DeviceLogData
   @see DeviceLogIndex
   @see org.mbari.siam.distributed.SensorDataPacket
   @see org.mbari.siam.distributed.DevicePacket
   @author Tim Meese
*/

public class DeviceLog {
	
	
    /** Log4j logger */
    static private Logger _log4j = Logger.getLogger(DeviceLog.class);

    public static final byte[] SYNC_PATTERN = {0xB, 0xB, 0xB, 0xB};
    public static final long EOF = -1L;
    public static final long SYNC_NOT_FOUND = -2L;

    private byte[] _syncByteBuf = new byte[SYNC_PATTERN.length];

    private static final Object _classLock = DeviceLog.class;
    private long _deviceId = 0;
    private long _metadataRef = 0;
	
    // Timestamp on last (i.e. highest-indexed) packet in file
    private long _lastTimestamp = 0;
    private SequenceNumber _sequenceNumber;
    protected DeviceLogIndex _index;
    protected DeviceLogData  _data;
    protected StopWatch _writeTimer = new StopWatch(false);	

    /**
       Create DeviceLog for specified device, sequence number, in
       specified directory.
    */
    protected DeviceLog(long deviceId, int segmentNum, String directory) 
	throws IOException, FileNotFoundException {
	this._deviceId = deviceId;
		
	// NOTE: this version of DeviceLog only supports one 'set' of
	// index and data classes. This will be fixed with the addition
	// of a DeviceLogEntry class
		
	_index = new DeviceLogIndex(_deviceId, segmentNum, directory);
	_data  = new DeviceLogData(_deviceId, segmentNum, directory);
	long startSequence = _index.getLastSequenceNumber();
	try {
	    _sequenceNumber = 
		new SequenceNumber(Long.MIN_VALUE, Long.MAX_VALUE, 
				   startSequence);
	}
	catch (RangeException e) {
	    throw new IOException(e.getMessage());
	}
		
	_metadataRef = _index.getLastMetadataRef();
	
	try {
	    DevicePacket packet = getLastPacket();
	    _lastTimestamp = packet.systemTime();
	}
	catch (NoDataException e) {
	    _lastTimestamp = 0;
	}
	catch (NullPointerException e) {
	    _lastTimestamp = 0;
	}
    }
	
	
    /**
       Create DeviceLog for specified device, with storage in specified
       directory.
    */
    public DeviceLog(long deviceId, String directory) 
	throws FileNotFoundException, IOException {
		
	this(deviceId, 0, directory);
    }
	
	
    /** Return last metadata reference number. */
    public long getLastMetadataRef() {
	return _index.getLastMetadataRef();
    }
	
	
    /** returns the ISI deviceId associated with this <code>DeviceLog</code> */
    public long getDeviceId() {
	return this._deviceId;
    }
	
    /** This method appends the specified <code>DevicePacket</code> object to
	the log that is being maintained. The object is serialized and 
	subsequently written to a data file while indexing information is 
	maintained. If argument setSequenceNos is set to 'true', then
	appendPacket() will automatically set the packet's sequence number
	and metadata reference.
	@param packet <code>DevicePacket</code> object to append to active log
    */

    public synchronized void appendPacket(DevicePacket packet, 
					  boolean setSequenceNos, 
					  boolean checkTime) {

	// The time-rollback algorithm is not valid for multi-threaded
	// system, so disable it here.
	checkTime = false;

	ByteArrayOutputStream byteOutput = null;
	ObjectOutputStream objectOutput = null;
	DeviceLogIndex.Entry newIndexEntry;
	long key;
	byte[] serializedPacket = null;
	int objectSize = 0;
		
	// TODO: check packet validity
		
	// extract metadata needed to create an index entry
		
	boolean timeError = false;

	if (setSequenceNos) {
	    long sequenceNumber = _sequenceNumber.getNext();
	    packet.setSequenceNo(sequenceNumber);
			
	    packet.setMetadataRef(_metadataRef);
			
	    if(packet instanceof MetadataPacket){
		_metadataRef = sequenceNumber;
		_index.updateMetadataRef(_metadataRef);
	    }
	}
		
	key = packet.systemTime();

	if (checkTime) {
	    if (key < _lastTimestamp) {
		_log4j.error("appendPacket() detected time rollback");
		_log4j.debug("seqno=" + 
			      packet.sequenceNo() + 
			      ", key=" + key + ", last stamp=" + 
			      _lastTimestamp);
		timeError = true;
	    }
	}
	// Keep track of last appended timestamp
	_lastTimestamp = key;
		
	// serialize the packet
	try {
	    byteOutput = new ByteArrayOutputStream();	
	    byteOutput.write(SYNC_PATTERN);
	    objectOutput = new ObjectOutputStream(byteOutput);
			
	    objectOutput.writeObject(packet);
	    //packet.serialize(objectOutput);
	    objectOutput.flush();
			
	    serializedPacket = byteOutput.toByteArray();
	    objectSize = serializedPacket.length;
	}
	catch (Exception e) {
	    _log4j.error("appendPacket: serialization exception", e);
	}
		
	newIndexEntry = new DeviceLogIndex.Entry();
		
	newIndexEntry.setKey(key);
	newIndexEntry.setDataSize(objectSize);
		
	newIndexEntry.setSequenceNumber(packet.sequenceNo());
		
	// give the serialized packet to the DeviceLogData object to
	// insert the indexing data
		
	//create a running stopwatch to time the file write to logs
	//StopWatch writeTimer = new StopWatch(true);
	if(_log4j.isDebugEnabled()==true){
	_writeTimer.clear();
	_writeTimer.start();

	_data.appendLogData(serializedPacket, newIndexEntry);

	_writeTimer.stop();
	if ( _writeTimer.read() > 1000)
	    _log4j.warn("_data.appendLogData() took " + 
			 _writeTimer.read() + " ms");
	// now add the complete indexing entry with DeviceLogIndex object
	_writeTimer.clear();
	_writeTimer.start();

	_index.addIndexEntry(newIndexEntry);

	_writeTimer.stop();
	if ( _writeTimer.read() > 1000)
	    _log4j.warn("_data.addDeviceLogIndex() took " + 
			 _writeTimer.read() + " ms");
	}else{
	_data.appendLogData(serializedPacket, newIndexEntry);
	_index.addIndexEntry(newIndexEntry);
	}
	try {
	    // close objectOutput, byteOutput
	    objectOutput.close();
	    byteOutput.close();
	}
	catch(Exception e) {
	    _log4j.error("appendPacket:append exception on close", 
			  e);
	}
		
	if (timeError) {
	    // Time rollback was detected; append a message packet to log
	    DeviceMessagePacket message = 
		new DeviceMessagePacket(packet.sourceID());
			
	    message.setMessage(System.currentTimeMillis(), 
			       ("Time rollback detected in packet seqNo=" + 
				packet.sequenceNo()).getBytes());
			
	    appendPacket(message, true, false);
	}
		
	return;
    }
	
    /** This method appends the specified <code>DevicePacket</code> object to
	the log that is being maintained. The object is serialized and 
	subsequently written to a data file while indexing information is 
	maintained. This version of appendPacket() will automatically set 
	the packet's sequence number and metadata reference.
	@param packet <code>DevicePacket</code> object to append to active log
    */
    public synchronized void appendPacket(DevicePacket packet) {
	appendPacket(packet, true, true);
    }



    /** returns a <code>DevicePacketSet</code> object populated with
	<code>DeviceLog</code> objects (i.e. a result set) that matches
	the key comparison criterion specified by the parameters, up
	to the maximum number of packets specified.
	@param startKey beginning key value (inclusive) of the result set
	@param endKey ending key value (inclusive) of the result set
	@param maxEntries maximum number of packets to include in the set
	@see java.util.Vector
	@see org.mbari.siam.distributed.DevicePacket
    */
    public DevicePacketSet getPackets(long startKey, long endKey,
				      int maxEntries) 
	throws NoDataException {
		
	// query the range and get the number of index entries
	int nEntries = _index.nEntries(startKey, endKey);
		
	boolean complete = true;
		
	if (nEntries > maxEntries) {
	    complete = false;
	    nEntries = maxEntries;
	}
		
	// query the range and get a Vector of index entries
	Vector indexSet = _index.getEntries(startKey, nEntries);
		
	// Key(s) out of range
	if (indexSet == null) {
	    _log4j.info("No data for device " + _deviceId);
	    throw new NoDataException();
	}
		
	_log4j.debug("getPackets(): Found " 
		      + indexSet.size()
		      + " entries for device " + _deviceId);
		
	if (complete)
	    _log4j.debug(" Set is complete");
	else
	    _log4j.debug(" Set is INCOMPLETE");
		
		
	// for each index entry, get the DevicePacket
	int i = 0;
	DeviceLogIndex.Entry indexEntry;
		
	// iterate through the vector, getting each index entry
	// and replacing it with the packet in the data log
		
	while (i < indexSet.size()) {
	    indexEntry = (DeviceLogIndex.Entry)indexSet.elementAt(i);
	    indexSet.setElementAt((Object)this.getPacket(indexEntry), i);
	    i++;
	}
		
	return new DevicePacketSet(indexSet, complete);
    }
	

	
    /**
       Call getPacketKeyRange() - calls getPackets().
       getPacketKeyRange() is an obsolete method name, but we keep it
       for patch compatibility.
    */
    public DevicePacketSet getPacketKeyRange(long startKey, long endKey,
					     int maxEntries) 
	throws NoDataException {

	return getPackets(startKey, endKey, maxEntries);
    }


    /**
     * Return the last sequential packet in the log.
     * @return last sequential packet (DevicePacket)
     * @throws NoDataException
     */
    public DevicePacket getLastPacket() throws NoDataException {

	DeviceLogIndex.Entry entry = new DeviceLogIndex.Entry();
		
	_index.getIndexEntry(_index.getMaxIndex(), entry);
		
	return getPacket(entry);
    }
	
	
    /** returns the next unread DevicePacket in sequential order. 
	@see org.mbari.siam.distributed.DevicePacket
    */
    public DevicePacket getNextPacket() throws NoDataException {
	if (_index.nUnreadEntries() > 0) {
	    DeviceLogIndex.Entry indexEntry = _index.getNextIndexEntry();
	    return this.getPacket(indexEntry);
	}
	else {
	    throw new NoDataException("No unread entries");
	}
    }



	
    protected synchronized DevicePacket getPacket(DeviceLogIndex.Entry indexEntry) 
	throws NoDataException {

	DevicePacket retPacket = null;
	byte[] serializedPacket;
	ByteArrayInputStream byteInput;
	ObjectInputStream objectInput;
		

	if (indexEntry == null) {
	    throw new NoDataException("indexEntry is null");
	}
		
	serializedPacket = _data.readLogData(indexEntry);
		
	if (serializedPacket != null) {
	    try {
		byteInput = new ByteArrayInputStream(serializedPacket);
		byteInput.mark(SYNC_PATTERN.length);
		if (byteInput.read(_syncByteBuf, 0, SYNC_PATTERN.length) != 
		    SYNC_PATTERN.length) {
		    throw new NoDataException("couldn't read sync bytes");
		}
		// Check for sync pattern bytes
		boolean gotSync = true;
		for (int i = 0; i < SYNC_PATTERN.length; i++) {
		    if (_syncByteBuf[i] != SYNC_PATTERN[i]) {
			gotSync = false;
			break;
		    }
		}

		if (!gotSync) {
		    _log4j.warn("getPacket: Sync not found");
		    byteInput.reset();
		}

		objectInput = new ObjectInputStream(byteInput);		


		retPacket = (DevicePacket)objectInput.readObject();
		//retPacket=DevicePacket.deserialize((DevicePacket)retPacket,objectInput);
				
		objectInput.close();
		byteInput.close();
				
	    } catch (StreamCorruptedException sce) {
		_log4j.error("getPacket: object deserialize error",
			      sce);
		throw new NoDataException("StreamCorruptedException: " + 
					  sce.getMessage());
	    } catch (IOException ioe) {
		_log4j.error("getPacket: io error", ioe);
		throw new NoDataException("IOException: " + 
					  ioe.getMessage());
	    } catch (ClassNotFoundException cnfe) {
		_log4j.error("getPacket: class not found",
			      cnfe);
		throw new NoDataException("ClassNotFoundException: " + 
					  cnfe.getMessage());
	    }
	}	

	return (DevicePacket)retPacket;	
    }
	
	
    /** Return total number of packets in log. */
    public int nPackets() {
		
	// grab the journal entry
	DeviceLogIndex.JournalEntry journalEntry =
	    _index.getJournalEntry();
		
	if (journalEntry == null) {
	    _log4j.debug("nPackets(): no index entries\n");
	    return 0;
	}
		
	// Return number of entries 
	return journalEntry.getNumEntries();
    }
	
	
	
	
    /** returns the number of unread packets remaining if packets are retrieved
	using the <code>getNextPacket()</code> method
	@see DeviceLog#getNextPacket()
    */
    public int getNumUnreadPackets() {
	return _index.nUnreadEntries();
    }
	
    /** returns the minimum key that has been registered by all DevicePacket
	storage operations
	@see DeviceLog#getPacketKeyRange
    */
    public long getMinTimestamp() {
	return _index.getMinTimestamp();
    }
	
    /** returns the maximum key that has been registered by all DevicePacket
	storage operations
	@see DeviceLog#getPacketKeyRange
    */
    public long getMaxTimestamp() {
	return _index.getMaxTimestamp();
    }
	
	
    /** Close the DeviceLog and associated files. This DeviceLog instance
	is no longer usable after close() has been called. */
    public synchronized void close() throws IOException {
	_index.close();
	_data.close();
    }
    /**
       Returns the offset (in bytes) of the beginning of the next
       sync pattern, starting the search at offset bytes into
       the file.

       Originally, this used a FileInputStream wrapper around the
       RandomAccessFile, but this resulted in an IOException with
       a "handle not valid" message.

       Alternatively, a read implementation has been implented
       (DeviceLogData.readBytes()) that uses RandomAcessFile.read() 
       directly.

       @return offset of next sync pattern on success
       @return DeviceLog.EOF if end of file is reached before finding the sync pattern
       @return DeviceLog.SYNC_NOT_FOUND if sync pattern is not found for any other reason
    */
    public static long getNextSync(DeviceLog log, long offset){
	long syncStart=DeviceLog.SYNC_NOT_FOUND;
	boolean foundSyncStart=false;
	boolean gotSync=true;
	boolean eof=false;
	int test;

	    while(eof==false){
		try{

		// seek last offset (file pointer may be
		// changed by other operations?)
		log._data.seek(offset);

		// clear buffer for sync pattern bytes
		for(int i = 0; i < DeviceLog.SYNC_PATTERN.length; i++)
		    log._syncByteBuf[i]='\0';

		// Detect start of sync pattern
		foundSyncStart=false;
		while(foundSyncStart==false && eof==false){
		    log._syncByteBuf[0]='\0';
		    test=log._data.readBytes(log._syncByteBuf,0,1);
		    offset++;
		    if(test==-1){
			_log4j.debug("end of file detected");
			return DeviceLog.EOF;
		    }
		    if(log._syncByteBuf[0] == DeviceLog.SYNC_PATTERN[0]){
			foundSyncStart=true;
			break;
		    }
		}

		// check for the rest of sync pattern
		gotSync=true;
		for(int i = 1; i <= (DeviceLog.SYNC_PATTERN.length-1); i++) {
		    test=(log._data.readBytes(log._syncByteBuf,i,1));
		    if(test== -1L)
			return DeviceLog.EOF;
		    offset++;
		    if(log._syncByteBuf[i] != DeviceLog.SYNC_PATTERN[i]){
			gotSync=false;
			break;
		    }
		}
		if(gotSync){
		    syncStart=offset-DeviceLog.SYNC_PATTERN.length;
		    break;
		}
	    }catch(EOFException e){
		_log4j.error("getNextSync: reached end of file at offset "+offset);
		syncStart=DeviceLog.EOF;
		break;
	    }catch(IOException e){
		_log4j.error("getNextSync: exception at offset "+offset);
		e.printStackTrace();
		break;
	    }
	}
	return syncStart;    
    }

    /**
       When applications like logView and logPublish encounter
       corrupt data packets or index entries, they fail and 
       can not proceed beyond the bad packets. 

       This method is used to read each packet in the specified 
       log, report errors, and optionally construct a new index 
       (.idx) file if repair is set to true. It does not change 
       the original data file or index. When the -repair option
       is given, it produces a new index file in the current
       working directory:

          <id>_<segment>.rebuilt.idx

       To invoke this method, use

           logView -check [-repair] <id> <directory>

       Other logView options (e.g. filters) are ignored inside check.

       The check() method works by traversing the binary sync patterns
       in the data (.dat) file. When it finds a sync pattern, it
       finds the next sync pattern and attempts to read everything 
       between as a DevicePacket. If the read succeeds, a new 
       IndexEntry is added to the new index (.idx) file. Otherwise,
       an error is counted, and the process repeats.

       Note that because the index and its journal contain no
       information about what types of packets are in the log,
       it is not possible to directly detect when a metadata 
       packet is missing or corrupt. check() tracks metadata
       packets and looks at each packet's metadata reference 
       value on the fly. It keeps track of metadata packets
       and references, and reports the number of references
       without corresoponding metadata packets. This may report
       a missing packet for the first packet, and would fail
       to detect when a reference value comes before its 
       metadata packet (which should not happen in general).

       Originally, this used a FileInputStream wrapper around the
       RandomAccessFile, but this resulted in an IOException with
       a "handle not valid" message.

       Alternatively, a read implementation has been implented
       (DeviceLogData.readBytes()) that uses RandomAcessFile.read() 
       directly.

    */
    public static void check(DeviceLog log,boolean repair){
	int recordIndex=0;
	DeviceLogIndex.Entry _indexEntry = new DeviceLogIndex.Entry();
	DeviceLogIndex newIndex=null;

	long offset=0L;
	long syncStart=0L;
	boolean foundSyncStart=false;
	boolean gotSync=true;
	boolean eof=false;

	boolean readingRecord=false;
	int test;
	int readSizeMismatchErrors=0;
	int eofErrors=0;
	int corruptErrors=0;
	int indexErrors=0;
	int unknownErrors=0;
	int missingMetadataErrors=0;
	int newIndices=0;

	long metadataPackets=0L;
	long sensorDataPackets=0L;
	long summaryPackets=0L;
	long messagePackets=0L;
	long measurementPackets=0L;

	long seqNo=0L;
	long mdRef=0L;
	Vector mdRefs=new Vector();
	Vector missingMDRefs=new Vector();
	Vector parentIDs=new Vector();

	long syncTest;
	try{

	    if(repair==true){
		newIndex=new DeviceLogIndex(log._data._deviceId,log._data._segment,".rebuilt",".");
	    }
	    log._data.seek(0L);

	    //FileInputStream fis=log._data.getFileInputStream();

	    while(eof==false){
		// find the next sync pattern
		gotSync=false;
		syncTest=getNextSync(log, offset);
		_log4j.debug("next sync from offset "+offset+" is "+syncTest);

		// if we don't find a sync pattern
		// we're done
		if(syncTest>=0L){
		    syncStart=syncTest;
		    offset=syncStart+DeviceLog.SYNC_PATTERN.length;
		    gotSync=true;
		}else{
		    break;
		}

		// if complete sync pattern detected, 
		// continue checking record and index 
		// entry for errors, 
		// else keep going
		if(gotSync){

		    _log4j.debug("got sync at "+syncStart);

		    try {

			// find the next sync pattern
			// to compute data size
			long dataStart=syncStart;
			long dataStop=getNextSync(log,offset);
			if(dataStop<=0L){
			    dataStop=log._data.getLength();
			}
			long dataSize=(dataStop-dataStart);
			_log4j.debug("dataStart:"+dataStart+" dataStop:"+dataStop+" data size:"+dataSize);
			_log4j.debug("current offset:"+offset+" next offset:"+dataStop);

			// increase recordIndex 
			// (before trying to read record, otherwise
			// an exception could cause it not get incremented)
			recordIndex++;

			// point to the start of the record
			// (just after the sync pattern)
			log._data.seek(syncStart+DeviceLog.SYNC_PATTERN.length);

			// read the number of record bytes 
			// (distance to next sync pattern)
			// into a byte array
			int readLen=(int)(dataSize-DeviceLog.SYNC_PATTERN.length);
			byte[] objectBytes=new byte[readLen];
			//int recordBytes=fis.read(objectBytes);
			int recordBytes=log._data.readBytes(objectBytes,0,readLen);

			// compare bytes read (not including sync pattern)
			// to bytes expected
			if(recordBytes!=(dataSize-DeviceLog.SYNC_PATTERN.length)){
			    _log4j.warn("Read size mismatch - record:"+(recordIndex-1)+" offset:"+offset);
			    _log4j.warn("expected:"+(dataSize-DeviceLog.SYNC_PATTERN.length)+" read:"+recordBytes);
			    readSizeMismatchErrors++;
			}


			// create an input stream to read the record
			// (use a ByteArrayInputStream wrapped in an
			// ObjectInputStream)
			ByteArrayInputStream byteInput = 
			    new ByteArrayInputStream(objectBytes);
			//_log4j.debug("byteInput.available:"+byteInput.available());
			ObjectInputStream objectInput = 
			    new ObjectInputStream(byteInput);

			// Try to read the packet from the input
			// stream.
			DevicePacket packet = null;
			_log4j.debug("reading packet....");
			readingRecord=true;
			packet = (DevicePacket)objectInput.readObject();			    
			readingRecord=false;
			_log4j.debug("packet read OK....");
			_log4j.debug("data:\n"+packet.toString());
			
			// grab the sequence number and 
			// metadata reference since we'll
			// need them in multiple places below
			// and want to reduce overhead.
			seqNo=packet.sequenceNo();
			mdRef=packet.metadataRef();
			Long lmd=new Long(mdRef);

			// do packet type stats and 
			// metadata ref calcs
			if(packet instanceof MetadataPacket){
			    metadataPackets++;
			    if(mdRefs.contains(new Long(seqNo))==false){
				mdRefs.add(new Long(seqNo));
			    }
			}
			if(packet instanceof SensorDataPacket)
			    sensorDataPackets++;
			if(packet instanceof SummaryPacket)
			    summaryPackets++;
			if(packet instanceof DeviceMessagePacket)
			    messagePackets++;
			if(packet instanceof MeasurementPacket)
			    measurementPackets++;

			if(mdRefs.contains(lmd)==false){
			    missingMetadataErrors++;
			    if(missingMDRefs.contains(lmd)==false)
				missingMDRefs.add(lmd);
			    _log4j.warn("Missing Metadata - record:"+(recordIndex-1)+" offset:"+offset+" mdRef:"+mdRef);
			}

			Long parentID=new Long(packet.getParentId());
			if(parentIDs.contains(parentID)==false){
			    parentIDs.add(parentID);
			}

			// if repair option specified, generate a
			// new DeviceLogIndex.Entry based on the 
			// DevicePacket header info
			if(repair==true){
			    DeviceLogIndex.Entry tempEntry = 
				new DeviceLogIndex.Entry(packet.systemTime(),
							 (int)dataSize,
							 dataStart,
							 seqNo);
			    _log4j.debug("writing new index entry:"+tempEntry);
			    newIndex.addIndexEntry(tempEntry);
			    if(packet instanceof MetadataPacket)
				newIndex.updateMetadataRef(seqNo);
			    newIndices++;
			    _log4j.debug("writing index entry OK");
			}

			// adjust offset past bytes read
			// so we don't go through them again
			// looking for next sync
			// (note that if the read fails, we pick
			// up after just after the sync pattern
			// that failed)
			offset+=recordBytes;

			try {
			    // close objectOutput, byteOutput
			    objectInput.close();
			    byteInput.close();
			}
			catch(Exception e) {
			    _log4j.error("exception closing input streams", 
					 e);
			}
			
		    }catch(StreamCorruptedException s){
			_log4j.warn("StreamCorruptedException - record:"+(recordIndex-1)+" offset:"+offset);
			_log4j.debug(s);
			corruptErrors++;
			// could dump bad data to a file
		    }catch(EOFException e){
			if(readingRecord==true){
			    _log4j.warn("EOFException - record:"+(recordIndex-1)+" offset:"+offset);
			    eofErrors++;
			}else{
			    _log4j.debug("EOFException - record:"+(recordIndex-1)+
					 " offset:"+offset+" (not reading record)");
			}
		    }catch(Exception e){
			_log4j.warn("Exception ("+e.getMessage()+") - record:"+(recordIndex-1)+" offset:"+offset);
			_log4j.debug(e);
			unknownErrors++;
		    }
		}
	    }

	    // print a summary of the findings
	    System.out.println("");
	    System.out.println("Log Check Summary:");
	    System.out.println("");
	    System.out.println("Device ID        : "+log.getDeviceId());
	    System.out.println("repair index     : "+(repair==true?"YES":"NO"));
	    System.out.println("sync patterns    : "+recordIndex);
	    System.out.println("valid records    : "+(metadataPackets+sensorDataPackets+
							 summaryPackets+messagePackets+
							 measurementPackets));
	    System.out.println("parentIDs        : "+parentIDs.size());
	    System.out.println("");
	    System.out.println("# Index Summary");
	    System.out.println(" entries         : "+log._index.getJournalEntry().getNumEntries());
	    long minKey=log._index.getJournalEntry().getMinKey();
	    long maxKey=log._index.getJournalEntry().getMaxKey();
	    String mindate=DateFormat.getDateTimeInstance().format(new Date(minKey));
	    String maxdate=DateFormat.getDateTimeInstance().format(new Date(maxKey));
	    System.out.println(" min key         : "+minKey+" ("+mindate+")");
	    System.out.println(" max key         : "+maxKey+" ("+maxdate+")");
	    System.out.println(" last seq number : "+log._index.getJournalEntry().getLastSequenceNumber());
	    System.out.println(" last md ref     : "+log._index.getJournalEntry().getLastMetadataRef());
	    System.out.println(" total md refs   : "+(mdRefs.size()+missingMDRefs.size()));

	    if(repair==true){
		System.out.println("");
		System.out.println("# New Index Summary");
		System.out.println(" entries         : "+newIndex.getJournalEntry().getNumEntries());
		minKey=newIndex.getJournalEntry().getMinKey();
		maxKey=newIndex.getJournalEntry().getMaxKey();
		mindate=DateFormat.getDateTimeInstance().format(new Date(minKey));
		maxdate=DateFormat.getDateTimeInstance().format(new Date(maxKey));
		System.out.println(" min key         : "+minKey+" ("+mindate+")");
		System.out.println(" max key         : "+maxKey+" ("+maxdate+")");
		System.out.println(" last seq number : "+newIndex.getJournalEntry().getLastSequenceNumber());
		System.out.println(" last md ref     : "+newIndex.getJournalEntry().getLastMetadataRef());
	    }
	    System.out.println("");
	    System.out.println("# Record Summary");
	    System.out.println(" metadataPackets    : "+metadataPackets);
	    System.out.println(" sensorDataPackets  : "+sensorDataPackets);
	    System.out.println(" summaryPackets     : "+summaryPackets);
	    System.out.println(" messagePackets     : "+messagePackets);
	    System.out.println(" measurementPackets : "+measurementPackets);

	    System.out.println("");
	    System.out.println("# Error Summary");
	    System.out.println(" stream corrupted   : "+corruptErrors);
	    System.out.println(" eof errors         : "+eofErrors);
	    System.out.println(" metadata not found : "+missingMetadataErrors);
	    System.out.println(" invalid md packets : "+missingMDRefs.size());
	    System.out.println(" unknown errors     : "+unknownErrors);
	    System.out.println(" read size mismatch : "+readSizeMismatchErrors);

	    if(repair==true)
		newIndex.close();

	}catch(IOException e){
	    e.printStackTrace();
	}
    }

}
