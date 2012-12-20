/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

import java.util.EventListener;
import java.util.Vector;
import java.util.Iterator;
import org.apache.log4j.Logger;

/**
   EventManager is a mechanism for notifying objects of system
   level events. It is superficially similar to the AWT event
   model. Custom AWT events were not used for several reasons:

   The EventManager is implemented as a singleton; it is typically started
   by the NodeManger. It has a Queue member field, which implements the event
   queue thread. Once created, the EventManager waits for events to be posted;
   when an event is posted, a DispatchThread (internal class) is created to 
   dispatch the event to all listeners registered for that event type. Each 
   listener for a given event type implements a corresponding listener interface,
   containing methods called by the dispatch thread. 

   ** Note that each dispatch thread calls the listener methods sequentially; a
   rougue listener could delay or hang the dispatch thread. **

   Above was changed in revision 1.8.  Now the EventDispatcher thread is created
   and allocated, one per listener.  When an event comes in, it is posted to each
   EventDispatcher that handles an event of that type.  This resolves the problem
   noted above, where a rogue listener can hang the dispatch thread.  With this
   version, and rogue listener only hangs itself.  rah, 6Dec2007.

   Also with this revision, AWT or Swing events are no longer used.  rah, 6Dec2007.

   Objects may register as listeners of various event types, or post
   events to the EventManager queue. 
   
   There are currently four event types:

   ServiceEvent, SchedulerEvent, PowerEvent, and CommEvent
   
   All event types must be extended from NodeEvent, which extends
   java.util.EventObject.

*/

public class EventManager
{
    private static Logger _log4j = Logger.getLogger(EventManager.class);

    protected static final int MAX_QUEUE_SIZE = 20;
    protected static final int LOG_MODULUS = 20;

    // Fields
    /** THE event manager instance */
    private static EventManager _theEventManager=null;

    /** Event Queue */
    protected Vector _listenerList = new Vector();

    //Constructors
    private EventManager(){
    }

    // Methods
    /** Get EventManager Instance */
    public synchronized static EventManager getInstance(){
	if(_theEventManager==null){
	    // Get new event manager
	    _theEventManager = new EventManager();
	}
	return _theEventManager;
    }

    /** Add Listener for event type.
	Note that, starting with revision 1.8, the type parameter is unused.
     */
    public void addListener(Class type, EventListener listener)
    {
	EventDispatcher dispatcher;

	if (listener instanceof ServiceListener)
	    dispatcher = new ServiceEventDispatcher(listener);
	else if (listener instanceof LogSampleListener)
	    dispatcher = new LogSampleEventDispatcher(listener);
	else if (listener instanceof PowerListener)
	    dispatcher = new PowerEventDispatcher(listener);
	else if (listener instanceof CommListener)
	    dispatcher = new CommEventDispatcher(listener);
	else if (listener instanceof SchedulerListener)
	    dispatcher = new SchedulerEventDispatcher(listener);
	else 
	    dispatcher = new EventDispatcher(listener);

	_listenerList.add(dispatcher);
	dispatcher.start();

	_log4j.debug("Added listener " + listener.getClass().getName() + " type " + type.getName()
		     + " listener " + ", total " +  _listenerList.size() + " listeners");
    }

    /** Post an event to the EventQueue */
    public void postEvent(NodeEvent event)
    {
//	_log4j.debug("postEvent():  " + event.getClass().getName());

	for (Iterator it = _listenerList.iterator(); it.hasNext(); )
	    ((EventDispatcher)(it.next())).dispatchEvent(event);
    }


    /** Does actual work of discovering event type and calling 
	appropriate methods for its listeners.  This base class
	schedules any event not picked up by the subclasses below.
     */
    class EventDispatcher extends Thread
    {
	Vector	_eventQ;
	EventListener _listener;
	int	_pktsDropped;

	public EventDispatcher(EventListener listener)
	{
	    super();
	    _eventQ = new Vector();
	    _listener = listener;
	    _log4j.debug("Created EventDispatcher: " + this.getClass().getName());
	    _pktsDropped = 0;
	}

	public void dispatchEvent(NodeEvent event)
	{
	    if (_log4j.isDebugEnabled()) {
		_log4j.debug("EventManager.dispatchEvent(): " + event.getClass().getName());
	    }

	    if (_eventQ.size() >= MAX_QUEUE_SIZE) {
		if ((_pktsDropped++ % LOG_MODULUS) == 0) {
		    _log4j.warn("dispatchEvent(): queue size = " + _eventQ.size() +
				" Dropping event, total dropped = " + _pktsDropped);
		}
	    }
	    else {
		synchronized(this) {
		    _eventQ.add(event);
		    notify();
		}
	    }
	}

	public void run()
	{
	    NodeEvent event;

	    _log4j.debug("Starting EventDispatcher type " + this.getClass().getName());

	    while(true)
	    {
		//Wait for an event
		synchronized(this)
		{
		    while (_eventQ.size() == 0)
		    {
			try {
			    wait(2000);
			} catch (InterruptedException e) {
			} catch (Throwable e) {
			    _log4j.warn("caught Throwable in run().wait()" + 
					e);
			}
		    }
		
		    try {
			event = (NodeEvent)(_eventQ.firstElement());
			_eventQ.remove(0);
		    } catch (Throwable e) {
			_log4j.error("Caught throwable in getting request from Vector: " + 
				     e);
			continue;
		    }
		}

		_log4j.debug("EventDispatcher for " + _listener.getClass().getName() +
			     " got event:  " + event.getClass().getName());

		try {
		    callListener(event);
		}
		catch (Throwable e) {
		    _log4j.warn("run() - Caught throwable from callListener()" +
				e);
		}
	    }
	}
	
	public void callListener(NodeEvent event)
	{
	    _log4j.warn("Got unknown event type: " + event);
	}
    }

    /** Dispatcher for ServiceEvents
     */
    class ServiceEventDispatcher extends EventDispatcher
    {
	public ServiceEventDispatcher(EventListener listener)
	{
	    super(listener);
	}

	public void dispatchEvent(NodeEvent event)
	{
	    if (event instanceof ServiceEvent) {
		super.dispatchEvent(event);
	    }
	}

	public void callListener(NodeEvent event)
	{
	    ServiceEvent se = (ServiceEvent)event;
	    ServiceListener sl = (ServiceListener)_listener;
	    int stateChange = se.getStateChange();

	    _log4j.debug("Dispatching ServiceEvent: " + 
			 se.getClass().getName());

	    if(stateChange==ServiceEvent.INSTALLED)
		sl.serviceInstalled(se);
	    else if(stateChange==ServiceEvent.REMOVED)
		sl.serviceRemoved(se);
	    else if(stateChange==ServiceEvent.REQUEST_COMPLETE)
		sl.serviceRequestComplete(se);
	}
    }

    /** Dispatcher for LogSampleEvents
     */
    class LogSampleEventDispatcher extends EventDispatcher
    {
	public LogSampleEventDispatcher(EventListener listener)
	{
	    super(listener);
	}

	public void dispatchEvent(NodeEvent event)
	{
	    if (event instanceof LogSampleServiceEvent) {
		super.dispatchEvent(event);
	    }
	}

	public void callListener(NodeEvent event)
	{
	    ((LogSampleListener)_listener).sampleLogged((LogSampleServiceEvent)event);
	}
    }

    /** Dispatcher for PowerEvents
     */
    class PowerEventDispatcher extends EventDispatcher
    {
	public PowerEventDispatcher(EventListener listener)
	{
	    super(listener);
	}

	public void dispatchEvent(NodeEvent event)
	{
	    if (event instanceof PowerEvent) {
		super.dispatchEvent(event);
	    }
	}

	public void callListener(NodeEvent event)
	{
	    PowerEvent pe = (PowerEvent)event;
	    PowerListener pl = (PowerListener)_listener;

	    _log4j.debug("event ID = " + pe.getID());
	    
	    if(pe.getID()==PowerEvent.POWER_FAILURE_DETECTED) {
		_log4j.debug("notifying listener: POWER_FAILURE_DETECTED");
		pl.failureDetected(pe);
	    }
	}
    }

    /** Dispatcher for CommEvents
     */
    class CommEventDispatcher extends EventDispatcher
    {
	public CommEventDispatcher(EventListener listener)
	{
	    super(listener);
	}

	public void dispatchEvent(NodeEvent event)
	{
	    if (event instanceof CommEvent) {
		super.dispatchEvent(event);
	    }
	}

	public void callListener(NodeEvent event)
	{
	    _log4j.debug("Dispatcher ("+this.getName()+") received CommEvent: " 
			 + event.getClass().getName());
	}
    }

    /** Dispatcher for SchedulerEvents
     */
    class SchedulerEventDispatcher extends EventDispatcher
    {
	public SchedulerEventDispatcher(EventListener listener)
	{
	    super(listener);
	}

	public void dispatchEvent(NodeEvent event)
	{
	    if (event instanceof SchedulerEvent) {
		super.dispatchEvent(event);
	    }
	}

	public void callListener(NodeEvent event)
	{
	    _log4j.debug("Dispatcher ("+this.getName()+") received SchedulerEvent: "
			 + event.getClass().getName());
	}
    }

}
