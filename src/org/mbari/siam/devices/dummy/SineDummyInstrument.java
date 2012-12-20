/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.dummy;

import java.util.Vector;
import java.util.Iterator;
import java.text.NumberFormat;
import java.lang.StringBuffer;
import java.lang.Math;

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
import org.mbari.siam.distributed.PacketParser;
import org.mbari.siam.distributed.TimeoutException;


/**
 * Fake instrument for testing purposes.
 */
public class SineDummyInstrument 
	extends PolledInstrumentService 
    implements Instrument{
		
		/** Log4j logger */
		protected static Logger _log4j = 
		Logger.getLogger(SineDummyInstrument.class);
		StringBuffer _stringBuf=new StringBuffer();
		double _wavet=0.0;
		double _wavei=0.1;
		double _wavep=1.0;
		private int _nSamples = 0;
		double _pi=Math.PI;
		SineDummyParser _parser=null;
		NumberFormat _nf=NumberFormat.getInstance();
		
		
		public SineDummyInstrument() throws RemoteException {
			super();
			_nf.setMaximumFractionDigits(3);
		}
		
		/** Request a data sample from instrument. */
		protected void requestSample() throws TimeoutException, Exception{
		// nothin' doin'
		}

 		/**
		 * Read raw sample bytes from serial port into buffer, return number of
		 * bytes read. Reads characters from serial port until sample terminator
		 * string is encountered.
		 *
		 * @param sample
		 *            output buffer
		 */
		protected int readSample(byte[] sample) throws TimeoutException,
		IOException, Exception {
			
			SensorDataPacket dataPacket = new SensorDataPacket(getId(), 100);
			_stringBuf.setLength(0);
			
			for(int i=1;i<=9;i++){
				_stringBuf.append(_nf.format(Math.sin(2*_pi*_wavet/(i*_wavep)))+",");
			}
			_stringBuf.append(_nf.format(Math.sin(2*_pi*_wavet/(10*_wavep))));
			_wavet+=_wavei;
			_log4j.debug("data: "+_stringBuf.toString());
			byte[] sampleBytes=_stringBuf.toString().getBytes();
			
			for(int i=0;i<sample.length;i++){
				if(i<sampleBytes.length){
					sample[i]=sampleBytes[i];
				}else{
					sample[i]=(byte)'\0';
				}
			}
			return sampleBytes.length;
		}
		/** Specify compass device startup delay (millisec) */
		protected int initInstrumentStartDelay() {
			return 0;
		}
		
		/** Specify compass prompt string. */
		protected byte[] initPromptString() {
			return ">".getBytes();
		}
		
		/** Specify sample terminator. */
		protected byte[] initSampleTerminator() {
			return "\r\n".getBytes();
		}
		
		/** Specify maximum bytes in raw compass sample. */
		protected int initMaxSampleBytes() {
			return 128;
		}
		
		/** Specify current limit. */
		protected int initCurrentLimit() {
			return 100;
		}
		
		/** Return initial value of instrument power policy. */
		protected PowerPolicy initInstrumentPowerPolicy() {
			return PowerPolicy.NEVER;
		}
		
		/** Return initial value of communication power policy. */
		protected PowerPolicy initCommunicationPowerPolicy() {
			return PowerPolicy.NEVER;
		}
		/** Return metadata. */
		protected byte[] getInstrumentMetadata() {
			
			return "DummyInstrument METADATA GOES HERE".getBytes();
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
			// Sample every 30 seconds by default
			return new ScheduleSpecifier(30000);
		}
		
		/** Return parameters to use on serial port. */
		public SerialPortParameters getSerialPortParameters()
		throws UnsupportedCommOperationException {
			
			return new SerialPortParameters(9600, SerialPort.DATABITS_8,
											SerialPort.PARITY_NONE, 
											SerialPort.STOPBITS_1);
		}
		
		/** Return a PacketParser. */
		public PacketParser getParser() throws NotSupportedException{
			if(_parser==null){
				_parser=new SineDummyParser(_instrumentAttributes.registryName,",");
				try{
					_parser.setNamingPrefix(_instrumentAttributes.registryName);
				}catch (Exception e) {
					_log4j.error("Could not set naming prefix for ["+_instrumentAttributes.registryName+"]:"+e);
				}
			}
			return _parser;
		}
		
	}