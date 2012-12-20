// Copyright 2002 MBARI
package org.mbari.siam.distributed;

import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.Externalizable;
import java.io.DataOutput;
import java.io.ObjectOutput;
import java.io.ObjectInput;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.StreamCorruptedException;
import java.io.OptionalDataException;
import java.io.IOException;
import java.lang.ClassNotFoundException;
import java.util.Iterator;
import java.util.Map;
import java.util.Hashtable;
import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.mbari.siam.distributed.Exportable;

/**
   MnemonicIntegerAttributeValueObject - The value for state attributes which have
   mnemonics that represent integer values (e.g., powerPolicy -- "never":0; "sampling":1; etc.).
   toString() returns the mnemonic, value() returns the Integer value.

   @author Kent Headley
*/

public class MnemonicIntegerAttributeValueObject extends AttributeValueObject{
    /** Log4j logger */
    protected static Logger _logger = Logger.getLogger(StateAttribute.class);

    /** Serial version ID */
    private static final long serialVersionUID=0x0L;

    private Map _map = null;

    public MnemonicIntegerAttributeValueObject(){}

    /** Contructor 
	@param s constructor calls parse(s) to obtain _value member
	@param map mapping between strings and values
     */
    public MnemonicIntegerAttributeValueObject(String s, Map map) throws Exception{
	_map = map;
	_value = parse(s);
    }

    public MnemonicIntegerAttributeValueObject(int i, Map map) throws Exception{
	_map = map;
	_value = parse(i);
    }

    /** Return String representation. */
    public String toString(){
	return ((String)_value);
    }

    /** Parse and validate the value of 
	this attribute from the specified string 
    */
    public Object parse(String s) throws Exception{
	try{
	    if(_map.containsKey(s)){
		_value=s;
		return _value;
	    }else{
		Integer i = new Integer(s);
		if(_map.containsValue(i)){
		    for(Iterator e = _map.keySet().iterator();e.hasNext();){
			String nextKey = (String)e.next();
			if( ((Integer)_map.get(nextKey)).equals(i) ){
			    _value = nextKey;
			    return _value;
			}
		    }   
		}
	    }
	}catch(NumberFormatException e){
	    throw(new Exception("MnemonicIntegerAttributeValueObject: invalid value "+s));
	}
	throw new Exception("Invalid Value");
    }

    /** Parse and validate the value of 
	this attribute from the specified string 
    */
    public Object parse(int n) throws Exception{
	Integer i = new Integer(n);
	for(Iterator e = _map.keySet().iterator();e.hasNext();){
	    String nextKey = (String)e.next();
	    if( ((Integer)_map.get(nextKey)).equals(i) ){
		_value = nextKey;
		return _value;
	    }
	}
	throw new Exception("Invalid Value");
    }

    /** value() is overridden to return the numeric value */
    public Object value(){
	return (_map.get(_value));
    }

    public void writeExternal(ObjectOutput out) 
	throws IOException{

	//_logger.debug("Externalizing MnemonicIntegerAttributeValueObject ***************************");
	out.writeInt(_map.size());
	for(Iterator i = _map.keySet().iterator();i.hasNext();){
	    String key = (String)i.next();
	    Integer value = (Integer)_map.get(key);
	    out.writeInt(key.getBytes().length);
	    out.write(key.getBytes());
	    out.writeInt(value.intValue());
	}
	out.writeInt(((Integer)this.value()).intValue());
    }

    public void readExternal(ObjectInput in) 
	throws IOException,ClassNotFoundException{

	//_logger.debug("Un-Externalizing MnemonicIntegerAttributeValueObject ***************************");
	_map = new Hashtable();
	int mapSize = in.readInt();
	_logger.debug("map size = "+mapSize);
	for(int j=0;j<mapSize;j++){
	    int keySize = in.readInt();
	    byte[] key = new byte[keySize];
	    in.readFully(key,0,keySize);
	    Integer value = new Integer(in.readInt());
	    _logger.debug("key= "+new String(key)+" value="+value);
	    _map.put(new String(key),value);
	}
	int i = in.readInt();
	for(Iterator e = _map.keySet().iterator();e.hasNext();){
	    String nextKey = (String)e.next();
	    if( ((Integer)_map.get(nextKey)).intValue()==i ){
		_value = nextKey;
		return;
	    }
	}
	throw new IOException("Invalid value: "+i);
    }
    /** Fulfills Exportable interface */
    public void export(DataOutput out)
	throws IOException{
	out.writeShort(Exportable.EX_MNEMONICINTEGEROBJATT);	
	out.writeLong(serialVersionUID);

	out.writeInt(_map.size());
	for(Iterator i = _map.keySet().iterator();i.hasNext();){
	    String key = (String)i.next();
	    Integer value = (Integer)_map.get(key);
	    out.writeInt(key.getBytes().length);
	    out.write(key.getBytes());
	    out.writeInt(value.intValue());
	}
	out.writeInt(((Integer)this.value()).intValue());	
    }
}





