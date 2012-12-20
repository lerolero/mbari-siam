/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed.devices;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.Serializable;
import java.io.IOException;

import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.NoDataException;

/** Remote Interface for operating the FOCE louvers via remote methods implemented by the instrument service.
 */
/*
 $Id: $
 $Name: $
 $Revision: $
 */

public interface ElmoLouverIF extends ElmoIF{
	
	/////////////////////////////////
	//        Louver Control       //
	/////////////////////////////////
	/** If invertHallPosition is true, the order of the feedback readings is reversed,
	 ranging from 15-0 instead of 0-15. This is used when the feedback board may be 
	 mounted in either direction along the lead screw axis while maintaining the same 
	 logical feedback sense (0 is closed, 15 is open)
	 */
	public void setInvertHallPosition(boolean value) throws RemoteException;
	/** If invertHallPosition is true, the order of the feedback readings is reversed,
	 ranging from 15-0 instead of 0-15. This is used when the feedback board may be 
	 mounted in either direction along the lead screw axis while maintaining the same 
	 logical feedback sense (0 is closed, 15 is open)
	 */
	public boolean getInvertHallPosition() throws RemoteException;
	
	/** Find and stop at the nearest Hall sensor feedback transition location. 
	 
	 Throws IllegalArgumentException if current position is above upper 
	 or below lower hall feedback limit (0x0, 0xF),
	 
	 @param positive If positive is true, find the transition by moving in the positive direction, 
	 otherwise move in the negative direction.
	 @param speedCounts operate at speedCounts counts/sec
	 @return position counter value at transition
	 */
	public long findBoundary(boolean positive,int speedCounts)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException,Exception,RemoteException;
	
	/** Find the center of the current Hall effect feedback position. 
	 Notes: 
	 - Disables the motor
	 
	 */
	public long center(int speedCounts)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException,Exception;
	
	/** Home to one of the louver's 16 Hall effect position
	 feedback switches. Use the current speed motion settings
	 and do not change the position counter.
	 */
	public void home(int position, int velocityCounts)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException,Exception,RemoteException;
	
	/** Home to one of the louver's 16 Hall effect position
	 feedback switches.  use the specified speed setting
	 and optionally set the position counter at the home position
	 */
	public void home(int position, boolean setPx, long counterValue,int vLo, int vHi)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException,Exception,RemoteException;
	
	/** Set an interpolated louver position between 
	 _louverUnitsMin and _louverUnitsMax.
	 Position is expressed as a percentage of full
	 range, 1.0 being open and 0.0 being closed.
	 */
	public void setLouverPercent(double positionPercent)
	throws IllegalArgumentException, Exception,RemoteException;

	/** Set an interpolated louver position between 
	 _louverUnitsMin and _louverUnitsMax.
	 Position is expressed in engineering units.
	 */
	public void setLouverDegrees(double positionDegrees)
	throws IllegalArgumentException, Exception,RemoteException;

	/** Get louver position (in engineering units)
	 @return louver position (degrees)
	 */
	public double getLouverPositionDegrees()
	throws TimeoutException, IOException, NullPointerException, Exception,RemoteException;
	
	/** Get louver position (as a percent of full travel)
	 @return louver position (percent, 0.0-1.0)
	 */
	public double getLouverPositionPercent()
	throws TimeoutException, IOException, NullPointerException, Exception,RemoteException;
	
	
	/** return a message indicating the state of several motor registers */
	public String getLouverStatusMessage()
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException,Exception,RemoteException;
	
	/** return a (terse) message indicating the state of several motor registers */
	public String getLouverSampleMessage()
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException,Exception,RemoteException;
		
}
