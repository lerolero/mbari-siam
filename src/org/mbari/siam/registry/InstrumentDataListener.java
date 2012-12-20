/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.registry;

import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.DevicePacket;

/** InstrumentDataListener is an interface that must
    be implemented by objects that wish to intercept new
    data records from the InstrumentRegistry
 */


public interface InstrumentDataListener
{
    /** Callback for new data record
	@param sensorData - SensorDataPacket that instrument logged
	@param fields - Result of passing sensorData to PacketParser.parseFields()
	if registered DeviceService is an instanceof BaseInstrumentService.  Else null.
    */
    public void dataCallback(DevicePacket sensorData, PacketParser.Field[] fields);

    /**Action performed when service installed */
    public void serviceRegisteredCallback(RegistryEntry entry);

} /* class InstrumentDataListener */
