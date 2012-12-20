// MBARI copyright 2003
package org.mbari.siam.distributed;

/**
Location captures the nominal physical location of an ISI device
 (Not, in general, to a precision or accuracy suitable for scientific 
 purposes).

@author Kent Headley
*/
public class RelCartesian{
    double _nodeRelX=0;
    double _nodeRelY=0;
    double _nodeRelZ=0;

    public RelCartesian(){
	super();
    }
    public RelCartesian(double x,double y,double z){
	super();
	_nodeRelX = x;
	_nodeRelY = y;
	_nodeRelZ=  z;

    }
    /** Get x */
    public double getX(){
	return _nodeRelX;
    }
    /** Set x */
    public void setX(double x){
	_nodeRelX=x;
    }
    /** Get y */
    public double getY(){
	return _nodeRelY;
    }
    /** Set y */
    public void setY(double y){
	_nodeRelY=y;
    }
    /** Get z */
    public double getZ(){
	return _nodeRelZ;
    }
    /** Set z */
    public void setZ(double z){
	_nodeRelZ=z;
    }
    /** Convert to string */
    public String toString(){
	return ("["+_nodeRelX+","+_nodeRelY+","+_nodeRelZ+"]");
    }
}

