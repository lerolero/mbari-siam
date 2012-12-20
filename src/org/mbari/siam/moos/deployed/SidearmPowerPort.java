// Copyright MBARI 2003
package org.mbari.siam.moos.deployed;
import java.text.SimpleDateFormat;
import java.text.NumberFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.CommsMode;
import org.mbari.siam.moos.distributed.dpa.Dpa;
import org.mbari.siam.moos.distributed.dpa.DpaPortStatus;
import org.mbari.siam.distributed.PowerPort;
import org.mbari.siam.moos.deployed.DpaBoard;
import org.mbari.siam.distributed.RangeException;


public class SidearmPowerPort implements PowerPort {

    private static Logger _logger = Logger.getLogger(SidearmPowerPort.class);
    private DpaBoard.DpaChannel  _dpaChannel = null;
    private int _currentLimit = 0;
    private String _name = null;
    private static final float DEGS_PER_COUNT = 0.061f;
    private static final float AMPS_PER_COUNT = 0.00305f;
    private static final float VOLTS_PER_COUNT = 0.00366f;

    private SimpleDateFormat _dateFormat = 
	new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");

	private NumberFormat _nf=NumberFormat.getInstance();
	
    private boolean _sampled = false;

    private DiagnosticSample _latest = new DiagnosticSample("latest");
    private DiagnosticSample _minAmps = new DiagnosticSample("minAmp");
    private DiagnosticSample _maxAmps = new DiagnosticSample("maxAmp");
    private DiagnosticSample _minVolts = new DiagnosticSample("minVolt");
    private DiagnosticSample _maxVolts = new DiagnosticSample("maxVolt");
    private DiagnosticSample _minTmprt = new DiagnosticSample("minHtSnk");
    private DiagnosticSample _maxTmprt = new DiagnosticSample("maxHtSnk");

    // Time at which min/max tracking started
    private long _startStatusTime = 0;

    public SidearmPowerPort(String name, DpaBoard.DpaChannel c){
	_name = name;
	_dpaChannel = c;
    }

    public SidearmPowerPort(String name){
	_name = name;
    }

    /** Return name. */
    public String getName() {
	return _name;
    }

    /** Set DpaChannel. */
    public void setDpaChannel(DpaBoard.DpaChannel c) {
	_logger.debug("SidearmPowerPort.initialize()");
	_dpaChannel = c;
    }

    /** Initialize the port. */
    public void initialize() {
	_logger.debug("SidearmPowerPort.initialize()");
	if (_dpaChannel != null) {
	   _logger.debug(getName() + 
				" initializing channel " + _dpaChannel);
	   
	   // Default current limit is set in DpaChannel.initialize
	   // and may be overridden by each instrument's initialize() function
	   // Instrument and comms power are CONNECTED and OFF by
	   // default. Communications ground is ISOLATED by default
	   // (i.e., not tied to power ground) 
	   // Each instrument must control its own instrument 
	   // and comms power.
	   //_dpaChannel.setCurrentLimit(getCurrentLimit());
	   _logger.debug("Isolating Comms Ground");
	   _dpaChannel._relayReg.isolateCommunicationsGround();
	   _logger.debug("Connecting Instrument Power");
	   _dpaChannel._relayReg.connectInstrumentPower();
	   _dpaChannel._relayReg.write();
	   
	}
	else {
	   _logger.debug(getName() + 
				" startup(): DPA channel is NULL");	
	}

	// Start tracking min/max values
	resetStatus();

	_logger.debug("startup(): Done with DPA initialization");
    }

    /** Get current limit on port. */
    public int getCurrentLimit() 
	throws NotSupportedException {
	return _currentLimit;
    }

    /** Get the voltage level of the DpaChannel in volts. */
    public float getVoltageLevel() {
	return (_dpaChannel._channelAdc.getVoltage() * VOLTS_PER_COUNT);
    }

    /** Read relay register */
    public int getRelayReg(){
	return _dpaChannel._relayReg.read()&Dpa.RELAY_MASK;
    }

    /** Read channel control register */
    public int getChannelControlReg(){
	return _dpaChannel._channelCtrlReg.read()&Dpa.CONTROL_MASK;
    }

    /** Read interrupt register */
    public int getInterruptControlReg()
    {
        return _dpaChannel._interruptCtrlReg.read()&Dpa.INTERRUPT_MASK;
    }

    
    /** Read power state (relay=CON|ISO,power=ON|OFF) */
    public void getPowerState(){
	//_logger.debug("spiDebug: SidearmPowerPort: getPowerState");

	int rr = getRelayReg();
	int cr = getChannelControlReg();
        boolean ocf = _dpaChannel._interruptCtrlReg.isOvercurrentFlagSet();

	_logger.debug("Power Relay: "+(((rr & Dpa.RELAY_IPOWER_ISO)!=0)?"ISO":"CON")+
                      "\nInstrument Power: "+(((cr & Dpa.CONTROL_IPOWER_ON))!=0?"ON":"OFF")+
                      "\nOver current: " + ocf);
    }

    /** Read comms state (relay=CON|ISO,power=ON|OFF) */
    public void getCommsState(){
	//_logger.debug("spiDebug: SidearmPowerPort: getCommsState");

	int rr = getRelayReg();
	int cr = getChannelControlReg();
	_logger.debug("Comms Relay: "+(((rr & Dpa.RELAY_COMM_ISO)!=0)?"ISO":"CON")+
			     "\nRS485 Terminators: "+(((rr & Dpa.RELAY_485_CON)!=0)?"CON":"ISO")+
			     "\nStatus: "+(((cr & Dpa.CONTROL_STATUS_FAULT)!=0)?"OK":"FAULT")+
			     "\nSerial Direction: "+(((cr & Dpa.CONTROL_TXPOWER_HI)!=0)?"HI":"LOW")+
			     "\nComm Fast: "+(((cr & Dpa.CONTROL_SLEW_UNLIMITED)!=0)?"UNLIMITED":"SRL")+
			     "\nComm Mode: "+(((cr & Dpa.CONTROL_MODE_485)!=0)?"485":"232")+
			     "\nDuplex: "+(((cr & Dpa.CONTROL_DUP_HALF)!=0)?"HALF":"FULL")+
			     "\nComms Power: "+(((cr & Dpa.CONTROL_CPOWER_ON)!=0)?"ON":"OFF"));
    }

    /** Get the current level of the DpaChannel in amps. */
    public float getCurrentLevel() {
	return (_dpaChannel._channelAdc.getCurrent() * AMPS_PER_COUNT);
    }
    
    /** Set current limit on port. */
    public void setCurrentLimit(int currentLimit) throws RangeException
    {
	if (_dpaChannel != null) 
        {
            _logger.debug("SidearmPowerPort.setCurrentLimit()");
            
            if(currentLimit<0)
                throw new RangeException("current limit must be positive");
            
            _currentLimit=currentLimit;
            _dpaChannel.setCurrentLimit(_currentLimit);
        }
	else 
        {
            _currentLimit = currentLimit;
	    _logger.debug("setCurrentLimit() null _dpaChannel in " + 
                                 getName());
	}
    }

    /** Enable communications */
    public void enableCommunications() {
	_logger.debug("SidearmPowerPort.enableCommunications()");
	commPowerOn();
	commTxHiPower();
    }

    /** Disable communications */
    public void disableCommunications() {
	_logger.debug("SidearmPowerPort.disableCommunications()");
	commPowerOff();
	commTxLoPower();
    }

    /** Connect instrument to power. */
    public void connectPower() {
	_logger.debug("SidearmPowerPort.connectPower()");
	instrumentPowerOn();
    }

    /** Disconnect instrument from power. */
    public void disconnectPower() {
	_logger.debug("SidearmPowerPort.disconnectPower()");
	instrumentPowerOff();
    }

    /** Isolate comms and power from port. */
    public void isolatePort()
    {
	_logger.debug("SidearmPowerPort.isolatePort()");
        isolateCommGround();
        isolateInstrumentPower();
    }

    /** Turn on power to instrument. */
    protected void instrumentPowerOn(){
	if(_dpaChannel != null) {
	   _dpaChannel._channelCtrlReg.setInstrumentPowerOn();
	   _dpaChannel._channelCtrlReg.write();
	}
	else {
	    _logger.debug("instrumentPowerOn() null _dpaChannel in " +
				 getName());
	}
    }

    /** Turn off power to instrument. */
    protected void instrumentPowerOff(){
	if (_dpaChannel != null) {
	   _dpaChannel._channelCtrlReg.setInstrumentPowerOff();	
	   _dpaChannel._channelCtrlReg.write();
	}
	else {
	    _logger.debug("instrumentPowerOff() null _dpaChannel in " +
				 getName());
	}
    }

    /** Turn on communications to instrument. */
    protected void commPowerOn() {
	if (_dpaChannel != null) {
	   _dpaChannel._channelCtrlReg.setCommPowerOn();
	   _dpaChannel._channelCtrlReg.write();
	}
	else {
	    _logger.debug("commPowerOn() null _dpaChannel in " +
				 getName());
	}
    }

    /** Turn off communications to instrument. */
    protected void commPowerOff() {
	if (_dpaChannel != null){
	   _dpaChannel._channelCtrlReg.setCommPowerOff();
	   _dpaChannel._channelCtrlReg.write();
	}
	else {
	  _logger.debug("commPowerOff() null _dpaChannel in " +
			       getName());
	}
    }

    /** Set communications TX to high power */
    protected void commTxHiPower() {
	if (_dpaChannel != null) {
	   _dpaChannel._channelCtrlReg.setTxHiPower();
	   _dpaChannel._channelCtrlReg.write();
	}
	else {
	    _logger.debug("commTxHiPower() null _dpaChannel in " +
				 getName());
	}
    }

    /** Set communications TX to low power */
    protected void commTxLoPower() {
	if (_dpaChannel != null) {
	   _dpaChannel._channelCtrlReg.setTxLoPower();
	   _dpaChannel._channelCtrlReg.write();
	}
	else {
	  _logger.debug("commTxLoPower() null _dpaChannel in " +
			       getName());
	}
    }

    /** Isolate instrument power. */
    protected void isolateInstrumentPower() {
	if (_dpaChannel != null) {
	   _dpaChannel._relayReg.isolateInstrumentPower();
	   _dpaChannel._relayReg.write();
	}
	else {
	  _logger.debug("isolateInstrumentPower() null _dpaChannel in "
			       + getName());
	}
    }

    /** Connect instrument power. */
    protected void connectInstrumentPower() {
	if (_dpaChannel != null) {
	   _dpaChannel._relayReg.connectInstrumentPower();
	   _dpaChannel._relayReg.write();
	}
	else {
	    _logger.debug("connectInstrumentPower() null _dpaChannel in "
				 + getName());
	}
    }

    /** Isolate communications ground. */
    protected void isolateCommGround() {
	if (_dpaChannel != null) {
	   _dpaChannel._relayReg.isolateCommunicationsGround();
	   _dpaChannel._relayReg.write();
	}
	else {
	  _logger.debug("isolateCommGround() null _dpaChannel in " +
			       getName());
	}
    }

    /** Connect communications ground. */
    protected void connectCommGround() {
	if (_dpaChannel != null) {
	   _dpaChannel._relayReg.connectCommunicationsGround();
	   _dpaChannel._relayReg.write();
	}
	else {
	  _logger.debug("connectCommGround() null _dpaChannel in " +
			       getName());
	}
    }

    /** Get heatsink temperature (deg C). */
    public float getTemperature() {

	return (_dpaChannel._parent._boardAdc.getHeatSinkTemp() * 
		DEGS_PER_COUNT - 60);
    }


    /** Get status message. */
    public String getStatusMessage() {

	_sampled = true;
	//_logger.debug("spiDebug: SidearmPowerPort: getStatusMessage");

	_latest.setData(getTemperature(), getVoltageLevel(), 
			getCurrentLevel(), getRelayReg(), 
			getChannelControlReg(), getInterruptControlReg(),
			System.currentTimeMillis());

	_logger.debug(_latest);


	if (_latest.volts > _maxVolts.volts) {
	    _maxVolts.setData(_latest);
	}
	if (_latest.volts < _minVolts.volts) {
	    _minVolts.setData(_latest);
	}

	if (_latest.current < _minAmps.current) {
	    _minAmps.setData(_latest);
	}

	if (_latest.current > _maxAmps.current) {
	    _maxAmps.setData(_latest);
	}

	if (_latest.tmprt > _maxTmprt.tmprt) {
	    _maxTmprt.setData(_latest);
	}

	if (_latest.tmprt < _minTmprt.tmprt) {
	    _minTmprt.setData(_latest);
	}

	return _latest.toString();
    }


    /** Get status summary message. */
    public String getStatusSummaryMessage() {

	String msg = 
	    "DPA_FORMAT:name;htSnk;volts;amps;relayReg;cntrlReg;" + 
            "interruptReg;time\n";
                 
	if (!_sampled) {
	    msg += " no new samples\n";
	    return msg;
	}
		
	msg += _latest + "\n" + 
	    _minTmprt + "\n" + _maxTmprt + "\n" + 
		_minVolts + "\n" + _maxVolts + "\n" + 
	    _minAmps + "\n" + _maxAmps + "\n";

	return msg;
    }

    /** Get a terse status message */
    public String getTerseStatus() {
	if (DpaPortStatus.overCurrentTripped(getInterruptControlReg(), 
				      isLeftChannel())) {
	    return "OVRCURR";
	}
	else {
	    return "OK";
	}
    }


    /** Reset status. */
    public void resetStatus() {
	_sampled = false;

	_minTmprt.resetMin();
	_minVolts.resetMin();
	_minAmps.resetMin();

	_maxTmprt.resetMax();
	_maxVolts.resetMax();
	_maxAmps.resetMax();

	_startStatusTime = System.currentTimeMillis();
    }


    /** If left-side channel, return true. Else return false. */
    public boolean isLeftChannel() {
	if (_dpaChannel instanceof DpaBoard.DpaLeftChannel) {
	    return true;
	}
	else {
	    return false;
	}
    }


    /** DiagnosticSample contains diagnostics information. */
    class DiagnosticSample {
	String name;
	float tmprt;
	float volts;
	float current;
	long timestamp;
	int relayReg;
	int controlReg;
        int _interruptReg;

	DiagnosticSample(String name) {
	    this.name = name;
	}

	void setData(DiagnosticSample sample) {
	    setData(sample.tmprt, sample.volts, sample.current, 
		    sample.relayReg, sample.controlReg, sample._interruptReg,
		    sample.timestamp);
	}

	void setData(float tmprt, float volts, float current, 
		     int relayReg, int controlReg, int interruptReg,
		     long timestamp) {
	    this.tmprt = tmprt;
	    this.volts = volts;
	    this.current = current;
	    this.relayReg = relayReg;
	    this.controlReg = controlReg;
	    this._interruptReg = interruptReg;
	    this.timestamp = timestamp;
	}

	public String toString() {
		_nf.setMinimumIntegerDigits(1);
	    _nf.setMinimumFractionDigits(2);
	    _nf.setMaximumFractionDigits(2);
	    _nf.setGroupingUsed(false);
		
		
	    return name + ";" + 
		new String(_nf.format(tmprt) + ";" +_nf.format(volts) + ";" + _nf.format(current) + ";0x" +
                           Integer.toHexString(relayReg) + ";0x" + Integer.toHexString(controlReg) + ";0x" + 
                           Integer.toHexString(_interruptReg) + "(" + 
			   getTerseStatus() + ");" +
                           _dateFormat.format(new Date(timestamp)));
	}

	void resetMax() {
	    tmprt = volts = current = -Float.MAX_VALUE;
	}

	void resetMin() {
	    tmprt = volts = current = Float.MAX_VALUE;
	}
    }

    /** Set communications mode (RS422,RS485,RS232)
	satisfies InstrumentPort interface
     */
    public void setCommsMode(CommsMode commsMode){
	_logger.debug("setting commsMode via SidearmPowerPort.setCommsMode() to "+commsMode);
	if(_dpaChannel==null){
	    _logger.debug("setCommsMode() null _dpaChannel in "
				 + getName());
	    return;
	}
	if(commsMode==CommsMode.RS232){
	    _dpaChannel._channelCtrlReg.setCommModeRs232();
	    _dpaChannel._channelCtrlReg.setCommFullDuplex();
	    _dpaChannel._channelCtrlReg.write();
	    _dpaChannel._relayReg.disconnect485Terminators();
	    _dpaChannel._relayReg.write();
	    _logger.debug("commsMode set to "+commsMode);
	    return;
	}
	if(commsMode==CommsMode.RS485){
	    _dpaChannel._channelCtrlReg.setCommModeRs485();
	    _dpaChannel._channelCtrlReg.setCommHalfDuplex();
	    _dpaChannel._channelCtrlReg.write();
	    _dpaChannel._relayReg.connect485Terminators();
	    _dpaChannel._relayReg.write();
	    _logger.debug("commsMode set to "+commsMode);
	    return;
	}
	if(commsMode==CommsMode.RS422){
	    _dpaChannel._channelCtrlReg.setCommModeRs485();
	    _dpaChannel._channelCtrlReg.setCommFullDuplex();
	    _dpaChannel._channelCtrlReg.write();
	    _dpaChannel._relayReg.connect485Terminators();
	    _dpaChannel._relayReg.write();
	    _logger.debug("commsMode set to "+commsMode);
	    return;
	}else{
	    _logger.debug("setCommsMode() invalid comms mode: "
			  + commsMode);
	}
	return;	
    }
}
