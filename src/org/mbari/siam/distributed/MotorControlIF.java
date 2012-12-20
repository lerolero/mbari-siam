/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

import org.mbari.siam.distributed.TimeoutException;

/** Presents and interface implementing motor control.
    Used by ControlLoop to control FOCE fan motors.
    Using interface enables different motor controllers to be used;
    they need only implement this interface to be used by the
    ControlLoop.
*/
/*
  $Id: MotorControlIF.java,v 1.3 2012/12/17 21:35:59 oreilly Exp $
  $Name: HEAD $
  $Revision: 1.3 $
*/

public interface MotorControlIF{

    /** enable/disable the motor. */
    public void setEnable(boolean value, long timeoutMsec) throws TimeoutException;

    /** set motor velocity in rpm */
    public void setVelocity(double rpm);

    /** get motor feedback velocity in rpm. */
    public double getVelocity();

    /** return motor status. */
    public int getMotorStatus();

    /** return detailed motor fault information. */
    public int getMotorFault();

}
