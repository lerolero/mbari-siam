/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.wetlabs;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.utils.StreamUtils;

import org.apache.log4j.Logger;
import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.DeviceServiceIF;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.NotSupportedException;
import org.mbari.siam.distributed.Safeable;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.TimeoutException;

/**
 * Implements instrument service for WET Labs ECO "Triplet" instrument.
 */
public class WetLabsTriplet extends WetLabsECO implements Instrument, Safeable {
    
    /**
     *
     */
    private static final long serialVersionUID = -6847732815613058691L;
    
    // log4j Logger
    static private Logger _log4j = Logger.getLogger(WetLabsTriplet.class);
    
    static final String PUCK_PROMPT = "PUCKRDY";
    
    static final String INSTRUMENT_MODE_CMD = "PUCKIM";
    
    private final DevicePacketParser devicePacketParser = new DevicePacketParser();
    
    
    public WetLabsTriplet() throws RemoteException {
	_attributes = new Attributes(this);
    }
    
    /** Issue "soft break" until instrument goes into command mode. */
    protected void doSoftBreak() throws Exception {
        final int maxTries = 5;
        for (int i = 0; i < maxTries; i++) {
            try {
                // Issue 7 since first 2 get lost while ECO powering up
                _log4j.debug("doSoftBreak() - issue !s");
                for (int j = 0; j < 7; j++) {
                    _toDevice.write("!".getBytes());
                    _toDevice.flush();
                    Thread.sleep(200);
                }
                _toDevice.write("\r".getBytes());
                _toDevice.flush();
                
                int nBytes = StreamUtils.readBytes(_fromDevice, _scratch, 0, _scratch.length, 3000);
                
                String buf = new String(_scratch, 0, nBytes);
                
                // Look for menu
                if (buf.indexOf(menuPrefix()) >= 0) {
                    // Got menu - return
                    _log4j.debug("doSoftBreak() - now in menu");
                    return;
                }
                
                if (buf.indexOf(PUCK_PROMPT) >= 0) {
                    // Got PUCK prompt - issue menu command
                    _log4j.debug("doSoftBreak() - got PUCK prompt - go to instrument mode");
                    doCommand(INSTRUMENT_MODE_CMD, menuPrefix());
                    _log4j.debug("doSoftBreak() - Now in menu - return");
                    return;
                }
                
            } catch (TimeoutException e) {
                _log4j.info("enterCommandMode() - no menu prompt yet");
            } catch (Exception e) {
                // maybe
                _log4j.error("Exception " + e);
            }
        }
        _log4j.error("doSoftBreak() failed");
        throw new Exception("doSoftBreak() failed");
    }
    
    /** Return parameters to use on serial port. */
    public SerialPortParameters getSerialPortParameters() throws UnsupportedCommOperationException {
        
        return new SerialPortParameters(((Attributes )_attributes).baud, 
					SerialPort.DATABITS_8, 
					SerialPort.PARITY_NONE,
					SerialPort.STOPBITS_1);
    }
    
    /** Return leading string of menu */
    String menuPrefix() {
        return "ver FLNTU";
    }
    
    public org.mbari.siam.distributed.DevicePacketParser getDevicePacketParser() {
        return devicePacketParser;
    }
    
    
    /** Configurable attributes */
    protected class Attributes extends WetLabsECO.Attributes {
        
        
        /** Constructor, with required InstrumentService argument */
        protected Attributes(DeviceServiceIF service) {
            super(service);
            summaryVars = new String[] {
                DevicePacketParser.CHLOROPHYLL,
                DevicePacketParser.NTU1,
                DevicePacketParser.NTU2,
            };
        }
        
        /** Instrument baud */
        int baud = 19200;
    }
    
    /**
     * Parses SensorDataPackets containing records from the WetlabsTriplet. A sample is tabbed delimited and looks
     * something like:
     * <pre>
     * 04/16/06 19:28:41    5828    10135   17154
     * 04/16/06 19:28:42    17373   10135   17154
     * 04/16/06 19:28:42    17373   10135   17154
     * </pre>
     */
    class DevicePacketParser extends org.mbari.siam.distributed.DevicePacketParser {
        
        static final String CHLOROPHYLL = "Chlorophyll";
        static final String NTU1 = "NTU1";
        static final String NTU2 = "NTU2";
        static final String SUBSAMPLE_COUNT = "Samples";
        
        private Vector lines = new Vector();
        
        protected void parseFields(DevicePacket packet) throws NotSupportedException, Exception {
            if (packet instanceof SensorDataPacket) {
                /*
                 * If it's a sensorDataPacet then tokeize by lines and store each line in a Vector for later processing.
                 * Each line in a packet will be averaged together to produce a single record.
                 */
                SensorDataPacket sdPacket = (SensorDataPacket) packet;
                byte[] dataBuffer = sdPacket.dataBuffer();
                if (dataBuffer != null) {
                    String msg = new String(dataBuffer);
                    StringTokenizer lineTok = new StringTokenizer(msg, "\n");
                    while (lineTok.hasMoreTokens()) {
                        lines.add(lineTok.nextToken());
                    }
                    processLines();
                }
            }
        }
        
        /**
         * This method averages each line in a sample together for later processing.
         * @throws Exception
         */
        private void processLines() throws Exception {
            int[] chl = new int[lines.size()];
            int[] ntu1 = new int[lines.size()];
            int[] ntu2 = new int[lines.size()];
            
            /*
             * Parse the values out of each line
             */
            int lineNumber = 0;
            Enumeration e = lines.elements();
            while (e.hasMoreElements()) {
                String line = (String) e.nextElement();
                StringTokenizer tok = new StringTokenizer(line);
                int count = 0;
                while(tok.hasMoreTokens()) {
                    String value = tok.nextToken();
                    try {
                        switch (count) {
                            case 2:
                                chl[lineNumber] = Integer.parseInt(value);
                                break;
                            case 3:
                                ntu1[lineNumber] = Integer.parseInt(value);
                                break;
                            case 4:
                                ntu2[lineNumber] = Integer.parseInt(value);
                                break;
                        }
                    } catch (Exception ex) {
                        if (_log4j.isDebugEnabled()) {
                            _log4j.debug("Failed to parse record", ex);
                            ex.fillInStackTrace();
                            throw ex;
                        }
                    }
                    count++;
                }
                lineNumber++;
                
            }
            
            /*
             * Average like values so that we can produce a single data record.
             */
            int sumChl = 0;
            int sumNtu1 = 0;
            int sumNtu2 = 0;
            for (int i = 0; i < chl.length; i++) {
                sumChl += chl[i];
                sumNtu1 += ntu1[i];
                sumNtu2 += ntu2[i];
            }
            
            int aChl = sumChl / chl.length;
            int aNtu1 = sumNtu1 / ntu1.length;
            int aNtu2 = sumNtu2 / ntu2.length;
            
            /*
             * Add the important measurements to the record
             */
            addMeasurement(CHLOROPHYLL, "Chlorophyll concentration", "counts", aChl);
            addMeasurement(NTU1, "Nominal Transmission units #1", "counts", aNtu1);
            addMeasurement(NTU2, "Nominal Transmission units #2", "counts", aNtu2);
            addMeasurement(SUBSAMPLE_COUNT, "Samples per DataPacket", "samples", chl.length);
            
            // Important to clear out the old data
            lines.clear();
            
        }
        
        
        
    }
    
}
