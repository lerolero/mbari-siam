// Copyright 2002 MBARI
package org.mbari.siam.distributed.portal;

/**
Describes an instrument.
*/
public class InstrumentDescription extends DeviceDescription {

    /** Platform channel number. */
    public int _channel;

    /** Print description. */
    public String print() {
	return _type + "-" + Long.toString(_deviceID) + " channel=" + 
	    _channel;
    }
}
