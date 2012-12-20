/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.microStrain;

/** 
SynchDetector determines when a specified synch pattern of bytes has been 
detected in a byte stream
*/
public class SyncDetector {

    byte[] _syncPattern;
    int _syncIndex = 0;

    SyncDetector(byte[] syncPattern) {
	_syncPattern = new byte[syncPattern.length];
	System.arraycopy(syncPattern, 0, _syncPattern, 0, syncPattern.length);
	reset();
    }


    /** Return true if specified byte completes the sync pattern */
    public boolean foundSync(byte b) {
	if (b == _syncPattern[_syncIndex]) {
	    _syncIndex++;
	    if (_syncIndex == _syncPattern.length) {
		// Matches pattern; reset detector for next time
		reset();
		return true;
	    }
	    else {
		// Matched a byte; might be in pattern but not done yet
		return false;
		}
	}
	else {
	    // Not in sync pattern
	    reset();
	    return false;
	}
    }


    /** Reset the detector */
    public void reset() {
	_syncIndex = 0;
    }

    /** Return pattern */
    public byte[] pattern() {
	return _syncPattern;
    }
}



