/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.text.NumberFormat;

import org.mbari.siam.distributed.InvalidPropertyException;

import org.apache.log4j.Logger;

public class FilterInput{
    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(FilterInput.class);
	
	public static final int TRIGGER_ON_UPDATE=0;
	public static final int TRIGGER_ON_CHANGE=1;
	public static final double DEFAULT_WEIGHT=1.0;
	public static final double DEFAULT_OFFSET=0.0;
	public static final int DEFAULT_MODE=TRIGGER_ON_UPDATE;
	public static final String DEFAULT_NAME="default";
	public static final int DEFAULT_ID=0;

	protected boolean _isChange=false;
	protected boolean _isUpdate=false;
	protected boolean _inhibit=false;
	protected double _value=0.0;
	protected double _rawValue=0.0;
	protected double _initialValue=0.0;
	protected double _weight = DEFAULT_WEIGHT;
	protected double _offset = DEFAULT_OFFSET;
	protected String _name=DEFAULT_NAME;
	protected int _id=DEFAULT_ID;
	protected Vector _filters=new Vector();
	protected int _mode=TRIGGER_ON_UPDATE;
	protected NumberFormat _dfmt;
	
	public FilterInput(){
		super();
		_dfmt=NumberFormat.getInstance();
		_dfmt.setMinimumFractionDigits(0);
		_dfmt.setMaximumFractionDigits(2);				
	}
	public FilterInput(int id){
		this(DEFAULT_NAME,id,DEFAULT_WEIGHT,DEFAULT_OFFSET,DEFAULT_MODE);
	}
	
	public FilterInput(String name){
		this(name,DEFAULT_ID,DEFAULT_WEIGHT,DEFAULT_OFFSET,DEFAULT_MODE);
	}
	
	public FilterInput(String name, int id){
		this(name,id,DEFAULT_WEIGHT,DEFAULT_OFFSET,DEFAULT_MODE);
	}
	public FilterInput(int id, int mode){
		this(DEFAULT_NAME,id,DEFAULT_WEIGHT,DEFAULT_OFFSET,mode);
	}
	public FilterInput(String name,int id, int mode){
		this(name,id,DEFAULT_WEIGHT,DEFAULT_OFFSET,mode);
	}
	
	public FilterInput(int id, double weight,double offset){
		this(DEFAULT_NAME,id,weight,offset,DEFAULT_MODE);
	}
	public FilterInput(String name, int id, double weight,double offset){
		this(DEFAULT_NAME,id,weight,offset,DEFAULT_MODE);
	}
	public FilterInput(String name, int id, double weight,double offset, int mode){
		this();
		_name=name;
		_id=id;
		_weight=weight;
		_offset=offset;
		_mode=mode;		
	}
	
	
	
	/** Attach this FilterInput to a Filter */
	public void attach(Filter filter){
		if(_filters.contains(filter)==false){
			_filters.add(filter);	    
		}
	}
	
	public boolean isChange(){
		return _isChange;
	}
	public boolean isUpdate(){
		return _isUpdate;
	}
	
	public void set(double value){
		if(value!=_rawValue){
			setChange(true);
		}else{
			setChange(false);			
		}
		_rawValue=value;
		_value=_rawValue*_weight+_offset;
		//System.out.println("set - r:"+_rawValue+" w:"+_weight+" o:"+_offset+" v:"+_value);
		//System.out.println(this);
		setUpdate(true);
	}
	public void set(long value){
		set((double)value);
	}
	public void set(int value){
		set((double)value);
	}
	
	public void put(double value){
		//_log4j.debug(_name+" put double");
		set(value);
		trigger();
	}
	public void put(int value){
		//_log4j.debug(_name+" put int");
		put((double)value);
	}
	public void put(long value){
		//_log4j.debug(_name+" put long");
		put((double)value);
	}
	
	public void trigger(){
		//_log4j.debug(_name+"input triggering");
		if(_inhibit==false && (
							   (_mode==TRIGGER_ON_CHANGE && isChange()) ||
							   (_mode==TRIGGER_ON_UPDATE && isUpdate())
							   )){
			//_log4j.debug(_name+" calling filter trigger(s)");
			for(Enumeration e=_filters.elements();e.hasMoreElements();){
				Filter f=(Filter)e.nextElement();
				//_log4j.debug(_name+"calling triggerIn");
				f.triggerIn(this);
			}
		}
	}
	public boolean getInhibit(){
		return _inhibit;
	}
	public void setInhibit(boolean value){
		_inhibit=value;
	}
	public void setInit(double value){
		_initialValue=value;
	}
	public void setInit(int value){
		setInit((double)value);
	}
	public void setInit(long value){
		setInit((double)value);
	}
	
	public void reset(double value){
		setChange(false);
		setUpdate(false);
		_inhibit=false;
		_rawValue=value;
		_value=_rawValue*_weight+_offset;
	}
	public void reset(){
		reset(_initialValue);
	}
	public void reset(int value){
		reset((double)value);
	}
	public void reset(long value){
		reset((double)value);
	}
	
	private void setChange(boolean value){
		_isChange=value;
	}
	private void setUpdate(boolean value){
		_log4j.debug("FilterInput."+this._name+".setUpdate("+value+")");
		_isUpdate=value;
	}
	
	public int intValue(){
		return (int)doubleValue();
	}
	public double doubleValue(){
		return _value;
	}
	public long longValue(){
		return (long)doubleValue();
	}
	public void setName(String name){
		_name=name;
	}
	public String name(){
		return _name;
	}
	public int id(){
		return _id;
	}
	public void setID(int id){
		_id=id;
	}
	
	public void setWeight(double value){
		_weight=value;
	}
	public void setOffset(double value){
		_offset=value;
	}
	public double weight(){
		return _weight;
	}
	public double offset(){
		return _offset;
	}
	 
	public String toString(){
		StringBuffer sb=new StringBuffer();
		sb.append("["+_name+" id:"+_id+" w:"+_weight+" o:"+_dfmt.format(_offset)+" m:"+_mode+" u:"+_isUpdate+" c:"+_isChange+" i:"+_inhibit+"][r:"+_dfmt.format(_rawValue)+" v:"+_dfmt.format(_value)+"]");
		return sb.toString();
		
	}
}

