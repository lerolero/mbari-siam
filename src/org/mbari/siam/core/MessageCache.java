/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;
import java.util.HashMap;
import java.util.Vector;
import java.util.Iterator;

/**
 * MessageCache contains a hashmap of message texts generated by an 
 * associated instrument service; each text has associated 
 * list of times at which the text occurred. As messages are added, the cache 
 * will be written to the service's device log when the cache limit is 
 * exceeded.
 * 
 * @author oreilly
 *
 */
public class MessageCache {
	/** Associated instrument service. */
	protected BaseInstrumentService _service = null;
	
	/** Hashmap of unique messages */
	HashMap _messages = new HashMap();
	
	/** Maximum total number of message occurrences to be cached before
	 * writing to service's device log.
	 */
	int _cacheLimit = 0;
	
	public MessageCache(BaseInstrumentService service, int cacheLimit) {
		_service = service;
		_cacheLimit = cacheLimit;
	}
	
	/** Total number of message occurrences */
	protected int _nOccurrences = 0;
	
	/** Add a message. If cache limit exceeded, write cache to log. */
	public synchronized void add(String msgText, long timestamp) {
		Object entry = _messages.get(msgText);
		if (entry != null) {
			// Already have entry for this error msg
			Message msg = (Message )entry;
			msg.add(timestamp);
		}
		else {
			// This is a new message
			Message msg = new Message(msgText, timestamp);
			_messages.put(msgText, msg);
		}
		
		// If cache limit exceeded, flush to device log
		if (++_nOccurrences >= _cacheLimit) {
			flush();
		}
	}
	
	/** If cache not empty, write cache entries to service log, then clear
	 * cache. Return true if cache wasn't empty, else return false.
	 * @return true if cache not empty
	 */ 
	public synchronized boolean flush() {
	  if (nOccurrences() <= 0) {
		  return false;
	  }
	  // Write string representation to service's device log
	  _service.annotate(this.toString().getBytes());
	  clear();
	  
	  return true;
	}
	
	/** Return count of all message occurences currently in cache. */
	public int nOccurrences() {
		return _nOccurrences;
	}
	
	/** Clear message counts, but leave message objects in cache for 
	 * possible later use. */
	public void clear() {
		// Iterate through messages in cache, set their counts to zero, but
		// don't actually delete the message - save for possible later use.
		Iterator iterator = _messages.values().iterator();
		while (iterator.hasNext()) {
			Message msg = (Message )iterator.next();
			msg.clear();
		}
		// Clear the number of occurrences
		_nOccurrences = 0;
	}
	
	public String toString() {
		// Iterate through messages in cache, and append string representation
		// if message count is greater than zero.
		StringBuffer buf = new StringBuffer();
		Iterator iterator = _messages.values().iterator();
		while (iterator.hasNext()) {
			Message msg = (Message )iterator.next();
			if (msg.count() > 0) {
				buf.append(msg.toString());
				buf.append("\n");
			}
		}
		return new String(buf);
	}
	
	/** Message represents message text, and times at which message occurred. */
	class Message {
		private Vector _timestamps = new Vector();
		private String _text = "";
		
		Message(String text, long timestamp) {
			_text = text;
			add(timestamp);
		}
		
		/** Add another message occurrence */
		void add(long timestamp) {
			_timestamps.add(new Long(timestamp));
		}
		
		/** Clear timestamps/count */
		void clear() {
			_timestamps.clear();
		}
		
		/** Return message occurrence count. */
		int count() {
			return _timestamps.size();
		}
		
		/** Return String representation */
		public String toString() {
		    Long firstTimestamp = (Long )_timestamps.elementAt(0);
		    return _text + " (" + _timestamps.size() + "x since " +
			firstTimestamp.longValue()/1000 + " s)";
		}
	}
}
