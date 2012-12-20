/**
 * @Title Aanderaa Oxygen Optode Instrument Driver
 * @author Karen A. Salamy
 * @version 1.3
 * @date 9/16/2005
 *
 * Copyright MBARI 2005
 */

/**
 * RS-232C Port Protocol.  The following RS-232C settings should be used:
 * 9600 Baud
 * 8 Data bits
 * 1 Stop bit
 * No Parity
 * Xon/Xoff Handshake
 *
 * Supply Voltage for RS-232C is +5 to +14V dc!
 */


package org.mbari.siam.devices.aanderaa;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.StringTokenizer;

import org.mbari.siam.core.BaseInstrumentService;
import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.utils.StreamUtils;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.SensorDataPacket;


public class AanderaaO2 extends PolledInstrumentService implements Instrument {
    /**
     * log4j Logger
     */
    static private Logger _logger = Logger.getLogger(AanderaaO2.class);
	
    /**
     * Default AanderaaO2 serial baud rate
     */
    static final int _BAUD_RATE = 9600;
	
    /**
     * Default AanderaaO2 serial data bits
     */
    static final int _DATA_BITS = SerialPort.DATABITS_8;
	
    /**
     * Default AanderaaO2 serial stop bits
     */
    static final int _STOP_BITS = SerialPort.STOPBITS_1;
	
    /**
     * Default AanderaaO2 serial parity bits
     */
    static final int _PARITY = SerialPort.PARITY_NONE;
	
    /**
     * !! Default AanderaaO2 Handshake information (??!!!) System states Xon/Xoff Handshake
     * No setting yet established in the InstrumentService.java template yet.
     * Default AanderaaO2 current limit for the optode (milliamps)
     * 5mA quiescent; 32mA sampling !! THIS NEEDS TO BE RESOLVED !!  Should it be 200?
     */
	
    /** Default AanderaaO2 current limit for the optode (milliamps)
     * 5mA quiescent; 32mA sampling !! THIS NEEDS TO BE RESOLVED !!  Should it be 200?
     */
    static final int _INSTRUMENT_CURRENT_LIMIT = 1000;
	
	
    static final byte[] _VALID_RESPONSE = "#\r\n".getBytes();
	
    /**
     * Default AanderaaO2 start delay (ms) - Optode samples right out of the gate and present result.
     */
    static final int _INSTRUMENT_START_DELAY = 1000;
	
    /** After each sample, RS232 input buffer is checked for 100 ms. If buffer contains any characters
     * the timeout is increased to 1 second and the software starts interpreting the RS-232C input.
     * If the interval is empty, the optode will continue to sample and present data accordingly to
     * the setting of the Interval property. If the interval is set to 0, the user can initiate a new
     * sample by use of a Do_Sample command. After ~25 seconds (settling time) without any valid
     * command inputs, the sensor will enter a sleep mode until the next interval starts.  In sleep
     * mode, the sensor will NOT respond to RS-232C input commands.  However, before entering the sleep
     * mode, the optode stops the host's transmission by sending an XOFF handshake-control character.
     * After waking up and finishing the next sample, the host transmission is turned on again.
     * When this handshake method is used the host's output will be buffered until the sensor is ready
     * receive.  This relieves the host from the need to synchronize the communication with the sensors
     * sampling interval.
     */
	
    /** AanderaaO2 COMMAND STRING INFORMATION
     * The command string must be terminated by a Line Feed character (ASCII code 10).
     * Termination with Carriage Return followed by a Line Feed is also allowed (ASCII codes r and n).
     * The command string is not case sensitive (UPPER/lower-case).
     * A valid command string is acknowledged with the character "#" while character "*"
     * indicates an error.  Both are followed by Carriage Return/Line Feed (CRLF). For most
     * errors a short error message is also given subsequent to the error indicator.
     */
	
	
    /**
     * COMMANDS  Command Do_Subcmd(s) - Executes various subcommands.
     */
    /** Command Do_Subcmd(s) - Executes various subcommands. */
    static final String COMMAND_DO_SAMPLE = "Do_Sample";  // Perform a poll request for sample data.
	
    /**
     * Command Get_Property(s) - Outputs a Property Value.
     * Protection of property read/write access.  Run before Set_Output.
     */
    static final String COMMAND_SET_PROTECT = "Set_Protect(1)";
    static final String COMMAND_GET_ALL = "Get_All";
	
    /**
     * Command Set_Property(s) - Sets a Property Value. The values (0) should not have to change.
     */
    static final String COMMAND_SET_INTERVAL_POLL = "Set_Interval(0)";  // "0" Sets system to POLL mode
    static final String COMMAND_SET_OUTPUT_SIMPLE = "Set_OutPut(0)"; // "0" Sets system to simple RS-232C data output mode.
	
    /**
     * Command Save - Save Settings
     */
    static final String COMMAND_SAVE = "Save";  // Store current settings.
	
    private DevicePacketParser devicePacketParser = new DevicePacketParser();
	
	
    /**
     * Configurable AanderaaO2 attributes
     */
    Attributes _attributes = new Attributes(this);
	
	
    public AanderaaO2() throws RemoteException {
    }
	
    /**
     * Sets AanderaaO2 power policy
     */
    protected PowerPolicy initInstrumentPowerPolicy() {
        return PowerPolicy.WHEN_SAMPLING;
    }
	
    /**
     * Sets AanderaaO2 communications power policy
     */
    protected PowerPolicy initCommunicationPowerPolicy() {
        return PowerPolicy.WHEN_SAMPLING;
    }
	
    /**
     * Set AanderaaO2 startup delay time. Set to 0 above.
     */
    protected int initInstrumentStartDelay() {
        //AanderaaO2 software: RS-232C input buffer is checked for 100 ms after each sample.
        return _INSTRUMENT_START_DELAY;
    }
	
    /**
     * Sets AanderaaO2 current limit. Set to 1000 above.
     */
    protected int initCurrentLimit() {
        return _INSTRUMENT_CURRENT_LIMIT;
    }
	
	
    /**
     * Sets the AanderaaO2 sample terminator. The string is terminated by CR (\r) and LF (\n).
     */
    protected byte[] initSampleTerminator() {
        return "\r\n".getBytes();
    }
	
    /**
     * Sets the AanderaaO2 command prompt. There is no command prompt.
     */
    protected byte[] initPromptString() {
        return "".getBytes();
    }
	
    /**
     * Sets the AanderaaO2 maximum number of bytes in a instrument data sample
     */
    protected int initMaxSampleBytes() {
        // 300 byte maximum per sample - in full RS-232C comprehensive output
        return 512;
    }
	
	
    /**
     * Initialize the Instrument
     * This means to set all initial settings - This section for one time only instrument settings
     */
    protected void initializeInstrument() throws InitializeException, Exception {
		
        _logger.debug("initializeInstrument()");
		/*
        try {
            setSampleTimeout(_SAMPLE_TIMEOUT);
        }
        catch (RangeException e) {
            _logger.error(e);
        }
		*/
        // Set the Sample Interval to "0" and save.  This puts system into poll mode.
        _logger.debug(COMMAND_SET_INTERVAL_POLL);
        _toDevice.write(mkCmd(COMMAND_SET_INTERVAL_POLL)); // "0" call already incorporated in command.
        StreamUtils.skipUntil(_fromDevice, _VALID_RESPONSE, getSampleTimeout());
		
        _logger.debug(COMMAND_SAVE);
        _toDevice.write(mkCmd(COMMAND_SAVE));
        StreamUtils.skipUntil(_fromDevice, _VALID_RESPONSE, getSampleTimeout());
		
        _logger.debug(COMMAND_SET_PROTECT);
        _toDevice.write(mkCmd(COMMAND_SET_PROTECT)); // Protection of property. Called to allow R/W access.
        StreamUtils.skipUntil(_fromDevice, _VALID_RESPONSE, getSampleTimeout());
		
        _logger.debug(COMMAND_SET_OUTPUT_SIMPLE);
        _toDevice.write(mkCmd(COMMAND_SET_OUTPUT_SIMPLE)); // "0" call already incorporated in command.
        StreamUtils.skipUntil(_fromDevice, _VALID_RESPONSE, getSampleTimeout());
		
        _logger.debug(COMMAND_SAVE);
        _toDevice.write(mkCmd(COMMAND_SAVE));
        StreamUtils.skipUntil(_fromDevice, _VALID_RESPONSE, getSampleTimeout());
		
		_toDevice.flush();
        _logger.debug("Done with initializeInstrument().");
    }
	
    /**
     * Return parameters to use on serial port.
     */
    public SerialPortParameters getSerialPortParameters()
	throws UnsupportedCommOperationException {
		
        return new SerialPortParameters(_BAUD_RATE, _DATA_BITS, _PARITY,
										_STOP_BITS);
    }
	
	
    protected void prepareToSample() throws Exception {
        _fromDevice.flush();  // Clear first sample.
    }
	
	
    /**
     * Request a sample from the instrument
     */
    protected void requestSample() throws IOException {
		if(_logger.isDebugEnabled()){
			_logger.debug("Sampling AanderaaO2 Oxygen Optode...");
		}
		
        // This method requests one polled sample from instrument.
        try {
            _toDevice.write(mkCmd(COMMAND_DO_SAMPLE));
			_toDevice.flush();
        }
        catch (Exception e) {
            String error = "requestSample() failed: " + e;
            _logger.error(error);
        }
    }
	
	
    /**
     * Get Instrument State Metadata. Currently filled in space until later. TRUE STATE OF DEVICE.
     */
    protected byte[] getInstrumentStateMetadata() {
		if(_logger.isDebugEnabled()){
			_logger.debug("Retrieving AanderaaO2 state Metadata...");
		}
		
		byte[] bytes = new byte[2048];
        try {
            _fromDevice.flush();
            _logger.debug(COMMAND_GET_ALL);
            _toDevice.write(mkCmd(COMMAND_GET_ALL));
            _toDevice.flush();
            int nBytes = StreamUtils.readUntil(_fromDevice, bytes,
											   _VALID_RESPONSE, 
											   getSampleTimeout());
			
			String buf = new String(bytes, 0, nBytes);
			return buf.getBytes();
        }
        catch (Exception e) {
            String error = "getInstrumentStateMetadata() failed: " + e;
            _logger.error(error);
			return "Couldn't get device metadata".getBytes();
        }
		
    }
	
	
    /**
     * setClock() not implemented. Samples are locally timestamped.
     *
     * @param t Set the AanderaaO2's clock.  This does nothing in the AanderaaO2 driver
     */
	
    /** Set the AanderaaO2's clock.  This does nothing in the AanderaaO2 driver */
    public void setClock(long t) {
        return;
    }
	
    /**
     * Self-test routine; This does nothing in the AanderaaO2 driver
     */
    public int test() {
        return Device.OK;
    }
	
    public org.mbari.siam.distributed.DevicePacketParser getDevicePacketParser() {
        return devicePacketParser;
    }
	
	
    /**
     * Return specifier for default sampling schedule.
     */
    protected ScheduleSpecifier createDefaultSampleSchedule()
	throws ScheduleParseException {
        // Sample every 60 seconds by default
        return new ScheduleSpecifier(60000);
    }
	
	
    /**
     * Remove leading control characters from raw data byte array
     */
    protected SensorDataPacket processSample(byte[] sample, int nBytes)
	throws Exception
    {
        String sampleBuf = new String(sample);
        // Skip over leading control characters to actual MEASUREMENT label
        int index = sampleBuf.indexOf("MEASUREMENT");
		
        if (index == -1) {
            // Couldn't find measurement label
            _logger.error("processSample() - couldn't find MEASUREMENT");
        }
        else {
            // Get substring, convert to bytes and copy into sample byte array
            String substring = sampleBuf.substring(index);
            System.arraycopy(substring.getBytes(), 0, sample, 0,
							 substring.length());
            nBytes = nBytes - index;
        }
		
        // Call the framework's processSample()
        return super.processSample(sample, nBytes);
    }
	
	
    /**
     * PRIVATE METHODS *
     * Utility method for sending commands to device.
     * Returns this as a byte array for transmission.
     * Tacks on a Carriage Return at end of String.
     *
     * @param cmd basic command string to construct
     * @return byte array of command
     */
	
    /**
     * Utility method for sending commands to device.
     * Returns this as a byte array for transmission.
     * Tacks on a Carriage Return at end of String.
     *
     * @param cmd
     *            basic command string to construct
     *
     * @return byte array of command
     */
    private byte[] mkCmd(String cmd) {
        return (cmd + "\r\n").getBytes();
    }
	
	
    class Attributes extends InstrumentServiceAttributes {
		
		
        Attributes(BaseInstrumentService service) {
            super(service);
            summaryVars = new String[]{DevicePacketParser.OXYGEN};
        }
    }
	
    class DevicePacketParser extends org.mbari.siam.distributed.DevicePacketParser {
		
        static final String OXYGEN = "Oxygen";
        static final String SATURATION = "Saturation";
        static final String TEMPERATURE = "Temperature";
		String record;
		int count;
		Float ocon=null;
		Float osat=null;
		Float otmp=null;
        /*
         * A record is in the following format (each value is tab separated):
         * MEASUREMENT	  3830	   677	Oxygen: 	   263.58	Saturation: 	   100.33	Temperature: 	    23.96
         */
        protected void parseFields(DevicePacket packet) throws NotSupportedException, Exception {
            //To change body of implemented methods use File | Settings | File Templates.
			/*
			 if (packet instanceof SensorDataPacket) {
			 SensorDataPacket sdPacket = (SensorDataPacket) packet;
			 byte[] dataBuffer = sdPacket.dataBuffer();
			 if (dataBuffer != null) {
			 String msg = new String(dataBuffer);
			 
			 int count = 0;
			 StringTokenizer tok = new StringTokenizer(msg);
			 while (tok.hasMoreTokens()) {
			 String value = tok.nextToken();
			 try {
			 switch (count) {
			 case 4:
			 addMeasurement(OXYGEN, "Oxygen Concentration", "mL/L", new Float(value));
			 break;
			 case 6:
			 addMeasurement(SATURATION, "Oxygen Saturation", "%", new Float(value));
			 break;
			 case 8:
			 addMeasurement(TEMPERATURE, "Temperature of O2 sensor", "Celsius", new Float(value));
			 break;
			 }
			 }
			 catch (Exception e) {
			 if (_logger.isDebugEnabled()) {
			 _logger.debug("Failed to parse datapacket: " + new String(sdPacket.dataBuffer()), e);
			 }
			 }
			 count++;
			 }
			 }
			 }
			 */
			// This parse algorithm is more robust than counting fields. It checks
			// for correct field names and is optimized for a single delimiter.
			
			SensorDataPacket sdPacket = (SensorDataPacket) packet;
			byte[] dataBuffer = sdPacket.dataBuffer();
			String field=null;
			
			if (dataBuffer != null) {
				record = new String(dataBuffer);
				
				// skip the unused tokens
				// and reject if the first field is missing
				count=record.indexOf(OXYGEN);
				
				if(count>=0){
					record=record.substring(count);
				}else{
					if (true) {
						_logger.error("parse error: invalid packet - "+record);
					}
					return;						
				}
				if(_logger.isDebugEnabled()){
					_logger.debug("record: ["+record+"]");
				}
				// process fields
				// may throw exceptions for invalid fields
				try{
					StringTokenizer st=new StringTokenizer(record);
					if(st.countTokens()!=6){
						if (true) {
							_logger.error("parse error: invalid packet - "+record);
						}
						return;										
					}
					
					count=0;
					while(st.hasMoreTokens()){
						field=st.nextToken();
						if(_logger.isDebugEnabled()){
							_logger.debug("field["+count+"]:"+field);
						}
						switch(count++){
							case 0:
								// validate oxygen header
								if(!field.startsWith(OXYGEN)){
									if (true) {
										_logger.error("parse error: missing OXYGEN "+field);
									}
									return;						
								}
								break;
							case 1:
								// get oxygen concentration value
								ocon=Float.valueOf(field);
								break;
							case 2:
								if(!field.startsWith(SATURATION)){
									if (true) {
										_logger.error("parse error: missing SATURATION "+field);
									}
									return;						
								}								
								break;
							case 3:
								osat=Float.valueOf(field);
								break;
							case 4:
								if(!field.startsWith(TEMPERATURE)){
									if (true) {
										_logger.error("parse error: missing TEMPERATURE "+field);
									}
									return;						
								}
								break;
							case 5:
								otmp=Float.valueOf(field);
								break;
						}
						
					}
					
					if(ocon!=null && osat!=null && otmp!=null){
						if(_logger.isDebugEnabled()){
							_logger.debug("ocon="+ocon+" osat="+osat+" otmp="+otmp);
						}
						addMeasurement(OXYGEN, "Oxygen Concentration", "mL/L", ocon);
						addMeasurement(SATURATION, "Oxygen Saturation", "%", osat);
						addMeasurement(TEMPERATURE, "Temperature of O2 sensor", "Celsius", otmp);
					}else{
						_logger.warn("parse error: null measurement oc:"+ocon+" os:"+osat+" ot:"+otmp);
					}
				}catch(Exception e) {
					_logger.error("parse error: invalid field: "+field+"\n"+e);
				}
			}
			return;
        }
    }
} // End of AanderaaO2 class
