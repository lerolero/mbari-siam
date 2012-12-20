// Copyright 2002 MBARI
package org.mbari.siam.tests.state;

import org.mbari.siam.distributed.DevicePacket;
import org.mbari.siam.distributed.ServiceAttributes;


/**
   MetadataPacket contains information relating to "state" of 
   the generating device.
   @author Tom O'Reilly
*/
public class NewMetadataPacket extends DevicePacket {


    /** Serial version ID */
    private static final long serialVersionUID=0L;

    /** Indicates that packet was generated autonomously (i.e. not from
	any external command. */
    public static final int AUTO_GENERATED = -1;


    /** Indication of what generated the MetadataPacket */
    private byte _cause[];

    /** Static documentation (e.g. XML description). */
    protected byte[] _staticDoc = null;

    /** Status string generated by device.*/
    protected byte[] _deviceStatus = null;

    /** Dynamic service state attributes. */
    protected ServiceAttributes _serviceAttributes = null;



    /**
       @param sourceID unique identifier of source device
       @param cause what triggered metadata packet creation
    */
    public NewMetadataPacket(long sourceID, byte[] cause) {

	super(sourceID);

	_cause = new byte[cause.length];
	System.arraycopy(cause, 0, _cause, 0, cause.length);

    }


    /** Set static doc. 
       @param staticDoc static documentation bytes
    */
    public void setStaticDoc(byte[] staticDoc) {
	_staticDoc = new byte[staticDoc.length];
	System.arraycopy(staticDoc, 0, _staticDoc, 0, staticDoc.length);
    }


    /** Set service state. 
     @param serviceAttributes service state object 
    */
    public void setServiceAttributes(ServiceAttributes serviceAttributes) {
	_serviceAttributes = serviceAttributes;
    }


    /** Set device status. 
	@param deviceStatus status string read from device
     */
    public void setDeviceStatus(byte[] deviceStatus) {
	_deviceStatus = new byte[deviceStatus.length];
	System.arraycopy(deviceStatus, 0, _deviceStatus, 0, 
			 deviceStatus.length);    
    }

    /** Indication of what generated the MetadataPacket */
    public byte[] getCause() {
	return _cause;
    }


    /** Get status bytes. */
    public byte[] getBytes() {
	return toString().getBytes();
    }

    /** Return String representation. */
    public String toString() {


	String retVal = super.toString();
	retVal += ("cause=" + new String(_cause) + "\n");

	if (_staticDoc != null) {
	    retVal += ("staticDoc=" + new String(_staticDoc) + "\n");
	}

	if (_deviceStatus != null) {
	    retVal += ("deviceStatus=" + new String(_deviceStatus) + "\n");
	}

	if (_serviceAttributes != null) {
	    retVal += _serviceAttributes.toString();
	}
	return retVal;
    }
}

