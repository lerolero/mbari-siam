/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

/**
 * Interface between a ServiceAttributes object and the DeviceService
 * that contains contains it.
 */
public interface DeviceServiceIF {

    /** Set the ServiceAttributes object for specified DeviceService object. */
    public void setAttributes(ServiceAttributes attributes);

    /** Get the name of the service. */
    public byte[] getName();

    
}
