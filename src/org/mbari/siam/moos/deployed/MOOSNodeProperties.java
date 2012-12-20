/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.moos.deployed;

import org.apache.log4j.Logger;
import org.mbari.siam.core.DevicePort;
import org.mbari.siam.core.SerialDevicePort;
import org.mbari.siam.core.NodeProperties;
import org.mbari.siam.core.NullPowerPort;
import org.mbari.siam.distributed.CommsMode;
import org.mbari.siam.distributed.PowerPort;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.MissingPropertyException;

/**
   MOOSNodeProperties contains properties for a MOOSNode. Some of
   these properties are required.
 */
public class MOOSNodeProperties extends NodeProperties
{
    /** Looks for "DPA" keyword and returns a DPAPresentPowerPort if present.
	Else returns NullPowerPort.
	@param key - Property key for the PowerPort.
    */
    public PowerPort getPowerPort(String key) throws MissingPropertyException
    {
	String powerName = getRequiredProperty(key);
	if (powerName.equals("DPA"))
	    return(new DpaPresentPowerPort());
	return(new NullPowerPort());
    }
}

