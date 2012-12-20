/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.co2subsys;
import java.util.Vector;
import java.util.Iterator;
import java.lang.StringBuffer;
import java.text.NumberFormat;
import java.text.ParseException;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import org.mbari.siam.registry.InstrumentDataListener;
import org.mbari.siam.registry.InstrumentRegistry;
import org.mbari.siam.registry.RegistryEntry;

import org.mbari.siam.core.DeviceService;
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

import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.Velocity;
import org.mbari.siam.distributed.CommsMode;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.Parent;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.ScheduleParseException;
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
import org.mbari.siam.distributed.InvalidDataException;

import org.mbari.siam.distributed.devices.CO2SubsystemMonitorIF;

/**
 reads and writes the B&B 232SPDA module used to interface various analog and digital 
 signals from within the CO2 electronics housing. It has seven 12-bit A/D inputs, 
 four 8-bit D/A outputs, two digital inputs and one digital output. It is powered 
 by +12Vdc and is communicated to via an RS-232 connection. 

 It is serial port #yy and it's serial parameters are xxxx,x,x,x with xx hardware control. 
 See appendicies for specific command structures.
 
 Analog In #0: Volt_24V
 This is a scaled version of the main +24Vdc bus. The input range is a DC 
 voltage from 0 volts to 5 volts, which maps to an input voltage range of 0 
 to +30V. The defining equation is 
 y (Bus volts) = 6.0x (ADC volts). 
 
 Analog In #1: Curr_24V
 This is a representative voltage of the current used by the +24Vdc bus. 
 The input range is a DC voltage from 0 volts to 5 volts, which maps to an 
 input current range of 0 to 10A. 
 The defining equation is 
 y (Bus amps) = 2.0x (ADC volts).  
 
 Analog In #2: HTM2530 Humidity
 This is the ambient humidity inside of the FOCE electronics can. It outputs 
 a 1-4V signal which corresponds to 0-100% Relative Humidity (RH). 
 The defining equation is 
 y (%RH) = 37.5 x (ADC volts) - 37.5. 
 
 Analog In #3: HTM2530 Temperature
 This is the ambient temperature inside of the FOCE electronics can. It outputs
 a 0 to 5V signal which corresponds to a -40 C to +125 C temperature range. 
 There are two possible defining equations, a more 	precise third order polynomial
 and a simpler linear equation. The linear 	equation works sufficiently around our 
 expected operating range but generates considerable errors at either extreme. 

 Third Order: y = 7.689x^3-48.935x^2+137.49x-132.34
      Linear: y = 40.944x - 73.787
 where y is the measured temperature in degress Celsius x is the ADC volts
 
 Analog In #4: Fan State
 This signal is actually a digital input signal used to indicate the stae of the 
 cooling fan. High = Fan OFF, Low = Fan ON. 
 
 Analog In #5: Reserved for future use
 Analog In #6: Reserved for future use
 
 Digital In #0: Water Sensor #1
 This is a digital input signal tied to water detector #1. A digital high 
 indicates no water present, while a digital low indicates water in the 
 housing. 
 
 Digital In #1: Water Sensor #2
 This is a digital input signal tied to water detector #2. A digital high 
 indicates no water present, while a digital low indicates water in the housing.
 
 Digital Out #0: Fan Control
 This is a digital output signal used to control the state of the cooling fan. 	
 Setting this bit high activates the fan, while a low de-activates the fan. 
 
 Analog Out #0-#3: Not Used
 These signals are not brought out to the Power Monitor Board.
 
 */
public class CO2SubsystemMonitor extends PolledInstrumentService
    implements Instrument, InstrumentDataListener, CO2SubsystemMonitorIF{

		/** Log4j logger */
		protected static Logger _log4j = Logger.getLogger(CO2SubsystemMonitor.class);
		
		// 
		public static final int VOLTAGE_24V    =BB232SPDA.RA_AI_0;
		public static final int CURRENT_24V    =BB232SPDA.RA_AI_1;
		public static final int HUMIDITY       =BB232SPDA.RA_AI_2;
		public static final int TEMPERATURE    =BB232SPDA.RA_AI_3;
		public static final int FAN_STATE      =BB232SPDA.RA_AI_4;
		public static final int ANALOG_IN_5    =BB232SPDA.RA_AI_5;
		public static final int ANALOG_IN_6    =BB232SPDA.RA_AI_6;
		public static final int WATER_SENSOR_1 =BB232SPDA.RA_DI_0;
		public static final int WATER_SENSOR_2 =BB232SPDA.RA_DI_1;
		public static final int FAN_CONTROL    =BB232SPDA.RA_DO_0;
		public static final int AD_BITS=12;
		public static final double AD_COUNTS=Math.pow(2,12);
		public static final short AD_MASK=0x0FFF;
		public static final double VREF=5.0;
		
		
		StringBuffer _dataBuffer;
		BB232SPDA _io_module;
		NumberFormat _numberFormat;
		NumberFormat _intFormat;
		byte[] _stateBytes=null;
		SensorDataPacket _statePacket;
		/** PacketParser instance */
		PacketParser _packetParser=null;
		
		/** Service attributes */
		public CO2SubsystemMonitorAttributes _attributes;
				
		/** Zero-arg constructor	*/
		public CO2SubsystemMonitor() 
		throws RemoteException{
			super();
			if(_log4j.isDebugEnabled()){
			//_log4j.debug("*************** CO2 monitor Constructor");
			}
			_attributes=new CO2SubsystemMonitorAttributes(this);
			_dataBuffer=new StringBuffer();
			_numberFormat=NumberFormat.getInstance();
			_numberFormat.setMaximumFractionDigits(5);
			_numberFormat.setMinimumFractionDigits(3);
			_numberFormat.setMinimumIntegerDigits(1);
			_numberFormat.setGroupingUsed(false);			
			_intFormat=NumberFormat.getInstance();
			_intFormat.setMaximumFractionDigits(0);
			_intFormat.setMinimumIntegerDigits(1);
			_intFormat.setGroupingUsed(false);			
		}
				
		/** Self-test not implemented. */
		public int test() {return -1;}
		
		/** required by PolledInstrumentService */
		protected ScheduleSpecifier createDefaultSampleSchedule(){return null;}
		
		/**required by BaseInstrumentService */
		protected void requestSample() throws Exception {
			if(_log4j.isDebugEnabled()){
			//_log4j.debug("*************** CO2 monitor in faking request sample");
			}
		}
		/**required by BaseInstrumentService */
		//protected  SensorDataPacket acquire(boolean logSample){return null;}
		/**required by BaseInstrumentService */
		protected  PowerPolicy initInstrumentPowerPolicy(){return PowerPolicy.NEVER;}
		/**required by BaseInstrumentService */
		protected  PowerPolicy initCommunicationPowerPolicy(){return PowerPolicy.NEVER;}
		/**required by BaseInstrumentService */
		protected int initMaxSampleBytes() {return 128;}
		/**required by BaseInstrumentService */
		protected byte[] initPromptString() { return "klh".getBytes();}
		/**required by BaseInstrumentService */
		protected byte[] initSampleTerminator() {return "\n".getBytes();}
		/**required by BaseInstrumentService */
		protected int initCurrentLimit() {return 1000;}
		/**required by BaseInstrumentService */
		protected int initInstrumentStartDelay() {return 0;}
		/**required by DeviceService */
		public SerialPortParameters getSerialPortParameters()
		throws UnsupportedCommOperationException
		{
			return new SerialPortParameters(((CO2SubsystemMonitorAttributes)_attributes).serial_baud,
											((CO2SubsystemMonitorAttributes)_attributes).serial_databits,
											((CO2SubsystemMonitorAttributes)_attributes).serial_parity,
											((CO2SubsystemMonitorAttributes)_attributes).serial_stopbits);
		}
		
		/** Register us for data callbacks from the temperature device */
		protected void initializeInstrument() 
		throws InitializeException, Exception{
			if(_log4j.isDebugEnabled()){
			//_log4j.debug("*************** CO2 monitor initializing...");
			}
			SerialInstrumentPort sp = (SerialInstrumentPort)(this._instrumentPort);
			if(_log4j.isDebugEnabled()){
			//_log4j.debug("*************** CO2 monitor initializing IO module");
			}
			_io_module=new BB232SPDA(sp.getSerialPort(),_attributes.serial_mode);
			
			if(_log4j.isDebugEnabled()){
			//_log4j.debug("*************** CO2 monitor initializing cooling fan");
			}
			String fstring=((CO2SubsystemMonitorAttributes)_attributes).fan_command;
			int fanCommand=CO2SubsystemMonitorIF.FAN_CONTROL_ON;
			if(fstring.equalsIgnoreCase("OFF")){
				fanCommand=CO2SubsystemMonitorIF.FAN_CONTROL_OFF;
			}
			setFanControl(fanCommand);
			if(_log4j.isDebugEnabled()){
			//_log4j.debug("*************** CO2 monitor done initializing");
			}
		}
		
		protected double counts2volts(short counts){
			double counts_12bit=(double)(counts&AD_MASK);
			return (double)(counts_12bit*VREF/AD_COUNTS);
		}
		
		protected double[] convertData(short[] rawData){
			if(rawData.length<=0){
				return null;
			}
			double[] convertedData=new double[rawData.length];
			
			convertedData[VOLTAGE_24V]=_attributes.voltage_24v_a*counts2volts(rawData[VOLTAGE_24V]);
			convertedData[CURRENT_24V]=_attributes.current_24v_a*counts2volts(rawData[CURRENT_24V]);
			convertedData[HUMIDITY]=_attributes.humidity_a*counts2volts(rawData[HUMIDITY])+_attributes.humidity_b;
			
			double rawVolts=counts2volts(rawData[TEMPERATURE]);
			convertedData[TEMPERATURE]=_attributes.temperature_a*rawVolts*rawVolts*rawVolts+
			_attributes.temperature_b*rawVolts*rawVolts+
			_attributes.temperature_c*rawVolts+
			_attributes.temperature_d;
			
			convertedData[FAN_STATE]=rawData[FAN_STATE];
			convertedData[WATER_SENSOR_1]=rawData[WATER_SENSOR_1];
			convertedData[WATER_SENSOR_2]=rawData[WATER_SENSOR_2];
			convertedData[FAN_CONTROL]=rawData[FAN_CONTROL];
			
			return convertedData;
		}
		
		protected int readSample(byte[] sample) 
		throws TimeoutException, IOException, Exception{
			if(_log4j.isDebugEnabled()){
			//_log4j.debug("*************** CO2 monitor in read Sample");
			}
			// read all analog and digital channels
			short[] rawData=null;
			synchronized (this) {
				rawData=_io_module.readAll();
			}
			/*
			 if(_log4j.isDebugEnabled()){
			for(int i=0;i<rawData.length;i++){
				_log4j.debug("*************** raw data["+i+"]:"+rawData[i]);
			}
			 }
			 */
			// convert to engineering units
			double[] convertedData=convertData(rawData);
			// format ASCII record
			_dataBuffer.setLength(0);
			_dataBuffer.append(_numberFormat.format(convertedData[VOLTAGE_24V])+",");
			_dataBuffer.append(_numberFormat.format(convertedData[CURRENT_24V])+",");
			_dataBuffer.append(_numberFormat.format(convertedData[HUMIDITY])+",");
			_dataBuffer.append(_numberFormat.format(convertedData[TEMPERATURE])+",");
			_dataBuffer.append(_intFormat.format(convertedData[FAN_STATE])+",");
			_dataBuffer.append(_intFormat.format(convertedData[WATER_SENSOR_1])+",");
			_dataBuffer.append(_intFormat.format(convertedData[WATER_SENSOR_2])+",");
			_dataBuffer.append(_intFormat.format(convertedData[FAN_CONTROL]));

			// make sure the data will fit in the sample buffer
			if(_log4j.isDebugEnabled()){
			//_log4j.debug("CO2 monitor sample: "+_dataBuffer.toString());
			}
			byte[] dbytes=_dataBuffer.toString().getBytes();
			if(dbytes.length>sample.length){
				throw new Exception("Sample buffer smaller than data buffer ["+sample.length+","+dbytes.length+"]");
			}
			
			// copy into sample buffer
			for(int i=0;i<dbytes.length;i++){
				sample[i]=dbytes[i];
			}
			
			// return record length
			return dbytes.length;
		}
		
		/** Return a PacketParser (create one if an instance doesn't exist). */
		public PacketParser getParser() throws NotSupportedException{
			// lazy-create the parser
			if(_packetParser==null){
				_packetParser=new CO2SubsystemMonitorParser(_attributes.registryName,",");
			}
			// return the instance
			return _packetParser;
		}

		/** Parse a SensorDataPacket into a double[] array (used by infrastructure)  */
		public Object parseDataPacket(SensorDataPacket pkt) 
		throws InvalidDataException{
			try{
				PacketParser parser=getParser();
				return parser.parseFields(pkt);
			}catch(NotSupportedException e){
				_log4j.error(e.toString());
			}catch(ParseException p){
				throw new InvalidDataException("ParseException caught: "+p.toString());
			}
			return null;
		}
		
		/////////////////////////////////////////////////////////
		//        CO2SubsystemMonitorIF methods                //
		/////////////////////////////////////////////////////////

		/** Set the CO2 cooling fan control bit. valid state values 
		    are FAN_CONTROL_ON, FAN_CONTROL_OFF
		 */
		public void setFanControl(int state)
		throws Exception{
			boolean bstate=true;
			
			switch (state) {
				case FAN_CONTROL_ON:
					bstate=false;
					break;
				case FAN_CONTROL_OFF:
					bstate=true;
					break;
				default:
					throw new Exception("Invalid state ["+state+"] in setFanControl. Use FAN_CONTROL_OFF and FAN_CONTROL_ON");
			}
			// the logic sense is inverted, so 
			_io_module.writeDigitalOut(bstate);
		}

		/** Read the monitor state, but do not log the sample */		
		public PacketParser.Field[] getMonitorState()
		throws Exception{
			if(_stateBytes==null){
				_stateBytes=new byte[getMaxSampleBytes()];
			}
			int bytesRead=readSample(_stateBytes);
			if(_statePacket==null){
				_statePacket=new SensorDataPacket(getId(),getMaxSampleBytes());
			}
			_statePacket = processSample(_stateBytes,bytesRead);
			PacketParser parser=getParser();
			return parser.parseFields(_statePacket);
		}
		
		/////////////////////////////////////////////////////////
		//        InstrumentDataListener Implementations       //
		/////////////////////////////////////////////////////////
		
		/** dataCallback from the sensors.
		 Fulfills InstrumentDataListener interface
		 */
		public void dataCallback(DevicePacket sensorData, PacketParser.Field[] fields){
			_log4j.warn("dataCallback does nothing");
		}

		
		/** Callback for InstrumentDataListener interface, called when 
		 service is registered with the InstrumentRegistry
		 Fulfills InstrumentDataListener interface
		 */
		public void serviceRegisteredCallback(RegistryEntry entry){
			_log4j.info("serviceRegisteredCallback does nothing");
		}
		
	}