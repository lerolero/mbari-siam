/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

import java.util.Enumeration;
import java.util.Vector;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

public class Queue implements Runnable {

    // Fields
    /** log4j logger */
    static Logger _log4j = Logger.getLogger(Queue.class);

    /** Vector containing queued items */
    private Vector _queue;

    /** Queue thread */
    Thread _queueThread;

    /** Dispatcher (must implement QueueDispatcher interface, which 
	provides a dispatch(Object) method)
    */
    QueueDispatcher _dispatcher=null;

    // Constructors

    /** Default Constructor */
    public Queue(){
	_queue=new Vector(16);
    }

    /** Initializing Constructor */
    public Queue(int capacity) {
	_queue=new Vector(capacity);
    }

    /** Initializing Constructor */
    public Queue(int capacity, QueueDispatcher dispatcher ) {
	_queue=new Vector(capacity);
	_dispatcher=dispatcher;
    }

    // Methods
    /** Set _dispatcher field */
    public void setDispatcher(QueueDispatcher dispatcher){
	_dispatcher=dispatcher;
    }

    /** Place on object in the queue */
    public synchronized void enqueue(Object o) {
	_queue.addElement(o);
	notify();
    }

    /** Retrieve and remove the next element in the queue
	Return null if queue is empty.
    */
    public synchronized Object dequeue() {
	if (_queue.isEmpty()) 
	    return null;

	Object retVal = _queue.elementAt(0);
	_queue.removeElementAt(0);
	return retVal;
    }

    /** Retrieve and remove the nth element in the queue
	Return null if queue is empty.
     */
    public synchronized Object dequeue(int index) throws ArrayIndexOutOfBoundsException {
	if (_queue.isEmpty()) 
	    return null;

	Object retVal = _queue.elementAt(index);
	_queue.removeElementAt(index);
	return retVal;
    }

    /** Block until an object is enqueued. 
	Returns object enqueued. 
    */
    public synchronized Object blockingDequeue() {
	//_log4j.debug("blockingDequeue...waiting");
	if (_queue.isEmpty()) {
	    try {
		wait();
	    } catch (InterruptedException ie) {
		_log4j.error("Queue.blockingDequeue() caught "+ie);
	    }
	}
	//_log4j.debug("blockingDequeue...got event");
	
	Object retVal = _queue.elementAt(0);
	_queue.removeElementAt(0);
	return retVal;
    }

    /** Return Next Element without removing it from the queue */
    public Object peek() throws ArrayIndexOutOfBoundsException {
	return this.elementAt(0);
    }

    /** Return element at specified index */
    public Object elementAt(int index) throws ArrayIndexOutOfBoundsException{
	return _queue.elementAt(index);
    }

    /** Get enumerator over elements */
    public Enumeration elements(){
	return _queue.elements();
    }

    /** Returns true if queue is empty */
    public synchronized boolean isEmpty() {
	return _queue.isEmpty();
    }

    /** Get next object in queue and dispatch it. */
    public void getNext(){
	Object next = blockingDequeue();
	_dispatcher.dispatch(next);
    }

    /** Begin waiting for objects */
    public synchronized void startQueue() {
	_queueThread = new Thread(this);
        _queueThread.start();
    }

    /** Queue thread main loop */
    public void run() {
        //_log4j.debug("Queue thread starting...");
	while (true) {
	    getNext();
	}
    }

}
