// Copyright MBARI 2007
package org.mbari.siam.distributed.devices;

import java.rmi.RemoteException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.NoDataException;


/** 
    Remote interface to network switch service
*/
public interface NetworkSwitch extends Instrument {

    /** Get NetworkSwitch's CPU IP address */
    public InetAddress getCpuAddress() 
	throws UnknownHostException, RemoteException;
}
