// Copyright 2002 MBARI
package org.mbari.siam.distributed.leasing;

/**
This exception is thrown by LeaseManager when client tries to
renew a lease that has already expired.

@author Tom O'Reilly
*/

public class LeaseExpired extends LeaseRefused {
    
    public LeaseExpired() {
	super("Lease has expired");
    }
}

