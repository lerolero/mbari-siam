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
import org.mbari.siam.core.EventManager;
import org.mbari.siam.core.ServiceListener;
import org.mbari.siam.core.ServiceEvent;

import org.mbari.siam.distributed.DuplicateIdException;

/** InstrumentRegistry is a facility that allows you to register DeviceServices,
    and to look them up by registry name.  You can also register to get
    callbacks when registrants are added or deleted, and through those registrants,
    you can get callbacks when their instruments have new data.
    <p>
    The core class {@link org.mbari.siam.core.DeviceService DeviceService} contains
    the {@link org.mbari.siam.distributed.ServiceAttributes ServiceAttributes}.
    These ServiceAttributes contain, in particular, an attribute named <b>registryName</b>,
    which by default is declared as null.
    <p>
    The <b>registryName</b> attribute is used by InstrumentRegistry as a lookup key.
    The system integrator can use any of the normal facilities to set this attribute
    for any DeviceService.  These facilities include building it into the service driver,
    setting the attribute in make/Makefile, or setting it on the fly with the utility
    <b>setProperty</b>.
    <p>
    Other services may look up the desired DeviceService using the <b>find()</b> method.
    They can also register to get a callback when that service acquires data by
    implementing the {@link org.mbari.siam.registry.InstrumentDataListener
    InstrumentDataListener} interface, and registering with the <b>registerDataCallback()</b>
    method.

    @author Bob Herlien
 */

public class InstrumentRegistry implements ServiceListener
{
    protected static Logger _log4j = Logger.getLogger(InstrumentRegistry.class);

    protected Vector _entries = new Vector();
    protected Vector _listeners = new Vector();
    protected Vector _deferredListeners = new Vector();
    private static InstrumentRegistry _registry = null;

    /** Constructor is private to enforce singleton nature of the InstrumentRegistry */
    private InstrumentRegistry()
    {
	EventManager.getInstance().addListener(ServiceListener.class, this);
    }

    /** Get the singleton InstrumentRegistry	*/
    public synchronized static InstrumentRegistry getInstance()
    {
	if (_registry == null)
	    _registry = new InstrumentRegistry();

	return(_registry);
    }

    /** Add a RegisryListener			*/
    public void addListener(RegistryListener listener)
    {
	_listeners.add(listener);
    }

    /** Find a RegistryEntry by registryName String */
    public RegistryEntry findEntry(String registryName)
    {
	Iterator it = _entries.iterator();
	RegistryEntry entry;

	while(it.hasNext())
	{
	    entry = (RegistryEntry)it.next();
	    if (entry.equals(registryName))
		return(entry);
	}
	return(null);
    }

    /** Find a RegistryEntry by instrument ID */
    public RegistryEntry findEntry(long id)
    {
	Iterator it = _entries.iterator();
	RegistryEntry entry;

	while(it.hasNext())
	{
	    entry = (RegistryEntry)it.next();
	    if (entry.equals(id))
		return(entry);
	}
	return(null);
    }

    /** Find a DeviceService by registryName String */
    public DeviceService find(String registryName)
    {
	RegistryEntry entry = findEntry(registryName);

	return((entry == null) ? null : entry.getService());
    }

    /** Add a RegistryEntry				*/
    public synchronized void add(RegistryEntry entry) throws DuplicateIdException
    {
	if (_entries.contains(entry))
	    throw new DuplicateIdException("RegistryEntry already exists for Instrument ID "
					   + entry.getId());

	_log4j.debug("Adding entry for " + entry.getId() + ", " + entry.registryName());

	int i = _deferredListeners.indexOf(entry);
	if (i >= 0)
	{
	    try {
		RegistryEntry deferredEntry = (RegistryEntry)_deferredListeners.get(i);
		_deferredListeners.remove(i);

		_log4j.debug("Moving entry for ID " + entry.getId() + " from deferred to active. " +
			     "There are now " + _deferredListeners.size() + " deferred entries.");

		Iterator fdl = deferredEntry._dataListeners.iterator();

		while (fdl.hasNext())
		{
		    InstrumentDataListener listener = (InstrumentDataListener)fdl.next();
		    entry.addDataListener(listener);
		    listener.serviceRegisteredCallback(entry);
		}
	    } catch (Exception e) {
		_log4j.error("Exception while moving deferredListener to active: " + e);
	    }
	}

	entry.start();
	_entries.add(entry);

	Iterator it = _listeners.iterator();

	while (it.hasNext())
	    ((RegistryListener)(it.next())).newRegistrantCallback(entry);
    }

    /** If the DeviceService contains a non-null Service Attribute String named
	<b>registryName</b>, then create a RegistryEntry for it and add it
	to the InstrumentRegistry.
	@param service - The DeviceService to (conditionally) add to the InstrumentRegistry
    */
    public void add(DeviceService service) throws DuplicateIdException
    {
	if (service.registryName() != null)
	    add(new RegistryEntry(service, service.registryName()));
	else
	    _log4j.debug("ID " + service.getId() + " has no registryName.");
    }

    /** Register for a Data Callback
     *  This function will either register a DataListener for the instrument that
     *  matches the registryName, or, if none exists yet, will retain the information
     *  and register when such an instrument becomes available.
     * @param listener - The InstrumentDataListener to register.
     * @param registryName - String identifying the DeviceService to look up.  Required.
     * @return - RegistryEntry matching the registryName specification.  Null if not found;
     * in this case, when the DeviceService comes online, you'll be registered and will
     * get a serviceRegisteredCallback.
     */
    public RegistryEntry registerDataCallback(InstrumentDataListener listener,
					      String registryName)
    {
	RegistryEntry entry = findEntry(registryName);

	if (entry == null)
	{
	    _log4j.debug("Creating deferredListener for " + registryName);
	    entry = new RegistryEntry(null, registryName);
	    _deferredListeners.add(entry);
	    entry.addDataListener(listener);
	    _log4j.debug("There are now " + _deferredListeners.size() + " deferred listeners.");
	    return(null);
	}
	else
	{
	    entry.addDataListener(listener);
	    return(entry);
	}
    }

    /** Remove a RegistryEntry and call the removeRegistrantCallbacks.
    */
    public synchronized void remove(RegistryEntry entry)
    {
	if (entry != null)
	{
	    entry.close();
	    _entries.remove(entry);

	    if (entry._dataListeners.size() > 0)
	    {
		entry._service = null;
		_deferredListeners.add(entry);
	    }

	    Iterator it = _listeners.iterator();

	    while (it.hasNext())
		((RegistryListener)(it.next())).removeRegistrantCallback(entry);
	}
    }

    /** serviceInstalled() method for ServiceListener interface */
    public void serviceInstalled(ServiceEvent e)
    {
    }

    /** serviceRemoved method for the ServiceListener interface.
	We look to see if there's a RegistryEntry matching this event,
	and if so, remove it and call the removeRegistrantCallbacks.
    */
    public void serviceRemoved(ServiceEvent e)
    {
	RegistryEntry entry = findEntry(e.getServiceID());

	if (entry != null)
	    remove(entry);
    }

    /** serviceRequestComplete() method for ServiceListener interface */
    public void serviceRequestComplete(ServiceEvent e)
    {
    }

    public String registryStatus()
    {
	StringBuffer sb = new StringBuffer();
	Iterator it;
	RegistryEntry entry;

	sb.append("InstrumentRegistry contains ");
	sb.append(_entries.size());
	sb.append(" entries:\n");

	for (it = _entries.iterator(); it.hasNext(); )
	{
	    entry = (RegistryEntry)(it.next());
	    sb.append(entry.toString());
	    sb.append("\n");
	}

	if (_deferredListeners.size() > 0)
	{
	    sb.append("\nInstrumentRegistry contains ");
	    sb.append(_deferredListeners.size());
	    sb.append(" deferred entries\n");
	    sb.append("These are entries that someone is listening for, but haven't yet registered themselves:\n");

	    for (it = _deferredListeners.iterator(); it.hasNext(); )
	    {
		entry = (RegistryEntry)(it.next());
		sb.append(entry.toString());
		sb.append("\n");
	    }
	}

	return(sb.toString());
    }

} /* class InstrumentRegistry */
