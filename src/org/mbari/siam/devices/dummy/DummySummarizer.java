/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.dummy;

import java.util.Vector;
import java.util.Iterator;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.core.SerialPortParameters;

import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DevicePacketParser;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.Summarizer;
import org.mbari.siam.distributed.SummaryPacket;
import org.mbari.siam.distributed.Safeable;

/**
 * Fake instrument for testing purposes.
 */
public class DummySummarizer
    extends DummyInstrument 
    implements Instrument, Safeable {

    /** Log4j logger */
    protected static Logger _log4j = 
	Logger.getLogger(DummySummarizer.class);


    private int _nSamples = 0;
	
    boolean _summarize = true;

    //    Object _semaphore = new Object();

    Attributes _attributes = new Attributes(this);

    private Vector _samplingThreads = new Vector();

    public DummySummarizer() throws RemoteException {
    }

    private PacketParser _packetParser = new PacketParser();


    /**
     * Extend InstrumentServiceAttributes, as a test.
     * @author oreilly
     *
     * TODO To change the template for this generated type comment go to
     * Window - Preferences - Java - Code Style - Code Templates
     */
    protected class Attributes extends DummyInstrument.Attributes {
	Attributes(DeviceServiceIF service) {
	    super(service);
		
	    summaryVars = new String[] {
		"field1", "field2", "field3"
	    };
	}

    }


    class PacketParser extends DevicePacketParser {

	protected void parseFields(DevicePacket packet) {
	    addMeasurement("field1", "dummy field 1", "units", 1111);
	    addMeasurement("field2", "dummy field 2", "units", 2222);
	    addMeasurement("field3", "dummy field 3", "units", 3333);
	}
    }



    public DevicePacketParser getDevicePacketParser() {
        return _packetParser;
    }

} // end of class
