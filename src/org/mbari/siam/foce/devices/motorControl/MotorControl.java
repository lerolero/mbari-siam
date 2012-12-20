/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.motorControl;

import java.util.Vector;
import java.util.Iterator;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import org.mbari.siam.registry.InstrumentDataListener;
import org.mbari.siam.registry.InstrumentRegistry;
import org.mbari.siam.registry.RegistryEntry;

import org.mbari.siam.core.BaseInstrumentService;
import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.core.NodeProperties;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.core.InstrumentPort;
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
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.Summarizer;
import org.mbari.siam.distributed.SummaryPacket;
import org.mbari.siam.distributed.Safeable;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.PropertyException;


/**
 * MotorControl implements a PID loop for the FOCE thruster motors, using a set of
 * AllMotion EZServo EZSV23 Motor Servo Boards, with communication via an RS-485
 * serial port.
 */
public class MotorControl extends PolledInstrumentService
    implements Instrument, InstrumentDataListener
{
    /** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(MotorControl.class);

    protected static final int    MAX_SERVOS = 4;
    protected static final int    MAX_MOTOR_RPM = 4000;
    protected static final String INIT_STRING = "s0N1u500m100V0P0L1000R";
    protected static final String EXEC_INIT_CMD = "e0R";
    protected static final String RESET_CMD = "ar5073R";
    protected static final String CANCEL_CMD = "T";
    protected static final String STATUS_CMD = "Q";
    protected static final String VELOCITY_CMD = "V";
    protected static final String GET_POSITION_CMD = "?8";
    protected static final String POS_DIRECTION_CMD = "V0P0R";
    protected static final String NEG_DIRECTION_CMD = "V0D0R";
    protected static final String FIRMWARE_CMD = "&";
    protected static final int    ERR_CLEAR = 3;

    MotorControlAttributes _attributes = new MotorControlAttributes(this);

    protected int[] _lastVelocityCmd = new int[MAX_SERVOS+1];
    protected int[] _lastPosition    = new int[MAX_SERVOS+1];
    protected int[] _errors          = new int[MAX_SERVOS+1];
    protected int[] _totErrors       = new int[MAX_SERVOS+1];
    protected int[] _good            = new int[MAX_SERVOS+1];
    protected boolean[] _stalled     = new boolean[MAX_SERVOS+1];
    protected Velocity	_instantaneousVelocity = new Velocity();
    protected Velocity	_waterVelocity = new Velocity();
    protected Velocity	_waterVelocityAccum = new Velocity();
    protected Velocity	_adcpVelocity = new Velocity();
    protected int	_waterVelocitySamples = 0;

    protected PrintfFormat _format = new PrintfFormat(" %6.2lf");
    protected byte[]	_nullBytes = "".getBytes();
    protected boolean	_debug = false;
    protected byte[]	_sampleTerm = {3};
    protected byte[]	_commBuf = new byte[256];

    public MotorControl() throws RemoteException {
	super();
    }


    /** Specify device startup delay (millisec) */
    protected int initInstrumentStartDelay() {
	return(1000);
    }

    /** Specify prompt string. */
    protected byte[] initPromptString() {
	return(_nullBytes);
    }

    /** Specify sample terminator. */
    protected byte[] initSampleTerminator() {
	return(_sampleTerm);
    }

    /** Specify maximum bytes in raw sample. */
    protected int initMaxSampleBytes() {
	return(128);
    }

    /** Specify current limit. */
    protected int initCurrentLimit() {
	return(5000);
    }

    /** Return initial value of instrument power policy. */
    protected PowerPolicy initInstrumentPowerPolicy() {
	return(PowerPolicy.ALWAYS);
    }

    /** Return initial value of communication power policy. */
    protected PowerPolicy initCommunicationPowerPolicy() {
	return(PowerPolicy.ALWAYS);
    }

    /** Request a data sample */
    protected void requestSample() throws Exception {
    }

    /** Initialize the servos		*/
    protected void initializeInstrument() 
	throws InitializeException, Exception
    {
	super.initializeInstrument();
	_instrumentPort.setCommsMode(CommsMode.RS485);
	_toDevice.setInterByteMsec(5);
	clearErrors();

	StopWatch.delay(200);
	transactAll(INIT_STRING);
	StopWatch.delay(1000);
	transactAll(EXEC_INIT_CMD);
	StopWatch.delay(200);
	_log4j.debug("initializeInstrument() - done");


	InstrumentRegistry reg = InstrumentRegistry.getInstance();

	if (reg.registerDataCallback(this, _attributes.advLookup) == null)
	    _log4j.warn("ADV device not found");

	if (reg.registerDataCallback(new ADCPListener(), _attributes.adcpLookup) == null)
	    _log4j.warn("ADCP device not found");
    }

    /** Callback for InstrumentDataListener interface, called when the ADV
	service is registered with the InstrumentRegistry
    */
    public void serviceRegisteredCallback(RegistryEntry entry)
    {
	_log4j.info("serviceRegisteredCallback for ADV");
    }


    protected void setVelocity(Velocity velocity, PacketParser.Field[] fields)
	throws NoDataException
    {
	PacketParser.Field velField;

	velField = PacketParser.getField(fields, "velocityX");
	velocity.setX(((Number)velField.getValue()).doubleValue());
	velField = PacketParser.getField(fields, "velocityY");
	velocity.setY(((Number)velField.getValue()).doubleValue());
	velField = PacketParser.getField(fields, "velocityZ");
	velocity.setZ(((Number)velField.getValue()).doubleValue());
    }


    /** dataCallback from the ADV device */
    public void dataCallback(DevicePacket sensorData, PacketParser.Field[] fields)
    {
	try {
	    setVelocity(_instantaneousVelocity, fields);
	    _waterVelocityAccum.add(_instantaneousVelocity);
	    _waterVelocitySamples++;
	} catch (NoDataException e) {
	    _log4j.error("ADV dataCallback: missing data from parsed fields - " + e);
	}
    }

    class ADCPListener implements InstrumentDataListener
    {
	/** Callback for InstrumentDataListener interface, called when the ADCP
	    service is registered with the InstrumentRegistry
	*/
	public void serviceRegisteredCallback(RegistryEntry entry)
	{
	    _log4j.info("serviceRegisteredCallback for ADCP");
	}

	/** dataCallback from the ADCP device */
	public void dataCallback(DevicePacket sensorData, PacketParser.Field[] fields)
        {
	    try {
		setVelocity(_adcpVelocity, fields);
	    } catch (NoDataException e) {
		_log4j.error("ADCP dataCallback: missing data from parsed fields - " + e);
	    }
	}
    }


    /** Look for "/0" in _commBuf.  This indicates the start of the response to the command.
     * Return index of next byte after the response, or -1 if not found.
     * Note that response must contain at least one byte after "/0".  This is the status byte.
     */
    protected int findResponse(int nbytes)
    {
	for (int i = 0; i < nbytes-2; i++)
	{
	    if ((_commBuf[i] == '/') && (_commBuf[i+1] == '0'))
		return(i+2);
	}
	return(-1);
    }

    /** Perform one I/O transaction to a servo.  Return number of bytes in response. */
    protected int transact(int servo, String cmd, boolean logIt) 
	throws IOException, IllegalArgumentException
    {
	int nbytes, firstByte;
	String fullCmd = "/" + servo + cmd + "\r";

	if (servo > _attributes.servos)
	    throw new IllegalArgumentException("Bad servo number: " + servo);

	if (logIt)
	    _log4j.debug("Sending " + fullCmd);

	for (int i = 0; i < _attributes.retries; i++)
	{
	    try {
		_fromDevice.flush();
		_toDevice.write(fullCmd.getBytes());
		nbytes = StreamUtils.readUntil(_fromDevice, _commBuf, _sampleTerm,
					       getSampleTimeout());
		if (_debug)
		    printBuffer(_commBuf, nbytes);

		firstByte = findResponse(nbytes);
		if (firstByte >= 0)
		{
		    nbytes -= firstByte;
		    System.arraycopy(_commBuf, firstByte, _commBuf, 0, nbytes);
		    return(nbytes);
		}
	    } catch (Exception e) {
		_log4j.warn("Retrying transact(" + fullCmd.trim() + "): " + e);
	    }
	}

	throw new IOException("No response to " + fullCmd.trim());
    }

    /** Send a command to all servos	*/
    protected boolean transactAll(String cmd)
    {
	boolean ok = true;

	for (int i = 1; i <= _attributes.servos; i++)
	{	
	    try {
		transact(i, cmd, false);
	    } catch (Exception e) {
		_log4j.error("Exception in sending \"" + cmd + "\" to servo " + i + ": " + e);
		ok = false;
	    }
	    StopWatch.delay(200);
	}

	return(ok);
    }

    /** Get motor status		*/
    protected int status(int servo) throws IOException, IllegalArgumentException
    {
	int nbytes = transact(servo, STATUS_CMD, false);

	if (nbytes < 1)
	    throw new IOException("Bad return to status command");

	return((int)_commBuf[0] & 0x6f);
    }

    /** Set Motor direction for one servo
	@param servo - servo number (1-4)
	@param dir - direction, true for positive direction
     */
    protected void setMotorDirection(int servo, boolean dir) 
	throws IOException, IllegalArgumentException
    {
	_log4j.debug("setMotorDirection(" + servo + ", " + dir + ")");
	if ((servo > _attributes.servos) || (servo <= 0))
	    return;

	transact(servo, CANCEL_CMD, true);
	StopWatch.delay(500);
	transact(servo, dir ? POS_DIRECTION_CMD : NEG_DIRECTION_CMD, true);
	_log4j.debug("setMotorDirection done");
    }

    /** Set motor velocity for one servo	*/
    protected void setMotorVelocity(int servo, int velocity)
	throws IOException, IllegalArgumentException
    {
	int nbytes;
	byte status = 0;
	String cmd;
	boolean logIt = false;

	if ((servo > _attributes.servos) || (servo <= 0))
	    return;
	
	for (int i = 0; i < _attributes.retries; i++)
	{
	    if (_lastVelocityCmd[servo] == velocity)
	    {
		if (_stalled[servo])
		    return;
		cmd = STATUS_CMD;
		logIt = false;
	    }
	    else
	    {
		/* Multiplying yields neg result iff one is positive and other is neg */
		/* Also, if stalled and new velocity, clear the stall		*/
		if ((velocity * _lastVelocityCmd[servo] <= 0) || _stalled[servo])
		{
		    setMotorDirection(servo, (velocity > 0));
		    StopWatch.delay(500);
		    _stalled[servo] = false;
		}

		cmd = VELOCITY_CMD + Math.abs(velocity);
		logIt = true;
	    }

	    nbytes = transact(servo, cmd, logIt);

	    if (nbytes > 0)
	    {
		/* Get servo status */
		status = _commBuf[0];
		status &= 0x6f;
	    }

	    /* Should be not ready (bit 5 cleared), and no error or command overflow */
	    if ((status == 0x40) || (status == 0x4f))
	    {
		_lastVelocityCmd[servo] = velocity;

		if (_good[servo]++ > ERR_CLEAR) {
		    _errors[servo] = 0;
		    _stalled[servo] = false;
		}
		return;
	    }

	    /* If any other status, clear the decks and start over		*/
	    _errors[servo]++;
	    _totErrors[servo]++;
	    _good[servo] = 0;

	    if (_errors[servo] >= _attributes.stallTries) {
		_stalled[servo] = true;
		throw new IOException("Exceeded stall retry attempts");
	    }

	    /* setMotorDirection() will clear and start up command again */
	    setMotorDirection(servo, (velocity>0));
	    _lastVelocityCmd[servo] = 0;
	}
	
	_stalled[servo] = true;
	throw new IOException("Could not set motor velocity.  Status = " + status);
    }

    /** Get Motor position for one servo
	@param servo - servo number (1-4)
     */
    protected int getMotorPosition(int servo) 
	throws IOException, IllegalArgumentException, NumberFormatException
    {
	if ((servo > _attributes.servos) || (servo <= 0))
	    throw new IllegalArgumentException("Bad servo number: " + servo);

	int nBytes = transact(servo, GET_POSITION_CMD, false);

	return(Integer.parseInt(new String(_commBuf, 1, nBytes-1)));
    }

    /** Run once through PID loop	*/
    protected void pidLoop()
    {
	int motor1Cmd, motor2Cmd, i;

	if (_waterVelocitySamples > 0)
	{
	    _waterVelocityAccum.div(_waterVelocitySamples);
	    _waterVelocity = _waterVelocityAccum;
	    _waterVelocityAccum = new Velocity();
	    _log4j.debug("Averaged " + _waterVelocitySamples + " water velocity samples.");
	    _waterVelocitySamples = 0;
	}

	switch(_attributes.mode)
	{
	/* Mode: 0=off, 1=constant motor RPM, 2=constant water velocity, 3=track ADCP */
	  case 0:
	      for (i = 1; i <= _attributes.servos; i++) {
		  try {
		      setMotorVelocity(i, 0);
		  } catch (Exception e) {
		      _log4j.warn("Exception turning off motor " + i + ": " + e);
		  }
	      }
	      break;

	   case 1:
	       try {
		   motor1Cmd = (_attributes.motor1RPM * _attributes.encoderMult) / 1000;
		   setMotorVelocity(_attributes.posXservo, motor1Cmd);
	       } catch (Exception e) {
		   _log4j.error("Exception in pid loop: " + e);
	       }
	       try {
		   motor2Cmd = (_attributes.motor2RPM * _attributes.encoderMult) / 1000;
		   setMotorVelocity(_attributes.negXservo, motor2Cmd);
	       } catch (Exception e) {
		   _log4j.error("Exception in pid loop: " + e);
	       }
	       break;

	   case 2:
	   case 3:
	   default:
	       _log4j.debug("Mode " + _attributes.mode + " not implemented");

	}

	for (i = 1; i <= _attributes.servos; i++)
	{
	    try {
		_lastPosition[i] = getMotorPosition(i);
	    } catch (Exception e) {
		_log4j.error("Exception getting motor position: " + e);
	    }
	}
    }

    /** Run the loop, log the results		*/
    protected int readSample(byte[] sample)
    {
	pidLoop();

	byte[] results = new String(_lastVelocityCmd[_attributes.posXservo]*1000/_attributes.encoderMult + " " +
				    _lastVelocityCmd[_attributes.negXservo]*1000/_attributes.encoderMult + " " +
				    _format.sprintf(_waterVelocity.getX()) + " " +
				    _format.sprintf(_waterVelocity.getY()) + " " +
				    _format.sprintf(_waterVelocity.getZ()) + " " +
				    _format.sprintf(_adcpVelocity.getX()) + " " +
				    _format.sprintf(_adcpVelocity.getY()) + " " +
				    _format.sprintf(_adcpVelocity.getZ()) + " " +
				    _lastPosition[_attributes.posXservo] + " " +
				    _lastPosition[_attributes.negXservo]).getBytes();

	System.arraycopy(results, 0, sample, 0, results.length);

	return(results.length);
    }

    protected void clearErrors()
    {
	for (int i = 1; i <= _attributes.servos; i++) {
	    _errors[i] = 0;
	    _totErrors[i] = 0;
	    _good[i] = 0;
	    _stalled[i] = false;
	}
    }

    /** Return metadata. */
    protected byte[] getInstrumentStateMetadata()
    {
	StringBuffer sb = new StringBuffer();
	int	     i, nbytes;
	String	     str;

	sb.append("Mode = ");
	sb.append(_attributes.mode);
	sb.append(", motor1RPM (request) = ");
	sb.append(_attributes.motor1RPM);
	sb.append(", motor2RPM (request) = ");
	sb.append(_attributes.motor2RPM);
	sb.append(", waterVelocity (request) = ");
	sb.append(_attributes.waterVelocity);
	sb.append("\nminSpeed = ");
	sb.append(_attributes.minSpeed);
	sb.append(", maxSpeed = ");
	sb.append(_attributes.maxSpeed);
	sb.append("\nP, I, D = ");
	sb.append(_attributes.Pgain);
	sb.append(", ");
	sb.append(_attributes.Igain);
	sb.append(", ");
	sb.append(_attributes.Dgain);
	sb.append("\nTotal errors by servo:");
	for (i = 1; i <= _attributes.servos; i++) {
	    sb.append(" ");
	    sb.append(_totErrors[i]);
	}
	sb.append("\n");
	for (i = 1; i <= _attributes.servos; i++) {
	    try {
		nbytes = transact(i, FIRMWARE_CMD, true);
		sb.append("Servo " );
		sb.append(i);
		sb.append(": ");
		sb.append(new String(_commBuf, 1, nbytes-1));
		sb.append("\n");
	    } catch (Exception e) {
		_log4j.error("Can't get firmware string for servo " + i);
	    }
	}

	return(sb.toString().getBytes());
    }

    public String shutdownInstrument()
    {
	boolean ok = transactAll(RESET_CMD);
	if (!transactAll(CANCEL_CMD))
	    ok = false;
	return(ok ? "OK" : "ERROR");
//	return(transactAll(CANCEL_CMD) ? "OK" : "ERROR");
    }

    protected void printBuffer(byte[] buf, int nbytes)
    {
	StringBuffer sb = new StringBuffer();

	sb.append("Got ");
	sb.append(nbytes);
	sb.append(" bytes: ");
	for (int j = 0; j < nbytes; j++)
	{
	    sb.append(" ");
	    sb.append(Byte.toString(_commBuf[j]));
	}
	_log4j.debug(sb.toString());
    }

    /** No internal clock. */
    public void setClock() throws NotSupportedException {
	throw new NotSupportedException("Dummy.setClock() not supported");
    }

    /** Self-test not implemented. */
    public int test() {
	return Device.OK;
    }

    /** Return specifier for default sampling schedule. */
    protected ScheduleSpecifier createDefaultSampleSchedule()
	throws ScheduleParseException {
	// Sample every 2 seconds by default
	return new ScheduleSpecifier(2000);
    }

    /** Return parameters to use on serial port. */
    public SerialPortParameters getSerialPortParameters()
	throws UnsupportedCommOperationException
    {
        return new SerialPortParameters(9600, SerialPort.DATABITS_8,
                SerialPort.PARITY_NONE, SerialPort.STOPBITS_1);
    }


    /** Attributes for MotorControl PID
     * @author Bob Herlien
     */
    class MotorControlAttributes extends InstrumentServiceAttributes
    {
	MotorControlAttributes(DeviceServiceIF service) {
	    super(service);
	}

	/** Mode: 0=off, 1=constant motor RPM, 2=constant water velocity, 3=track ADCP */
	protected int mode = 1;

	/** RPM for Motor 1 in mode 1.  Positive for motor 1, negative for motor 2.  Not used otherwise. */
	protected int motor1RPM = 0;

	/** RPM for Motor 2 in mode 1.  Positive for motor 1, negative for motor 2.  Not used otherwise. */
	protected int motor2RPM = 0;

	/** Max motor RPM allowed to be set via motor{1,2}RPM properties */
	protected int maxMotorRPM = MAX_MOTOR_RPM;

	/** Desired water velocity in cm/s when in mode 2.  Can be positive or negative.
	 *  Not used otherwise.
	 */
	protected double waterVelocity = 10.0;

	/** advLookup identifies the instrument from which we get internal water velocity
	 *  information in modes 2 or 3.
	 */
	protected String advLookup = "VELOCITY";

	/** adcpLookup identifies the instrument from which we get external water velocity
	 *  information in mode 3.
	 */
	protected String adcpLookup = "ADCP";

	/** Minimum water speed when tracking ADCP	*/
	protected double minSpeed = 1.0;

	/** Maximum water speed when tracking ADCP	*/
	protected double maxSpeed = 10.0;

	public String registryName = "MotorControl";

	/** EZServo Velocity command to get motor speed of 1000 RPM */
	protected int encoderMult = 2204;

	/** Motor RPM required to get 10 cm/sec water velocity	*/
	protected int motorRPMfor10cm = 810;

	/** Proportional gain for servo loop	*/
	protected int Pgain;

	/** Integral gain for servo loop	*/
	protected int Igain;

	/** Derivative gain for servo loop	*/
	protected int Dgain;

	/** Default sample timeout		*/
	protected long sampleTimeoutMsec = 250;

	/** Number of tries per transaction	*/
	protected int retries = 3;

	/** Number of times to try to clear stall */
	protected int stallTries = 3;

	/** Number of servos			*/
	protected int servos = 2;

	/** Servo number for positive X axis	*/
	protected int posXservo = 1;

	/** Servo number for negative X axis	*/
	protected int negXservo = 2;

	/** Servo number for positive Y axis	*/
	protected int posYservo = 3;

	/** Servo number for negative Y axis	*/
	protected int negYservo = 4;

	/** Set to 1 to clear errors.  Will reset to 0*/
	protected int clearErrors = 0;

        /**
         * Throw InvalidPropertyException if any invalid attribute values found
         */
        public void checkValues() throws InvalidPropertyException
	{
	    if ((minSpeed < 0.0) || (maxSpeed <= 0.0))
                throw new InvalidPropertyException("min, maxSpeed must be > 0");

	    if (servos < 1)
                throw new InvalidPropertyException("Must have at least one servo");

	    if (servos > MAX_SERVOS)
                throw new InvalidPropertyException("Maximum of " + MAX_SERVOS + " servos");

	    if ((motor1RPM > maxMotorRPM) || (motor1RPM < -maxMotorRPM) ||
		(motor2RPM > maxMotorRPM) || (motor2RPM < -maxMotorRPM))
                throw new InvalidPropertyException("Maximum motor RPM is " + MAX_MOTOR_RPM);

	    if (clearErrors > 0) {
		clearErrors();
		clearErrors = 0;
	    }
		
	    sync(0);
	}
    }

} // end of class
