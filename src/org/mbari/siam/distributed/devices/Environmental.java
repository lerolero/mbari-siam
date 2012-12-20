// Copyright MBARI 2003
package org.mbari.siam.distributed.devices;

import java.rmi.RemoteException;
import java.io.Serializable;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.NoDataException;


/** 
    Interface to provide Environmental Service
    @author Bob Herlien
*/
public interface Environmental extends Instrument {

    /** Reset the compass turns counter to specified count. */
    public void resetTurnsCounter(int nTurns)
	throws RemoteException;

    /** Get the latest data sample. */
    public Data getDataValues(boolean logData) 
	throws NoDataException, RemoteException;

    /** Container for environmental data. */
    public class Data implements Serializable, Cloneable {
	public long timestamp;
	public float temperature;
	public float humidity;
	public float pressure;
	public float groundFaultLow;
	public float groundFaultHigh;
	public float heading;
	public int turnsCount;


	public Object clone() {
	    Data object = new Data();
	    object.timestamp = timestamp;
	    object.temperature = temperature;
	    object.humidity = humidity;
	    object.pressure = pressure;
	    object.groundFaultHigh = groundFaultHigh;
	    object.groundFaultLow = groundFaultLow;
	    object.heading = heading;
	    object.turnsCount = turnsCount;

	    return object;
	}
    }
}
