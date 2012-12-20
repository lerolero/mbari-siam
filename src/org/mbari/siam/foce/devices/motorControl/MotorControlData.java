/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.motorControl;

import java.io.Serializable;
import org.mbari.siam.distributed.Velocity;

/** MotorControlData represents one data point of output from MotorControl
 * @author Bob Herlien
*/

public class MotorControlData implements Serializable
{
    protected int _velocityCmdX, _velocityCmdY;
    protected Velocity _waterVelocity;
    protected Velocity _adcpVelocity;

    public MotorControlData(int velocityCmdX, int velocityCmdY,
			    Velocity waterVelocity, Velocity adcpVelocity)
    {
	_velocityCmdX = velocityCmdX;
	_velocityCmdY = velocityCmdY;
	_waterVelocity = waterVelocity;
	_adcpVelocity  = adcpVelocity;
    }

} // end of class
