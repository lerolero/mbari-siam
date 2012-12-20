/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

public interface QueueDispatcher {
    /** Dispatch objects that are placed in the queue */
    public void dispatch(Object o);
}
