/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.foce.devices.co2subsys;
import java.util.Vector;
import java.util.Iterator;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.lang.StringBuffer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


import gnu.io.SerialPort;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.NoSuchPortException;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

import org.mbari.siam.utils.StreamUtils;
import org.mbari.siam.distributed.TimeoutException;

/**
 The 232SPDA module is used to interface various analog and digital 
 signals from within the CO2 electronics housing. It has seven 12-bit A/D inputs, 
 four 8-bit D/A outputs, two digital inputs and one digital output. It is powered 
 by +12Vdc and is communicated to via an RS-232 connection. 
 
 */
public class BB232SPDA{
	/** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(BB232SPDA.class);
	
	/** Driver serial mode - use local serial port
	 If this mode is selected command pacing is not used during cmdWriteRead
	 (not an Elmo mode) 
	 */
    public final static int MODE_SERIAL_LOCAL=0;
	/** Driver RFC1722 mode - use ethernet/serial converter
	 e.g. Digi. 
	 If this mode is selected, command pacing is used during cmdWriteRead
	 (not an Elmo mode) 
	 */
    public final static int MODE_SERIAL_RFC1722=1;
	////////////////////////////////////
	// Timing Constants               //
	////////////////////////////////////
	
    /** Command timeout (milliseconds) */
    public final static long TM_CMD_MSEC=2000L;//1000L;
    /** motor startup timeout (milliseconds) */
    public final static long TM_START_MSEC=500L;
    /** motor stop timeout (milliseconds) */
    public final static long TM_STOP_MSEC=500L;
    /** polling delay during motor start/stop (milliseconds) */
    public final static long TM_POLL_DELAY_MSEC=10L;
    /** default timeout during input stream flush */
    public final static long TM_EMPTY_INPUT_MSEC=0L;//5L;
    /** default write/read cycle timeout */
    public final static long TM_WRITEREAD_MSEC=3000L;
    /** default motion wait polling interval */
    public final static long TM_MOTION_WAIT_MSEC=125L;
	/** time between writing and reading command (see cmdWriteRead) */
    public final static long TM_CMD_DELAY_MSEC=0L;
	/** time following digitial output write */
    public final static long TM_WRITE_DELAY_MSEC=200L;
	
	/** Buffer size (32) */
    public final static int BUF32=32;

	public static final int ANALOG_INPUTS=7;
	public static final int ANALOG_OUTPUTS=4;
	public static final int DIGITAL_INPUTS=2;
	public static final int DIGITAL_OUTPUTS=1;
	
	public static final int MAX_ANALOG_INPUT=6;

	/////////////////////////////////
	// Field maps for the arrays
	// returned by the read commands
	//////////////////////////////////
	// Read Analog Inputs	
	public static final int RAI_AI_0=0;
	public static final int RAI_AI_1=1;
	public static final int RAI_AI_2=2;
	public static final int RAI_AI_3=3;
	public static final int RAI_AI_4=4;
	public static final int RAI_AI_5=5;
	public static final int RAI_AI_6=6;
	// Read Digital IO
	public static final int RDIO_DO_0=0;
	public static final int RDIO_DI_0=1;
	public static final int RDIO_DI_1=2;
	// Read All 
	public static final int RA_AI_0=0;
	public static final int RA_AI_1=1;
	public static final int RA_AI_2=2;
	public static final int RA_AI_3=3;
	public static final int RA_AI_4=4;
	public static final int RA_AI_5=5;
	public static final int RA_AI_6=6;
	public static final int RA_DO_0=7;
	public static final int RA_DI_0=8;
	public static final int RA_DI_1=9;
	
	//////////////////////////////////////////////////
	// masks and bits for isolating digital IO fields
	//////////////////////////////////////////////////
	public static final byte DO_0_MASK=0x08;
	public static final byte DI_0_MASK=0x10;
	public static final byte DI_1_MASK=0x20;
	public static final int DO_0_BIT=3;
	public static final int DI_0_BIT=4;
	public static final int DI_1_BIT=5;
	
	/////////////////////////////////
	//  command protocal
	/////////////////////////////////
	public static final String READ_ANALOG_IN     ="!0RA";
	public static final String READ_DIGITAL_IO    ="!0RD";
	public static final String WRITE_DIGITAL_OUT  ="!0SO";
	public static final byte[] WRITE_DIGITAL_OUT_BYTES  =WRITE_DIGITAL_OUT.getBytes();
	public static final String READ_ALL_ANALOG    ="!0RA"+MAX_ANALOG_INPUT;

	/** serial port */
	SerialPort _serialPort;
	/** Serial port input stream */
    InputStream _serialRx;
    /** Serial port output stream */
    OutputStream _serialTx;
	/** Serial port mode. 
	 Use MODE_SERIAL_LOCAL if using a standard hardware serial port
	 User MODE_SERIAL_RFC_1722 if using a serial/ethernet converter (e.g. Digi)
	 */
	protected int _serialMode=MODE_SERIAL_LOCAL;
	/** Response terminator (as String) */
    protected String _terminatorString=";";
    /** Response terminator (as byte array) */
    protected byte[] _terminator=_terminatorString.getBytes();
	/** line end for terminating command */
 	public final static byte[] LINE_END_BYTES="\n".getBytes();

	double[] _analog_in=new double[ANALOG_INPUTS];
	double[] _analog_out=new double[ANALOG_OUTPUTS];
	double[] _digital_in=new double[DIGITAL_INPUTS];
	double[] _digital_out=new double[DIGITAL_OUTPUTS];

	
	public BB232SPDA(){
	}
	
	public BB232SPDA(SerialPort port, int serialMode)
	throws IOException, IllegalArgumentException {
		this();
		setSerialPort(port);
		setSerialMode(serialMode);
	}
	
	/** Set the SerialPort for this controller to use.
	 @param port SerialPort to use
	 */
    public void setSerialPort(SerialPort port)throws IOException{
		_serialPort=port;
		if(_serialPort!=null){
			_serialRx=port.getInputStream();
			_serialTx=port.getOutputStream();
		}
    }
    /** Return SerialPort used by this controller.
	 @return serial port or null if not set.
	 */
    public SerialPort getSerialPort(){
		return _serialPort;
    }
	
	/** Return serial port input stream used by this controller.
	 @return input stream or null if not set.
	 */
    public InputStream getInputStream(){
		return _serialRx;
    } 
	/** Return serial port output stream used by this controller.
	 @return output stream or null if not set.
	 */
	public OutputStream getOutputStream(){
		return _serialTx;
    }
	
	public void configure(){
	}
	
	/** max channel 0:MAX_ANALOG_INPUT */
	public short[] readADChannels(int maxChannel)
	throws Exception{
		int channels=maxChannel+1;
		int readBytes=2*(channels);
		byte[] rawData=cmdWriteReadBytes(mkCmd(READ_ANALOG_IN,maxChannel), readBytes, TM_CMD_MSEC);
		
		if(rawData.length<readBytes){
			throw new Exception("analog input returned "+rawData.length+" bytes; expected "+readBytes);
		}
		
		short[] outData=new short[channels];
		if(_log4j.isDebugEnabled()){
		//_log4j.debug("channels:"+channels+" maxChan:"+maxChannel+" rawBytes:"+rawData.length);
		}
		for(int i=0;i<channels;i++){
			int index=2*i;
			short msb=(short)rawData[index];
			msb =  (short)(msb<<8);
			msb &= (short)0xFF00;
			short lsb=(short)rawData[index+1];
			lsb &= (short)0x00FF;
			outData[channels-i-1]=(short)(lsb+msb);
		}
		return outData;
	}
	
	public void writeAnalogOutput(int channel,short data)
	throws Exception{
		throw new Exception("This feature is not implemented");
	}
	
	public short[] readDigitalIO()
	throws Exception{

		byte[] rawData=cmdWriteReadBytes(READ_DIGITAL_IO, 1, TM_CMD_MSEC);
		
		if(rawData.length<1){
			throw new Exception("analog input returned "+rawData.length+" bytes; expected 1");
		}
		int len=DIGITAL_INPUTS+DIGITAL_OUTPUTS;
		short[] outData=new short[len];
		byte dio_states=rawData[0];
		
		outData[RDIO_DO_0]=(short)((dio_states&DO_0_MASK)>>DO_0_BIT);
		outData[RDIO_DI_0]=(short)((dio_states&DI_0_MASK)>>DI_0_BIT);
		outData[RDIO_DI_1]=(short)((dio_states&DI_1_MASK)>>DI_1_BIT);
		return outData;
	}
	
	public void writeDigitalOut(boolean state)
	throws Exception{
		byte outByte= (state ? (byte)(1<<DO_0_BIT) : 0);
		int len=WRITE_DIGITAL_OUT_BYTES.length+1;
		byte[] byteCmd=new byte[len];
		for(int i=0;i<WRITE_DIGITAL_OUT_BYTES.length;i++){
			byteCmd[i]=WRITE_DIGITAL_OUT_BYTES[i];
		}
		byteCmd[len-1]=(byte)((int)outByte+'0');
		String cmd=new String(byteCmd);
		if(_log4j.isDebugEnabled()){
		//_log4j.debug("writing digital out ["+cmd+"]");
		}
		writeCommand(cmd);
		delay(TM_WRITE_DELAY_MSEC);
	}
	
	public short[] readAll()
	throws Exception{
		short[] analogInputs=readADChannels(MAX_ANALOG_INPUT);
		short[] digitalIO=readDigitalIO();
		short[] outData=new short[analogInputs.length+digitalIO.length];
		outData[RA_AI_0]=analogInputs[RAI_AI_0];
		outData[RA_AI_1]=analogInputs[RAI_AI_1];
		outData[RA_AI_2]=analogInputs[RAI_AI_2];
		outData[RA_AI_3]=analogInputs[RAI_AI_3];
		outData[RA_AI_4]=analogInputs[RAI_AI_4];
		outData[RA_AI_5]=analogInputs[RAI_AI_5];
		outData[RA_AI_6]=analogInputs[RAI_AI_6];
		outData[RA_DO_0]=digitalIO[RDIO_DO_0];
		outData[RA_DI_0]=digitalIO[RDIO_DI_0];
		outData[RA_DI_1]=digitalIO[RDIO_DI_1];
		return outData;
	}
	
	/** set serial mode */
	public void setSerialMode(int mode) throws IllegalArgumentException{
		switch(mode){
			case MODE_SERIAL_LOCAL:
			case MODE_SERIAL_RFC1722:
				_serialMode=mode;
				break;
			default:
				throw new IllegalArgumentException("Invalid serial mode:["+mode+"]");
		}
	}
	/** set serial mode */
	public int getSerialMode() {
		return _serialMode;
	}
	
	/** Sleep for specified time
	 @param delayMsec Time to sleep (milliseconds)
     */
    protected void delay(long delayMsec){
		try{
			Thread.sleep(delayMsec);
		}catch(Exception e){
		}
    }
	
	public static String mkCmd(String cmd, int value){
		return cmd+value;
	}
	
	/** Flush the serial input stream.
	 (using default (this) input stream)
	 @param timeoutMsec (milliseconds)
	 */
    public void emptyInput(long timeoutMsec)
	throws IOException {
		emptyInput(_serialRx,timeoutMsec);
    }
	
    /** Flush the serial input stream.
	 (using default (this) input stream)
	 @param instream an InputStream to flush
	 @param timeoutMsec (milliseconds)
	 */
    protected synchronized void emptyInput(InputStream instream, long timeoutMsec)
    throws IOException
    {
		if(_log4j.isDebugEnabled()){
		//_log4j.debug("emptyInput - t:"+timeoutMsec);
		}
		int nextByte;
		if(timeoutMsec>0L){
			long start=System.currentTimeMillis();
			while( ((System.currentTimeMillis()-start)<timeoutMsec) ){
				if( (_serialRx.available()>0L) ){
					// if available, go ahead and read as many as
					// possible, even if it goes over the timeout
					while( (_serialRx.available()>0L) ){
						nextByte=_serialRx.read();
						if(_log4j.isDebugEnabled()){
						//_log4j.debug("emptyInput -ct:"+(char)nextByte);
						}
					}
				}			}
		}else{
			// if no timeout, check once and
			// read until none available
			while(_serialRx.available()>0L){
				nextByte=_serialRx.read();
			}
		}
		if(_serialRx.available()>0L){
			// if there's any left on the way out, issue warning
			_log4j.warn("emptyInput - hey, there's still stuff in the buffer!!");
		}
		if(_log4j.isDebugEnabled()){
		//_log4j.debug("emptyInput on exit: avail:"+_serialRx.available()+" read: "+buf.length()+" bytes:["+buf.toString()+"] count:"+count+"\n\n");
		}
    }
	
    /** Write a command to the serial port.
	 terminates the command with a newline and
	 flush the serial port.
	 @param cmd command string to write
	 */
    protected synchronized void writeCommand(String cmd)
    throws IOException
    {
		if(_log4j.isDebugEnabled()){
		//_log4j.debug("writeCommand - emptying");
		}
		emptyInput(_serialRx,0L);
		if(_log4j.isDebugEnabled()){
		//_log4j.debug("writeCommand - writing "+cmd);
		}
		_serialTx.write(cmd.getBytes());
		_serialTx.write(LINE_END_BYTES);
		_serialTx.flush();
    }
	
	/** Perform one I/O transaction, (write then read).  
	 @param cmd Command to send
	 @param buf Destination buffer
	 @param startIndex Buffer start position
	 @param len Buffer length
	 @param timeoutMsec Timeout (milliseconds)
	 @return  number of bytes in response. 
	 */
	
    protected synchronized int cmdWriteRead(String cmd, byte[] buf, int startIndex, int len,long timeoutMsec) 
	throws TimeoutException, IOException, NullPointerException, Exception
    {
		if(_log4j.isDebugEnabled()){
		//_log4j.debug("cmdWriteRead - c:"+cmd+" tm:"+new String(terminator)+" to:"+timeoutMsec+" toe:"+TM_EMPTY_INPUT_MSEC);
		}
		
		if(_log4j.isDebugEnabled()){
		//_log4j.debug("cmdWriteRead - emptying");
		}
		emptyInput(_serialRx,TM_EMPTY_INPUT_MSEC);
		
		if(_log4j.isDebugEnabled()){
		//_log4j.debug("cmdWriteRead - writing");
		}
		writeCommand(cmd);
				
		if(_log4j.isDebugEnabled()){
		//_log4j.debug("cmdWriteRead - reading");
		}
		int retval= StreamUtils.readBytes(_serialRx,buf,startIndex,len,timeoutMsec);
		
		if(_log4j.isDebugEnabled()){
		//_log4j.debug("cmdWriteRead - returning "+retval);
		}
		return retval;
    }
		
	/** Write one command and parse the resulting return value as a String.
	 @param cmd command to send
	 @param len maximum expected return length (bytes)
	 @param timeoutMsec timeout for return value (milliseconds)
	 @return command return value (String)
	 */
    protected byte[] cmdWriteReadBytes(String cmd,int len, long timeoutMsec) 
	throws TimeoutException, IOException, NullPointerException, Exception
    {
		if(len<=0){
			throw new Exception("len must be >0");
		}
		byte[]	byteBuf=new byte[len];
		Arrays.fill(byteBuf,(byte)'\0');
		int bytesRead=cmdWriteRead(cmd,byteBuf,0,len,timeoutMsec);
		if(bytesRead<=0){
			throw new IOException("stream read returned ["+bytesRead+"]");
		}
		return byteBuf ;
    }
}