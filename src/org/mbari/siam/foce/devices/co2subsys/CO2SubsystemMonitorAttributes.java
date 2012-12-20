/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.co2subsys;

import java.util.Vector;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;

import org.apache.log4j.Logger;
import gnu.io.SerialPort;

import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.PropertyException;
import org.mbari.siam.distributed.devices.CO2SubsystemMonitorIF;

public class CO2SubsystemMonitorAttributes extends InstrumentServiceAttributes {
	/** Log4j logger */
	protected static Logger _log4j = Logger.getLogger(CO2SubsystemMonitorAttributes.class);
	
	public int serial_baud=9600;
	public int serial_databits=SerialPort.DATABITS_8;
	public int serial_parity=SerialPort.PARITY_NONE;
	public int serial_stopbits=SerialPort.STOPBITS_1;
	public int serial_mode=BB232SPDA.MODE_SERIAL_RFC1722;
	
	public double voltage_24v_a=6.0;//volts/volt
	public double current_24v_a=2.0;//amps/volt
	public double humidity_a=37.5;//%RH/volt
	public double humidity_b=-37.5;//%RH
	public double temperature_a=7.689;
	public double temperature_b=-48.935;
	public double temperature_c=137.49;
	public double temperature_d=-132.34;

	public String osdtHost="localhost";
	public String fan_command="ON";
	
	private CO2SubsystemMonitor monitor=null;
	
	/**
	 * Constructor, with required InstrumentService argument
	 */
	public CO2SubsystemMonitorAttributes(DeviceServiceIF service) {
		super(service);
		// override InstrumentServiceAttributes defaults here
		if(service instanceof CO2SubsystemMonitor){
			monitor=(CO2SubsystemMonitor)service;
		}
	}

	/** Standard Constructor (with dummy arg to differentiate signatures) */
	public CO2SubsystemMonitorAttributes(DeviceServiceIF service,boolean dummyArg) {
		this(service);
		isiID=9999;
		locationName="right here";
		rbnbServer=osdtHost;
		serviceName="CO2SubsystemMonitor".getBytes();
		advertiseService=true;
		registryName="regControlLoop";
		//rbnbCacheFrames=1024;
		//rbnbArchiveFrames=1024;
	}

	/**
     * Called when specified attribute has been found. Throw
     * InvalidPropertyException if specified attribute has invalid value.
     * Note that the ServiceAttributes base class automatically validates
     * the value type before setAttributeCallback() is invoked; so this
     * method needs only to validate the value.
     * 
     * @param attributeName
     *            name of parsed attribute
     */
    protected void setAttributeCallback(String attributeName, String valueString)
	throws InvalidPropertyException {
		// only 9600N81 supported
		if(attributeName.equals("serial_baud")){
			validateInt(valueString,9600,9600,true,true);
		}else
		if(attributeName.equals("serial_databits")){
			validateInt(valueString,SerialPort.DATABITS_8,SerialPort.DATABITS_8,true,true);
		}else
		if(attributeName.equals("serial_parity")){
			validateInt(valueString,SerialPort.PARITY_NONE,SerialPort.PARITY_NONE,true,true);
		}else
		if(attributeName.equals("serial_stopbits")){
			validateInt(valueString,SerialPort.STOPBITS_1,SerialPort.STOPBITS_1,true,true);
		}else
		if(attributeName.equals("serial_mode")){
			try{
				
				//validateInt(valueString,BB232SPDA.MODE_SERIAL_RFC1722,BB232SPDA.MODE_SERIAL_RFC1722,true,true);
			}catch (Exception ipe) {
				// OK if it fails, except for the last one
				ipe.printStackTrace();
				if(_log4j.isDebugEnabled()){
				_log4j.debug("*** check serial_mode OK falling through to alternative value");
				}
			}
			//validateInt(valueString,BB232SPDA.MODE_SERIAL_LOCAL,BB232SPDA.MODE_SERIAL_LOCAL,true,true);
		}else
		if(attributeName.equals("fan_command")){
			int command=-1;
			if(valueString.equalsIgnoreCase("ON")){
				command=CO2SubsystemMonitorIF.FAN_CONTROL_ON;
			}else if(valueString.equalsIgnoreCase("OFF")){
				command=CO2SubsystemMonitorIF.FAN_CONTROL_OFF;
			}else{
				throw new InvalidPropertyException("Invalid fan_command ["+valueString+"]. Valid values: 'ON' or 'OFF'");
			}
			if(monitor!=null){
				try{
					monitor.setFanControl(command);
				}catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	public void validateDouble(String valueString, double min, double max, boolean inside, boolean includeEnds) 
	throws InvalidPropertyException{
		double value;
		try{
			value=Double.parseDouble(valueString);
		}catch (Exception e) {
			throw new InvalidPropertyException("parse error for "+valueString);
		}
		if(inside){
			if(includeEnds){
				if((value<min) || (value>max)){
					throw new InvalidPropertyException("range error for "+valueString);
				}		
			}else{
				if((value<=min) || (value>=max)){
					throw new InvalidPropertyException("range error for "+valueString);
				}						
			}
		}else{
			if(includeEnds){
				if((value>min) || (value<max)){
					throw new InvalidPropertyException("range error for "+valueString);
				}		
			}else{
				if((value>=min) || (value<=max)){
					throw new InvalidPropertyException("range error for "+valueString);
				}						
			}
			
		}
	}
	
	public void validateInt(String valueString, int min, int max, boolean inside, boolean includeEnds) 
	throws InvalidPropertyException{
		int value;
		try{
			value=Integer.parseInt(valueString);
		}catch (Exception e) {
			throw new InvalidPropertyException("parse error for "+valueString);
		}
		if(inside){
			if(includeEnds){
				if((value<min) || (value>max)){
					throw new InvalidPropertyException("range error for "+valueString);
				}		
			}else{
				if((value<=min) || (value>=max)){
					throw new InvalidPropertyException("range error for "+valueString);
				}						
			}
		}else{
			if(includeEnds){
				if((value>min) || (value<max)){
					throw new InvalidPropertyException("range error for "+valueString);
				}		
			}else{
				if((value>=min) || (value<=max)){
					throw new InvalidPropertyException("range error for "+valueString);
				}						
			}
			
		}
	}
	
}
