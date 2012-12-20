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

/** The RangeValidator evaluates the value of its input against a specified range of values.
	The output is valid if the value is within the specified range. If the input value is
	outside the valid range, there are several options, set by a member variable:
	- REJECT         : reject the value; do not change output
	- USE_LAST_VALID : output last valid input value
    - USE_AVERAGE    : average up to _average_n previous valid values
    - USE_CONSTANT   : use the same constant value

	This class is intended to be immutable: if you want a different one, create a new one.
 */

public class RangeValidator{
	/////////////////////////////////////
	// Constants, public members
	/////////////////////////////////////
	public static final int REJECT=0;
	public static final int USE_LAST_VALID=1;
	public static final int USE_AVERAGE=2;
	public static final int USE_CONSTANT=3;
	
	public static final int RANGE_INSIDE=1;
	public static final int RANGE_OUTSIDE=2;
	
	public static final int DEFAULT_AVERAGE_N=1;
	public static final double DEFAULT_CONSTANT=Double.NaN;
	public static final long DEFAULT_VALID=0L;
	public static final long DEFAULT_INVALID=0L;

    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(RangeValidator.class);
	
	/////////////////////////////////////
	// Configuration variables
	/////////////////////////////////////
	protected int _range_region=RANGE_INSIDE;
	protected boolean _includeLower=true;
	protected boolean _includeUpper=true;
	protected double _lowerBound=Double.NaN;
	protected double _upperBound=Double.NaN;
	protected double _constant=Double.NaN;
	protected int _rejectPolicy=REJECT;
	protected int _average_n=1;
	/////////////////////////////////////
	// State variables
	/////////////////////////////////////
	protected double _lastValidValue=Double.NaN;
	protected double[] _lastNValues;
	protected int _pastValues=0;
	protected int _index=0;
	protected long _validCount;
	protected long _invalidCount;
	/////////////////////////////////////
	// private, protected member objects
	/////////////////////////////////////
	protected StringBuffer _stringBuffer=null;
	protected double _outputValue=Double.NaN;
	protected NumberFormat _dfmt;
	
	/** No-arg constructor */
    public RangeValidator()
	{
		super();
		_validCount=0L;
		_invalidCount=0L;
		_dfmt=NumberFormat.getInstance();
		_dfmt.setMinimumFractionDigits(0);
		_dfmt.setMaximumFractionDigits(2);
    }
	
	/** Constructor for any type */
    private RangeValidator(int region, double lowerBound, double upperBound, boolean includeLower, boolean includeUpper, int rejectPolicy, int averageN, double constant, long validCount, long invalidCount ) 
	throws Exception{
		this();
		setBounds(region,  lowerBound,  upperBound,  includeLower,  includeUpper);
		setRejectPolicy(rejectPolicy);
		setAverageN(averageN);
		setConstant(constant,true);
		_validCount=validCount;
		_invalidCount=invalidCount;
    }
	
	/////////////////////////////////////
	// public static factory methods
	// to resolve similar signatures
	// and prevent confusion over which 
	// to call
	/////////////////////////////////////

	/////////////////////////////////////
	// initailize valid/invalid count to 0
	/////////////////////////////////////

	public static RangeValidator getRejectingValidator(int region, double lowerBound, double upperBound, boolean includeLower, boolean includeUpper)
	throws Exception{
		return new RangeValidator(region,  lowerBound,  upperBound,  includeLower,  includeUpper,  REJECT, DEFAULT_AVERAGE_N, DEFAULT_CONSTANT, DEFAULT_VALID,DEFAULT_INVALID);
	}
	public static RangeValidator getAveragingValidator(int region, double lowerBound, double upperBound, boolean includeLower, boolean includeUpper, int averageN)
	throws Exception{
		return new RangeValidator( region,  lowerBound,  upperBound,  includeLower,  includeUpper,  USE_AVERAGE, averageN, DEFAULT_CONSTANT, DEFAULT_VALID,DEFAULT_INVALID);
	}
	
	public static RangeValidator getConstantValidator(int region, double lowerBound, double upperBound, boolean includeLower, boolean includeUpper, double constant)
	throws Exception{
		return new RangeValidator( region,  lowerBound,  upperBound,  includeLower,  includeUpper,  USE_CONSTANT, DEFAULT_AVERAGE_N, constant, DEFAULT_VALID,DEFAULT_INVALID);
	}
	
	public static RangeValidator getLastValidValidator(int region, double lowerBound, double upperBound, boolean includeLower, boolean includeUpper)
	throws Exception{
		return new RangeValidator( region,  lowerBound,  upperBound,  includeLower,  includeUpper,  USE_CONSTANT, DEFAULT_AVERAGE_N, DEFAULT_CONSTANT, DEFAULT_VALID,DEFAULT_INVALID);
	}
	
	/////////////////////////////////////
	// specify valid/invalid count to 
	// to transfer state to a new instance
	/////////////////////////////////////
	public static RangeValidator getRejectingValidator(int region, double lowerBound, double upperBound, boolean includeLower, boolean includeUpper, long validCount, long invalidCount )
	throws Exception{
		return new RangeValidator(region,  lowerBound,  upperBound,  includeLower,  includeUpper,  REJECT, DEFAULT_AVERAGE_N, DEFAULT_CONSTANT, validCount,invalidCount);
	}
	public static RangeValidator getAveragingValidator(int region, double lowerBound, double upperBound, boolean includeLower, boolean includeUpper, int averageN, long validCount, long invalidCount )
	throws Exception{
		return new RangeValidator( region,  lowerBound,  upperBound,  includeLower,  includeUpper,  USE_AVERAGE, averageN, DEFAULT_CONSTANT, validCount,invalidCount);
	}
	
	public static RangeValidator getConstantValidator(int region, double lowerBound, double upperBound, boolean includeLower, boolean includeUpper, double constant, long validCount, long invalidCount )
	throws Exception{
		return new RangeValidator( region,  lowerBound,  upperBound,  includeLower,  includeUpper,  USE_CONSTANT, DEFAULT_AVERAGE_N, constant, validCount,invalidCount);
	}
	
	public static RangeValidator getLastValidValidator(int region, double lowerBound, double upperBound, boolean includeLower, boolean includeUpper, long validCount, long invalidCount )
	throws Exception{
		return new RangeValidator( region,  lowerBound,  upperBound,  includeLower,  includeUpper,  USE_CONSTANT, DEFAULT_AVERAGE_N, DEFAULT_CONSTANT, validCount,invalidCount);
	}
	
	
	protected void setBounds(int region, double lowerBound, double upperBound, boolean includeLower, boolean includeUpper)
	throws Exception{
		switch (region) {
			case RANGE_INSIDE:
			case RANGE_OUTSIDE:
				_range_region=region;
				break;
			default:
				throw new Exception("invalid range region ["+region+"]");
		}
		_lowerBound=lowerBound;
		_upperBound=upperBound;
		_includeLower=includeLower;
		_includeUpper=includeUpper;
	}
	
	protected void setConstant(double constant,boolean allowOutsideRange )throws Exception{
		if(allowOutsideRange==false && accept(constant)==false){
			throw new Exception("constant ["+constant+"] not within defined range limit");
		}
		_constant=constant;
		
	}
	
	protected void setRejectPolicy(int policy)
	throws Exception{
		switch (policy) {
			case REJECT:
			case USE_LAST_VALID:
			case USE_AVERAGE:
			case USE_CONSTANT:
				_rejectPolicy=policy;
				break;
			default:
				throw new Exception("invalid reject policy ["+policy+"]");
		}
	}
	
	protected void setAverageN(int averageN)
	throws Exception{
		
		if(averageN<=0){
			throw new Exception("invalid averageN ["+averageN+"] must be >0");
		}
		
		double[] newArray=new double[averageN];
		/*
		if(_pastValues>0){
			int n=( (newArray.length < _pastValues) ? newArray.length : _pastValues);
			
			_pastValues=0;
			_index=0;
			for(int i=0;i<n;i++){
				newArray[i]=_lastNValues[i];
				_pastValues++;
				_index++;
			}
			_lastNValues=newArray;
			_average_n=averageN;
		}else{*/
			_lastNValues=new double[averageN];
			_pastValues=0;
			_index=0;
			_average_n=averageN;
		//}
	}

	protected void storeValue(double value){
		_log4j.debug("storeValue: val:"+_dfmt.format(value)+" index:"+_index+" pastValues:"+_pastValues);
		if(Double.isNaN(value)){
			_log4j.debug("found NaN, returning");
			return;
		}
	
		if(_index>=_average_n){
			_index=0;
		}
		
		_lastNValues[_index]=value;

		if(_pastValues>=_average_n){
			_pastValues=_average_n;
		}else{
			_pastValues=_index+1;		
		}
		_index++;
		_log4j.debug("storeValue: index:"+_dfmt.format(_index)+" pastValues:"+_pastValues);
	}
	
	protected double getAverage(){
		if(_pastValues<=0){
			return Double.NaN;
		}
		double sum=0.0;
		for(int i=(_pastValues>0?_pastValues-1:_pastValues);i>=0;i--){
				sum+=_lastNValues[i];
		}
		double avg=sum/_pastValues;

		return avg;
	}
	
	public long getValidCount(){
		return _validCount;
	}
	public long getInvalidCount(){
		return _invalidCount;
	}
	public long getSampleCount(){
		return _validCount+_invalidCount;
	}
	public double getValidRatio(){
		if(_invalidCount==0L && _validCount==0L){
			return 0.0;
		}
		return (double)((double)_validCount/(double)(_invalidCount+_validCount));
	}
	
	public boolean accept(double value){
		boolean accept=false;
		if(_range_region==RANGE_INSIDE){
			if(_includeLower){
				if(_includeUpper){
					if(value>=_lowerBound && value<=_upperBound){
						accept=true;
					}
				}else{
					if(value>=_lowerBound && value<_upperBound){
						accept=true;
					}
				}
			}else{
				if(_includeUpper){
					if(value>_lowerBound && value<=_upperBound){
						accept=true;
					}
				}else{
					if(value>_lowerBound && value<_upperBound){
						accept=true;
					}
				}
			}
		}else if(_range_region==RANGE_OUTSIDE){
			if(_includeLower){
				if(_includeUpper){
					if(value<=_lowerBound || value>=_upperBound){
						accept=true;
					}
				}else{
					if(value<=_lowerBound || value>_upperBound){
						accept=true;
					}
				}
			}else{
				if(_includeUpper){
					if(value<_lowerBound || value>=_upperBound){
						accept=true;
					}
				}else{
					if(value<_lowerBound || value>_upperBound){
						accept=true;
					}
				}
			}
		}
		return accept;
	}
	
    /** Perform the filter function with the new input value.
	Sets member variable _outputValue, which will be passed
	to the output by triggerOut() if the output gating
	conditions are met.
	Overrides base class default.
    */
    public double validate(double value){
		boolean accept=accept(value);

		if(accept){
			_log4j.debug("accepted");
			_outputValue=value;
			_lastValidValue=value;
			storeValue(_lastValidValue);
			if(_validCount==Long.MAX_VALUE){
				_validCount=0L;
				_invalidCount=0L;
			}
			_validCount++;
			
		}else{
			switch (_rejectPolicy) {
				case REJECT:
					_log4j.debug("rejected");
					return Double.NaN;
				case USE_LAST_VALID:
					_outputValue=_lastValidValue;
					storeValue(_lastValidValue);
					break;
				case USE_AVERAGE:
					_outputValue=getAverage();
					_lastValidValue=_outputValue;
					storeValue(_lastValidValue);
					break;
				case USE_CONSTANT:
					_outputValue=_constant;
					_lastValidValue=_outputValue;
					storeValue(_lastValidValue);
					break;
			}
			if(_invalidCount==Long.MAX_VALUE){
				_validCount=0L;
				_invalidCount=0L;
			}
			_invalidCount++;
			
		}
		
		return _outputValue;
    }

	public String toString(){
		if(_stringBuffer==null){
			_stringBuffer=new StringBuffer();
		}
		_stringBuffer.setLength(0);
		//_stringBuffer.insert(0,super.toString());
		_stringBuffer.append("impl [lb:"+_dfmt.format(_lowerBound)+" ub:"+_dfmt.format(_upperBound));
		_stringBuffer.append(" re:"+_range_region+" il:"+_includeLower+" iu:"+_includeUpper);
		_stringBuffer.append(" rp:"+_rejectPolicy+" n:"+_average_n);
		_stringBuffer.append(" lvv:"+_dfmt.format(_lastValidValue)+" idx:"+_index);
		_stringBuffer.append("  pv:"+_pastValues+"]\n");
		_stringBuffer.append("pvals[ ");
		for(int i=0;i<_pastValues;i++){
			_stringBuffer.append(_dfmt.format(_lastNValues[i])+" ");
		}
		_stringBuffer.append("]\n");

		return _stringBuffer.toString();
	}

}
