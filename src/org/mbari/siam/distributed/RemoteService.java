/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

import java.rmi.Remote;
import java.rmi.RemoteException;

/** Extends Remote, including method to 'ping' the remote service. */
public interface RemoteService extends Remote {

    /** Try to 'ping' the remote service */
    public void ping() throws RemoteException;
}
