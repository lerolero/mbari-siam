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
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

public abstract class Filter{
    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(Filter.class);
	
	public static final int ACTION_OK=0;
	public static final int ACTION_CANCEL=-1;
	public static final int DEFAULT_ID=0;
	public static final String DEFAULT_NAME="unknown";
	
    //Hashtable _inputs = new Hashtable();
    Vector _inputs = new Vector();
    Vector _outputs = new Vector();
    double _filterOutput=0.0;
    double _filterValue=0.0;
	String _name=DEFAULT_NAME;
	int _id=DEFAULT_ID;
	NumberFormat _dfmt;

    public Filter(){
		super();
		_dfmt=NumberFormat.getInstance();
		_dfmt.setMinimumFractionDigits(0);
		_dfmt.setMaximumFractionDigits(2);		
    }
/*
    public Filter(String name){
		this();
		setName(name);
	}
    public Filter(int id){
		this();
		setID(id);
	}
	*/
	public Filter(String name, int id)
	{
		this();
		setName(name);
		setID(id);
	}
	
	public Filter(String name, int id, Vector inputs)
	throws InvalidPropertyException{
		this(name,id);
		addInputs(inputs);
	}
	/*
    public Filter(String name, Vector inputs)
	throws InvalidPropertyException{
		this();
		setName(name);
		addInputs(inputs);
	}
    public Filter(int id, Vector inputs)
	throws InvalidPropertyException{
		this();
		setID(id);
		addInputs(inputs);
		_dfmt=NumberFormat.getInstance();
		_dfmt.setMinimumFractionDigits(0);
		_dfmt.setMaximumFractionDigits(2);		
	}
	*/
	public void setName(String name){
		_name=name;
	}
	public String name(){
		return _name;
	}
	public void setID(int id){
		_id=id;
	}
	public int getID(){
		return _id;
	}
	
	public void addInputs(Vector inputs)
	throws InvalidPropertyException{
		for(Enumeration e=inputs.elements();e.hasMoreElements();){
			FilterInput fi=(FilterInput)e.nextElement();
			this.addInput(fi);
		}		
	}
	
     public void addInput(FilterInput input)
	throws InvalidPropertyException
    {
		for(Enumeration e=_inputs.elements();e.hasMoreElements();){
			FilterInput f=(FilterInput)e.nextElement();
			if(f.name().equals(input.name())){
				throw new InvalidPropertyException("input name "+input.name()+" already exists");
			}
			if( f.id()==input.id() ){
				throw new InvalidPropertyException("input id "+input.id()+" already exists");
			}
		}
		_inputs.add(input);
		input.attach(this);
    }
	
    /** Attach this Filter's output to a FilterInput */
    public void attach(FilterInput input){
		if(_outputs.contains(input)==false){
			_outputs.add(input);	    
		}
    }
	
    /** Get a named input. Use key Filter.DEFAULT_INPUT_KEY to get the
	 the default input.
	 returns null if the requested input does not exist
     */
    public FilterInput getInput(String key){
		for(Enumeration e=_inputs.elements();e.hasMoreElements();){
			
			FilterInput f=(FilterInput)e.nextElement();
			
			if(f.name().equals(key)){
				return f;
			}			
		}
		
		return null;
    }
    public FilterInput getInput(int index){
		return (FilterInput)_inputs.get(index);
    }
	
	/** Remove the first input with name=key.
		If no input with this name exists, the inputs
		
	 */
    public void removeInput(String key){
		for(Enumeration e=_inputs.elements();e.hasMoreElements();){
			
			FilterInput f=(FilterInput)e.nextElement();
			
			if(f.name().equals(key)){
				_inputs.remove(f);
				return;
			}			
		}
		
		return;
    }
	
	public void removeAllInputs(){
		_inputs.clear();
	}
	
	public int indexOf(String key){
		FilterInput f=getInput(key);
		if( f!=null){
			return _inputs.indexOf(f);
		}
		return -1;
	}
	public int inputCount(){
		return _inputs.size();
	}
	
	
    /** Gate filter processing based on logic in this method.
	 For example, may not perform processing if some inputs haven't changed.
	 By default, returns true, allowing processing to occur.
     */
    public boolean inputGate(){
		// do nothing by default
		// may override in subclasses
		return true;
    }
	
    /** Gate filter output based on logic in this method.
	 For example, may not change output if some inputs haven't changed.
	 By default, returns true, allowing processing to occur.
     */
    public boolean outputGate(){
		// do nothing by default
		// may override in subclasses
		return true;
    }
	
    /** Notify the filter that an input value is available.
	 Trigger filter action based on gating function.
	 */
    public void triggerIn(FilterInput input){
		if(inputGate()==true){
			//_log4j.debug("calling doFilterAction");
			int action=doFilterAction(input.doubleValue());
			if(action==ACTION_OK){
				//_log4j.debug("calling triggerOut");
				triggerOut();
			}
			//_log4j.debug("resetting input "+input.name());
			//input.reset();
		}
    }
	
    /** Propagate outputs pending output gating function.
	 */
    protected void triggerOut(){
		if(outputGate()==true){
			_filterOutput=_filterValue;
			for(Enumeration e=_outputs.elements();e.hasMoreElements();){
				FilterInput output=(FilterInput)e.nextElement();
				output.put(_filterOutput);
			}
		}
    }
	
    /** Perform the filter function with the new input value.
	 Sets member variable _filterValue, which will be passed
	 to the output by triggerOut() if the output gating
	 conditions are met.
	 Default action is to pass values to output.
	 */
	protected abstract int doFilterAction(double value);
/*
    protected int doFilterAction(double value){
		// pass input to output by default
		// sub classes should override
		// and perform filter logic here
		_filterValue=value;
		return ACTION_OK;
    }
	*/
    /** Reset filter.
	 Does nothing by default. 
	 Subclasses may use this to provide a way to reset the filter.
	 */
    public void reset(){
		// do nothing
    }
	
    public double doubleValue(){
		return _filterOutput;
    }
    public float floatValue(){
		return (float)_filterOutput;
    }
    public int intValue(){
		return (int)_filterOutput;
    }
    public long longValue(){
		return (long)_filterOutput;
    }
	
	public String toString(){
		StringBuffer sb=new StringBuffer();
		String name="UNDEFINED";
		if(_name!=null){
			name=_name;
		}
		
		sb.append(name+".state ["+"value:"+_dfmt.format( _filterValue)+", output:"+_dfmt.format(_filterOutput)+"]\n");
		if(_inputs!=null){
			for(Enumeration e=_inputs.elements();e.hasMoreElements();){
				FilterInput fi=(FilterInput)e.nextElement();
				sb.append(name+".I:"+fi+"\n");
			}
		}
		if(_outputs!=null){
			for(Enumeration e=_outputs.elements();e.hasMoreElements();){
				FilterInput fo=(FilterInput)e.nextElement();
				sb.append(name+".O:"+fo+"\n");
			}
		}
		//sb.append("]");
		return sb.toString();
	}
	
}
