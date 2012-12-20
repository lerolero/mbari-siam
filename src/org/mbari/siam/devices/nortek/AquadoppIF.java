/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.nortek;

import java.rmi.RemoteException;
import org.mbari.siam.distributed.Instrument;

/**
Remote interface to interact with Nortek Aquadopp instrument service. 
*/
public interface AquadoppIF extends Instrument {

    /**  Erase recorder. */
    public void eraseRecorder() throws RemoteException, Exception;

}
