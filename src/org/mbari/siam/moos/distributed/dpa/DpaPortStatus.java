/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.moos.distributed.dpa;

import java.io.Serializable;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.moos.deployed.SidearmPowerPort;

/**
Describes the status (temperature, current, voltage, etc) of a DPA port.
*/
public class DpaPortStatus implements Serializable {

    private static Logger _log4j = Logger.getLogger(DpaPortStatus.class);

    private int _portNumber;

    /** Flags to indicate instrument power, comms power, isolation,
     overcurrent status, etc. */
    private int _relayRegister;
    private int _controlRegister;
    private int _interruptRegister;

    private float _temperature;

    private float _voltage;

    private float _currentMA;

    private float _currentLimitMA;

    private boolean _isLeftChannel;

    private byte[] _serviceMnemonic;
    private long _serviceID;

    private byte[] _statusMsg;
    private byte[] _summaryMsg;

    private boolean _hasPuck = false;

    /**
       Constructor.
       @param portNumber
       @param powerPort  Associated SidearmPowerPort
       @param serviceMnemonic  Mnemonic of service (if any) associated with port
       @param serviceID  Unique ID of service (if any) associated with port
       @param hasPuck
    */
    public DpaPortStatus(int portNumber, SidearmPowerPort powerPort, 
			 byte[] serviceMnemonic, long serviceID, 
			 boolean hasPuck) {

	_portNumber = portNumber;
	_temperature = powerPort.getTemperature();
	_voltage = powerPort.getVoltageLevel();
	_currentMA = powerPort.getCurrentLevel();
	//_log4j.debug("spiDebug: DpaPortStatus: init relayReg");
	_relayRegister = powerPort.getRelayReg();
	_controlRegister = powerPort.getChannelControlReg();
	_interruptRegister = powerPort.getInterruptControlReg();
	_isLeftChannel = powerPort.isLeftChannel();

	try {
	    _currentLimitMA = powerPort.getCurrentLimit();
	}
	catch (NotSupportedException e) {
	    _log4j.error(e);
	}
	_serviceMnemonic = serviceMnemonic;
	_serviceID = serviceID;

	//_log4j.debug("spiDebug: DpaPortStatus: init statusMessage");
	_statusMsg = powerPort.getStatusMessage().getBytes();
	//_log4j.debug("spiDebug: DpaPortStatus: init summaryMessage");
	_summaryMsg = powerPort.getStatusSummaryMessage().getBytes();
	_hasPuck = hasPuck;
    }

    /**
       Constructor.
       @param powerPort  Associated SidearmPowerPort
    */
    public DpaPortStatus(int portNumber, SidearmPowerPort powerPort,
			 boolean hasPuck) {
	this(portNumber, powerPort, null, -1, hasPuck);
    }


    /** Return port number. */
    public int getPortNumber() {
	return _portNumber;
    }


    /** Return true if instrument is powered. */
    public boolean instrumentPowerOn() {
	if ((_controlRegister & Dpa.CONTROL_IPOWER_ON) != 0) {
	    return true;
	}
	else {
	    return false;
	}
    }


    public boolean overcurrentTripped() {
	return overCurrentTripped(_interruptRegister, _isLeftChannel);
    }




    /** Returns voltage. */
    public float getVoltage() {
	return _voltage;
    }

    /** Returns current in milliAmps. */
    public float getCurrentMA() {
	return _currentMA;
    }

    /** Returns temperature in degrees Centigrade. */
    public float getTemperature() {
	return _temperature;
    }

    /** Returns current limit in milliAmps. */
    public float getCurrentLimitMA() {
	return _currentLimitMA;
    }

    /** Returns value of relay register. */
    public int getRelayReg() {
	return _relayRegister;
    }

    /** Returns value of control register. */
    public int getControlReg() {
	return _controlRegister;
    }

    /** Returns value of interrupt register. */
    public int getInterruptReg() {
	return _interruptRegister;
    }


    /** Return ISI ID of service (if any) associated with this port. If no
	service, return -1. */
    public long getServiceID() {
	return _serviceID;
    }


    /** Return mnemonic of service (if any) associated with this port. If no
	service, return null. */
    public String getServiceMnemonic() {
	if (_serviceMnemonic != null) {
	    return new String(_serviceMnemonic);
	}
	else
	    return null;
    }

    /** Indicate if port has associated PUCK. */
    public boolean hasPuck() {
	return _hasPuck;
    }

    public String toString() {

	//_log4j.debug("spiDebug: DpaPortStatus: toString");

	int rr = _relayRegister;
	int cr = _controlRegister;
	StringBuffer buffer = new StringBuffer();

	buffer.append("\nport #: " + getPortNumber());

	buffer.append("\nrelayReg: 0x" + 
		      Integer.toHexString(getRelayReg()) + 
		      ", controlReg: 0x" + 
		      Integer.toHexString(getControlReg()) + 
		      ", interruptReg: 0x" + 
		      Integer.toHexString(getInterruptReg()) + 
		      "\n\noverCurrent: " + 
		      overcurrentTripped() +
		      "\nvoltage: " + getVoltage() + 
		      "\ncurrent: " + getCurrentMA() +
		      "\ncurrentLimit: " + 
		      getCurrentLimitMA() +
		      "\nheatsinkTemperature: " + getTemperature());

	buffer.append("\n\nRS485Term Relay: "+(((rr & Dpa.RELAY_485_CON)!=0)?"CON":"ISO")+
		      "\ncommsIso Relay: "+(((rr & Dpa.RELAY_COMM_ISO)!=0)?"ISO":"CON")+
		      "\npowerIso Relay: "+(((rr & Dpa.RELAY_IPOWER_ISO)!=0)?"ISO":"CON")+
		      
		      "\n\nstatus: "+(((cr & Dpa.CONTROL_STATUS_FAULT)!=0)?"OK":"FAULT")+
		      "\ncommTxPower: "+(((cr & Dpa.CONTROL_TXPOWER_HI)!=0)?"HI":"LOW")+
		      "\ncommDuplex: "+(((cr & Dpa.CONTROL_DUP_HALF)!=0)?"HALF":"FULL")+
		      "\ncommMode: "+(((cr & Dpa.CONTROL_MODE_485)!=0)?"485":"232")+
		      "\ncommSlew: "+(((cr & Dpa.CONTROL_SLEW_UNLIMITED)!=0)?"FAST":"SRL")+
		      "\ncommsPower: "+(((cr & Dpa.CONTROL_CPOWER_ON)!=0)?"ON":"OFF") +
                      "\ninstrumentPower: "+(((cr & Dpa.CONTROL_IPOWER_ON))!=0?"ON":"OFF") +
		      "\nhas Puck: " + hasPuck());

	if (_serviceMnemonic == null) {
	    buffer.append("\nservice: none");
	}
	else {
	    buffer.append("\nservice: " + new String(_serviceMnemonic) + 
			  " (ID=" + _serviceID + ")");
	}
	return new String(buffer);
    }



    /** Return true if channel overcurrent is tripped. */
    public static boolean overCurrentTripped(int interruptRegister,
					     boolean isLeftChannel) {
	if ((interruptRegister & Dpa.INTERRUPT_OCE_GLOBAL) != 0) {
	    if (isLeftChannel) {
		if ((interruptRegister & Dpa.INTERRUPT_OCF_CH0) != 0) {
		    return true;
		}
		else
		    return false;
	    }
	    else {
		// This is right-side channel
		if ((interruptRegister & Dpa.INTERRUPT_OCF_CH1) != 0) {
		    return true;
		}
		else
		    return false;
	    }
	}
	else {
	    return false;
	}

    }
}
