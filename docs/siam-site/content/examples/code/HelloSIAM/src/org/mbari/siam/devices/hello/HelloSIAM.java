package org.mbari.siam.devices.hello;

import java.util.Vector;
import java.util.Iterator;
import java.util.Arrays;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.core.SerialPortParameters;

import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.Summarizer;
import org.mbari.siam.distributed.SummaryPacket;
import org.mbari.siam.distributed.Safeable;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.utils.*;

/**
 * Fake instrument for testing purposes.
 */
public class HelloSIAM 
    extends PolledInstrumentService 
    implements Instrument, Safeable {
		
		/** Log4j logger */
		protected static Logger _log4j = Logger.getLogger(HelloSIAM.class);
		
		/** Command used to request a data sample */
		public static final String CMD_GET_DATA="getData";
		/** Command used to change instrument data increment */
		public static final String CMD_SET_INC="setInc";
		/** Command used to change instrument data increment */
		public static final String CMD_GET_INC="getInc";
		
		/** Extended attributes */
		HelloAttributes _attributes;
		
		/** No-arg constructor */
		public HelloSIAM() throws RemoteException {
			super();
			_attributes = new HelloAttributes(this);
		}
		
		//////////////////////////////
		// Instrument specific methods
		//////////////////////////////
		/** Expose setInc function in Fake-O-Tron API */
		public void setInc(int newInc) 
		throws Exception{
			_log4j.debug("writing setInc");
			writeRead((CMD_SET_INC+" "+newInc));			
		}
		
		/** Expose getInc function in Fake-O-Tron API */
		public int getInc() 
		throws Exception{
			_log4j.debug("writing getInc");
			String inc=writeRead(CMD_GET_INC);
			if(inc!=null){
				return 	Integer.parseInt(inc.trim());
			}	
			throw new Exception("could not get increment [writeRead returned null]");	
		}
		
		/** Utility method to send a Fake-O-Tron command and return the response */
		public String writeRead(String dataToDevice)
		throws Exception{
			sendData(dataToDevice);
			return readData();
		}
		
		/** Send an output line and clean up the echoed characters */
		public void sendData(String data) 
		throws Exception{
			_log4j.debug("writing data ["+data+"]");
			byte[] outBytes=(data+"\n").getBytes();
			_toDevice.write(outBytes);
			_toDevice.flush();
			// Look for response
			int len=StreamUtils.skipUntil(_fromDevice, outBytes, 2000);
			_log4j.debug("skipping echoed command...");
			_log4j.debug("skipped ["+len+"] bytes");
		}
		
		/** Read to sample terminator and skip any bytes up to the prompt */
		public String readData() 
		throws Exception{
			_log4j.debug("reading data");
			byte[] dataIn=new byte[getSampleBuf().length];
			Arrays.fill(dataIn,(byte)0);
			String returnData=null;
			try{
				// read the data
				int len=StreamUtils.readUntil(_fromDevice, dataIn, getSampleTerminator(),
											  _attributes.sampleTimeoutMsec);
				_log4j.debug("read "+len+" bytes");
				// Skip the prompt
				StreamUtils.skipUntil(_fromDevice, getPromptString(), 2000);
				if(len<=0){
					returnData= null;
				}else{
					returnData=new String(dataIn);
				}
			}catch (Exception e) {
				e.printStackTrace();
			}
			return returnData;
		}
		
		//////////////////////////////////
		// Abstract method implementations
		// and overrides
		//////////////////////////////////
		
		/** Specify device startup delay between power on and sample request */
		protected int initInstrumentStartDelay() {
			return 0;
		}
		
		/** Specify prompt string. */
		protected byte[] initPromptString() {
			return "F>".getBytes();
		}
		
		/** Specify sample terminator. */
		protected byte[] initSampleTerminator() {
			return "\n".getBytes();
		}
		
		/** Specify maximum bytes received in raw compass sample. */
		protected int initMaxSampleBytes() {
			return 64;
		}
		
		/** Specify current limit (mA) */
		protected int initCurrentLimit() {
			return 100;
		}
		
		/** Return initial value of instrument power policy. */
		protected PowerPolicy initInstrumentPowerPolicy() {
			return PowerPolicy.NEVER;
		}
		
		/** Return initial value of communication power policy. */
		protected PowerPolicy initCommunicationPowerPolicy() {
			return PowerPolicy.ALWAYS;
		}
		
		/** Request a data sample */
		protected void requestSample() throws Exception {
			System.err.println("HelloSIAM - requestSample()!");
			_log4j.debug("writing request");
			sendData(CMD_GET_DATA);
		}
		
		/** Read a data sample */
		protected int readSample(byte[] sample) throws TimeoutException,
		IOException, Exception{
			System.err.println("HelloSIAM - readSample()");
			_log4j.debug("reading...");
			int len=0;
			
			len=super.readSample(getSampleBuf());
			// Look for response
			StreamUtils.skipUntil(_fromDevice, getPromptString(), 2000);
			return len;
		}
		
		/**
		 * Get instrument 'metadata' from device itself. By default, this method
		 * does nothing, and can be overridden. This method should return any
		 * available configuration information that is available from the
		 * instrument device. Note that this method should NOT power-manage
		 * the device, as that is the responsibility of the caller.
		 */
		protected byte[] getInstrumentStateMetadata()
		throws Exception {
			String metadata=null;
			try{
				// Get the current instrument setting for the data increment value
				int dataInc=getInc();
				metadata="inc:"+dataInc;
			}catch (Exception e) {
				metadata="Error: could not get metadata ["+e.getMessage()+"]";
			}
			return metadata.getBytes();
		}
		/** Initialize the instrument (executed once at service start) */
		protected void initializeInstrument() 
		throws InitializeException, Exception {
			
			// Initialize the instrument increment value 
			_log4j.debug("configuring Fake-O-Tron dataIncrement");
			// set the increment to the value specified by attributes
			setInc(_attributes.dataIncrement);
			_log4j.debug("initializeInstrument() - done");
		}
		
		/** Set instrument internal clock */
		public void setClock() throws NotSupportedException {
			throw new NotSupportedException("Dummy.setClock() not supported");
		}
		
		/** Perform instrument self-test */
		public int test() {
			return Device.OK;
		}
		
		/** Return specifier for default sampling schedule. */
		protected ScheduleSpecifier createDefaultSampleSchedule()
		throws ScheduleParseException {
			// Sample every 30 seconds by default
			return new ScheduleSpecifier(30000);
		}
		
		/** Return serial port configuration parameters. */
		public SerialPortParameters getSerialPortParameters()
		throws UnsupportedCommOperationException {
			
			return new SerialPortParameters(9600, SerialPort.DATABITS_8,
											SerialPort.PARITY_NONE, 
											SerialPort.STOPBITS_1);
		}
		
		/** Enter safe mode, i.e. prepare for immediate power down.
		 This method may be invoked by the power management system,
		 allowing critical instruments to reduce sample rates, configure 
		 internal logging etc. and continue operating for as long as possible 
		 during an unscheduled power outage.
		 */
		public void enterSafeMode(){
			// do nothing
		}
		
		/**
		 * Extend InstrumentServiceAttributes.
		 *
		 */
		protected class HelloAttributes extends InstrumentServiceAttributes {
			// amount to add to data counter for each request
			int dataIncrement = 1;
			
			protected HelloAttributes(DeviceServiceIF service) {
				super(service);
				dataIncrement=1;
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
				if(attributeName.equals("dataIncrement")){
					try{
					_log4j.debug("validating ["+attributeName+"]->["+valueString+"]");
						// try to parse the value as an integer (type should be pre-validated)
						int test=Integer.parseInt(valueString);
						// if a non-zero int, try to set the instrument value
						if(test!=0){
							// if the serial output stream is not null
							// (i.e., the service is initialized)
							// set the value on the device
							if(_toDevice!=null){
								setInc(test);
							}
							// if successful update the attribute value
							dataIncrement=test;
							return;
						}
					}catch (Exception e) {
						e.printStackTrace();
						// if we fail, throw an exception
						throw new InvalidPropertyException("set dataIncrement failed ["+e.getMessage()+"]");
					}
					throw new InvalidPropertyException("Invalid dataIncrement value ["+valueString+"]");
				}
			}
		}
	} // end of class
