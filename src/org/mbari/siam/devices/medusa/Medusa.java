/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.medusa;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.StringTokenizer;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import org.mbari.siam.core.PolledInstrumentService;
import org.mbari.siam.distributed.RangeException;
import org.mbari.siam.core.SerialPortParameters;
import org.mbari.siam.utils.StreamUtils;
import org.mbari.siam.distributed.PowerPolicy;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.ScheduleSpecifier;
import org.mbari.siam.distributed.TimeoutException;
import org.mbari.siam.distributed.ScheduleParseException;
import org.mbari.siam.distributed.devices.NetworkSwitch;
import org.apache.log4j.Logger;

/**
Service for Medusa optical interface card.
 */
public class Medusa extends PolledInstrumentService implements NetworkSwitch {

    // CVS revision 
    private static String _versionID = "$Revision: 1.2 $";

    static private Logger _log4j = Logger.getLogger(Medusa.class);

    /** Constructor. */
    public Medusa() throws RemoteException {
    }

    static final byte[] SAMPLE_REQUEST = "sfpdiag\r\n".getBytes();
    static final byte[] DISABLE_ECHO = "echo n\r\n".getBytes();
    static final byte[] ENTER = "\r\n".getBytes();
    static final String SET_IP_ADDRESS = "saipset";


    // Some times Medusa barfs and resends data if it 
    // receives a '\n' or '\r' while executing a 
    // command; hence, just use '\r'
    static final String CMD_META = "meta\r";
    static final String CMD_NVSHOW = "nvshow\r";
    static final String CMD_PORTS = "ports\r";
    static final String CMD_UIPSTAT = "uipstat\r";
    static final String CMD_EVENT = "event\r";
    static final String CMD_U0STAT = "u0stat\r";
    static final String CMD_SFPDIAG = "sfpdiag\r";
    static final String CMD_RESET = "reset\r";
    static final String CMD_SHOWIPADDRESS = "ipset\r";
    static final String CMD_SHOWSTATS = "showstats ";
    static final int METADATA_TIMEOUT_MSEC = 15000;
    private byte[] _metadataBuf = new byte[1024];

    /** Time required for reset */
    static final int RESET_MSEC = 10000;

    /** Return initial value for instrument's "prompt" character. */
    protected byte[] initPromptString() {
	return "->".getBytes();
    }

    /** Return initial value for instrument's sample terminator */
    protected byte[] initSampleTerminator() {
	return "\r\n\r\n->".getBytes();
    }

    /** Return initial value of DPA current limit. */
    protected int initCurrentLimit() {
	return 1;
    }

    /** Return initial value of instrument power policy. */
    protected PowerPolicy initInstrumentPowerPolicy() {
	return PowerPolicy.NEVER;
    }

    /** Return initial value of instrument power policy. */
    protected PowerPolicy initCommunicationPowerPolicy() {
	return PowerPolicy.NEVER;
    }

    /** Return initial value of instrument startup time in millisec. */
    protected int initInstrumentStartDelay() {
	return 2000;
    }

    /**
     * Return initial value for maximum number of bytes in a instrument data
     * sample.
     */
    protected int initMaxSampleBytes() {
	return 1000;
    }

 
    /** Get a Medusa sample, which includes diagnostic information.  */
    protected void requestSample() {
	try {
	    // Verify connection...
	    _log4j.debug("requestSample(): looking for Medusa prompt...");
	    getPrompt();

	    // Get status/config info...
	    _log4j.debug("requestSample(): sending sample request...");
	    _toDevice.write(SAMPLE_REQUEST);

	} catch (Exception e) {
	    _log4j.error("requestSample() caught Exception", e);
	}
	return;
    }

    /** Get prompt character from Medusa. */
    protected void getPrompt() throws TimeoutException, IOException,
				      NullPointerException, Exception {
	_log4j.debug("getPrompt()");
	_fromDevice.flush();
	_toDevice.write(ENTER);
	StreamUtils.skipUntil(_fromDevice, getPromptString(),
			      getSampleTimeout());

	_log4j.debug("getPrompt() - got it");
    }


    /** Self-test routine; not yet implemented. */
    public int test() {
	return Device.OK;
    }

    /** Initialize the Medusa. */
    protected void initializeInstrument() throws Exception {

	// First reset the Medusa
	_toDevice.write(CMD_RESET.getBytes());
	_toDevice.flush();
	Thread.sleep(RESET_MSEC);

	getPrompt();

	// Disable command echo
	_toDevice.write(DISABLE_ECHO);

	// Send host IP address to Medusa
	try {
	    InetAddress host = InetAddress.getLocalHost();
	    _toDevice.write((SET_IP_ADDRESS + " " + 
			     host.getHostAddress() + ENTER).getBytes());
	}
	catch (UnknownHostException e) {
	    _log4j.error(e);
	}
    }


    /** Return parameters to use on serial port. */
    public SerialPortParameters getSerialPortParameters()
	throws UnsupportedCommOperationException {

	return new SerialPortParameters(38400, SerialPort.DATABITS_8,
					SerialPort.PARITY_NONE, 
					SerialPort.STOPBITS_1);
    }


    /** Return specifier for default sampling schedule. */
    protected ScheduleSpecifier createDefaultSampleSchedule()
	throws ScheduleParseException {

	// Sample every 300 seconds by default
	return new ScheduleSpecifier(300000);
    }

    /** Set Medusa's clock. */
    public void setClock(long epochMsec) {
	_log4j.info("setClock() not implemented");
    }


    /** Return metadata. */
    protected byte[] getInstrumentStateMetadata() {
	boolean gotPrompt = false;
	String sMetadata="";

	for (int i = 0; i < 3; i++) {
	    try {
		getPrompt();
		gotPrompt = true;
		break;
	    }
	    catch (Exception e) {
	    }
	}
	if (!gotPrompt) {
	    _log4j.error("Couldn't get prompt");
	    return "getPrompt() failed".getBytes();
	}

	try {

	    sMetadata+="\nMETA:\n"+writeReadCommand(CMD_META);
	    sMetadata+="\n\nPORTS:\n"+writeReadCommand(CMD_PORTS);
	    sMetadata+="\n\nNVSHOW:\n"+writeReadCommand(CMD_NVSHOW);
	    sMetadata+="\n\nUIPSTAT:\n"+writeReadCommand(CMD_UIPSTAT);
	    sMetadata+="\n\nEVENT:\n"+writeReadCommand(CMD_EVENT);
	    sMetadata+="\n\nU0STAT:\n"+writeReadCommand(CMD_U0STAT);
	    sMetadata+="\n\nSFPDIAG:\n"+writeReadCommand(CMD_SFPDIAG);
	    for( int i=0;i<6;i++) {
		sMetadata+="\nSHOWSTATS["+i+"]:\n\n"+writeReadCommand((CMD_SHOWSTATS+" "+i+"\r"));
	    }

	    return sMetadata.trim().getBytes();
	}
	catch (Exception e) {
	    _log4j.error("getInstrumentStateMetadata(): ", e);

	    String s="Couldn't get metadata";

	    if(sMetadata.length()>0)
		s="Partial metadata returned:\n"+sMetadata;

	    return s.getBytes();
	}
    }

    private String writeReadCommand(String cmd){
	String ret="";
	try{

	    for(int i=0;i<_metadataBuf.length;i++)
		_metadataBuf[i]='\0';

	    // flush input stream
	    _fromDevice.flush();

	    // write command
	    //_log4j.debug("writeReadCommand writing cmd: "+cmd);
	    _toDevice.write(cmd.getBytes());
	    _toDevice.flush();

	    int nBytes = 
		StreamUtils.readUntil(_fromDevice, _metadataBuf,
				      getSampleTerminator(), 
				      METADATA_TIMEOUT_MSEC);

	    //_log4j.debug("writeReadCommand read "+nBytes+" bytes");

	    if(nBytes>0){
		byte[] retBuf=new byte[nBytes];

		for(int i=0;i<nBytes;i++)
		    retBuf[i]=_metadataBuf[i];

		ret=new String(retBuf);
		ret=ret.trim();
		//_log4j.debug("writeReadCommand returning "+ret.length()+" bytes:\n"+ret);
		return ret;
	    }

	    _log4j.error("writeReadCommand Error: No data read");

	}catch (Exception e) {
	    _log4j.error("writeReadCommand Error: "+ e);
	}

	return "ERROR";

    }


    /** Get Medusa cpu IP address */
    public InetAddress getCpuAddress() throws UnknownHostException {

	String buf = writeReadCommand(CMD_SHOWIPADDRESS);
	StringTokenizer tokenizer = new StringTokenizer(buf);
	String token = null;
	while (tokenizer.hasMoreTokens()) {
	    token = tokenizer.nextToken();
	}

	// Last token is the cpu IP address
	return InetAddress.getByName(token);
    }
}


