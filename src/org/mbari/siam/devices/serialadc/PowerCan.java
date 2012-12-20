/**
 * Copyright 2004 MBARI. MBARI Proprietary Information. All rights reserved.
 */
package org.mbari.siam.devices.serialadc;

import java.rmi.RemoteException;

import org.mbari.siam.distributed.devices.Power;
import org.mbari.siam.utils.StopWatch;
import org.mbari.siam.utils.StreamUtils;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

public class PowerCan extends SerialAdc implements Power {

    // log4j Logger
    static private Logger _log4j = Logger.getLogger(PowerCan.class);

	/** Default PowerCan high voltage switch retries */
	static final int _HIGH_VOLTAGE_RETRIES = 3;

	/** Default PowerCan backup enable/disable retries */
	static final int _BACKUP_RETRIES = 3;



	/** PowerCan constructor */
	public PowerCan() throws RemoteException {

	}

	/** Enable high voltage power to sub sea nodes. 
	    Fulfills Power interface
	 */
	public void enableHiVoltage() throws Exception {

      	    byte[] ret = new byte[32];
	    managePowerWake();
	    try{

		ret=writeReadCommand(Power.CMD_HIGH_VOLTAGE_ON, ret, getSampleTerminator(), 
				     _HIGH_VOLTAGE_RETRIES, getSampleTimeout());
		//check for state here
		String state = new String(ret).trim();
				
	    }catch (Exception e) {
		
		managePowerSleep();

		// Timed out, or some other problem
		_log4j.error("PowerCan.enableHiVoltage(): "
				   + e.getMessage());
		throw new Exception("PowerCan.enableHiVoltage(): "
				+ "Exception "+e);
	    }
	    catch(Error n){
		managePowerSleep();

		// Timed out, or some other problem
		_log4j.error("PowerCan.enableHiVoltage(): Error "
				   + n.getMessage());
		throw new Exception("PowerCan.enableHiVoltage(): "
				+ "Error "+n);
		
	    }

	    managePowerSleep();
	    return;
	}

	/** Disable high voltage power to sub sea nodes.
	    Fulfills Power interface
	 */
	public void disableHiVoltage() throws Exception {

      	    byte[] ret = new byte[32];
	    managePowerWake();
	    try{

		ret=writeReadCommand(Power.CMD_HIGH_VOLTAGE_OFF, ret, getSampleTerminator(), 
				     _HIGH_VOLTAGE_RETRIES, getSampleTimeout());
		//check for state here
		String state = new String(ret).trim();
				
	    }catch (Exception e) {
		
		managePowerSleep();

		// Timed out, or some other problem
		_log4j.error("PowerCan.disableHiVoltage(): "
				   + e.getMessage());
		throw new Exception("PowerCan.disableHiVoltage(): "
				+ "Exception "+e);
	    }	    
	    catch(Error n){
		managePowerSleep();

		// Timed out, or some other problem
		_log4j.error("PowerCan.disbleHiVoltage(): Error "
				   + n.getMessage());
		throw new Exception("PowerCan.disableHiVoltage(): "
				+ "Error "+n);
		
	    }
	    
	    managePowerSleep();
	    return;

	}


        /** Check state of high voltage switch on sub sea nodes 
	    Fulfills Power interface
	 */
        public boolean isHighVoltageEnabled() throws Exception
        {

            boolean hi_volt_enabled = true;
      	    byte[] ret = new byte[32];
	    managePowerWake();
	    try{

		ret=writeReadCommand(Power.CMD_HIGH_VOLTAGE_STATUS, ret, getSampleTerminator(), 
				     _HIGH_VOLTAGE_RETRIES, getSampleTimeout());
		//check for state here
		String state = new String(ret).trim();
		
		if ( state.trim().toUpperCase().startsWith("OFF") )
		    hi_volt_enabled = false;
		else if ( state.trim().toUpperCase().startsWith("ON"))
		    hi_volt_enabled = true;
		else
		    throw new Exception("invalid state: " + state);
		
	    }catch (Exception e) {
		
		managePowerSleep();

		// Timed out, or some other problem
		_log4j.error("PowerCan.isHighVoltageEnabled(): "
				   + e.getMessage());
		throw new Exception("PowerCan.isHighVoltageEnabled(): "
				+ "Exception "+e);
	    }catch(Error n){
		managePowerSleep();

		// Timed out, or some other problem
		_log4j.error("PowerCan.isHighVoltageEnabled(): Error "
				   + n.getMessage());
		throw new Exception("PowerCan.isHighVoltageEnabled(): "
				+ "Error "+n);
		
	    }
	    
	    managePowerSleep();
	    return hi_volt_enabled;
	}

    /** Query BIN 400V switch status
	Fulfills Power interface
     */
    public byte[] queryBIN400V() throws Exception
    {

	managePowerWake();
	byte[] ret = new byte[64];
	try{
	    ret=writeReadCommand((Power.CMD_BIN_400VSW_STATUS+"\r").getBytes(), ret, getSampleTerminator(), 
				 _HIGH_VOLTAGE_RETRIES, getSampleTimeout());
	}catch (Exception e) {
	    
	    managePowerSleep();
	    
	    // Timed out, or some other problem
	    _log4j.error("PowerCan.switchBIN400V(): "
			       + e.getMessage());
	    throw new Exception("PowerCan.switchBIN400V(): "
				+ "Exception: "+e);
	}
	catch(Error n){
		managePowerSleep();

		// Timed out, or some other problem
		_log4j.error("PowerCan.switchBIN400V(): Error "
				   + n.getMessage());
		throw new Exception("PowerCan.switchBIN400V(): "
				+ "Error "+n);
	}
	
	managePowerSleep();
	return ret;
    }
    
    /** Perform High Voltage power switching operation on BIN 
	Fulfills Power interface
     */
    public byte[] switchBIN400V(int[] switchStates) throws Exception
    {

	String sCommand=Power.CMD_BIN_400VSW_STATUS;

	for(int i=0;i<switchStates.length;i++){
	    switch(switchStates[i]){
	    case Power.BIN_SWITCH_ON:
	    case Power.BIN_SWITCH_ALLON:
		sCommand+=" "+i+Power.CMD_BIN_400VSW_ON;
		break;
	    case Power.BIN_SWITCH_OFF:
	    case Power.BIN_SWITCH_ALLOFF:
		sCommand+=" "+i+Power.CMD_BIN_400VSW_OFF;
		break;
	    case Power.BIN_SWITCH_QUERY:
	    case Power.BIN_SWITCH:
		break;
	    default:
		return(("Unsupported switch state["+i+"]: "+switchStates[i]).getBytes());
	    }	    
	}

	sCommand+="\r";
	byte[] command=sCommand.getBytes();

	managePowerWake();
	byte[] ret = new byte[64];
	try{
	    ret=writeReadCommand(command, ret, getSampleTerminator(), 
				    _HIGH_VOLTAGE_RETRIES, getSampleTimeout());
	}catch (Exception e) {
		
	    managePowerSleep();

	    // Timed out, or some other problem
	    _log4j.error("PowerCan.switchBIN400V(): "
			       + e.getMessage());
	    throw new Exception("PowerCan.switchBIN400V(): "
				+ "Exception: "+e);
	}
	catch(Error n){
		managePowerSleep();

		// Timed out, or some other problem
		_log4j.error("PowerCan.switchBIN400V(): Error "
				   + n.getMessage());
		throw new Exception("PowerCan.switchBIN400V(): "
				+ "Error "+n);
	}

	managePowerSleep();
	return ret;
    }

    /** Enable/Disable battery backup or holdup capacitors on BIN 
	Fulfills Power interface
     */
    public byte[] binBackups(int batteryBackup, int capacitorsBackup) throws Exception
    {
	String command=Power.CMD_BIN_BACKUP_STATUS;

	switch(batteryBackup){
	case Power.BIN_BACKUP_EN:
	case Power.BIN_BACKUP_ALLEN:
	    command+=" B"+Power.CMD_BIN_BACKUP_EN;
	    break;
	case Power.BIN_BACKUP_DI:
	case Power.BIN_BACKUP_ALLDI:
	    command+=" B"+Power.CMD_BIN_BACKUP_DI;
	    break;
	case Power.BIN_BACKUP_QUERY:
	case Power.BIN_BACKUP:
	    break;
	}

	switch(capacitorsBackup){
	case Power.BIN_BACKUP_EN:
	case Power.BIN_BACKUP_ALLEN:
	    command+=" C"+Power.CMD_BIN_BACKUP_EN;
	    break;
	case Power.BIN_BACKUP_DI:
	case Power.BIN_BACKUP_ALLDI:
	    command+=" C"+Power.CMD_BIN_BACKUP_DI;
	    break;
	case Power.BIN_BACKUP_QUERY:
	case Power.BIN_BACKUP:
	    break;
	}

	command+="\r";

	managePowerWake();
	byte[] ret = new byte[64];
	try{
	    ret=writeReadCommand(command.getBytes(), ret, getSampleTerminator(), 
				    _BACKUP_RETRIES, getSampleTimeout());
	}catch (Exception e) {
		
	    managePowerSleep();

	    // Timed out, or some other problem
	    _log4j.error("PowerCan.binBackups(): "
			       + e.getMessage());
	    throw new Exception("PowerCan.binBackups(): "
				+ "Exception: "+e);
	}
	catch(Error n){
		managePowerSleep();

		// Timed out, or some other problem
		_log4j.error("PowerCan.binBackups(): Error "
				   + n.getMessage());
		throw new Exception("PowerCan.binBackups(): "
				+ "Error "+n);
	}

	managePowerSleep();
	return ret;

    }

    /** Write a command (byte[]) and return response 
	using _toDevice and _fromDevice
     */
    private byte[] writeReadCommand(byte[] command, byte[] buffer, 
				    byte[] terminator, int retries, long timeout)
    throws Exception{
	int bytes_read=-1;
	String eb=new String(buffer);
	for (int i = 0; i < retries; i++) {
	    try {
		// flush input
		_fromDevice.flush();
		
		//write command  and flush output
		_toDevice.write(command);
		_toDevice.flush();

		//read back response bytes
		bytes_read = StreamUtils.readUntil(_fromDevice, buffer,
						       terminator, timeout);
		// got bytes? return 'em
		// (readUntil returns number of bytes *less the terminator*
		// here it just finds terminator and returns 0)
		if(bytes_read>=0)
		    return buffer;
		
	    } catch (Exception e) {
		
		// Timed out, or some other problem
		_log4j.error("PowerCan.writeReadCommand(): Exception "
				   + e.getMessage());

	    }
	    catch(Error n){
		
		// Timed out, or some other problem
		_log4j.error("PowerCan.writeReadCommand(): Error "
				   + n.getMessage());
	    }

	    // pause before next try
	    StopWatch.delay(100);
	}
	// exceeded retries, throw exception
	String b=new String(buffer);
	String c=new String(command);
	throw new Exception("PowerCan.writeReadCommand(): "
			    +"exceeded retries ("+retries+")"
			    +"\ncmd={\n"+c+"\n} len="+c.length()
			    +"\ntimeout="+timeout
			    +"\nread "+bytes_read+" bytes"
			    +"\nbuffer={\n"+b+"\n} len="+b.length()
			    +"\non entry"
			    +"\nbuffer={\n"+eb+"\n} len="+eb.length());
    }
}

