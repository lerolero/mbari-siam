// Copyright 2002 MBARI
package org.mbari.siam.distributed.leasing;

/**
This exception is thrown by LeaseManager when it refuses a
client's lease request. Reasons for refusal include 
client specification of an invalid lease period and an 
expired lease.
@author Tom O'Reilly
*/
public class LeaseRefused extends Exception {

    /**
       @param message Message associated with exception
    */
    public LeaseRefused(String message) {
	super(message);
    }
}

