// Copyright 2001 MBARI
package org.mbari.siam.distributed.platform;

import gnu.io.CommPort;

/**
   Interface for services which interact with a device through
   a CommPort interface. 
   <p>
   NOTE: PortService implementation constructors
   are not allowed to take arguments.
   @author Tom O'Reilly
 */
public interface PortService
{
    /** Start the service on the specified CommPort. 
	@param port device communications interface
	@param args command line arguments
     */
    public void start(CommPort port, String[] args);

    /** Stop the service. */
    public void stop();
}
