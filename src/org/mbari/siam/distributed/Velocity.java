/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

import java.io.Serializable;

/**
 * Generic Velocity Object.  Units are cm/s.
 */

/** Velocity */
public class Velocity implements Serializable 
{
    double	_x, _y, _z;

    /** No-argument constructor, initializes velocity to zero */
    public Velocity() {
	_x = 0.0;
	_y = 0.0;
	_z = 0.0;
    }

    /** X/Y/Z constructor, values in cm/sec	*/
    public Velocity(double x, double y, double z) {
	_x = x;
	_y = y;
	_z = z;
    }

    /** Return velocity in X direction in cm/sec */
    public double getX() {
        return(_x);
    }

    /** Return velocity in Y direction in cm/sec */
    public double getY() {
        return(_y);
    }

    /** Return velocity in Z direction in cm/sec */
    public double getZ() {
        return(_z);
    }

    /** Set velocity in X direction in cm/sec */
    public void setX(double x) {
        _x = x;
    }

    /** Set velocity in Y direction in cm/sec */
    public void setY(double y) {
        _y = y;
    }

    /** Set velocity in Z direction in cm/sec */
    public void setZ(double z) {
        _z = z;
    }

    /** Initialize velocity to 0	*/
    public void clear() {
	_x = 0.0;
	_y = 0.0;
	_z = 0.0;
    }

    /** Add Velocity vectors		*/
    public void add(Velocity v) {
	_x += v._x;
	_y += v._y;
	_z += v._z;
    }

    /** Divide Velocity by a scalar	*/
    public void div(int divisor) {
	_x /= divisor;
	_y /= divisor;
	_z /= divisor;
    }

    /** Return String representation */
    public String toString() {
	return("X=" + _x + "; Y=" + _y + ";Z=" + _z);
    }

}