/****************************************************************************/
/* Copyright 2011 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.distributed;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import org.mbari.siam.distributed.NotSupportedException;

/**
 * FOCENode interface. This interface adds  methods which are specific to FOCE
 * 
 * @author Bob Herlien
 */
public interface FOCENode extends Node
{
    /** Power up the CO2 Subsystem	*/
    public void powerUpCO2Subsystem()
	throws RemoteException, IOException, NotSupportedException;

    /** Power down the CO2 Subsystem	*/
    public void powerDownCO2Subsystem()
	throws RemoteException, IOException, NotSupportedException;

    /** Start up the Instrument Services associated with the CO2 Subsystem */
    public void startCO2SubsystemServices()
	throws RemoteException, IOException, NotSupportedException;

    /** Stop the Instrument Services associated with the CO2 Subsystem */
    public void stopCO2SubsystemServices()
	throws RemoteException, IOException, NotSupportedException;
}
