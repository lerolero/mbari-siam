/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.deployed;

import org.mbari.siam.core.ChannelParameters;
import org.mbari.siam.core.ChannelRange;
import org.mbari.siam.core.NodeProperties;
import org.mbari.siam.core.DevicePort;
import org.mbari.siam.core.AnalogDevicePort;
import org.mbari.siam.core.SerialDevicePort;
import org.mbari.siam.core.NullPowerPort;
import org.mbari.siam.core.DigitalInputDevicePort;

import org.mbari.siam.distributed.CommsMode;
import org.mbari.siam.distributed.PowerPort;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.devices.AnalogBoard;
import org.mbari.siam.distributed.devices.DigitalInputBoard;

import java.io.IOException;
import java.util.Vector;
import org.apache.log4j.Logger;

/**
 FOCENodeProperties contains properties for a FOCE Node. Some of
 these properties are required.
 */
public class FOCENodeProperties extends NodeProperties
	{
		/** log4j logger */
		static Logger _log4j = Logger.getLogger(FOCENodeProperties.class);
		
		public static final String ANALOG_PORT_KEY = "analogPort";
		public static final String ANALOG_NAME_KEY = "analogName";
		public static final String DIN_PORT_KEY = "dInPort";
		public static final String DIN_NAME_KEY = "dInName";
		public static final String POWER_ADDRESS_KEY = "platformPower";
		public static final String ANALOG_ADDRESS_KEY = "platformAnalog";
		public static final String ANALOG_TYPES_KEY = "platformAnalogTypes";
		public static final int    DEFAULT_PWR_BOARD_ADDR = 0x310;
		public static final int    DEFAULT_ANALOG_BOARD_ADDR = 0x300;
		public static final String ANALOG_BOARD_TYPE_DIAMOND = "D";
		public static final String ANALOG_BOARD_TYPE_SENSORAY = "S";
		public static final String DEFAULT_ANALOG_BOARD_TYPE = ANALOG_BOARD_TYPE_DIAMOND;
		
		protected FOCERelayBoard[] _relayBoards;
		protected AnalogBoard[] _analogBoards;
		
		/** Constructor with reference to the FOCENodeConfigurator */
		public FOCENodeProperties()
		{
			super();
		}
		
		/** Look for FOCE-specific notation for PowerPorts.
		    @param key - Property key for the PowerPort.
		 */
	        public PowerPort getPowerPort(String key)
		{
			try {
				int[] powerParms = getIntegerArrayProperty(key);
				
				_log4j.debug(key + " has " + powerParms.length + " values.");
				
				if ((powerParms.length < 2) || (powerParms[0] > _relayBoards.length))
					return(new NullPowerPort());
				else {
					int numBits = (powerParms.length > 2) ? powerParms[2] : 1;
					
					return(new FOCEPowerPort(key, _relayBoards[powerParms[0]],
											 powerParms[1], numBits));
				}
			} catch (Exception e) {
				if (e instanceof InvalidPropertyException)
					_log4j.debug(key + " property invalid: " + e);
			}
			
			return(new NullPowerPort());
		}
		
		
		/** Look for FOCE-specific DevicePorts.  Right now that means AnalogDevicePort.
		 */
		protected DevicePort getPlatformPort(int index, PowerPort powerPort,
											 String jarName, CommsMode cm)
		throws MissingPropertyException
		{
			String portName = ANALOG_PORT_KEY + index;
			
			try {
				String portDescription[] = getStringArrayProperty(portName,null," ");
				int boardNumber = Integer.parseInt(portDescription[0]);
				ChannelRange ranges[] = parseChannelRangeProperty(portDescription[1]);
				/*
				 _log4j.debug("got channel ranges for board "+boardNumber);
				 for(int x=0;x<ranges.length;x++)
				 _log4j.debug(ranges[x]);
				 */
				ChannelParameters portParams=new ChannelParameters(boardNumber,ranges);
				if ((_analogBoards == null) || (_analogBoards.length < boardNumber + 1))
					throw new MissingPropertyException("No platformAnalog board[" +
													   boardNumber + "] declared.");
				
				portName = getProperty(ANALOG_NAME_KEY + index, portName);
				
				_log4j.debug("Added AnalogDevicePort: " + portName);
				return(new AnalogDevicePort(index, portName, _analogBoards[boardNumber],
											powerPort, jarName, null, portParams));
			}
			catch (Exception e) {
			}
			
			try {
				portName = DIN_PORT_KEY + index;
				int[] parms = getIntegerArrayProperty(portName);
				if (parms.length >= 2)
				{
					int boardNumber = parms[0];
					if ((_analogBoards == null) || (_analogBoards.length < boardNumber + 1))
						throw new MissingPropertyException("No platformAnalog board[" +
														   boardNumber + "] declared.");
					if (!(_analogBoards[boardNumber] instanceof DigitalInputBoard))
						throw new MissingPropertyException("AnalogBoard " + boardNumber +
														   " does not have a digital input port");
					
					DigitalInputBoard board = (DigitalInputBoard)(_analogBoards[boardNumber]);
					portName = getProperty(DIN_NAME_KEY + index, portName);
					
					_log4j.debug("Added DigitalInputDevicePort: " + portName);
					return(new DigitalInputDevicePort(index, portName, board, powerPort,
													  jarName, null, parms));
				}
			}
			catch (Exception e) {
				_log4j.debug("Exception looking for " + portName + ": " + e);
			}
			
			// Returning null causes a MissingPropertyException in calling routine.
			return(null);
		}
		
		
		/** Look for platformPower key to change address of power relay board. */
		void getPowerBoards() throws IOException
		{
			int[] powerAddrs;
			
			try {
				powerAddrs = getIntegerArrayProperty(POWER_ADDRESS_KEY);
			} catch (Exception e) {
				_log4j.debug("No platformPower property found. Using default." + e);
				powerAddrs = new int[1];
				powerAddrs[0] = DEFAULT_PWR_BOARD_ADDR;
			}
			
			_relayBoards = new FOCERelayBoard[powerAddrs.length];
			
			_log4j.debug("Creating " + powerAddrs.length + " FOCERelayBoard(s)");
			
			for (int i = 0; i < powerAddrs.length; i++)
				_relayBoards[i] = new FOCERelayBoard(powerAddrs[i]);
		}
		
		/** Look for platformAnalog key to change address of data acquisition board. */
		void getAnalogBoards() throws IOException
		{
			int[] analogAddrs;
			String[] analogTypes;
			
			try {
				analogAddrs = getIntegerArrayProperty(ANALOG_ADDRESS_KEY);
			} catch (Exception e) {
				_log4j.debug("No platformAnalog property found. Using default." + e);
				analogAddrs = new int[1];
				analogAddrs[0] = DEFAULT_ANALOG_BOARD_ADDR;
			}
			
			try {
				String[] types={ANALOG_BOARD_TYPE_DIAMOND,ANALOG_BOARD_TYPE_SENSORAY};
				analogTypes = getStringArrayProperty(ANALOG_TYPES_KEY,types," \t,");
			} catch (Exception e) {
				_log4j.debug("No platformAnalogTypes property found. Using default." + e);
				analogTypes = new String[1];
				analogTypes[0] = DEFAULT_ANALOG_BOARD_TYPE;
			}
			
			_analogBoards = new AnalogBoard[analogAddrs.length];
			
			_log4j.debug("Creating " + analogAddrs.length + " FOCEAnalogBoard(s)");
			
			for (int i = 0; i < analogAddrs.length; i++){
				if(analogTypes[i].equalsIgnoreCase(ANALOG_BOARD_TYPE_DIAMOND)){
					_log4j.debug("Creating FOCEAnalogBoard ["+i+"] at " +
								 Integer.toHexString(analogAddrs[i]) + "...");
					_analogBoards[i] = new FOCEAnalogBoard(analogAddrs[i]);
				}else if(analogTypes[i].trim().equalsIgnoreCase(ANALOG_BOARD_TYPE_SENSORAY)){
					_log4j.debug("Creating FOCESensorayBoard ["+i+"] at " +
								 Integer.toHexString(analogAddrs[i]) + "...");
					_analogBoards[i] = new FOCESensorayBoard(analogAddrs[i]);
				}
				
				//Checking instanceof and Class.isInstance()
				/*
				 _log4j.debug("AnalogBoard " + i + " instanceof AnalogBoard = " +
				 (_analogBoards[i] instanceof AnalogBoard));
				 _log4j.debug("AnalogBoard " + i + " instanceof FOCEAnalogBoard = " +
				 (_analogBoards[i] instanceof FOCEAnalogBoard));
				 _log4j.debug("AnalogBoard " + i + " instanceof FOCESensorayBoard = " +
				 (_analogBoards[i] instanceof FOCESensorayBoard));
				 _log4j.debug("AnalogBoard " + i + " instanceof DigitalInputBoard = " +
				 (_analogBoards[i] instanceof DigitalInputBoard));
				 _log4j.debug("DigitalInputBoard.class.isInstance() = " +
				 DigitalInputBoard.class.isInstance(_analogBoards[i]));
				 */	    
			}
		}
		
	}

