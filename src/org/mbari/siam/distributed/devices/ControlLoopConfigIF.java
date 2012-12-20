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

import org.mbari.siam.distributed.devices.ProcessParameterIF;


/** Provides methods for changing the ControlLoop state.
    Remote Interface for performing closed loop control via remote methods implemented by the instrument service.
	Clients like the GUI may get the implementing service and cast to ControlLoopConfigIF to manipulate the control process.
 */
/*
 $Id: $
 $Name: $
 $Revision: $
 */

public interface ControlLoopConfigIF extends Remote{
	
	/** Filter ID: forward pH combiner (setFilterDivisor) */
	public static final int FID_FWD_COMBINER = ProcessParameterIF.FC_PH_INT_FWD;
	/** Filter ID: aft pH combiner (setFilterDivisor) */
	public static final int FID_AFT_COMBINER = ProcessParameterIF.AC_PH_INT_AFT;
	/** Filter ID: internal pH combiner (setFilterDivisor) */
	public static final int FID_INT_COMBINER = ProcessParameterIF.IC_PH_INT;
	/** Filter ID: external pH combiner (setFilterDivisor) */
	public static final int FID_EXT_COMBINER = ProcessParameterIF.EC_PH_EXT;
	
	/** Filter Input ID: pH fwd combiner left  (setFilterInputWeight) */
	public static final int IID_PH_FCOM_L = ProcessParameterIF.IFC_PH_INT_FWD_L;
	/** Filter Input ID: pH fwd combiner right  (setFilterInputWeight) */
	public static final int IID_PH_FCOM_R = ProcessParameterIF.IFC_PH_INT_FWD_R;
	/** Filter Input ID: pH aft combiner left  (setFilterInputWeight) */
	public static final int IID_PH_ACOM_L = ProcessParameterIF.IAC_PH_INT_AFT_L;
	/** Filter Input ID: pH aft combiner right  (setFilterInputWeight) */
	public static final int IID_PH_ACOM_R = ProcessParameterIF.IAC_PH_INT_AFT_R;
	
	/** Filter Input ID: pH external combiner left  (setFilterInputWeight) */
	public static final int IID_PH_ECOM_L = ProcessParameterIF.IEC_PH_EXT_MID_L;
	/** Filter Input ID: pH external combiner right  (setFilterInputWeight) */
	public static final int IID_PH_ECOM_R = ProcessParameterIF.IEC_PH_EXT_MID_R;
	/** Filter Input ID: pH internal combiner fwd (setFilterInputWeight) */
	public static final int IID_PH_ICOM_FWD = ProcessParameterIF.IIC_PH_INT_FWD;
	/** Filter Input ID: pH internal combiner aft (setFilterInputWeight) */
	public static final int IID_PH_ICOM_AFT = ProcessParameterIF.IIC_PH_INT_AFT;
	
	/** control mode manual (ph, velocity) */
	public static final int MODE_MANUAL      = ProcessParameterIF.CONTROL_MODE_MANUAL;
	/** control mode offset (ph, velocity) */
	public static final int MODE_OFFSET      = ProcessParameterIF.CONTROL_MODE_OFFSET;
	/** control mode constant (ph, velocity) */
	public static final int MODE_CONSTANT    = ProcessParameterIF.CONTROL_MODE_CONSTANT;
	/** control mode deadband (velocity) */
	public static final int MODE_DEADBAND    = ProcessParameterIF.CONTROL_MODE_DEADBAND;
	/** control mode panic (ph, velocity) */
	public static final int MODE_PANIC       =  ProcessParameterIF.CONTROL_MODE_PANIC;

	/** response mode PID (ph) */
	public static final int MODE_PID       =  ProcessParameterIF.RESPONSE_MODE_PID;
	/** response mode linear (ph) */
	public static final int MODE_LIN       =  ProcessParameterIF.RESPONSE_MODE_LIN;
	/** response mode exponential (ph) */
	public static final int MODE_EXP       =  ProcessParameterIF.RESPONSE_MODE_EXP;
	
	/** Control loop ID (pH loop) */
	public static final int LID_PH_LOOP       = 0;
	/** Control loop ID (velocity loop) */
	public static final int LID_VELOCITY_LOOP = 1;
	/** Control loop ID (all loops) */
	public static final int LID_ALL           = 3;
	
	
	
	/** control mode ph/manual velocity/manual (setControlMode) */
	//public static final int MODE_PH_MANUAL_V_MANUAL      =900;
	/** control mode ph/manual velocity/constant (setControlMode) */
	//public static final int MODE_PH_MANUAL_V_CONSTANT    =905;
	/** control mode ph/manual velocity/deadband (setControlMode) */
	//public static final int MODE_PH_MANUAL_V_DEADBAND    =910;
	/** control mode ph/manual velocity/offset (setControlMode) */
	//public static final int MODE_PH_MANUAL_V_OFFSET      =915;
	
	/** control mode ph/constant velocity/manual (setControlMode) */
	//public static final int MODE_PH_CONSTANT_V_MANUAL    =920;
	/** control mode ph/constant velocity/constant (setControlMode) */
	//public static final int MODE_PH_CONSTANT_V_CONSTANT  =925;
	/** control mode ph/constant velocity/deadband (setControlMode) */
	//public static final int MODE_PH_CONSTANT_V_DEADBAND  =930;
	/** control mode ph/constant velocity/offset (setControlMode) */
	//public static final int MODE_PH_CONSTANT_V_OFFSET    =935;
	
	/** control mode ph/offset velocity/manual (setControlMode) */
	//public static final int MODE_PH_OFFSET_V_MANUAL      =940;
	/** control mode ph/offset velocity/constant (setControlMode) */
	//public static final int MODE_PH_OFFSET_V_CONSTANT    =945;
	/** control mode ph/offset velocity/deadband (setControlMode) */
	//public static final int MODE_PH_OFFSET_V_DEADBAND    =950;
	/** control mode ph/offset velocity/offset (setControlMode) */
	//public static final int MODE_PH_OFFSET_V_OFFSET      =955;
		
	
	
	/** initialize control loop */
	public void initializeControl(int id) throws Exception, RemoteException;
	
	/** reset control loop */
	public void resetControl(int id) throws Exception, RemoteException;
	
	/** start control loop */
	public void startControl(int id) throws Exception, RemoteException;
	
	/** stop control loop */
	public void stopControl(int id) throws Exception, RemoteException;
	
	/** pause control loop */
	public void pauseControl(int id) throws RemoteException;
	
	/** resume (paused) control loop */
	public void resumeControl(int id) throws RemoteException;
		
	/** Enter a pre-defined pH control mode indicated by modeID */
	public void setPHControlMode(int modeID)throws Exception, RemoteException;
	
	/** Get pH control mode indicated (modeID) */
	public int getPHControlMode()throws Exception, RemoteException;
	
	/** Enter a pre-defined response mode indicated by modeID */
	public void setPHResponseMode(int modeID)throws Exception, RemoteException;
	
	/** Get response mode indicated (modeID) */
	public int getPHResponseMode()throws Exception, RemoteException;
	
	/** Enter a pre-defined velocity control mode indicated by modeID */
	public void setVelocityControlMode(int modeID)throws Exception, RemoteException;
	
	/** Get velocity control mode indicated (modeID) */
	public int getVelocityControlMode()throws Exception, RemoteException;
	
	/** Enter a pre-defined velocity response mode indicated by modeID */
	public void setVelocityResponseMode(int modeID)throws Exception, RemoteException;
	
	/** Get velocity response mode indicated (modeID) */
	public int getVelocityResponseMode()throws Exception, RemoteException;
	
	/** Fast shutdown of control loop, inputs and outputs. 
	 */
	public void panicStop(int id)throws Exception, RemoteException;
	
	/** Set the input weighting for the specified (weighted average) filter input. 
	 In this context, the weight value is generally >= 0, though it could be set negative
	 to create linear combinations of inputs.
	 The input weights may be used in conjuction with divisor to dynamically configure signal filtering.
	 
	 @see #setFilterDivisor(int filterID, int divisor)
	 */
	public void setFilterInputWeight(int inputID, double weight) throws Exception,RemoteException;

	/** Set the divisor for the specified (weighted average) filter. 
	 The divisor behavior is defined as follows:
	 
	 If divisor == 0, 
	 the average is computed using the sum of the uninhibited inputs
	 divided by the total number of uninhibited inputs.
	 If divisor != 0, 
	 the average is computed using the sum of the uninhibited inputs
	 divided by the divisor.
	 
	 In this context, the divisor must be >=0; 
	 
	 The divisor may be used in conjuction with input weights to dynamically configure signal filtering.
	 @see #setFilterInputWeight(int inputID, double weight)
	 */
	public void setFilterDivisor(int filterID, int divisor) throws Exception,RemoteException;
	
	/** Change filter configuration.
		This method is used by clients to change which inputs are used to produce 
		filtered signals like Internal pH. 

	 @see #setFilterInputWeight(int inputID, double weight)
	 @see #setFilterDivisor(int filterID, int divisor)
	 */
	public void configureFilter(int filterID, int[] inputIDs, double[] inputWeights, int divisor) throws Exception, RemoteException;
}
