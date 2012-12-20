// Copyright 2003 MBARI
package org.mbari.siam.core;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.mbari.siam.operations.utils.PuckCommandSpec;
import org.mbari.siam.utils.StopWatch;
import org.mbari.siam.utils.StreamUtils;
import org.mbari.siam.utils.ByteUtility;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.PowerPort;

import com.Ostermiller.util.MD5;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;


/**
   Implements power control and communications to an 
   instrument via serial port. Assumes a pucked instrument.
   @author Mike Risi
 */
public class PuckSerialInstrumentPort extends SerialInstrumentPort 
{
    private static Logger _log4j = 
	Logger.getLogger(PuckSerialInstrumentPort.class);

    //delays for puck power on and power off times
    private final byte[] _PUCK_TERM_BYTES = "RDY\r".getBytes();
    
    //non-iso puck must be off for at least 5 seconds to let the caps discharge
    private static final int _PUCK_POWER_OFF_DELAY = 5000;
    //measured 12V good to P4.2 HI on the MSP430 at ~11 ms on the non-iso pucks,
    //this is 30 times that amount.  Need to mesure the iso pucks.
    private static final int _PUCK_POWER_ON_DELAY = 300;
    private static final int _PUCK_SENSOR_MODE_DELAY = 100;

    //maximum number of times app will try to get the _PUCK_PROMPT
    private static final int _MAX_PUCK_ATTENTION_RETRIES = 4;
    
    //the pucks initial/discovery serial port settings
    private static final int _DEFAULT_PUCK_BAUD_RATE = 9600;
    private static final int _DEFAULT_PUCK_DATA_BITS = SerialPort.DATABITS_8;
    private static final int _DEFAULT_PUCK_STOP_BITS = SerialPort.STOPBITS_1;
    private static final int _DEFAULT_PUCK_PARITY = SerialPort.PARITY_NONE;
    
    //standard puck command prompt
    private static final byte[] _PUCK_PROMPT = "RDY\r".getBytes();
    
    //states that connectPower uses to determine what action to take
    private static final int _POWER_OFF = 0;
    private static final int _POWER_ON = 1;
    private static final int _POWER_UNKNOWN = 2;
    
    //state variables for tracking power and comms state of a 
    //pucked instrument port
    private int _powerState = _POWER_UNKNOWN;
    private int _commsPowerState = _POWER_UNKNOWN;

    //state variable tracks where the relay is in puck is in puck or sensor mode
    private boolean _sensorMode = true;
    
    //intput and output streams to puck, not instrument
    private InputStream _fromPuck = null;
    private OutputStream _toPuck = null;

    //instrument datasheet and payload infomation
    private InstrumentDatasheet _ids = null;
    private static final byte[] _PAYLOAD_TAG = "< SIAM PAYLOAD >".getBytes();
    private boolean _payloadPresent = false;
    private byte[] _payloadMD5 = new byte[16];
    private int _payloadSize = 0;

    //object used to store initial/discovery serial port settings
    private SerialPortParameters _defaultPuckSerialParams = null;
    
    //object used to store instrument serial port settings
    private SerialPortParameters _serialPortParameters = null;
    
    //StopWatch timer used to guarentee that the puck is powered off for at 
    //least _PUCK_POWER_OFF_DELAY
    private StopWatch _powerOffTimer = null;

    /** create a PuckSerialInstrumentPort  */
    public PuckSerialInstrumentPort(SerialPort serial, String serialName, 
				    PowerPort power)
    {
        super(serial, serialName, power);
        _powerOffTimer = new StopWatch(true);
    }

    /** initialize the InstrumentPort */
    public void initialize() throws InitializeException
    {
        super.initialize();

        try
        {
            _defaultPuckSerialParams = 
                new SerialPortParameters(_DEFAULT_PUCK_BAUD_RATE, 
                                         _DEFAULT_PUCK_DATA_BITS, 
                                         _DEFAULT_PUCK_PARITY, 
                                         _DEFAULT_PUCK_STOP_BITS);
        }
        catch(Exception e)
        {
            throw new InitializeException("SerialPortParams() failure: " + e);
        }

        try
        {
            _toPuck = _serialPort.getOutputStream();
        }
        catch(Exception e)
        {
            throw new InitializeException("getOutputStream() failure: " + e);
        }

        try
        {
            _fromPuck = _serialPort.getInputStream();
        }
        catch(Exception e)
        {
            throw new InitializeException("getInputStream() failure: " + e);
        }

        try
        {
            _serialPortParameters = getSerialPortParams();
        }
        catch(Exception e)
        {
            throw new InitializeException("getSerialPortParams() failure: " + e);
        }
       
        return;
    }

    /** copy the instrument datasheet bytes from the puck and create an 
     * InstrumentDatasheet object */
    private void initInstDatasheet() throws IOException
    {
        if ( _sensorMode )
            throw new IOException("not in puck mode");
        
        PuckInputStream pis = new PuckInputStream(_fromPuck, _toPuck);
        
        byte[] b = new byte[InstrumentDatasheet._SIZE];
        
        for (int i = 0; i <InstrumentDatasheet._SIZE; i++)
            b[i] = (byte)pis.read();
        

        //create the instrument data sheet
        _ids = new InstrumentDatasheet(b);

        //read the payload descriptor bytes
        for (int i = 0; i < 36; i++)
            b[i] = (byte)pis.read();
        
        pis.close();
        pis = null;
        
        // if the bytes don't match you don't have a payload
        for (int i = 0; i <_PAYLOAD_TAG.length; ++i )
            if ( b[i] != _PAYLOAD_TAG[i] )
                return;

        //looks like you gotta payload
        _payloadPresent = true;

        for (int i = 0; i < 16; ++i )
            _payloadMD5[i] = b[i + 16]; 

        try
        {
            _payloadSize = ByteUtility.bytesToInt(b, 32);
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            // should never get here
            _payloadSize = 0;
        }
    }

    /** Connect instrument to power. */
    public void connectPower()
    {
        switch ( _powerState )
        {
            case _POWER_ON: 
                 _log4j.debug("connectPower() case: _POWER_ON");
                break;
            case _POWER_OFF: 
                _log4j.debug("connectPower() case: _POWER_OFF");
                waitForPuckPowerUp();
                _powerPort.connectPower();
                _powerState = _POWER_ON;
                _sensorMode = false;
                
                //switch puck to sensor mode
                try
                {
                    setSensorMode();
                }
                catch(IOException e)
                {
                    _log4j.error("connectPower(): failed to connectPower.");
                }
                break;
            default:
                _log4j.debug("connectPower() case: default");
                cyclePuckPower();                
                
                //switch puck to sensor mode
                try
                {
                    setSensorMode();
                }
                catch(IOException e)
                {
                    _log4j.error("connectPower(): failed to connectPower.");
                }
        }
    }

    /** Disconnect instrument power. */
    public void disconnectPower()
    {
        _log4j.debug("disconnectPower()");
        _powerPort.disconnectPower();
        _powerOffTimer.clear();
        _powerState = _POWER_OFF;
        _sensorMode = true;
    }

    /** set the serial port parameters for the serial port associated with
    this instrument port */
    public void setSerialPortParams(SerialPortParameters params) 
        throws IOException, UnsupportedCommOperationException  
    {
        super.setSerialPortParams(params);
        _serialPortParameters = params;
    }

    /** get the attention of the puck */
    public void getPuckAttention() throws IOException
    {
        if ( _sensorMode )
            throw new IOException("getPuckAttention(): not in puck mode");

        //make sure the puck is fully powered up
        StopWatch.delay(_PUCK_POWER_ON_DELAY);
        
        int i;
        for (i = 0; i < _MAX_PUCK_ATTENTION_RETRIES; ++i)
        {
            try
            {
                //set the read position to zero
                _toPuck.write(new String(PuckCommandSpec.getInstance().setAddressOfMemoryPointerCommand() + " 0\r").getBytes());

                //wait for terminator
                StreamUtils.skipUntil(_fromPuck, _PUCK_PROMPT, 1000);
                break;
            }
            catch(Exception e)
            {
                _log4j.warn("getPuckAttention() failed on try " + 
                             (i + 1) + " :" + e);
            }
        }

        if (i == _MAX_PUCK_ATTENTION_RETRIES)
        {
            _log4j.error("getPuckAttention() failed");
            throw new IOException("getPuckAttention() failed");
        }        
    }

    /** get an input stream to the puck payload */
    public InputStream getPuckInputStream() throws IOException
    {
        if ( _sensorMode )
            throw new IOException("getPuckInputStream(): not in puck mode");

        //initialize the puck instrument datasheet if it has'nt been done
        if ( _ids == null )
            initInstDatasheet();

        
        //only give'm a stream if there is a payload
        if ( !_payloadPresent )
            throw new IOException("PUCK does not contain a SIAM payload");
        
        //create a new puck input stream
        PuckInputStream pis = new PuckInputStream(_fromPuck, _toPuck);
        
        //skip instrument datasheet bytes and SIAM payload description bytes
        pis.skip(_ids.getDsSize() + 36);

        //give'm the InputStream
        return pis;
    }

    /** get the size of the puck payload */
    public InstrumentDatasheet getDatasheet() throws IOException
    {
        if ( _ids == null)
            initInstDatasheet();

        return new InstrumentDatasheet(_ids.getDatasheetBytes());
    }

    
    /** get the size of the puck payload */
    public int getPuckPayloadSize() throws IOException
    {
        if ( _ids == null)
            initInstDatasheet();

        return _payloadSize;
    }


    /** get payload MD5 bytes */
    public byte[] getPuckPayloadMD5() throws IOException
    {
        if ( _ids == null)
            initInstDatasheet();
        
        byte[] md5Bytes = new byte[16];
        
        for (int i = 0; i < 16; i++)
            md5Bytes[i] = _payloadMD5[i];

        return md5Bytes;
    }

    /** set the baud rate of the puck */
    public boolean setPuckBaudRate(int baud) throws IOException
    {
        //form the command to chnage the baud rate on the puck
        String baudSbString = PuckCommandSpec.getInstance().setBaudRateCommand() + " " + baud + "\r";
        String baudVbString = PuckCommandSpec.getInstance().verifyBaudRateCommand() + " " + baud + "\r";
        String respString;
        byte[] resp = new byte[32];
        int bytesRead = 0; 

        //sync with puck
        try
        {
            sendPuckCmd("\r".getBytes(), null, 3, 3000);        
        }
        catch(Exception e)
        {
            throw new IOException("setPuckBaudRate(): failed to sync");
        }
        
        //query PUCK for new baud rate
        try
        {
            bytesRead = sendPuckCmd(baudVbString.getBytes(), resp, 3, 3000);        
        }
        catch(Exception e)
        {
            throw new IOException("setPuckBaudRate(): failed query PUCK baud");
        }

        if ( (bytesRead > 1) && (bytesRead < resp.length) )
            respString = new String(resp, 0, bytesRead);
        else
            throw new IOException("setPuckBaudRate(): unable to verify baud");
        
        //if the baud rate is not supported return false
        if ( respString.trim().equalsIgnoreCase("NO") )
            return false;

        //change the pucks baudrate
        _toPuck.write(baudSbString.getBytes());

        //wait for puck to finish RDY xmit at new baud rate
        StopWatch.delay(250);

        //change local baud
        try
        {
            _serialPort.setSerialPortParams(baud, 
                                            _serialPort.getDataBits(), 
                                            _serialPort.getStopBits(), 
                                            _serialPort.getParity());
        }
        catch (UnsupportedCommOperationException e)
        {
            throw new IOException("setPuckBaudRate(): " + 
                                  "UnsupportedCommOperationException: " + e);
        }
        
        //get the RDY or garbage chars
        _fromPuck.skip(_fromPuck.available());

        //sync with puck
        try
        {
            sendPuckCmd("\r".getBytes(), null, 3, 3000);        
        }
        catch(Exception e)
        {
            throw new IOException("setPuckBaudRate(): failed to sync");
        }

        return true;
    }

    /** put the port into puck mode */
    public void setPuckMode() throws IOException
    {
        try
        {
            //get the current serial port setup
            _serialPortParameters = getSerialPortParams();
        }
        catch(UnsupportedCommOperationException e)
        {
            throw new IOException("setPuckMode(): failed " + 
                                  "to get serial port params");
        }

        //set the baud rate to the pucks default
        try
        {
            if ( !serialParamsEqual(_defaultPuckSerialParams, 
                                    _serialPortParameters) )
            {
                //set the default puck baud rate
                _serialPort.setSerialPortParams(_DEFAULT_PUCK_BAUD_RATE, 
                                                _DEFAULT_PUCK_DATA_BITS, 
                                                _DEFAULT_PUCK_STOP_BITS, 
                                                _DEFAULT_PUCK_PARITY);
            }
        }
        catch(UnsupportedCommOperationException e)
        {
            throw new IOException("setPuckMode(): failed to " + 
                                  "set default puck serial port params");
        }
        
                    
        //if not powered connect power and return
        if ( _powerState == _POWER_OFF )
        {
            //make sure the minimum power off time has elapsed
            waitForPuckPowerUp();
            _powerPort.connectPower();
            _powerState = _POWER_ON;
            _sensorMode = false;
            return;
        }
        
        //power cycle the puck and enable the communications
        cyclePuckPower();
        _powerPort.enableCommunications();
    }
    
    /** put the port into sensor mode */
    public void setSensorMode() throws IOException
    {
        setSensorMode(false);
    }
    
    /** put the port into sensor mode.  If the force parameter is set to
     true it will attempt to put puck into sensor mode no matter
     what state the PuckSerialInstrumentPort thinks the puck is in. */
    public void setSensorMode(boolean force) throws IOException
    {
        if ( _sensorMode && !force )
            return;

        //comms need to be on no matter what policy
        _powerPort.enableCommunications();
        
        //switch the baud rate to the pucks default        
        try
        {
            if ( !serialParamsEqual(_defaultPuckSerialParams, 
                                    _serialPortParameters) )
            {
                _log4j.debug("setSensorMode() switching to pucks default " +
                              "baud rate");
                
                //set the default puck baud rate
                _serialPort.setSerialPortParams(_DEFAULT_PUCK_BAUD_RATE, 
                                                _DEFAULT_PUCK_DATA_BITS, 
                                                _DEFAULT_PUCK_STOP_BITS, 
                                                _DEFAULT_PUCK_PARITY);
            }
        }
        catch(UnsupportedCommOperationException e)
        {
            throw new IOException("setSensorMode(): failed to " + 
                                  "set default puck serial port params");
        }
        
        //wait for puck power & relay to settle
        StopWatch.delay(_PUCK_POWER_ON_DELAY);
        
        //sync serial stream with a <CR>
        _toPuck.write("\r".getBytes());
        StopWatch.delay(50);
        
        //send the sensor mode command
        _toPuck.write(new String(PuckCommandSpec.getInstance().putPuckInInstrumentModeCommand()+ "\r").getBytes());
        _toPuck.flush();

        //wait for the PUCK_IM command to execute and the relay to settle
        StopWatch.delay(_PUCK_SENSOR_MODE_DELAY);

        //switch back to instrument baud rate
        try
        {
            _log4j.debug("setSensorMode() switching to instruments baud rate");
            setSerialPortParams(_serialPortParameters);
        }
        catch(UnsupportedCommOperationException e)
        {
            throw new IOException("setSensorMode() failed to set instrument " + 
                                  "serial port params");
        }
    }
    
    /** if the instrument port is talking to the instrument or the puck */
    public boolean isSensorModeSet()
    {
        return _sensorMode; 
    }

    /** returns true is a SIAM payload was found on the PUCK */
    public boolean isPayloadAvailable()
    {
        return _payloadPresent; 
    }

    
    /** send PUCK command and process response */
    private int sendPuckCmd(byte[] cmd, byte[] resp, int retries, int timeout)
        throws IOException
    {
        int i;

        for (i = 0; i < retries; ++i)
        {
            try
            {
                _toPuck.write(cmd);
                
                if ( resp == null)
                {
                    return StreamUtils.skipUntil(_fromPuck, 
                                                 _PUCK_TERM_BYTES, 
                                                 timeout);
                }
                else
                {
                    return StreamUtils.readUntil(_fromPuck, 
                                                 resp, 
                                                 _PUCK_TERM_BYTES, 
                                                 timeout);
                }
                
            }
            catch(Exception e)
            {
                _log4j.error("sendPuckCmd(...) failed on try " + 
                                   (i + 1) + " :" + e);
            }
        }

        if (i == retries)
            throw new IOException("sendPuckCmd(...) failed");

        //you should never get here
        return -1;
    }

    /** cycles the the power to the pucked instrument port and observes the
    _PUCK_POWER_OFF_DELAY time */
    private void cyclePuckPower()
    {
        //power cycle outside of connectPower/disconnectPower and set state 
        //appropiately
        _powerPort.disconnectPower();
        StopWatch.delay(_PUCK_POWER_OFF_DELAY);
        _powerPort.connectPower();
        _powerState = _POWER_ON;
        _sensorMode = false;
    }

    /** make sure that the wait time of _PUCK_POWER_OFF_DELAY has elapsed */
    private void waitForPuckPowerUp()
    {
        if ( _powerOffTimer.read() < _PUCK_POWER_OFF_DELAY )
        {
            //calculate delay time
            int dTime = _PUCK_POWER_OFF_DELAY - (int)_powerOffTimer.read();
            
            //if you get a positive value, wait
            if ( dTime > 0)
            {
                _log4j.debug("waitForPuckPowerUp() waiting " + dTime + " ms");
                StopWatch.delay(dTime);
            }
        }
    }
    
    /** compare SerialPortParameters objects */
    private boolean serialParamsEqual(SerialPortParameters a, 
                                      SerialPortParameters b)
    {
        if ( (a.getBaud()     != b.getBaud())     ||
             (a.getDataBits() != b.getDataBits()) ||
             (a.getParity()   != b.getParity())   ||
             (a.getStopBits() != b.getStopBits()) )
            return false;
    
        return true;
    }

}
