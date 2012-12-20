/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.pump;

import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.DeviceServiceIF;

/** Configurable Pump attributes */
public class PumpAttributes 
    extends InstrumentServiceAttributes {

    /** Constructor, with required InstrumentService argument */
    public PumpAttributes(DeviceServiceIF service) {
	super(service);
    }

    /** Name under which pump service is registered with RMI. */
    public String rmiPumpServiceName = null;
}
    
