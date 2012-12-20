/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils;

import java.util.LinkedList;

/**
 Queue class in which a queue of objects can be removed in the order that
 they are added. This class actually implements a double-ended queue, so
 elements can be added or removed from either end. Based on the Queue class
 by Yasser EL-Manzalawy <ymelamanz@yahoo.com>
*/
public class Queue {

    private LinkedList _items;

    /**
     * Creats an empty queue
     */
	 
    public Queue()
    {
	_items = new LinkedList();
    }
	
    /**
     * Put a new element at the back of the queue.
     * @param element element to be inserted.
     */
    public Object pushBack(Object element) {
	_items.add (element);
	return element;
    }

    /**
     * Put a new element at the front of the queue.
     * @param element element to be inserted.
     */
    public Object pushFront(Object element) {
	_items.add (0, element);
	return element;
    }
	

    /**
     * Removes the element at the front of the queue.
     * @return the removed element.
     * @throws EmptyQueue if the queue is empty.
     */
    public Object popFront() throws EmptyQueue {
	if (_items.size()== 0) {
	    throw new EmptyQueue() ;
	}
	return _items.removeFirst();		
    }

    /**
     * Removes the element at the back of the queue.
     * @return the removed element.
     * @throws EmptyQueue if the queue is empty.
     */
    public Object popBack() throws EmptyQueue {
	if (_items.size()== 0) {
	    throw new EmptyQueue() ;
	}
	return _items.removeLast();		
    }

    /**
     * Inspects the element at the top of the queue without removing it.
     * @return the element at the top of the queue.
     * @throws EmptyQueueException if the queue is empty.
     */
    public Object front() throws EmptyQueue {
	if (_items.size()== 0) {
	    throw new EmptyQueue() ;
	}
	return _items.getFirst();	
    }
	
    /**
     * @return the number of elements at the queue.
     */
    public int size() {
	return _items.size();
    }

    /**
     * @return true of the queue is empty.
     */	
    public boolean empty() {
	return (size()==0);
    }

    /**
     * Removes all elements at the queue.
     */	
    public void clear () {
	_items.clear();
    }    



    /** Thrown when item is requested */
    public class EmptyQueue extends Exception {

	public EmptyQueue() { 
	    super();
	}
    }
}
