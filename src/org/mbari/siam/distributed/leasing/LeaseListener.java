/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed.leasing;

/**
LeaseListener is notified by LeaseManager when significant leasing
events occur. Note: it important that the callbacks implemented by
LeaseListeners execute in a timely manner, so that remote lease
clients are not blocked for an excessive amount of time.
*/
public interface LeaseListener {

    /** 
	Called when a lease is being established. Can throw LeaseRefused.
	This callback implementation must execute in a timely manner.
	@param id lease ID of session being established
	@param leasePeriod lease period in milliseconds
	@param nextEstablishTime next estimated lease establishment time 
	@param numConnections Total number of connections on the comms
	media, including this one.	
    */
    public void leaseEstablishCallback(int id, long leasePeriod,
			long nextEstablishTime, int numConnections) throws LeaseRefused;

    /**
       Called when lease is being terminated.
       This callback implementation must execute in a timely manner.
	@param id lease ID of session being terminated
	@param nextEstablishTime next estimated lease establishment time 
	@param numConnections Total number of connections on the comms
	media, after this one has been terminated
    */
    public void leaseTerminatedCallback(int id, long nextEstablishTime,
	int numConnections);

    /**
       Called when lease expires.
       This callback implementation must execute in a timely manner.
	@param id lease ID of session being established
	@param nextEstablishTime next estimated lease establishment time 
	@param numConnections Total number of connections on the comms
	media, after this one has expired
    */
    public void leaseExpiredCallback(int id, long nextEstablishTime,
	int numConnections);

    /**
       Called when lease is being renewed. Can throw LeaseRefused.
       This callback implementation must execute in a timely manner.
	@param id lease ID of session being renewed
	@param leasePeriod lease period in milliseconds
	@param numConnections Total number of connections on the comms
	media, including this one.	
    */
    public void leaseRenewalCallback(int id, long leasePeriod,
	int numConnections) throws LeaseRefused;

}

