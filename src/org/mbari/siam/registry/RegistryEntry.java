/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.registry;

import java.util.Vector;
import java.util.Iterator;

import org.apache.log4j.Logger;

import org.mbari.siam.core.DeviceService;
import org.mbari.siam.core.BaseInstrumentService;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.InvalidDataException;
import org.mbari.siam.distributed.PacketParser;

/** RegistryEntry encapsulates one registrant in the InstrumentRegistry.
    It includes the ability to receive callbacks when the Instrument that it represents
    posts new data.

    @author Bob Herlien
 */

public class RegistryEntry extends Thread
{
    protected static Logger _log4j = Logger.getLogger(RegistryEntry.class);

    protected static final int MAX_QUEUE_SIZE = 20;
    protected static final int LOG_MODULUS = 20;

    protected DeviceService _service;
    protected String _regName;
    protected long _isiId = 0;
    protected String _serviceName = "null";
    protected PacketParser _parser = null;

    protected Vector _dataListeners = new Vector();
    protected Vector _dataPackets = new Vector();
    protected boolean _running = true;
    protected int     _pktsDropped = 0;

    /** Constructor	*/
    public RegistryEntry(DeviceService service, String regName)
    {
	_service = service;
	_regName = regName;
	if (_service != null)
	{
	    _isiId = service.getId();
	    _serviceName = new String(_service.getName());
	}
	_log4j.debug("Created " + toString());
    }

    /** Get the DeviceService represented by this entry	*/
    public DeviceService getService()
    {
	return(_service);
    }

    /** Get the ISI ID for this entry's service		*/
    public long getId()
    {
	return(_isiId);
    }

    /** Add a InstrumentDataListener		*/
    public void addDataListener(InstrumentDataListener listener)
    {
	if (!(_dataListeners.contains(listener)))
	    _dataListeners.add(listener);
	_log4j.debug("Added listener for " + toString());
    }

    /** Remove a InstrumentDataListener		*/
    public void removeDataListener(InstrumentDataListener listener)
    {
	_dataListeners.remove(listener);
	_log4j.debug("Removed listener for " + toString());
    }

    /** Return number of InstrumentDataLiseners	*/
    public int numDataListeners()
    {
	return(_dataListeners.size());
    }


    /** Set the PacketParser to use for callDataListeners() */
    public void setPacketParser(PacketParser parser)
    {
	_parser = parser;
    }


    /** Call back the InstrumentDataListeners	*/
    public synchronized void callDataListeners(SensorDataPacket newData)
    {
	if (_dataPackets.size() >= MAX_QUEUE_SIZE) {
	    if ((_pktsDropped++ % LOG_MODULUS) == 0) {
		_log4j.warn("callDataListeners for " + _regName +
			    ": Dropping data packet due to queue size = " + 
			    _dataPackets.size() + " Total dropped = " + _pktsDropped);
		}
	    }
	else {
	    _dataPackets.add(newData);
	    try {
		notify();
	    } catch(Exception e) {
		_log4j.error("Exception in notify(): " + e);
	    }
	}
    }

    /** run() method for thread.  This thread actually calls the InstrumentDataListeners
	when new data arrives.
    */
    public void run()
    {
	SensorDataPacket sensorData;
	Iterator it;
	PacketParser.Field[] fields;
	
	while(_running)
	{
	    synchronized(this)
	    {
		while ((_dataPackets.size() == 0) && _running)
		{
		    try {
			wait(2000);
		    } catch (InterruptedException e) {
		    } catch (Exception e) {
			_log4j.warn("Exception in run().wait()" + e);
		    }
		}

		if (!_running)
		    return;

		try {
		    sensorData = (SensorDataPacket)_dataPackets.firstElement();
		    _dataPackets.remove(0);
		} catch (Exception e) {
		    _log4j.error("Exception in getting _dataPacket: " + e);
		    continue;
		}
	    }

	    fields = null;

	    if (_parser != null) {
		try {
		    fields = _parser.parseFields(sensorData);
		} catch (Exception e) {
		    _log4j.error("Error parsing packet: " + e);
		}
	    }
		
	    _log4j.debug("Calling DataListeners for " + _serviceName);

	    it = _dataListeners.iterator();

	    while (it.hasNext())
		try {
		    ((InstrumentDataListener)(it.next())).dataCallback(sensorData, fields);
		} catch (Exception e) {
		    _log4j.error("Exception in dataCallback() for " + _regName + ":  " + e);
		}
	}	
    }

    /** close() method forces the thread to exit	*/
    public synchronized void close()
    {
	_running = false;
	try {
	    notify();
	} catch(Exception e) {
	    _log4j.error("Exception in notify(): " + e);
	}
    }

    /** Object equals for Vector.contains(), or if String,
     * for InstrumentRegistry.find(String)
     */
    public boolean equals(Object obj)
    {
	if (obj instanceof RegistryEntry) {
	    RegistryEntry ent = (RegistryEntry)obj;
	    if (ent.equals(_isiId))
		return(true);
	    return(ent.equals(_regName));
	}
	if (obj instanceof String) {
	    return(((String)obj).equalsIgnoreCase(_regName));
	}

	return(false);
    }

    /** Object equals for InstrumentRegistry.find(String) */
    public boolean equals(String lookupName)
    {
	return(lookupName.equalsIgnoreCase(_regName));
    }

    /** Object equals for InstrumentRegistry.find(long) */
    public boolean equals(long isiId)
    {
	return(isiId == _isiId);
    }

    /** hashCode() for Vector operations */
    public int hashCode()
    {
	return((int)_isiId);
    }

    /** Return registry name that will satisfy InstrumentRegistry.find(String) */
    public String registryName()
    {
	return(_regName);
    }

    /** Return name of the DeviceService */
    public String serviceName()
    {
	return(_serviceName);
    }

    public String toString()
    {
	return(_regName + ": ID " + getId() + ", " + _dataListeners.size() + 
	       " listeners, device " + _serviceName);
    }

} /* class RegistryEntry */
