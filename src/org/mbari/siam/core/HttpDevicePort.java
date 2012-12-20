/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;
import org.mbari.siam.distributed.PowerPort;
import org.mbari.siam.distributed.InitializeException;

/** 
 */
class HttpDevicePort extends DevicePort {

    public HttpDevicePort(int index, String portName, PowerPort powerPort,
			  String jar, DeviceService service) {
	super(index, portName, powerPort, jar, service);
    }


    /** Open the underlying communications port */
    public void openComms() throws Exception {
    }

    /** Close the underlying communications port */
    public void closeComms() {
    }

    /** Create the appropriate InstrumentPort */
    public void createInstrumentPort() throws InitializeException {
	try {
	    _instrumentPort = new HttpInstrumentPort(_portName, _powerPort);
	}
	catch (Exception e) {
	    throw new InitializeException(e.getMessage());
	}
    }

}
