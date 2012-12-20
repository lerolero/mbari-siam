/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/** YSI base class
* @author Devin Bonnie
* @version 8/10/2009
*
*This class is meant to be an abstract for the Ysi 6-Series instruments. 
*
*/
//------------------------------------------------------------------
package org.mbari.siam.devices.ysi;

import java.rmi.RemoteException;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import org.apache.log4j.Logger;

import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.TimeoutException;

import org.mbari.siam.core.DeviceService;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.core.StreamingInstrumentService;

import org.mbari.siam.utils.DelimitedStringParser;
import org.mbari.siam.utils.StreamUtils;

import java.text.DecimalFormat;
import java.text.ParseException;

import java.util.ArrayList;
import java.util.HashMap;

//import org.mbari.siam.dataTurbine.Turbinator;

//------------------------------------------------------------------
public class Ysi extends StreamingInstrumentService implements Instrument {

	static private Logger _log4j = Logger.getLogger(Ysi.class);
	
	protected boolean _isSampling = false; //state of the instrument

	protected boolean _setSensors = false;
	
	protected String[] _startSampleCmd = { "menu\r", "1", "1", "1" };

	protected String _defaultSampleInterval = "4\r"; //to be added; manufacturer recommended

	protected String[] _setDiscreetSampleInterval = { "menu\r", "1", "1","2", _defaultSampleInterval }; //to be added

	protected String[] _disableHeader = { "menu\r", "5", "3","0\r" };//to be added

	protected String[] _disableAutoSleep = { "menu\r", "8","2","5" };//to be added

	public Attributes _attributes = new Attributes(this);

	//protected Turbinator _turbinator;

	
	/** Constructor */
	public Ysi() throws RemoteException {

    	}

	/** Initialize Instrument */
	protected void initializeInstrument() throws TimeoutException, InitializeException, Exception {
		
		_log4j.debug("initializeInstrument()");

		try {
			_attributes.checkValues();
			reportSetup();
			_log4j.debug("You have the Conn...");
		}
		catch (Exception e){		
			_log4j.error("Failed to initialize!", e);
		}	
	
		/*try {
			_turbinator = new Turbinator( getParser(), new String( getName() ), "134.89.13.71" );
		}
		catch (Exception e) {

			_log4j.error("Turbinator Initialization Failed!", e);
		}	*/			
	}

	/** Bring instrument to discreet streaming mode and parse to reach sample. */
	protected void startStreaming() throws Exception {
		
		int bytesRead = 0;	

		exitToPrompt();

		_log4j.debug("called startStreaming()");
					
		for (int x=0; x < ( (_startSampleCmd.length) - 1) ; x++) {
			
          		_toDevice.write(_startSampleCmd[x].getBytes());
			_toDevice.flush();
			Thread.sleep(500);

			try { 
				StreamUtils.skipUntil(_fromDevice, ":  \b".getBytes(), 500);
				_fromDevice.flush();
			} catch (Exception e) {

				_log4j.error("Cannot find menu prompt ':' !", e);
				_isSampling = false;
			throw new Exception("startStreaming failed at _startSampleCmd["+x+"]"); 
 			}			
		}
		
		_toDevice.write(_startSampleCmd[ (_startSampleCmd.length) - 1].getBytes());
		_toDevice.flush();
		
		try {
			bytesRead = StreamUtils.skipUntil(_fromDevice, "Stabilizing".getBytes(), 200);

			if(bytesRead != 0) {
				//Stabilizing has a 4 second countdown...
				StreamUtils.skipUntil(_fromDevice, "1\b \b\b \b\b \b\b \b\b \b\b \b\b \b\b \b\b \b\b \b\b \b\b \b\b \b".getBytes(),4000);
			}
			
			_isSampling = true;
			
		} catch (Exception e) {

			_log4j.debug("StartStreaming - Stabilizing not found, instrument already primed.");
		}

		if(bytesRead == 0) {
			//If "Stabilizing" is not found, then a carrige return must be sent to produce a different leading character.
			//If this is omitted then the sample will be truncated as skipUntil will flush the buffer. 

			_toDevice.write("\r".getBytes());
			StreamUtils.skipUntil(_fromDevice, "***\r\n".getBytes(),700);
		}
		Thread.sleep(100);	
	}

	/** Stop Ysi streaming - call method exitToPrompt(). */
	protected void stopStreaming() throws Exception {

		try {		
			_log4j.debug("called stopStreaming()");
			exitToPrompt();			
		} catch (Exception e) {
			_log4j.error("stopStreaming exception!", e);
		}
	}
	
	/** Ysi method to return to the "#" prompt no matter where you are. */
	public void exitToPrompt() throws Exception {
			
		int bytesRead = 0;
		int startBytesRead = 0;
		int menuCount = 0;
			
		_toDevice.write("\r".getBytes());
		_toDevice.flush();
		Thread.sleep(200);
		_toDevice.write("\r".getBytes());
		_toDevice.flush();
		
		try {
			startBytesRead = StreamUtils.skipUntil(_fromDevice, "#".getBytes(), 500);
			_fromDevice.flush();

		} catch (Exception e) {

			_log4j.debug("exitToPrompt() - Not at the prompt, now trying to escape to prompt");

		}

		while(bytesRead == 0 && startBytesRead == 0) {
			
			try {
				_toDevice.write("0".getBytes());
				_toDevice.flush();
				bytesRead = StreamUtils.skipUntil(_fromDevice, "(Y/N)?".getBytes(), 1000);
				_fromDevice.flush();
				menuCount++;
			} catch(Exception e) {

				_log4j.debug("exitToPrompt() - skipUntil() missed Y/N, keep writing escape characters...");
			}

			if(menuCount > 20) {

				_log4j.error("exitToPrompt(): exit to menu error!");
				throw new Exception("exitToPrompt: Cannot escape to prompt!");	
			}
		}

		if(bytesRead != 0) {
			_toDevice.write("y".getBytes());
			_toDevice.flush();
			_log4j.debug("writing 'y' to reach prompt...");
		}

		_isSampling = false;
		_log4j.debug("Leaving exitToPrompt()...");
	}

	/** Determine state of (Ysi 600) Sensors (Enabled / Disabled). This method is called in reportSetup().
	* Example of Sensor Menu:
	*	------------Sensors enabled------------
	*	1-(*)Time           5-(*)Pressure-Abs
	*	2-(*)Temperature    6-( )ISE1 pH
	*	3-(*)Conductivity   7-( )ISE2 Orp
	*	4-( )Dissolved Oxy  8-(*)Battery
	*
	*	Select option (0 for previous menu):
	*
	* Note that the Sensor # does not correspond to the _sensorStatesParsed[#]. See field comment.
	* 	
	* 	ALSO
	/** Compares the Ysi (600) parsed sensor states to the sensor state dependencies based on reported attributes and sets them accordingly.
	*
	* Case List:0=Time,1=Pressure-Abs,2=Temperature,3=ISE1 pH,4=Conductivity,5=ISE2 Orp,6=Dissolved Oxygen,7=Battery
	*
	*@return Boolean array of sensor states set.
	*/
	protected boolean[] getSensorState() throws Exception {
		
		int indexer = 0;
		int stateOffset = 1;
		int numberOffset = 4;
		
		byte[] outbuf = new byte[2000];

		String[] sensorIdentifier = { ")Time", ")Pressure-Abs",")Temperature", ")ISE1 pH", 
			")Conductivity ", ")ISE2 Orp", ")Dissolved Oxy", ")Battery" }; 

		String[] sensorCommand = new String[18];

		/** Sensor states parsed from the Ysi 600 menu. Array positions are as follows:
		*	0=Time, 1=Pressure-Abs, 2=Temperature, 3=ISE1 pH, 4=Conductivity, 5=ISE2 Orp, 6=Dissolved Oxygen, 7=Battery
		*/
		boolean[] sensorStatesParsed = { false, false, false, false, false, false, false, false };
	
		/** Sensor states set by Ysi 600 attributes. Used for checking reporting dependencies. Array positions are as follows:
		*	0=Time, 1=Pressure-Abs, 2=Temperature, 3=ISE1 pH, 4=Conductivity, 5=ISE2 Orp, 6=Dissolved Oxygen, 7=Battery
		*/
		boolean[] sensorStateDependencies = { false, false, false, false, false, false, false, false };

		boolean scase = false;

		//------------------Block to check Reported vs. Sensor Dependencies----------------
		if ( _attributes.dateReport || _attributes.timeReport ) {
			sensorStateDependencies[0] = true;
		}
		if ( _attributes.pressReport || _attributes.depthReport ) {
			sensorStateDependencies[1] = true;
		}
		if ( _attributes.tempReport ) {
			sensorStateDependencies[2] = true;
		}
		if ( _attributes.phReport || _attributes.phMvReport ) {
			sensorStateDependencies[3] = true;
		}
		if ( _attributes.spcondReport || _attributes.condReport || _attributes.resistReport || 
				_attributes.tdsReport || _attributes.salReport ) {
			sensorStateDependencies[4] = true;
		}
		if ( _attributes.orpReport ) {
			sensorStateDependencies[5] = true;
		}
		if ( _attributes.doPercentReport || _attributes.doReport || _attributes.doChrgReport ) {
			sensorStateDependencies[6] = true;
		}
		if ( _attributes.batteryReport ) {
			sensorStateDependencies[7] = true;
		}
		//-----------------------------------------------------------------------------------
		exitToPrompt();
				
		Thread.sleep(500);
		_toDevice.write("menu\r".getBytes());
		_toDevice.flush();
		_toDevice.write("7".getBytes());
		_toDevice.flush();

		StreamUtils.skipUntil(_fromDevice, "-Sensors enabled-".getBytes(), 500);
		StreamUtils.readUntil(_fromDevice, outbuf, "menu):".getBytes(), 500);
		_fromDevice.flush();

		String output = new String (outbuf);
		Character senCmd;

		for (int x=0; x < sensorStatesParsed.length; x++) {
			
			indexer = output.indexOf(sensorIdentifier[x]);	
			senCmd = new Character( (output.charAt(indexer-numberOffset)) ) ;
			sensorCommand[x] = senCmd.toString();			 
			sensorStatesParsed[x] = ( (output.charAt(indexer-stateOffset)) == '*' );
			_log4j.info(sensorIdentifier[x]+" = "+sensorStatesParsed[x]);
			
			scase = writeSensorStates(sensorStateDependencies[x], sensorStatesParsed[x], sensorCommand[x]);

			if(scase){
				_log4j.info("case "+x+" changed '"+sensorIdentifier[x]+"' sensor state.");
			}
		}	 

		_setSensors = true;
		return sensorStateDependencies;
	}
	/**Use in getSensorStates(). This will check the dependency vs. parsed and 'set' the sensor state.*/
	protected boolean writeSensorStates(boolean dep, boolean par, String cmd) throws Exception {
	
		boolean scase = false;

		if ( dep ^ par ) {
			_toDevice.write( cmd.getBytes() );
			_toDevice.flush();
			scase = true;
		}

		return scase;
	}

	/** Parse the Report Setup menu (Ysi 600) and check against sensors enabled && attributes (or assume setSensors has already been called).
	*	Following menu is with all sensors enabled.
	*
	*-------------Report setup--------------
	*1-(*)Date           A-(*)DO mg/L
	*2-(*)Time hh:mm:ss  B-( )DOchrg
	*3-(*)Temp C         C-( )Press
	*4-(*)SpCond mS/cm   D-(*)Depth meters
	*5-( )Cond           E-( )pH
	*6-( )Resist         F-( )pH mV
	*7-( )TDS            G-( )Orp mV
	*8-(*)Sal ppt        H-(*)Battery volts
	*9-(*)DOsat %
	*
	*Select option (0 for previous menu):
	*
	*/
	protected void reportSetup() throws Exception {

		int indexer = 0;
		int stateOffset = 1;
		int numberOffset = 4;

		byte[] outbuf = new byte[3000];

		/** Sensor states set by Ysi 600 attributes. Used for checking reporting dependencies. Array positions are as follows:
		*	0=Time, 1=Pressure-Abs, 2=Temperature, 3=ISE1 pH, 4=Conductivity, 5=ISE2 Orp, 6=Dissolved Oxygen, 7=Battery
		*/
		boolean sensorStates[] = getSensorState();

		if (!_setSensors) {	
			_log4j.error("reportSetup(): setSensorState not called or failed. reportSetup will / has failed!");
		}

		exitToPrompt();

		_toDevice.write("menu\r".getBytes());
		_toDevice.flush();
		_toDevice.write("6".getBytes());
		_toDevice.flush();

		StreamUtils.skipUntil(_fromDevice, "-Report setup-".getBytes(),600);
		StreamUtils.readUntil(_fromDevice, outbuf, "menu):".getBytes(), 600);
		_fromDevice.flush();

		String output = new String (outbuf);
		Character wrOut;
		
		//If statement activates the report state if needed to be set. 
		//Else If statement deactivates the report state if needed.
		//--------------------------------------------------------------------------------------------------------		
		if ( _attributes.dateReport ) {
			indexer = output.indexOf(")Date");
			
			if( !((output.charAt(indexer-stateOffset)) == '*') ) {			
				
				 wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write(wrOut.toString().getBytes() );
				//_toDevice.write( Character.toString(output.charAt(indexer-numberOffset)).getBytes() );
				_toDevice.flush();
			}
		}
		else if( !(_attributes.dateReport) && sensorStates[0] ) {
			indexer = output.indexOf(")Date");
			
			if( ((output.charAt(indexer-stateOffset)) == '*') ) {			
				
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();
			}
		}
		//--------------------------------------------------------------------------------------------------------
		if ( _attributes.timeReport ) {
			indexer = output.indexOf(")Time");
			
			if( !((output.charAt(indexer-stateOffset)) == '*') ) {			
				
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();
			}
		}
		else if( !(_attributes.timeReport) && sensorStates[0] ) {
			indexer = output.indexOf(")Time");
			
			if( ((output.charAt(indexer-stateOffset)) == '*') ) {	
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();
			}
		}
		//--------------------------------------------------------------------------------------------------------	
		//Options of 2-Temp C, 3-Temp F, 4-Temp K
		//Manually set to Temp C
		if (_attributes.tempReport) {
			indexer = output.indexOf(")Temp");
			
			if( !((output.charAt(indexer-stateOffset)) == '*') ) {			
				
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();
				_toDevice.write( "2".getBytes() );
				_toDevice.flush();
			}
		}
		else if( !(_attributes.tempReport) && sensorStates[2] ) {
			indexer = output.indexOf(")Temp");
			
			if( ((output.charAt(indexer-stateOffset)) == '*') ) {			
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();
				_toDevice.write( "1".getBytes() );
				_toDevice.flush();
			}
		}
		//--------------------------------------------------------------------------------------------------------	
		//Options of 2-SpCond mS/cm, 3-SpCond uS/cm
		if(_attributes.spcondReport) {
			indexer = output.indexOf(")SpCond");
			
			if( !((output.charAt(indexer-stateOffset)) == '*') ) {			
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();
				
				if(_attributes.spcondUnitsMicro) {
					_toDevice.write("3".getBytes());
					_toDevice.flush();
				}
				else {
					_toDevice.write("2".getBytes());
					_toDevice.flush();
				}
			}		
		}
		else if( !(_attributes.spcondReport) && sensorStates[4] ) {
			indexer = output.indexOf(")SpCond");
			
			if( ((output.charAt(indexer-stateOffset)) == '*') ) {			
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();
				_toDevice.write( "1".getBytes() );
				_toDevice.flush();
			}
		}
		//--------------------------------------------------------------------------------------------------------	
		//Options of 2-Cond mS/cm, 3-Cond uS/cm
		if(_attributes.condReport) {
			indexer = output.indexOf(")Cond");
			
			if( !((output.charAt(indexer-stateOffset)) == '*') ) {			
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();
				
				if(_attributes.condUnitsMicro) {
					_toDevice.write("3".getBytes());
					_toDevice.flush();
				}
				else {
					_toDevice.write("2".getBytes());
					_toDevice.flush();
				}
			}
		}
		else if( !(_attributes.condReport) && sensorStates[4] ) {
			indexer = output.indexOf(")Cond");
			
			if( ((output.charAt(indexer-stateOffset)) == '*') ) {			
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();
				_toDevice.write( "1".getBytes() );
				_toDevice.flush();
			}
		}
		//--------------------------------------------------------------------------------------------------------	
		//Options of 2-Resist MOhm*cm, 3-Resist KOhm*cm, 4-Resist Ohm*cm
		if(_attributes.resistReport) {
			indexer = output.indexOf(")Resist");
			
			if( !((output.charAt(indexer-stateOffset)) == '*') ) {
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();
				
				if(_attributes.resistUnitsMeg) {
					_toDevice.write("2".getBytes());
					_toDevice.flush();
				}
				else if (_attributes.resistUnitsKil) {
					_toDevice.write("3".getBytes());
					_toDevice.flush();
				}
				else {
					_toDevice.write("4".getBytes());
					_toDevice.flush();
				}
			}
		}
		else if( !(_attributes.resistReport) && sensorStates[4] ) {
			indexer = output.indexOf(")Resist");
			
			if( ((output.charAt(indexer-stateOffset)) == '*') ) {			
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();
				_toDevice.write( "1".getBytes() );
				_toDevice.flush();
			}
		}
		//--------------------------------------------------------------------------------------------------------	
		//Options of 2-TDS g/L, 3-TDS Kg/L
		if(_attributes.tdsReport) { 
			indexer = output.indexOf(")TDS");
			
			if( !((output.charAt(indexer-stateOffset)) == '*') ) {
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();
				
				if(_attributes.tdsUnitsKg) {
					_toDevice.write("3".getBytes());
					_toDevice.flush();
				}
				else {
					_toDevice.write("2".getBytes());
					_toDevice.flush();
				}
			}
		}
		else if( !(_attributes.tdsReport) && sensorStates[4] ) {
			indexer = output.indexOf(")TDS");
			
			if( ((output.charAt(indexer-stateOffset)) == '*') ) {			
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();
				_toDevice.write( "1".getBytes() );
				_toDevice.flush();
			}
		}
		//--------------------------------------------------------------------------------------------------------	
		if(_attributes.salReport) {
			indexer = output.indexOf(")Sal");
			
			if( !((output.charAt(indexer-stateOffset)) == '*') ) {			
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();
			}
		}
		else if( !(_attributes.salReport) && sensorStates[4] ) {
			indexer = output.indexOf(")Sal");
			
			if( ((output.charAt(indexer-stateOffset)) == '*') ) {			
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();

			}
		}
		//--------------------------------------------------------------------------------------------------------	
		if(_attributes.doPercentReport) {
			indexer = output.indexOf(")DOsat %");
			
			if( !((output.charAt(indexer-stateOffset)) == '*') ) {			
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();
			}
		}
		else if( !(_attributes.doPercentReport) && sensorStates[6] ) {
			indexer = output.indexOf(")DOsat %");
			
			if( ((output.charAt(indexer-stateOffset)) == '*') ) {			
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();
			}
		}
		//--------------------------------------------------------------------------------------------------------	
		if(_attributes.doReport) {
			indexer = output.indexOf(")DO mg/L");
			
			if( !((output.charAt(indexer-stateOffset)) == '*') ) {			
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();
			}
		}
		else if( !(_attributes.doReport) && sensorStates[6] ) {
			indexer = output.indexOf(")DO mg/L");
			
			if( ((output.charAt(indexer-stateOffset)) == '*') ) {			
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();
			}
		}
		//--------------------------------------------------------------------------------------------------------	
		if(_attributes.doChrgReport) {
			indexer = output.indexOf(")DOchrg");
			
			if( !((output.charAt(indexer-stateOffset)) == '*') ) {
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();
			}
		}
		else if( !(_attributes.doChrgReport) && sensorStates[6] ) {
			indexer = output.indexOf(")DOchrg");
			
			if( ((output.charAt(indexer-stateOffset)) == '*') ) {			
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();
			}
		}
		//--------------------------------------------------------------------------------------------------------	
		//Options of 2-Press psia, 3-Press psir
		if(_attributes.pressReport) {
			indexer = output.indexOf(")Press");
			
			if( !((output.charAt(indexer-stateOffset)) == '*') ) {			
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();

				if(_attributes.pressUnitsRel) {
					_toDevice.write("3".getBytes());
					_toDevice.flush();
				}
				else {
					_toDevice.write("2".getBytes());
					_toDevice.flush();
				}
			}
		}
		else if( !(_attributes.pressReport) && sensorStates[1] ) {
			indexer = output.indexOf(")Press");
			
			if( ((output.charAt(indexer-stateOffset)) == '*') ) {			
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();
				_toDevice.write( "1".getBytes() );
				_toDevice.flush();
			}
		}
		//--------------------------------------------------------------------------------------------------------	
		//Options of 2-Depth meters, 3-Depth feet
		//Manually set to 2 
		if(_attributes.depthReport) {
			indexer = output.indexOf(")Depth");
			
			if( !((output.charAt(indexer-stateOffset)) == '*') ) {			
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();
				_toDevice.write("2".getBytes());
				_toDevice.flush();
			}
		}
		else if( !(_attributes.depthReport) && sensorStates[1] ) {
			indexer = output.indexOf(")Depth");
			
			if( ((output.charAt(indexer-stateOffset)) == '*') ) {			
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();
				_toDevice.write( "1".getBytes() );
				_toDevice.flush();
			}
		}
		//--------------------------------------------------------------------------------------------------------	
		if(_attributes.phReport) {
			indexer = output.indexOf(")pH");
			
			if( !((output.charAt(indexer-stateOffset)) == '*') ) {			
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();
			}
		}
		else if( !(_attributes.phReport) && sensorStates[3] ) {
			indexer = output.indexOf(")pH");
			
			if( ((output.charAt(indexer-stateOffset)) == '*') ) {			
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();
			}
		}
		//--------------------------------------------------------------------------------------------------------	
		if(_attributes.phMvReport) {
			indexer = output.indexOf(")pH mV");
			
			if( !((output.charAt(indexer-stateOffset)) == '*') ) {			
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();
			}
		}
		else if( !(_attributes.phMvReport) && sensorStates[3] ) {
			indexer = output.indexOf(")pH mV");
			
			if( ((output.charAt(indexer-stateOffset)) == '*') ) {			
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();
			}
		}
		//--------------------------------------------------------------------------------------------------------	
		if(_attributes.orpReport) {
			indexer = output.indexOf(")Orp mV");
			
			if( !((output.charAt(indexer-stateOffset)) == '*') ) {			
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();
			}
		}
		else if( !(_attributes.orpReport) && sensorStates[5] ) {
			indexer = output.indexOf(")Orp mV");
			
			if( ((output.charAt(indexer-stateOffset)) == '*') ) {			
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();
			}
		}
		//--------------------------------------------------------------------------------------------------------	
		if(_attributes.batteryReport) {
			indexer = output.indexOf(")Battery volts");
			
			if( !((output.charAt(indexer-stateOffset)) == '*') ) {			
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();
			}
		}
		else if( !(_attributes.batteryReport) && sensorStates[7] ) {
			indexer = output.indexOf(")Battery volts");
			
			if( ((output.charAt(indexer-stateOffset)) == '*') ) {			
				wrOut = new Character( (output.charAt(indexer-numberOffset)) );
				_toDevice.write( wrOut.toString().getBytes() );
				_toDevice.flush();
			}
		}
		//--------------------------------------------------------------------------------------------------------	

		exitToPrompt();
	}

	/** Write Ysi data to the Turbinator. */
	protected SensorDataPacket processSample(byte[] sample, int nBytes) throws Exception {

		SensorDataPacket packet = super.processSample(sample, nBytes);
	
		PacketParser.Field[] field = getParser().parseFields(packet);	
		/*try {	
			_turbinator.write(packet);	
				
		} catch (Exception e) {
			_log4j.error("Turbinator failed to write packet", e);
		}*/	

		return packet;
	}

	/** Return Ysi streaming state. */
	protected boolean isStreaming() {

		return _isSampling;
	}
	/*Default sample schedule. */
	protected ScheduleSpecifier createDefaultSampleSchedule() throws ScheduleParseException {

		return new ScheduleSpecifier(36000); //30 second schedule
	}

	/** Return maximum number of bytes for instrument sample. */ 
	protected int initMaxSampleBytes() {

		return 512;
	}

	/** Return instrument prompt string. */
	protected byte[] initPromptString() {

		return "# ".getBytes();
	}

	/** Return instrument sample terminator. */
	protected byte[] initSampleTerminator() {

		return "\r\n".getBytes();
	}

	/** Return Instrument current limit. */
	protected int initCurrentLimit() {

		return 500;
	}

	/** Return instrument start delay. */
	protected int initInstrumentStartDelay() {

		return 500;
	}

	/**Set to NEVER for testing purposes. */
	protected PowerPolicy initInstrumentPowerPolicy() {

		return PowerPolicy.NEVER;
	}

	/**Set to NEVER for testing purposes. */
	protected PowerPolicy initCommunicationPowerPolicy() {

		return PowerPolicy.NEVER;
	}

	/**This doesn't really do anything...*/
	public int test() {

		return Device.OK;
	}
	/**Return Ysi Packet Parser.*/
	public PacketParser getParser() {

		return new ysiPacketParser(_attributes);
	}	

	/** Serial Port Params. */
	public SerialPortParameters getSerialPortParameters() throws UnsupportedCommOperationException {
        
        		return new SerialPortParameters(_attributes.baud, 
					SerialPort.DATABITS_8,
					SerialPort.PARITY_NONE,
					SerialPort.STOPBITS_1);
    	}

	/** Service attributes.*/
    	public class Attributes extends StreamingInstrumentService.Attributes {
        
        	public Attributes(StreamingInstrumentService service) {
       			super(service);			
       		}     
  
		int baud = 9600;
		int sampleIntervalSec = 6;

		//attributes for report (Ysi output - specified by the user)
		boolean dateReport = true;		
		boolean timeReport = true;
		boolean tempReport = true; //set report sets units to deg C as default
		boolean spcondReport = false;
			boolean spcondUnitsMicro = false; //if this is not set then the default is mS/cm.
		boolean condReport = false; //usually false
			boolean condUnitsMicro = false; //if this is not set then the default is mS/cm.
		boolean resistReport = false; //usually false
			boolean resistUnitsMeg = false;
			boolean resistUnitsKil = true;
		boolean tdsReport = false; //should always be false
			boolean tdsUnitsKg = false; //if this is not set then the default is g/L.
		boolean salReport = true; //usually false
		boolean doPercentReport = true;
		boolean doReport = true;
		boolean doChrgReport = false; //usually false
		boolean pressReport = false; //usually false
			boolean pressUnitsRel = false; //if this is not set then the default is psia.
		boolean depthReport = true; //set report sets units to meters as default
		boolean phReport = true; //usually false
		boolean phMvReport = false; //usually false
		boolean orpReport = false; //usually false
		boolean batteryReport = false;

		public void checkValues() throws InvalidPropertyException {

			if (_attributes.resistUnitsMeg && resistUnitsKil ) {
				
				throw new InvalidPropertyException("Resistivity Units: MegaOhms*cm and KOhms*cm both set to true. Only 1 or neither can be set 'true'.");
			}
		}
    	}
	
	/** Ysi packet parser */
	public static class ysiPacketParser extends DelimitedStringParser {

		static final String DATE_KEY = "date";
		static final String TIME_KEY = "time";
   		static final String TEMPERATURE_KEY = "temperature";
		static final String SPCONDUCTANCE_KEY = "specific conductance";
		static final String CONDUCTIVITY_KEY = "conductivity";
		static final String RESISTIVITY_KEY = "resistivity";
		static final String TOTALDISSOLVEDSOLIDS_KEY = "total dissolved solids";	
		static final String SALINITY_KEY = "salinity";
		static final String DISSOLVEDOXYGENSATURATION_KEY = "dissolved oxygen saturation";
		static final String DISSOLVEDOXYGEN_KEY = "dissolved oxygen";
		static final String DISSOLVEDOXYGENSENSORCHRG_KEY = "dissolved oxygen sensor charge";
		static final String PRESSURE_KEY = "pressure;";
		static final String DEPTH_KEY = "depth";
		static final String PH_KEY = "ph"; 
		static final String PHMV_KEY = "ph millivolts";
		static final String ORP_KEY = "oxidation reduction potential";
		static final String BATTERY_KEY = "battery";

		//Java1.5 - ArrayList<String> _reportFields = new ArrayList<String>();
		
		static ArrayList _reportFields = new ArrayList();

		//Java1.5 - HashMap<String, String> _reportUnits = new HashMap<String, String>();

		static HashMap _reportUnits = new HashMap();
		
		/**Constructor */
		public ysiPacketParser(Attributes a) {
		
			super(" ");
			parserReportedDependencies(a);
			parserUnits(a);
		} 	

		protected void parserUnits(Attributes _attributes) {

			//Units here will never change.
			_reportUnits.put(DATE_KEY, "mm/dd/yyyy" );
			_reportUnits.put(TIME_KEY, "hh:mm:ss" );
			_reportUnits.put(TEMPERATURE_KEY, "degrees C" );
			_reportUnits.put(SALINITY_KEY, "ppt" );
			_reportUnits.put(DISSOLVEDOXYGENSATURATION_KEY, "%" );
			_reportUnits.put(DISSOLVEDOXYGEN_KEY, "mg/L" );
			_reportUnits.put(DISSOLVEDOXYGENSENSORCHRG_KEY, "chrg" );
			_reportUnits.put(DEPTH_KEY, "meters" ); 
			_reportUnits.put(PH_KEY, "standard units" );
			_reportUnits.put(PHMV_KEY, "mVolts" );
			_reportUnits.put(ORP_KEY, "mVolts" );
			_reportUnits.put(BATTERY_KEY, "Volts" );

			//Units check: one key for one unit.
			//-------------------------------------------
			if ( _attributes.spcondUnitsMicro ) {
				_reportUnits.put(SPCONDUCTANCE_KEY, "uS/cm" );
			}
			else {
				_reportUnits.put(SPCONDUCTANCE_KEY, "mS/cm" );
			} 
			//-------------------------------------------
			if ( _attributes.condUnitsMicro ) {
				_reportUnits.put(CONDUCTIVITY_KEY, "uS/cm" );
			}
			else {
				_reportUnits.put(CONDUCTIVITY_KEY, "mS/cm" );
			}
			//-------------------------------------------
			if ( _attributes.resistUnitsMeg ) {
				_reportUnits.put(RESISTIVITY_KEY, "MOhm*cm" );
			}
			else if ( _attributes.resistUnitsKil ){
				_reportUnits.put(RESISTIVITY_KEY, "KOhm*cm" );
			}
			else {
				_reportUnits.put(RESISTIVITY_KEY, "Ohm*cm" );
			}
			//-------------------------------------------
			if ( _attributes.tdsUnitsKg ) {
				_reportUnits.put(TOTALDISSOLVEDSOLIDS_KEY, "kg/L" );
			}
			else {
				_reportUnits.put(TOTALDISSOLVEDSOLIDS_KEY, "g/L" );
			}
			//-------------------------------------------
			if ( _attributes.pressUnitsRel ) {
				_reportUnits.put(PRESSURE_KEY, "psir" );
			}
			else {
				_reportUnits.put(PRESSURE_KEY, "psia" );
			}
		}

		/** set arraylist for reported order and hashmap<string report enabled, string units enabled>. */
		protected void parserReportedDependencies(Attributes _attributes) {
		
			//--------------------------------------------------------------------
			if ( _attributes.dateReport ) {
				_reportFields.add(DATE_KEY);
			}
			//--------------------------------------------------------------------
			if ( _attributes.timeReport ) {
				_reportFields.add(TIME_KEY);
			}
			//--------------------------------------------------------------------
			if ( _attributes.tempReport ) {
				_reportFields.add(TEMPERATURE_KEY);
			}
			//--------------------------------------------------------------------
			if ( _attributes.spcondReport ) {
				_reportFields.add(SPCONDUCTANCE_KEY);
			}
			//--------------------------------------------------------------------
			if ( _attributes.condReport ) {
				_reportFields.add(CONDUCTIVITY_KEY);
			}
			//--------------------------------------------------------------------
			if ( _attributes.resistReport ) {
				_reportFields.add(RESISTIVITY_KEY);
			}
			//--------------------------------------------------------------------
			if ( _attributes.tdsReport ) {
				_reportFields.add(TOTALDISSOLVEDSOLIDS_KEY);
			}
			//--------------------------------------------------------------------
			if ( _attributes.salReport ) {
				_reportFields.add(SALINITY_KEY);
							}
			//--------------------------------------------------------------------
			if ( _attributes.doPercentReport ) {
				_reportFields.add(DISSOLVEDOXYGENSATURATION_KEY);
			}
			//--------------------------------------------------------------------
			if ( _attributes.doReport ) {
				_reportFields.add(DISSOLVEDOXYGEN_KEY);
			}
			//--------------------------------------------------------------------
			if ( _attributes.doChrgReport ) {
				_reportFields.add(DISSOLVEDOXYGENSENSORCHRG_KEY);
			}
			//--------------------------------------------------------------------
			if ( _attributes.pressReport ) {
				_reportFields.add(PRESSURE_KEY);
			}
			//--------------------------------------------------------------------
			if ( _attributes.depthReport ) {
				_reportFields.add(DEPTH_KEY);
			}
			//--------------------------------------------------------------------
			if ( _attributes.phReport ) {
				_reportFields.add(PH_KEY);
			}
			//--------------------------------------------------------------------
			if ( _attributes.phMvReport ) {
				_reportFields.add(PHMV_KEY);
			}
			//--------------------------------------------------------------------
			if ( _attributes.orpReport ) {
				_reportFields.add(ORP_KEY);
			}
			//--------------------------------------------------------------------
			if ( _attributes.batteryReport ) {
				_reportFields.add(BATTERY_KEY);
			}

			_reportFields.trimToSize();
		} 

		protected PacketParser.Field processToken(int nToken, String token) throws ParseException {
	
			Number value = decimalValue(token);
			String key = _reportFields.get(nToken).toString();
			String units = _reportUnits.get(key).toString();

			//ensures that date and time will not be "turbinated".	
			if ( ((key.equals(DATE_KEY))) || ((key.equals(TIME_KEY))) ) {

				return null;
			}
			else {
				return new Field(key, value, "units="+units);
			}
		}
	}
}
