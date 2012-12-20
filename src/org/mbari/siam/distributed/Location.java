// MBARI copyright 2003
package org.mbari.siam.distributed;

/**
Location captures the nominal physical location of an ISI device
 (Not, in general, to a precision or accuracy suitable for scientific 
 purposes).

@author Kent Headley
*/
public class Location{

    public Location(String loc){

	_x = _y = _z = 0.;
    }

    public Location(double x, double y, double z) {
	_x = x;
	_y = y;
	_z = z;
    }

    public double _x;

    public double _y;

    public double _z;

    public String toString() {
	return "x: " + _x + " y: " + _y + " z: " + _z;
    }
}
 





