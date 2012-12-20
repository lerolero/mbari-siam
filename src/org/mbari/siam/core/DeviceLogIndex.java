/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.SyncFailedException;
import java.util.Vector;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.mbari.siam.distributed.NoDataException;

/* $Id: DeviceLogIndex.java,v 1.4 2012/12/17 21:32:42 oreilly Exp $ */

/**
 * This class is utilized along with <code>DeviceLogData</code> to implement
 * an indexed data store capability. Query over a range of possible keys is also
 * supported. The database key is assumed to be a <code>long</code> quantity.
 * 
 * NOTE: THIS IMPLEMENTATION ASSUMES THAT INDEX IS SORTED IN ASCENDING KEY
 * ORDER!!!
 * 
 * @see DeviceLogData
 * @see DeviceLog
 * @author Tim Meese
 */
public class DeviceLogIndex {
    /** log4j logger */
    static Logger _log4j = Logger.getLogger(DeviceLogIndex.class);
	
    // Journal entry starts at beginning of file
    static final int JOURNAL_ENTRY_OFFSET = 0;
	
    // Index of first data entry
    // (Index 0 points to "journalling" entry)
    private static final int FIRST_DATA_INDEX = 1;
	
    // Define constants for the type of key search
    private static final int EXACT_MATCH_SEARCH = 0;
    private static final int LOWER_BOUND_SEARCH = 1;
    private static final int UPPER_BOUND_SEARCH = 2;
	
    /** Log4j logger */
    static Logger _logger = Logger.getLogger(DeviceLogIndex.class);
	
    protected RandomAccessFile _idxFile = null;
	
    /** Current size of index file (bytes) */
    protected long _currentIdxFileExtent = 0;
	
    /** Index of current last entry in log */
    private int _maxIndex = -1;
	
    /** Index of last entry that was accessed "sequentially" */
    private int _sequentialAccessedIndex = 0;
	
    /** Lowest timestamp in index */
    private long _minTimestamp = java.lang.Long.MAX_VALUE;
	
    /** Highest timestamp in index */
    private long _maxTimestamp = java.lang.Long.MIN_VALUE;
	
    /** Sequence number of last entry in index file */
    private long _lastSequenceNumber = 0L;
	
    /** Latest metadata reference */
    private long _lastMetadataRef = 0L;
	
    /** Device ID */
    private long _deviceId;
	
    // For internal use
    private Entry _indexEntry = new Entry();
	
	
    /**
     * default constructor
     * 
     * @param deviceId
     *            ISI Device ID
     * @param segmentNum  segement number
     */
	
    protected DeviceLogIndex(long deviceId, int segmentNum, String directory)
	throws FileNotFoundException, IOException {
	this(deviceId,segmentNum,"",directory);
    }
    protected DeviceLogIndex(long deviceId, int segmentNum, String suffix, String directory)
	throws FileNotFoundException, IOException {
		
	_deviceId = deviceId;
	String indexFileName = getFileName(segmentNum, suffix, directory);
		
	File openAttempt = new File(indexFileName);
		
	if (openAttempt.exists() == true) {
			
	    try {
		_idxFile = new RandomAccessFile(indexFileName, "rw");
	    } 
	    catch (FileNotFoundException e) {
		_logger.error("Constructor FileNotFoundException:",
			      e);
		throw e;
	    }
			
	    // restore state from existing file
			
	    // compute file size (extent)
	    try {
		_currentIdxFileExtent = _idxFile.length();
	    } catch (IOException ioe) {
		_logger.error("constructor IOException: ",
			      ioe);
		throw ioe;
	    }
			
	    // restore state from journal entry
			
	    restoreJournalInfo();
	} else {
			
	    // no state restore necessary -- create new file
	    try {
		_idxFile = new RandomAccessFile(indexFileName, "rw");
	    } catch (FileNotFoundException fnfe) {
		_logger.error("Ctor: #2: file \""
			      + indexFileName + 
			      "\" not found", fnfe);
		throw fnfe;
	    }
			
	    // initialize the journal entry
			
	    initJournalEntry();
	}
		
    }
	
    /** Return name of index file. */
    protected String getFileName(int segmentNum, String directory) {
	return getFileName(segmentNum,"",directory);
    }
    protected String getFileName(int segmentNum, String suffix, String directory) {
	return new String(directory + File.separator + _deviceId + "_" + 
			  segmentNum+
			  suffix+ ".idx");
    }
	
    /** Initialize index file journal entry. */
    private void initJournalEntry() {
		
	// update index entry info
	_maxIndex++;
	_sequentialAccessedIndex++;
		
	// bump the index file extent
	_currentIdxFileExtent += JournalEntry.LOG_JOURNAL_ENTRY_SIZE;
		
	// update the journal entry
	_logger.debug("_maxIndex=" + _maxIndex
		      + ", _sequentialAccessedIndex=" + _sequentialAccessedIndex
		      + ", _lastSequenceNumber=" + _lastSequenceNumber);
		
	updateJournalEntry();
    }
	
    private void updateJournalEntry() {
	JournalEntry journalEntry = 
	    new JournalEntry(_maxIndex, _sequentialAccessedIndex, 
			     _minTimestamp, _maxTimestamp,
			     _lastSequenceNumber, _lastMetadataRef);
		
	try {
	    _idxFile.seek(JOURNAL_ENTRY_OFFSET);
	} catch (IOException ioe) {
	    _logger.error("updateJournalEntry() seek fail", ioe);
	}
		
	// write the journal entry
	journalEntry.toFile(_idxFile);
    }
	
    private void restoreJournalInfo() {
		
	// get the journal entry (entry 0) from the index file
	try {
	    _idxFile.seek(JOURNAL_ENTRY_OFFSET);
	} catch (IOException ioe) {
	    _logger.error("restoreJournalInfo() seek error", ioe);
	}
		
	JournalEntry journalEntry = new JournalEntry();
	journalEntry.fromFile(_idxFile);
		
	// get state information from the journal entry
		
	_maxIndex = journalEntry.getNumEntries();
	_sequentialAccessedIndex = journalEntry.getLastEntryAccessed();
		
	_minTimestamp = journalEntry.getMinKey();
	_maxTimestamp = journalEntry.getMaxKey();
		
	_lastSequenceNumber = journalEntry.getLastSequenceNumber() + 1;
	_lastMetadataRef = journalEntry.getLastMetadataRef();
    }
	
    /**
     * adds a <code>Entry</code> object to the current index
     * file
     * 
     * @param entry
     *            index entry to add to the current index file
     */
    protected synchronized void addIndexEntry(Entry entry) {

	if (_maxIndex == java.lang.Integer.MAX_VALUE) {
	    // TODO: throw an exception that can be caught by caller to
	    // start a new sequence of files
	    _logger.warn("addIndexEntry: invalid maxIndex");
	    return;
	}
		
	if (entry == null) {
	    _logger.warn("addIndexEntry: cannot add null entry");
	    return;
	}

	long key = entry.getKey();
		
	if (key < _minTimestamp) {
	    _minTimestamp = key;
	}
		
	if (key > _maxTimestamp) {
	    _maxTimestamp = key;
	}
		
	entry.setEntryIndex(++_maxIndex);
		
	try {
	    _idxFile.seek(_currentIdxFileExtent);
	} catch (IOException ioe) {
	    _logger
		.error("addDeviceLogIndex: IOException while seeking "
		       + ioe);
	}

		
	// write the index entry
	entry.toFile(_idxFile);
		
	// update the index file extent
	_currentIdxFileExtent += Entry.LOG_INDEX_ENTRY_SIZE;
		
	//update the last packet Sequence Number
	_lastSequenceNumber = entry.getSequenceNumber();
		
	// update the journal entry
	updateJournalEntry();

    }
	
    /**
     * retrieve the <code>Entry</code> corresponding to the
     * given <code>entryIndex</code> in the index file
     * 
     * @param entryIndex
     *            number (1-based) of the index entry to retrieve
     * @param entry
     *            <code>Entry</code> object to use
     */
    protected synchronized Entry getIndexEntry(int entryIndex, Entry entry) 
	throws NoDataException {
		
	if ((entryIndex > _maxIndex) || (entry == null)) {
	    throw new NoDataException("DeviceLogIndex.getIndexEntry()");
	}
		
	// don't return the JournalEntry...
		
	if (entryIndex < FIRST_DATA_INDEX) {
	    throw new NoDataException("index is below first data index");
	}
		
	// TODO: add support for entry caching and lookahead cache update
	//       based on sequential record access
		
	// seek to the appropriate file position
		
	long entryOffset = 
	    (((entryIndex - 1) * Entry.LOG_INDEX_ENTRY_SIZE) + JournalEntry.LOG_JOURNAL_ENTRY_SIZE);
		
	if (entryOffset < _currentIdxFileExtent) {
	    try {
		_idxFile.seek(entryOffset);
	    } catch (IOException ioe) {
		_logger.error("getEntry: IOException: seek", ioe);
	    }
			
	    // instantiate a new Entry
			
	    entry.fromFile(_idxFile);
	}
		
	return entry;
    }
	
    protected synchronized JournalEntry getJournalEntry(
							JournalEntry journalEntry) {
		
	int entryIndex = 0;
		
	if ((entryIndex > _maxIndex) || (journalEntry == null)) {
	    return (JournalEntry) null;
	}
		
	long entryOffset = 0;
		
	if (entryOffset < _currentIdxFileExtent) {
	    try {
		_idxFile.seek(entryOffset);
	    } catch (IOException ioe) {
		_logger
		    .error("getEntry: seek"
			   + ioe.getMessage());
	    }
	}
		
	// instantiate a new JournalEntry
		
	journalEntry.fromFile(_idxFile);
		
	return journalEntry;
    }
	
    protected JournalEntry getJournalEntry() {
	return getJournalEntry(new JournalEntry());
    }
	
	
    /**
     * get the minimum key value that has been registered by all index add
     * operations
     */
    protected long getMinTimestamp() {
	return _minTimestamp;
    }
	
    /**
     * get the maximum key value that has been registered by all index add
     * operations
     */
    protected long getMaxTimestamp() {
	return _maxTimestamp;
    }
	
    /**
     * get the lastSequenceNumber value that has been registered
     */
    protected long getLastSequenceNumber() {
	return _lastSequenceNumber;
    }
	
    /**
     * get the lastMetadataRef value that has been registered
     */
    protected long getLastMetadataRef() {
	return _lastMetadataRef;
    }
	
    /**
     * set the lastMetadataRef value that has been registered
     */
    protected void updateMetadataRef(long mdref) {
	_lastMetadataRef = mdref;
	updateJournalEntry();
    }
	
    /**
     * Returns the number of entries that match the key comparison criteria
     * specified by the parameters
     * 
     * @param beginKey
     *            starting key value (inclusive) of the result set
     * @param endKey
     *            ending key value (inclusive) of the result set
     * @see Entry
     */
    protected int nEntries(long beginKey, long endKey) 
	throws NoDataException {
	// throws invalidQueryRangeException()
	_logger.debug("numberOfEntries, _deviceId=" + _deviceId);
	_logger.debug("beginKey=" + beginKey + ", _minTimestamp=" + _minTimestamp);
	_logger.debug("endKey=" + endKey + ", _maxTimestamp=" + _maxTimestamp);
		
	if (beginKey > endKey) {
	    _logger.error("numberOfEntries(): invalid key range for "
			  + _deviceId);
			
	    return 0;
	} else if (beginKey > _maxTimestamp) {
	    _logger.info("nEntries(): beginKey out of range for "
			 + "device " + _deviceId);
	    return 0;
	} else if (endKey < _minTimestamp) {
	    _logger.info("nEntries(): endKey out of range for "
			 + "device " + _deviceId);
	    return 0;
	}
		
	int beginIndex = findKeyIndex(beginKey, SearchType.FIND_UPPER);
	int endIndex = findKeyIndex(endKey, SearchType.FIND_LOWER);
	if (beginIndex == -1 || endIndex == -1) {
	    _logger.info("nEntries(): begin or end out of range");
	    return 0;
	}
	Range range = getDuplicateTimestampRange(beginIndex);
	beginIndex = range.lowIndex;
	range = getDuplicateTimestampRange(endIndex);
	endIndex = range.highIndex;
		
	return endIndex - beginIndex + 1;
    }
	
	
    /**
     * returns a <code>Vector</code> object populated with
     * <code>Entry</code> objects (i.e. the result set) that
     * match the key comparison criteria specified by the parameters
     * 
     * @param beginKey
     *            starting key value (inclusive) of the result set
     * @param nEntries
     *            number of entries to read
     * @see java.util.Vector
     * @see Entry
     */
    protected Vector getEntries(long beginKey, long nEntries) 
	throws NoDataException {
		
	_logger.debug("getEntries(), _deviceId=" + _deviceId + ", beginKey="
		      + beginKey + ", nEntries=" + nEntries);
		
	// Find first entry with key >= beginKey
	int index = 0;
	if ((index = findKeyIndex(beginKey, SearchType.FIND_UPPER)) == -1) {
	    _logger.info("beginKey " + beginKey + " out of range for device " +
			 _deviceId);
			
	    _logger.info("getEntries: beginKey=" + beginKey);
	    _logger.info("getEntries: _maxTimestamp=" + _maxTimestamp);
	    throw new NoDataException("beginKey out of range of device " +
				      _deviceId + " log");
	}
		
	Range range = getDuplicateTimestampRange(index);
	index = range.lowIndex;

	return getEntries(index, nEntries);


    }

    /**
     * returns a <code>Vector</code> object populated with
     * <code>Entry</code> objects (i.e. the result set) from the log,
     * starting at index and consisting of nEntries.
     * 
     * @param beginIndex
     *            starting index of the result set
     * @param nEntries
     *            number of entries to read
     * @see java.util.Vector
     * @see Entry
     */
    protected Vector getEntries(int beginIndex, long nEntries) 
	throws NoDataException {
	Vector resultSet = null;
		
	int index = beginIndex;
	for (int nRead = 0; index <= _maxIndex && nRead < nEntries; 
	     index++, nRead++) {
	    Entry entry = new Entry();
	    try {
		getIndexEntry(index, entry);
	    }
	    catch (NoDataException e) {
		_logger.error("Null index entry", e);
	    }
	    if (resultSet == null) {
		resultSet = new Vector();
	    }
			
	    // add a reference to the index entry just returned
	    // to the result set and make a new index entry
	    resultSet.add(entry);
	}
		
	return resultSet;
    }

	
    /**
     * returns the next <code>Entry</code> in the index file
     * according to a sequential access method. Note that index entries obtained
     * with key range queries do not effect sequential queries.
     */
    protected Entry getNextIndexEntry() 
	throws NoDataException {
	Entry retEntry = null;
		
	if (nUnreadEntries() > 0) {
	    retEntry = getIndexEntry(_sequentialAccessedIndex++, _indexEntry);
			
	    // update the journal entry since we updated _sequentialAccessedIndex
	    updateJournalEntry();
	} else {
	    _log4j.debug("getNextEntry: no logs");
	}
		
	return retEntry;
    }
	
    /**
     * returns the number of unread <code>DeviceLog</code> objects by
     * returning the number of index entries that have not yet been accessed
     * using the sequential retrieval method (i.e. <code>
     getNextEntry()</code>)
    */
    protected synchronized int nUnreadEntries() {
	// index entry 0 always reserved for journal entry
	return (_maxIndex - _sequentialAccessedIndex);
    }
	
    /**
     * Close the index file. This DeviceLogIndex instance is no longer usable
     * after close() has been called.
     */
    protected void close() throws IOException {
	_idxFile.close();
    }
	
	
    /** Return index corresponding to search criterion */
    private synchronized int findKeyIndex(long targetKey, 
					  SearchType searchType) throws NoDataException {
		
	int high = _maxIndex + 1;
	int low = FIRST_DATA_INDEX - 1;
	int probe;
		
	while (high - low > 1) {
	    probe = (high + low) / 2;
	    getIndexEntry(probe, _indexEntry);
	    if (_indexEntry.getKey() < targetKey) {
		low = probe;
	    }
	    else {
		high = probe;
	    }
	}
		
	if (validIndex(high)) {
	    getIndexEntry(high, _indexEntry);
	    long highKey = _indexEntry.getKey();
	    if (highKey == targetKey) {
		// Found exact match
		return high;
	    }
	}
		
	if (validIndex(low) && validIndex(high)) {
	    // targetKey lies between two indices
	    if (searchType == SearchType.FIND_LOWER) 
		return low;
	    else if (searchType == SearchType.FIND_UPPER)
		return high;
	}
	if (high == _maxIndex + 1 && searchType == SearchType.FIND_LOWER) {
	    // targetKey is greater than highest key
	    return _maxIndex;
	} 
	else if (low == FIRST_DATA_INDEX - 1
		 && searchType == SearchType.FIND_UPPER) {
	    // targetKey is lower than lowest key
	    return FIRST_DATA_INDEX;
	} 
		
	return -1;		
    }
	
	
    /** Return start and end indices for run of duplicate key that
     * includes specified index. */
    private Range getDuplicateTimestampRange(int index) 
	throws NoDataException {
	Range range = new Range();
		
	getIndexEntry(index, _indexEntry);
	long key = _indexEntry.getKey();
	int i;
	// Look for underlying duplicate keys
	for (i = index-1; i >= FIRST_DATA_INDEX; i--) {
	    getIndexEntry(i, _indexEntry);
	    if (_indexEntry.getKey() != key) {
		break;
	    }
	}
	range.lowIndex = i + 1;
		
	// Look for overlying duplicate keys
	for (i = index+1; i <= _maxIndex; i++) {
	    getIndexEntry(i, _indexEntry);
	    if (_indexEntry.getKey() != key) {
		break;
	    }
	}
	range.highIndex = i - 1;
		
	return range;
    }
	
	
    /** Return maximum index in log. */
    protected int getMaxIndex() {
	return _maxIndex;
    }
	
    /** Return true if specified index is in valid range, else return
     * false. 
     * @param index
     * @return true if valid, false otherwise
     */
    boolean validIndex(int index) {
	if (index >= FIRST_DATA_INDEX && index <= _maxIndex)
	    return true;
	else
	    return false;
    }
	
    /**
       This class provides a container for log journal information. It is 
       not meant to be instantiated directly, but is utilized by the 
       <code>DeviceLogIndex</code> class. 
    */
    protected static class JournalEntry {
		
	protected static final int LOG_JOURNAL_ENTRY_SIZE = 4+4+8+8+8+8;
		
	/** Number of entries, not including journal entry. */
	protected int _numEntries;
		
	/** Index of last sequentially-accessed entry. */
	protected int _lastEntryAccessed;
		
	/** Minimum key value in log. */
	protected long _minKey;
		
	/** Maximum key value in log. */
	protected long _maxKey;
		
	// Remember to adjust LOG_JOURNAL_ENTRY_SIZE if this is removed
	protected long _lastSequenceNumber;
	// Remember to adjust LOG_JOURNAL_ENTRY_SIZE if this is removed
	protected long _lastMetadataRef;
		
	/** No-argument constructor. */
	protected JournalEntry() {
	}
		
	/** Constructor with specified member values. */
	protected JournalEntry(int numEntries, int lastEntryAccessed, 
			       long minKey, long maxKey, 
			       long lastSequenceNumber, 
			       long lastMetadataRef) {
	    _numEntries = numEntries;
	    _lastEntryAccessed = lastEntryAccessed;
	    _minKey = minKey;
	    _maxKey = maxKey;
	    _lastSequenceNumber=lastSequenceNumber;
	    _lastMetadataRef=lastMetadataRef;
	}
		
	/** Return log's minimum key value. */
	protected long getMinKey() {
	    return _minKey;
	}
		
	/** Return log's maximum key value. */
	protected long getMaxKey() {
	    return _maxKey;
	}
		
	/** Return index of last sequentially accessed entry. */
	protected int getLastEntryAccessed() {
	    return _lastEntryAccessed;
	}
		
	/** Return number of index entries (not including journal entry). */
	protected int getNumEntries() {
	    return _numEntries;
	}
		
	/** Return last packet sequence number. */
	protected long getLastSequenceNumber() {
	    return _lastSequenceNumber;
	}
		
	/** Return last packet metadata reference. */
	protected long getLastMetadataRef() {
	    return _lastMetadataRef;
	}
		
	/** serializes this index entry and writes it to the index file
	    @param file <code>RandomAccessFile</code> to write index entry to 
	*/
	protected synchronized void toFile(RandomAccessFile file) {
	    FileDescriptor fd;
			
	    try {
		file.writeInt(_numEntries);
		file.writeInt(_lastEntryAccessed);
		file.writeLong(_minKey);
		file.writeLong(_maxKey);
		file.writeLong(_lastSequenceNumber);
		file.writeLong(_lastMetadataRef);
				
		// caller has to update index file extent
		// currentIdxFileExtent += 
		//     JournalEntry.LOG_JOURNAL_ENTRY_SIZE;
	    }
	    catch(IOException ioe) {
		_log4j.error("! IOException on journal entry write");
	    }
			
	    try {
		fd = file.getFD();
		try {
		    fd.sync();
		}
		catch (SyncFailedException sfe) {
		    _log4j.error("JournalEntry:toFile: sync fail");
		}
	    }
	    catch (IOException ioe) {
		_log4j.error("JournalEntry:toFile: getFD fail");
	    }
	}

	/** Deserializes this index entry from the index file
	    @param file <code>RandomAccessFile</code> to read index entry from
	*/
	protected synchronized void fromFile(RandomAccessFile file) {
	    try {
		_numEntries = file.readInt();
		_lastEntryAccessed = file.readInt();
		_minKey = file.readLong();
		_maxKey = file.readLong();
		_lastSequenceNumber = file.readLong();
		_lastMetadataRef = file.readLong();
	    }
	    catch (IOException ioe) {
		_log4j.error("! IOException on journal entry read");
	    }
	}
    }
	
	
    /**
       This class provides a container for each indexing information entry. It is 
       not meant to be instantiated directly, but is utilized by the 
       <code>DeviceLogIndex</code> class. 
    */
	
    protected static class Entry {
		
	protected static final int LOG_INDEX_ENTRY_SIZE = 4+4+8+8+8;
		
	protected int  _entryIndex;
	protected int  _dataSize;
	protected long _dataOffset;
	protected long _key;
	protected long _sequenceNumber; // remember to adjust LOG_INDEX_ENTRY_SIZE if this is removed
		
	/** optional constructor
	    @param key initial value of the key associated with this index entry
	    @param dataSize size of the data region associated with this entry
	    @param dataOffset offset of the data region associated with this entry
	    @param sequenceNumber sequenceNumber of the data region associated with this entry
	*/
	protected Entry(long key, int dataSize, long dataOffset,long sequenceNumber) {
	    _dataSize   = dataSize;
	    _dataOffset = dataOffset;
	    _key        = key;
	    _sequenceNumber = sequenceNumber;
	}

	/* copy constructor */
	protected Entry(Entry e) {
	    _key = e.getKey();
	    _dataSize =  e.getDataSize();
	    _dataOffset=e.getDataOffset();
	    _sequenceNumber=e.getSequenceNumber();
	    _entryIndex=e.getEntryIndex();
	    _sequenceNumber = e.getSequenceNumber();
	} 
		
	/** default constructor */
	protected Entry() {
	}
		
	/** sets the index number of this index entry
	    @param index index number to associate with this index entry
	*/
	protected void setEntryIndex(int index) {
	    _entryIndex = index;
	}
		
	protected int getEntryIndex() {
	    return _entryIndex;
	}
		
	/** sets the key of this index entry 
	    @param key key to associate with this index entry 
	*/
	protected void setKey(long key) {
	    _key = key;
	}
		
	/** retrieves the key of this index entry */
	protected long getKey() {
	    return _key;
	}
		
	/** sets the size of the data region associated with this index entry
	    @param dataSize the size of the data region associated with this entry
	*/
	protected void setDataSize(int dataSize) {
	    _dataSize = dataSize;
	}
	protected int getDataSize() {
	   return  _dataSize;
	}
		
	/** sets the data region offset associated with this index entry
	    @param dataOffset offset of the data region associated with this entry
	*/
	protected void setDataOffset(long dataOffset) {
	    _dataOffset = dataOffset;
	}
	protected long getDataOffset() {
	    return _dataOffset;
	}
		
	/** sets the sequenceNumber of this index entry 
	    @param sequenceNumber sequenceNumber of this index entry 
	*/

	protected void setSequenceNumber(long sequenceNumber) {
	    _sequenceNumber = sequenceNumber;
	}
		
	/** retrieves the sequenceNumber of this index entry */
	protected long getSequenceNumber() {
	    return _sequenceNumber;
	}
		
	/** serializes this index entry and writes it to the index file
	    @param file <code>RandomAccessFile</code> to write index entry to 
	*/
	protected synchronized void toFile(RandomAccessFile file) {
	    FileDescriptor fd;
			
	    try {

		file.writeInt(_entryIndex);
		file.writeInt(_dataSize);
		file.writeLong(_dataOffset);
		file.writeLong(_key);
		file.writeLong(_sequenceNumber);
				
		// caller has to update index file extent
		//currentIdxFileExtent += slie.LOG_INDEX_ENTRY_SIZE;
	    }
	    catch(IOException ioe) {
		_log4j.error("Entry:toFile: ! IOException on index file entry write");
	    }
			
	    try {
		fd = file.getFD();
		try {
		    fd.sync();
		}
		catch (SyncFailedException sfe) {
		    _log4j.error("Entry:toFile: sync failed");
		}
	    }
	    catch (IOException ioe) {
		_log4j.error("Entry:toFile: getFD exception");
	    }
			
	}
		
	/** deserializes this index entry from the index file
	    @param file <code>RandomAccessFile</code> to read index entry from
	*/
	protected synchronized void fromFile(RandomAccessFile file) {
	    try {
		_entryIndex = file.readInt();
		_dataSize   = file.readInt();
		_dataOffset = file.readLong();
		_key        = file.readLong();
		_sequenceNumber        = file.readLong();
	    }
	    catch (IOException ioe) {
		_log4j.error("Entry:fromFile: ! IOException on index file entry read");
	    }
	}
	public String toString(){
	    String ret;
	    ret="{"+_entryIndex+","+
		_dataSize+","+
		_dataOffset+","+
		_key+","+
		_sequenceNumber+"}";
	    return ret;
	}
    }


    /** Enumeration for search type. */
    static class SearchType {
	
	protected static SearchType FIND_LOWER = new SearchType();
	protected static SearchType FIND_UPPER = new SearchType();
    }

    /** Stores lower and upper indices of a range. */
    class Range {
	int lowIndex = 0;
	int highIndex = 0;
    }

}


