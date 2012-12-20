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
 $Id: ElmoIF.java,v 1.2 2009/06/04 22:55:39 headley Exp $
 $Name:  $
 $Revision: 1.2 $
 */

public interface ElmoIF extends Remote{
	
	/////////////////////////////////
	//     Motor Configuration     //
	/////////////////////////////////
	
	/** Initialize motor controller
	 */
	public void initializeController() 
	throws TimeoutException, IOException,Exception,RemoteException;
	
	public void initializeController(int serialMode,
									 int countsPerRevolution,
									 double gearRatio,
									 int mode,
									 int acceleration,
									 int deceleration,
									 int stopDeceleration) 
	throws TimeoutException, IOException, Exception,RemoteException;
	
	/** Convert motor speed (rpm)  to (counts/sec) 
	 (before gear train)
	 (using countsPerRevolution)
	 */
	public int rpm2counts(double rpm) throws Exception, RemoteException;
	
	/** Convert motor speed (counts/sec) to (rpm)
	 (before gear train) 
	 (using countsPerRevolution)
	 */
	public double counts2rpm(int counts) throws Exception, RemoteException;
	
	/** Convert output shaft speed (rpm) to (counts/sec)
	 (at output after gear train)
	 (using gear ratio and counts per revolution) 
	 */
	public int orpm2counts(double orpm) throws Exception, RemoteException;

	/** Convert output shaft speed (counts/sec) to (rpm)
	 (at output after gear train)
	 (using gear ratio and counts per revolution) 
	 */
	public double counts2orpm(int counts) throws Exception, RemoteException;

	/** Set serial port mode (MODE_SERIAL_LOCAL, MODE_SERIAL_RFC2217) */
	public void setSerialMode(int mode) throws IllegalArgumentException,RemoteException;

	/** Set commutation counts per motor revolution.
		Depends on motor type (number of poles, etc.)
	 */
	public void setCountsPerRevolution(int countsPerRevolution) throws IllegalArgumentException,RemoteException;	
	/** Get commutation counts per motor revolution.
		Depends on motor type (number of poles, etc.)
	 */
	public int getCountsPerRevolution() throws RemoteException;
	
    /** enable/disable the motor. */
    public void setEnable(boolean value, long timeoutMsec) 
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception,RemoteException;
	
	/** Set the gear ratio between the motor and the output shaft.
	 For example, if the output shaft turns 67.5 times slower than
	 the motor, the gear ratio should be set to 67.5.
	 */
	public void  setGearRatio(double value)
	throws IllegalArgumentException, RemoteException;
	
	/** Get the gear ratio between the motor and the output shaft.
	 For example, if the output shaft turns 67.5 times slower than
	 the motor, the gear ratio should be set to 67.5.
	 */
	public double getGearRatio() throws RemoteException;
	
	/** Set motor position counter for position modes
	 @param positionCounts new value of position counter (counts)
	 fulfills ElmoIF interface 
	 */
	public void setPositionCounter(long positionCounts) 
	throws TimeoutException, IOException,NullPointerException,IllegalArgumentException,Exception,RemoteException;

	/** Get motor position counter for position modes 
	 @return position counter (counts)
	 */
	public long getPositionCounter()
	throws TimeoutException, IOException, NullPointerException, Exception,RemoteException;
	
	/** get motor jogging (commanded) velocity in counts/sec. */
    public int getJoggingVelocity()
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception,RemoteException;
	
	/** get motor feedback velocity in counts/sec. */
    public int getEncoderVelocity()
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception,RemoteException;
    
	/** get (average of nSamples) motor feedback velocity in counts/sec. */
    public int getEncoderVelocity(int nSamples)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception,RemoteException;
	
    /** return motor enabled (MO) status. */
    public boolean isEnabled()
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception,RemoteException;

    /** return motor status. */
    public int getStatusRegister()
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception,RemoteException;
	
    /** return detailed motor fault information. */
    public int getFaultRegister()
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception,RemoteException;
	
	/** Get difference (counts) between commanded 
	 and actual position if available 
	 @return position error (counts)
	 */
	public long getPositionError()
	throws TimeoutException, IOException,Exception,RemoteException;
	
    /** set motor velocity in counts 
	 Do not initiate motion
	 */
    public void setJoggingVelocity(int counts)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception,RemoteException;

	/** set motor PTP velocity (used for Absolute motion) in counts 
	 Do not initiate motion
	 */
    public void setPTPSpeed(int counts)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception,RemoteException;
	
	/** show current configuration information */
	public String showConfiguration()
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception,RemoteException;

	/** read an Elmo register value */
	public String readRegister(String register)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception;

	/////////////////////////////////
	//     Motor Motion Commands   //
	/////////////////////////////////
	
    /** start motion using current settings n
	 */
    public void beginMotion()
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception,RemoteException;
	
    /** command motor velocity in counts/sec */
    public void jog(int counts)
	throws TimeoutException, IOException, NullPointerException, IllegalArgumentException, Exception,RemoteException;
	
	/** Move motor relative to current position
	 @param distanceCounts distance to move (counts)
	 */
	public void ptpRelative(long distanceCounts,boolean wait) 
	throws TimeoutException, IOException,NullPointerException,IllegalArgumentException,Exception,RemoteException;
	
	/** Move motor to absolute position
	 Motion may be subject to modulo position counting 
	 modes in effect.	 
	 @param position to move to (counts)
	 */
	public void ptpAbsolute(long position,boolean wait) 
	throws TimeoutException, IOException,NullPointerException,IllegalArgumentException,Exception,RemoteException;
	
	/** delay for specified number of milliseconds 
	 @param delayMsec delay duration in milliseconds
	 */
	public void delay(long delayMsec) throws RemoteException;
	
}
