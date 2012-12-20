/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.elmo.eswPump;

import java.util.Vector;
import java.util.Iterator;
import java.text.ParseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.DecimalFormat;

import org.apache.log4j.Logger;

import org.mbari.siam.registry.InstrumentDataListener;
import org.mbari.siam.registry.InstrumentRegistry;
import org.mbari.siam.registry.RegistryEntry;

import org.mbari.siam.core.BaseInstrumentService;
import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.core.NodeProperties;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.core.InstrumentPort;
import org.mbari.siam.core.SerialInstrumentPort;
import org.mbari.siam.core.AnalogInstrumentPort;
import org.mbari.siam.core.ServiceSandBox;

import org.mbari.siam.utils.StreamUtils;
import org.mbari.siam.utils.PrintfFormat;
import org.mbari.siam.utils.StopWatch;

import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.Parent;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.Velocity;
import org.mbari.siam.distributed.CommsMode;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.Summarizer;
import org.mbari.siam.distributed.SummaryPacket;
import org.mbari.siam.distributed.Safeable;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.PropertyException;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.InvalidDataException;
import org.mbari.siam.distributed.devices.ElmoIF;
import org.mbari.siam.distributed.devices.ElmoThrusterIF;
import org.mbari.siam.foce.devices.elmo.base.*;
import org.mbari.siam.foce.devices.elmo.thruster.*;

import org.mbari.siam.foce.deployed.IOMapper;

public class ESWPump extends ElmoThruster implements ElmoThrusterIF
{
		
	/** Log4j logger */
	protected static Logger _log4j = Logger.getLogger(ESWPump.class);
		
	public ESWPump()throws RemoteException{
		super();
		_attributes=new ESWPumpAttributes(this);
	}
	
	public void initializeController() 
	throws TimeoutException, IOException,Exception,RemoteException
	{
		/*
		try{
			super.initializeController();
		}catch(Exception e){
		 if(_log4j.isDebugEnabled()){
			_log4j.debug("**** Kilroy - foiled again - gear ratio = "+((ESWPumpAttributes)_attributes).gearRatio+" elmo GR:"+_elmo.getGearRatio());
		 }
		}
		_elmo.setGearRatio(((ESWPumpAttributes)_attributes).gearRatio);
		 ((ESWPumpAttributes)_attributes).subMinMotorCounts=orpm2counts(((ESWPumpAttributes)_attributes).subMinMotorRPM);
		 if(_log4j.isDebugEnabled()){
		 _log4j.debug("**** Kilroy - set subMinMotorCounts["+((ESWPumpAttributes)_attributes).subMinMotorRPM+"] = "+((ESWPumpAttributes)_attributes).subMinMotorCounts);
		 }
		 */
		super.initializeController();
		((ESWPumpAttributes)_attributes).subMinMotorCounts=orpm2counts(((ESWPumpAttributes)_attributes).subMinMotorRPM);
	}		
	/** Validate velocity value.
	 By default, does nothing and relies on the
	 configured ElmoImpl limits.
	 Subclasses may override this method to provide
	 special range validatation logic.
	 For example, a service may wish to impose constraints 
	 like a range that includes 0 rpm and 50-100 rpm, but 
	 no speeds between 0-50.
	 */
	public void validateSpeed(int counts)
	throws IllegalArgumentException{
		int minCounts=((ESWPumpAttributes)_attributes).subMinMotorCounts;
		
		if(counts==0){
			return;
		}
		
		if(Math.abs(counts)<minCounts){
			throw new IllegalArgumentException("Invalid speed ["+counts+"] in validateSpeed - valid values: 0,"+minCounts+"-"+((ESWPumpAttributes)_attributes).maxMotorCounts);
		}
		
		return;
	}
	
	public class ESWPumpAttributes extends ElmoThruster.ElmoThrusterAttributes {
		double subMinMotorRPM=50.0;
		int subMinMotorCounts=200;
		/**
		 * Constructor, with required InstrumentService argument
		 */
		public ESWPumpAttributes(DeviceServiceIF service) {
			super(service);
			gearRatio=15.0;
			minMotorCounts=0;
			maxMotorCounts=450;
			if(_log4j.isDebugEnabled()){
			//_log4j.debug("**** Kilroy - ESWPumpAttr CTOR subMinMotorCounts = "+subMinMotorCounts+" gearRatio="+gearRatio);
			}
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
			if(attributeName.equals("subMinMotorRPM")){
				if(_elmo!=null){
					double maxRPM=0.0;
					try{
						maxRPM=_elmo.counts2orpm(maxMotorCounts);
					}catch (Exception e) {
						throw new InvalidPropertyException("could not conver maxMotorCounts to output RPM ");
					}
					validateDouble(valueString,0.0,maxRPM,true,false);
					try{
						double counts=Double.parseDouble(valueString);
						((ESWPumpAttributes)_attributes).subMinMotorCounts=orpm2counts(counts);
						if(_log4j.isDebugEnabled()){
						//_log4j.debug("**** Kilroy - setAttr subMinMotorCounts = "+((ESWPumpAttributes)_attributes).subMinMotorCounts+" gearRatio="+((ESWPumpAttributes)_attributes).gearRatio);
						}
					}catch (Exception e) {
						throw new InvalidPropertyException("validation failed ["+attributeName+"="+valueString+"]:"+e.getMessage());
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
	}
}
