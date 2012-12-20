// Copyright MBARI 2003
package org.mbari.siam.distributed.devices;

import java.rmi.RemoteException;
import java.io.Serializable;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.NoDataException;


/** 
    Provide GPS service.
    @author Tom O'Reilly
*/
public interface GPS extends Instrument {

    /** Get most recent NMEA string from GPS. */
    public byte[] getLatestNMEA() throws NoDataException, RemoteException;
}
