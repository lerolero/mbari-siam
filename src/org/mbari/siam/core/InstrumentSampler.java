// Copyright MBARI 2003
package org.mbari.siam.core;

import java.rmi.RemoteException;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimerTask;

import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.SensorDataPacket;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

/**
   InstrumentSampler acquires samples from node instruments.
 */
public class InstrumentSampler extends TimerTask {

    /** log4j logger */
    static Logger _log4j = Logger.getLogger(InstrumentSampler.class);

    Node _node;

    public InstrumentSampler(Node node) {
	_node = node;
    }

    public void run() {
	System.out.println("Sample instruments: ");
	try {
	    Device[] devices = _node.getDevices();
	    for (int i = 0; i < devices.length; i++) {
		// If device is an instrument, then sample it
		if (devices[i] instanceof Instrument) {
		    synchronized (devices[i]) {
			if (devices[i].getStatus() != Device.SHUTDOWN) {
			    String name = new String(devices[i].getName());

			    System.out.println("\n** Sampling instrument " + 
					       i + " " + name + " (id=" + 
					       devices[i].getId() + ")");

			    Instrument instrument = (Instrument )devices[i];
			    try {
				SensorDataPacket packet = 
				    instrument.acquireSample(true);

				String timestamp = 
				    DateFormat.getDateTimeInstance().format(new Date(packet.systemTime()));
				System.out.println(timestamp + ": " + 
						   new String(packet.dataBuffer()));
			    }
			    catch (NoDataException e) {
				_log4j.error("No data from instrument: " +
						   e.getMessage());
			    }
			}
			else {
			    _log4j.error("Service " + 
					       new String(devices[i].getName()) + 
					       " is shutting down.");
			}
		    } // End synchronized block
			
		}
	    }
	}
	catch (RemoteException e) {
	    _log4j.error("InstrumentSampler.run() - " + 
			       "Caught RemoteException: " );
	    e.getMessage();
	}
    }
}

