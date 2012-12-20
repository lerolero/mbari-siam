/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.devices.nalModem;

import java.util.Vector;
import java.util.StringTokenizer;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataOutputStream;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.ShoreMessaging;
import org.mbari.siam.distributed.ShoreMessagingHelper;
import org.mbari.siam.utils.StreamUtils;
import org.mbari.siam.utils.ShoreMessagingService;

/** 
    Implements ShoreMessaging with NAL Research Iridium modem. 
*/
public abstract class NALMessagingService 
    extends ShoreMessagingService
    implements ShoreMessaging {

    protected static Logger _log4j = 
	Logger.getLogger(NALMessagingService.class);


    public static final int PROFILE_NUMBER = 1;

    private static final String INCORRECT_MSG_SIZE = "Incorrect msg size";
    private static final String SIGNAL_STRENGTH_RESPONSE = "+CSQ:";

    private CommPortIdentifier _commPortId = null;

    private static final int BAUD = 19200;

    protected static final int READ_TIMEOUT_MSEC = 6000;

    protected String _serialPortName;

    protected ShoreMessagingHelper.Message _uplinkMsg;

    protected boolean _checkForUplinks = false;

    /** Maximum uplinks per session */
    protected final static int MAX_UPLINKS_PER_SESSION = 10;

    /** Maximum consecutive uplink failures; give up, try later, 
	if exceeded */
    protected final static int MAX_CONSEC_UPLINK_FAILED = 10;

    /** Maximum consecutive downlink failures; give up, try later, 
	if exceeded */
    protected final static int MAX_CONSEC_DOWNLINK_FAILED = 3;

    /** Aggregate message buffer */
    protected byte[] _aggregateMsg = null;

    /** Number of uplinks pending on shore; may be StoreAndFwdIF::Unknown */
    protected int _nPendingUplinks;

    public NALMessagingService(String serialPortName) 
	throws Exception {

	_log4j.info("NOTE: This driver makes use of modem user profile #" + 
		    PROFILE_NUMBER + ", assumes baud=" + BAUD);

	_serialPortName = new String(serialPortName);

	_commPortId = CommPortIdentifier.getPortIdentifier(_serialPortName);
	_aggregateMsg = new byte[maxDownlinkMsgBytes()];
    }


    /** Return modem signal quality (0-5), -1 on error */
    protected int receivedSignalStrength(SerialPort serialPort,
					 InputStream inStream,
					 OutputStream outStream) 
	throws Exception {

	// Get signal quality
	outStream.write("at+csq\r".getBytes());

	byte[] buf = new byte[100];
	_log4j.debug("read signal strength");
	int nBytes = StreamUtils.readUntil(inStream,
					   buf, "OK\r\n".getBytes(), 20000);

	String response = (new String(buf, 0, nBytes)).trim();
	_log4j.debug("done reading signal strength");
	int index;
	if ((index = response.indexOf(SIGNAL_STRENGTH_RESPONSE)) >= 0) {
	    String buf2 = 
		response.substring(index + SIGNAL_STRENGTH_RESPONSE.length());
		
	    return Integer.parseInt(buf2);
	}
	else {
	    throw new Exception("Couldn't get signal strength");
	}
    }


    /** Configure modem for use by this driver */
    protected void configureModem(InputStream input, OutputStream output) 
	throws Exception {

	// Load this driver's profile number
	String cmd = "ATZ" + PROFILE_NUMBER + "\r";
	output.write(cmd.getBytes());

	sleep(5);

	// MUST disable 'quiet' mode for this driver to work properly
	output.write("ATQ0\r".getBytes());

	// MUST disable echo for this driver to work properly
	output.write("ATE0\r".getBytes()); 


	output.flush();

	try {
	    StreamUtils.skipUntil(input, "OK".getBytes(), 5000);
	    _log4j.info("Modem is configured for use");
	}
	catch (Exception e) {
	    _log4j.error("Couldn't configure modem: " + e);
	    _log4j.error("Is modem powered on?");

	    throw new Exception("Couldn't configure modem: is it powered on?");
	}
    }


    /** Restore modem to "default" configuration, for use by other
	applications - assumes profile #0
	is default */
    protected void restoreModem(OutputStream output) 
	throws Exception {

	output.write("ATZ0\r".getBytes());
	output.flush();
    }


    /** Return number of uplinks pending on shore; may return
	StoreAndFwdIF::Unknown */
    protected int nPendingUplinks() {
	return _nPendingUplinks;
    }

    /** Connect modem, exchange messages with shore */
    public synchronized void connect(int timeoutSec) 
	throws ShoreMessagingHelper.ConnectFailed {

	SerialPort serialPort = null;
	InputStream inStream = null;
	DataOutputStream outStream = null;

	try {
	    serialPort = openSerialPort(BAUD);
	}
	catch (Exception e) {
	    _log4j.warn("openSerialPort() failed: " + e, e);
	    throw new ShoreMessagingHelper.ConnectFailed();
	}

	// The 'finally' clause of this outer 'try' block ensures that 
	// input and output streams are closed when we're done, and that the
	// serial port gets closed. 
	try {
	    try {
		inStream = serialPort.getInputStream();

		outStream = 
		    new DataOutputStream(serialPort.getOutputStream());
	    }
	    catch (Exception e) {
		_log4j.error("Exception opening streams: ", e);
		if (inStream != null) { 
		    try {
			inStream.close(); 
		    }
		    catch (Exception e2) {
			_log4j.error("Exception closing inStream: ", e2);
		    }
		}
		if (outStream != null) { 
		    try {
			outStream.close(); 
		    }
		    catch (Exception e2) {
			_log4j.error("Exception closing outStream: ", e2);
		    }
		}
		throw new ShoreMessagingHelper.ConnectFailed();
	    }

	    try {
		configureModem(inStream, outStream);
	    }
	    catch (Exception e) {
		throw new ShoreMessagingHelper.ConnectFailed(e.getMessage());
	    }


	    _connectionTimer.reset(timeoutSec);

	    byte[] msgBody = new byte[1024];

	    if (_checkForUplinks) {
		try {
		    int nUplinks = getUplinkMsgs(serialPort, inStream, outStream);
		    _log4j.info("Received " + nUplinks + " uplinked messages");
		}
		catch (Exception e) {
		    _log4j.error("getUplinkMsgs(): " + e.getMessage());
		}

		if (_connectionTimer.timedOut()) {
		    _log4j.warn("connect() timed out while retrieving uplinks");
		}
	    }
	    else {
		_log4j.info("Skipping check for uplinked messages");
	    }

	    int nConsecFailed = 0;

	    // Send all messages in the downlink queue
	    while (!_downlinkMsgQ.empty() && !_connectionTimer.timedOut()) {

		if (nConsecFailed == MAX_CONSEC_DOWNLINK_FAILED) {
		    _log4j.error("Got " + nConsecFailed + 
				 " consecutive connection failures");
		    break;
		}

		Vector pendingDownlinkMsgs = new Vector();
		pendingDownlinkMsgs.clear();

		int nAggregateBytes = 0;

		int nAggregatedMsgs = 0;

		// Aggregate individual messages into a single SBD 
		// aggregate message
		while (!_downlinkMsgQ.empty()) {

		    _log4j.debug("get next downlink msg from queue");
		    ShoreMessagingHelper.Message msg;
		    try {
			msg = (ShoreMessagingHelper.Message)_downlinkMsgQ.popFront();
		    }
		    catch (Exception e) {
			_log4j.error("empty queue???");
			break;
		    }

		    if (msg.solitaryDownlink() && nAggregatedMsgs > 0) {
			// This msg needs to be downlinked by itself. Push it back 
			// onto the front queue and break out of this loop
			_log4j.debug("Solitary msg; put it back on queue and " + 
				     "break out of aggregation loop");
		    
			_downlinkMsgQ.pushFront(msg);
			break;
		    }

		    msgBody = msg.getBytes();
		    int nMsgBytes = msgBody.length;

		    // Save this message in pending downlink vector in case, since
		    // we'll have to requeue if downlink fails
		    _log4j.debug("save msg " + new String(msgBody) +  
				 "in pending vector");

		    pendingDownlinkMsgs.add(msg);
		    _log4j.debug("" + pendingDownlinkMsgs.size() + 
				 " msgs in pending vector");

		    int maxBytes = 0;
		    try {
			maxBytes = maxDownlinkMsgBytes();
		    }
		    catch (Exception e) {
			_log4j.error("exception from maxDownlinkMsgBytes()?");
			break;
		    }

		    if ((nAggregateBytes + nMsgBytes + 
			 ShoreMessagingHelper.MSG_DELIMITER.length) > maxBytes) {
			// Adding this message to aggregate would exceed maximum 
			// aggregate size, so we are done aggregating.
			// Put message back on queue
			_downlinkMsgQ.pushFront(msg);
			_log4j.debug("filled aggregate buffer with " + 
				     nAggregateBytes + " bytes");
			break;
		    }

		    // Append this message to the aggregate
		    for (int i = 0; i < nMsgBytes; i++) {
			_aggregateMsg[nAggregateBytes+i] = msgBody[i];
		    }
		    nAggregateBytes += nMsgBytes;
		    nAggregatedMsgs++;

		    if (msg.solitaryDownlink()) {
			// This should be the only message in the downlink; 
			// don't append delimiter, just break out of loop
			_log4j.debug("Solitary msg; break out of aggregation loop");
			break;
		    }

		    // Append message delimiter
		    for (int i = 0; i < ShoreMessagingHelper.MSG_DELIMITER.length; 
			 i++) {
			_aggregateMsg[nAggregateBytes+i] = 
			    ShoreMessagingHelper.MSG_DELIMITER[i];
		    }
		    nAggregateBytes += ShoreMessagingHelper.MSG_DELIMITER.length;
		}

		try {
		    downlinkMsg(serialPort, _aggregateMsg, nAggregateBytes,
				inStream, outStream);

		    for (int i = 0; i < pendingDownlinkMsgs.size(); i++) {

			_log4j.info("publish DOWNLINK_SUCCEEDED here");
		    }

		    nConsecFailed = 0;
		}
		catch (Exception e) {
		    _log4j.error("downlinkMsg() failed: " + e);
		    _log4j.info("publish DOWNLINK_FAILED here");

		    // Couldn't downlink msg - put it back in the queue
		    _log4j.error(e);
		    if (e.getMessage().equals(INCORRECT_MSG_SIZE)) {
			// Incorrect message size - don't retry
			_log4j.error("Discarding message - too many (" + 
				     nAggregateBytes + ") bytes?");
		    }
		    else {
			_log4j.debug("need to re-queue " + 
				     pendingDownlinkMsgs.size() + " messages");

			for (int i = pendingDownlinkMsgs.size(); i > 0; i--) {
			    _log4j.info("Re-queuing message # " + (i-1));
			    _downlinkMsgQ.pushFront(pendingDownlinkMsgs.elementAt(i-1));
			    _log4j.debug("Done re-queuing message");
			}

			nConsecFailed++;
		    }
		}
	    }

	    if (_connectionTimer.timedOut()) {
		_log4j.warn("connect() timed out");
	    }

	    _log4j.debug("Done trying to downlink msgs");

	    try {
		// Put back into default profile, so other applications 
		// can use modem
		restoreModem(outStream);
	    }
	    catch (Exception e) {
		throw new ShoreMessagingHelper.ConnectFailed("restoreModem() failed");
	    }
	}
	catch (ShoreMessagingHelper.ConnectFailed e) {
	    throw e;
	}
	finally {
	    // Free stream resources
	    try {
		if (inStream != null) {
		    inStream.close();
		}
		if (outStream != null) {
		    outStream.close();
		}
	    }
	    catch (Exception e) {
		_log4j.error("Exception closing streams: ", e);
	    }

	    // Always close serial port before returning, so other processes
	    // can use port
	    if (serialPort != null) {
		serialPort.close();
	    }
	}
    }

    /** ConnectionTimer keeps track of time elapsed since connection started */
    protected class ConnectionTimer {

	ConnectionTimer() {
	    reset(0);
	}

	/** Reset the connection timer */
	void reset(long timeoutSec) {
	    _timeoutSec = timeoutSec;
	    _startEpochSec = System.currentTimeMillis() / 1000;
	}

	/** Return true if timed out */
	boolean timedOut() {
	    long now = System.currentTimeMillis() / 1000;
	    if (now - _startEpochSec > _timeoutSec) {
		return true;
	    }
	    else {
		return false;
	    }
	}

	private long _timeoutSec;
	private long _startEpochSec;
    }


    protected ConnectionTimer _connectionTimer = new ConnectionTimer();

    /** Downlink a message */
    protected void downlinkMsg(SerialPort serialPort, 
			       byte[] msg, int nMsgBytes,
			       InputStream inStream,
			       DataOutputStream outStream) 
	throws Exception {

	_log4j.info("\nDownlink " + nMsgBytes + " bytes");
	_log4j.info("msg: " + new String(msg, 0, nMsgBytes));

	byte[] buf = new byte[100];

	// Wait a bit then clear the input buffer
	sleep(2);

	inStream.skip(inStream.available());

	try {
	    _log4j.info("Signal quality: " + 
			receivedSignalStrength(serialPort, 
					       inStream, outStream));
	}
	catch (Exception e) {
	    _log4j.error("Couldn't get received signal strength");
	}


	short checksum = 0;
	for (int i = 0; i < nMsgBytes; i++) {
	    checksum += msg[i];
	}

	_log4j.debug("write AT+SBDWB=" + nMsgBytes +"\r");
	outStream.write(("AT+SBDWB=" + nMsgBytes +"\r").getBytes());
	_log4j.debug("look for READY response");
	StreamUtils.skipUntil(inStream, "\r\n".getBytes(), 5000);

	int nBytes;
	String response = null;
	if ((nBytes = StreamUtils.readUntil(inStream,
					    buf, "\r\n".getBytes(), 
					    5000)) > 0) {
 
	    response = new String(buf, 0, nBytes);
	    if (response.indexOf("READY") >= 0) {
		_log4j.debug("Writing message: \"" + 
			     new String(msg, 0, nMsgBytes) + 
			     "\"");
		outStream.write(msg, 0, nMsgBytes);
		outStream.writeShort(checksum);
		outStream.flush();
	    }
	    else {

		throw new Exception("Didn't get \"READY\" response; got " +
				    response);
	    }
	}

	// Read response
	_log4j.debug("OK, look for response that msg was written");
	// StreamUtils.skipUntil(inStream, "\r\n".getBytes(), 5000);
	if ((nBytes = StreamUtils.readUntil(inStream, 
					    buf,
					    "OK".getBytes(), 
					    10000)) > 0) {

	    response = (new String(buf, 0, nBytes)).trim();
	    _log4j.debug("Got response: " + response);
	    if (response.indexOf("0") >= 0) {
		_log4j.info("Wrote message to modem downlink buffer");
	    } 
	    else {
		int code = Integer.parseInt(response);

		switch (code) {
		case 1:
		    throw new Exception("SBD timeout");

		case 2:
		    throw new Exception("SBD checksum mismatch");

		case 3:
		    throw new Exception("SBD " + INCORRECT_MSG_SIZE);

		default:
		    throw new Exception("Error; modem responded with " +
					response);
		}
	    }
	}
	else if (nBytes == -1) {
	    _log4j.warn("unable to read response to SBDWB command");
	    throw new Exception("unable to read response");
	}

	int timeoutMsec = 60000;
	_log4j.info("Get satellite connection...");
	outStream.write("AT+SBDI\r\n".getBytes());
	StreamUtils.skipUntil(inStream, "\r\n".getBytes(), timeoutMsec);
	_log4j.debug("read SBDI response");

	if ((nBytes = StreamUtils.readUntil(inStream, buf, 
					    "OK\r\n".getBytes(),  
					    timeoutMsec)) > 0) {

	    _log4j.debug("got some response from readUntil()");
	    response = (new String(buf, 0, nBytes)).trim();

	    int index;
	    if ((index = response.indexOf("+SBDI:")) >= 0) {

		SbdiResponse sbdiResponse = new SbdiResponse(response);

		if (sbdiResponse._uplinkStatus == 1) {
		    // Got an uplink - retrieve from modem and place 
		    // in uplink queue
		    try {
			retrieveAndQueueUplink(sbdiResponse._nUplinkBytes);
		    }
		    catch (Exception e) {
			_log4j.error("retrieveAndQueueUplink() failed: "
				     + e);
		    }
		}

		_log4j.info("MOMSN: " + sbdiResponse._downlinkSeqNo);


		if (sbdiResponse._downlinkStatus != 0 && 
		    sbdiResponse._downlinkStatus != 1) {

		    throw new Exception("Transmission failed with " + 
					"+SBDI error code " + 
					sbdiResponse._downlinkStatus);
		}
		_log4j.info("Transmission successful");
	    }
	}
	else {
	    _log4j.error("Didn't get +SBDI response; got " + response);
	    throw new Exception("No +SBDI response");
	}
    }



    /** Get uplinked message(s) through modem and add to uplink queue */
    protected int getUplinkMsgs(SerialPort serialPort,
				InputStream inStream,
				OutputStream outStream)
	throws Exception {

	byte[] buf = new byte[256];

	int nUplinks = 0;

	int timeoutMsec = 60000;

	// Clear downlink buffer (avoid duplicate downlinks...)
	_log4j.debug("clear downlink buffer to avoid duplicate downlinks...");
	outStream.write("AT+SBDD0\r".getBytes());
	StreamUtils.skipUntil(inStream, "OK\r\n".getBytes(), timeoutMsec);

	int consecFailed = 0;

	// Get all uplink currently messages queued on shore
	boolean done = false;
	while (!done && (consecFailed <= MAX_CONSEC_UPLINK_FAILED) && 
	       !_connectionTimer.timedOut()) {

	    _log4j.info("Check for uplinked messages...");
	    _log4j.info("Signal quality: " + 
			receivedSignalStrength(serialPort,
					       inStream, outStream));
	    outStream.write("AT+SBDI\r\n".getBytes());
	    StreamUtils.skipUntil(inStream, "\r\n".getBytes(), timeoutMsec);
	    _log4j.debug("read SBDI response");

	    int downlinkStatus, downlinkSeqNo;
	    int uplinkStatus, uplinkSeqNo;
	    int nUplinkBytes;

	    int nBytes;
	    if ((nBytes = StreamUtils.readUntil(inStream, 
						buf, "OK\r\n".getBytes(),  
						timeoutMsec)) > 0) {

		String response = (new String(buf, 0, nBytes)).trim();

		_log4j.debug("got some response from readUntil():\n" + 
			     response);

		int index;
		if ((index = response.indexOf("+SBDI:")) >= 0) {

		    SbdiResponse sbdiResponse = new SbdiResponse(response);
		    if (sbdiResponse._uplinkStatus == 1) {

			try {
			    retrieveAndQueueUplink(sbdiResponse._nUplinkBytes);
			    nUplinks++;
			}
			catch (Exception e) {
			    _log4j.error("retrieveAndQueueUplink() failed: " +
					 e);
			    consecFailed++;
			}
		    }

		    _log4j.info("There are " + _nPendingUplinks + 
				" pending uplinks at Iridium GSS"); 

		    if (_nPendingUplinks == 0) {
			// No more pending uplinks - all done
			done = true;
		    }

		    if (nUplinks >= MAX_UPLINKS_PER_SESSION) {
			_log4j.warn("Exceeded " + MAX_UPLINKS_PER_SESSION + 
				    " uplinks in this session - done for now");
			done = true;
		    }
		}
		else {
		    throw new Exception("Unexpected response to SBDI command");
		}
	    }
	    else {
		throw new Exception("No response to SBDI command");
	    }

	}

	if (consecFailed > MAX_CONSEC_UPLINK_FAILED) {
    
	    throw new Exception("Exceeded " + MAX_CONSEC_UPLINK_FAILED +
				" consecutive uplink attempts; done for now");
	}

	return nUplinks;
    }


    /** Retrieve uplink message from modem and queue it for clients */
    protected void retrieveAndQueueUplink(int nUplinkBytes) 
	throws Exception {
    }

    /** Initialize serial port */
    protected SerialPort openSerialPort(int baud) 
	throws PortInUseException, UnsupportedCommOperationException,
	       Exception {

	_log4j.info("Opening serial port at " + baud + " baud");

	SerialPort serialPort = null;

	_log4j.debug("openSerialPort() - get port");

	serialPort = 
	    (SerialPort) _commPortId.open(this.getClass().getName(), 1000);

	_log4j.debug("openSerialPort() - got port: " + serialPort);

	_log4j.debug("openSerialPort() - set serial params");

	try {
	    serialPort.setSerialPortParams(baud,
					   serialPort.getDataBits(), 
					   serialPort.getStopBits(), 
					   serialPort.getParity());
	}
	catch (Exception e) {
	    serialPort.close();
	    throw e;
	}

	return serialPort;
    }

    /** Sleep for specified number of seconds */
    protected void sleep(int sleepSec) {
	try {
	    Thread.sleep(sleepSec * 1000);
	}
	catch (Exception e) {
	    _log4j.error("sleep() - got exception: ", e);
	}
    }


    class SbdiResponse {

	int _downlinkStatus = -1;
	int _downlinkSeqNo = -1;
	int _uplinkStatus = -1;
	int _uplinkSeqNo = -1;
	int _nUplinkBytes = -1;
	int _nPendingUplinks = -1;

	SbdiResponse(String response) throws Exception {
	    parse(response);
	}

	void parse(String response) throws Exception {
	    StringTokenizer tokenizer = 
		new StringTokenizer(response, ", ");

	    for (int nToken = 0; tokenizer.hasMoreTokens(); nToken++) {
		String token = tokenizer.nextToken();
		    
		if (nToken == 0) {
		    // Skip the "+SBDI:" token
		    continue;
		}

		int value = 0;
		try {
		    value = Integer.parseInt(token);
		}
		catch (Exception e) {
		    throw new Exception("Error parsing SBDI response integer: " + 
					token);
		}

		switch (nToken) {
			
		case 1:
		    _downlinkStatus = value;
		    break;

		case 2:
		    _downlinkSeqNo = value;
		    break;

		case 3:
		    _uplinkStatus = value;
		    break;
			
		case 4:
		    _uplinkSeqNo = value;
		    break;

		case 5:
		    _nUplinkBytes = value;
		    break;

		case 6:
		    _nPendingUplinks = value;
		    break;

		default:
		    _log4j.error("Got " + nToken + 
				 "tokens in SBDI response");
		}

	    }
	}
    }
}
