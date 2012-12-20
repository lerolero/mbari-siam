// Copyright 2002 MBARI
package org.mbari.siam.distributed.portal;

import java.rmi.RemoteException;
import org.mbari.siam.distributed.Node;

/**
   QueuedCommands are stored in a Portal, and forwarded to their 
   destination node when the communications link is available. 
   @author Tom O'Reilly
*/
public abstract class QueuedCommand {

    /** Indicates command success. */
    public final static int OK = 0;

    /** Indicates command error. */
    public final static int ERROR = -1;

    // Earliest time to send command. 
    protected long _earliestSendTime;

    // Latest time to send command. */
    protected long _latestSendTime;

    // Time command was actually sent.
    protected long _actualSentTime;

    // Result of command; OKAY or ERROR.
    private int _result;

    // String indicating result of command.
    private String _resultString;

    // Indicates whether result has been set
    private boolean _resultSet = false;

    // E-mail to notify when command has been sent.
    protected String _notifyAddress;

    // Device to which command is addressed
    protected long _deviceID;

    /** Constructor. */
    QueuedCommand(long deviceID,
		  long earliestSendTime, 
		  long latestSendTime, 
		  String notifyAddress) {

	_deviceID = deviceID;
	_earliestSendTime = earliestSendTime;
	_latestSendTime = latestSendTime;
	_notifyAddress = notifyAddress;
    }

    /** Device to which command is addressed. */
    public long deviceID() {
	return _deviceID;
    }

    /** Earliest time to send command. */
    public long earliestSendTime() {
	return _earliestSendTime;
    }

    /** Latest time to send command. */
    public long latestSendTime() {
	return _latestSendTime;
    }

    /** Time command was actually sent. */
    public long actualSentTime() {
	return _actualSentTime;
    }

    /** Set sent time. */
    public void setActualSentTime(long sentTime) {
	_actualSentTime = sentTime;
    }

    /** Result of command; OKAY or ERROR. */
    public int result() {
	return _result;
    }

    /** String indicating result of command. */
    public String resultString() {
	return _resultString;
    }

    /** Email to notify when command has been sent. */
    public String notifyAddress() {
	return _notifyAddress;
    }

    /** String indicating type of QueuedCommand */
    public abstract String name();

    /** Clear result code and string. */
    public void clearResult() {
	_resultString = "";
	_result = OK;
	_resultSet = false;
    }

    /** Set result code and string. */
    public void setResult(int result, String resultString) {
	_result = result;
	_resultString = resultString;
	_resultSet = true;
    }


    /** Indicate whether result has been set. */
    public boolean resultSet() {
	return _resultSet;
    }


    /** Dispatch command to the node. Implementations must
	catch and process any exceptions specific to the subclass,
	and should call the setResult() method.
    */
    public abstract void dispatch(Node node) 
	throws RemoteException;
}
