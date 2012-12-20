// MBARI copyright 2003
package org.mbari.siam.distributed;

/**
Location captures the nominal physical location of an ISI device
 (Not, in general, to a precision or accuracy suitable for scientific 
 purposes).

@author Kent Headley
*/
public class LatLonDepth{
    double _lat=0;
    double _lon=0;
    double _depth=0;

    public LatLonDepth(){
	super();
    }
    public LatLonDepth(double lat,double lon,double depth){
	super();
	_lat=lat;
	_lon=lon;
	_depth=depth;
    }
    /** Get latitude */
    public double getLatitude(){
	return _lat;
    }
    /** Set latitude */
    public void setLatitude(double lat){
	_lat=lat;
    }
    /** Get longitude */
    public double getLongitude(){
	return _lon;
    }
    /** Set longitude */
    public void setLongitude(double lon){
	_lon=lon;
    }
    /** Get depth */
    public double getDepth(){
	return _depth;
    }
    /** Set depth */
    public void setDepth(double depth){
	_depth=depth;
    }
    /** Convert to string */
    public String toString(){
	return ("["+_lat+","+_lon+","+_depth+"]");
    }
}

