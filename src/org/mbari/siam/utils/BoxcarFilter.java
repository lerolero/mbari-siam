/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils;

import java.util.Vector;
import java.util.Enumeration;

import org.mbari.siam.distributed.InvalidPropertyException;

import org.apache.log4j.Logger;

public class BoxcarFilter extends Filter{
    public static final int DEFAULT_DEPTH=10;
    protected Vector _values;
    protected int _depth;
    protected int _index;

    public BoxcarFilter(){
		this(DEFAULT_DEPTH);
    }
	/*
    public BoxcarFilter(String name){
		this(name,DEFAULT_DEPTH);
    }
	 */
    public BoxcarFilter(int depth){
		this(DEFAULT_NAME,DEFAULT_ID,depth);
    }
    public BoxcarFilter(String name, int id){
		this(name,id,DEFAULT_DEPTH);
    }
    public BoxcarFilter(String name, int id, int depth){
		super(name,id);
		_values=new Vector();
		_depth=depth;
		_index=0;
    }
    public BoxcarFilter(String name,int id, Vector inputs,int depth)
	throws InvalidPropertyException{
		super(name,id,inputs);
		_values=new Vector();
		_depth=depth;
		_index=0;
    }
	
    /** Perform the filter function with the new input value.
	Sets member variable _filterValue, which will be passed
	to the output by triggerOut() if the output gating
	conditions are met.
	Overrides base class default.
    */
    protected int doFilterAction(double value){
		double sum=0.0;
		if(_index>=_depth){
			for(int i=(_depth-1);i<0;i--){
				_values.set(i,_values.get((i-1)));
			}
			_index=0;
		}
		if(_index>=_values.size())
			_values.add(_index++,new Double(value));
		else
			_values.set(_index++,new Double(value));
		
		for( Enumeration e=_values.elements();e.hasMoreElements();){
			double d=((Double)e.nextElement()).doubleValue();
			sum+=d;
			//_log4j.debug("val:"+d+" sum:"+sum+" sz:"+_values.size());
		}
		_filterValue=sum/_values.size();
		return ACTION_OK;
    }

    /** Reset filter.
	Does nothing by default. 
	Subclasses may use this to provide a way to reset the filter.
	Overrides base class default.
    */
    public void reset(){
	_values.removeAllElements();
	_index=0;
    }

    public int depth(){
	return _depth;
    }
	/** Resets the depth of this filter.
		Also empties filter contents
	*/
	public void setDepth(int depth)
	throws InvalidPropertyException{
		if(_depth<=0){
			throw new InvalidPropertyException("Invalid depth ["+depth+"] - must be >0");
		}
		_values.clear();
		_index=0;
		_depth=depth;
	}
}
