// Copyright MBARI 2003
package org.mbari.siam.operations.utils;

import java.rmi.RemoteException;
import java.util.Date;
import java.text.DateFormat;
import java.rmi.Naming;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.Port;
import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.Instrument;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.DeviceNotFound;
import org.mbari.siam.utils.PrintfFormat;


public class ListNodePorts {

    static final int PORTNAME_WIDTH = 13; // was 18
    static final int SERVICENAME_WIDTH = 37; // was 43
    static final int SERVICESTATUS_WIDTH = 9; 
    static final int ID_WIDTH = 8;
    static final int SERVICEINTEGRITY_WIDTH = 12;

    private boolean _showStats = false;

    public ListNodePorts(boolean showStats){
        _showStats = showStats;
    }

    /** Return mnemonic string for specified status. */
    public static String statusMnem(int status) {
	switch (status) {
	case Device.OK:
	    return "OK";

	case Device.ERROR:
	    return "ERROR";

	case Device.INITIAL:
	    return "INIT";

	case Device.SHUTDOWN:
	    return "SHUTDOWN";

	case Device.SUSPEND:
	    return "SUSPEND";

	case Device.SAMPLING:
	    return "SAMPLING";

	case Device.UNKNOWN:
	default:
	    return "UNKNOWN!";
	}
    }


    /** Append a field to a line. */
    void appendField(StringBuffer line, String fieldValue, int fieldWidth) {
	line.append(fieldValue);
	int nBlanks = fieldWidth - fieldValue.length();
	for (int i = 0; i < nBlanks; i++) {
	    line.append(' ');
	}
    }

    /** Return header text. */
    public String header() {
	StringBuffer lineBuffer = new StringBuffer(90);

	appendField(lineBuffer, "Port name", PORTNAME_WIDTH);
	appendField(lineBuffer, "Service", SERVICENAME_WIDTH);
	appendField(lineBuffer, "ISI-ID", ID_WIDTH);
	appendField(lineBuffer, "Status", SERVICESTATUS_WIDTH);
        if(_showStats){
            appendField(lineBuffer, " S  : E : R ", SERVICEINTEGRITY_WIDTH);
        }

	StringBuffer lineBuffer2 = new StringBuffer(90);

	appendField(lineBuffer2, "---------", PORTNAME_WIDTH);
	appendField(lineBuffer2, "-------", SERVICENAME_WIDTH);
	appendField(lineBuffer2, "------", ID_WIDTH);
	appendField(lineBuffer2, "------", SERVICESTATUS_WIDTH);
        
        if(_showStats){
            appendField(lineBuffer2, "-----------", SERVICEINTEGRITY_WIDTH);
        }
	return new String(lineBuffer + "\n" + lineBuffer2);
    }


    /** Return line for table. */
    public StringBuffer listPort(Node node, Port port) 
	throws RemoteException {

	StringBuffer lineBuffer = new StringBuffer(90);

	String portName = new String(port.getName());
	appendField(lineBuffer, portName, PORTNAME_WIDTH);

	try {
	    String serviceName = new String(port.getServiceMnemonic());

	    appendField(lineBuffer, serviceName, SERVICENAME_WIDTH);

	    appendField(lineBuffer, Long.toString(port.getDeviceID()),
			ID_WIDTH);

	    Device device = node.getDevice(port.getDeviceID());

	    appendField(lineBuffer, statusMnem(device.getStatus()),
			SERVICESTATUS_WIDTH);

            if(_showStats){
                appendField(lineBuffer, new PrintfFormat("%4i").sprintf(device.getSamplingCount()) + " " +
                                    new PrintfFormat("%3i").sprintf(device.getSamplingErrorCount()) + " " +
                                    new PrintfFormat("%3i").sprintf(device.getSamplingRetryCount()),
                                    SERVICEINTEGRITY_WIDTH);
            }
	}
	catch (DeviceNotFound e) {
	    appendField(lineBuffer, "-", SERVICENAME_WIDTH);
	    appendField(lineBuffer, "-", ID_WIDTH);
	    appendField(lineBuffer, "-", SERVICESTATUS_WIDTH);
            appendField(lineBuffer, "-", SERVICEINTEGRITY_WIDTH);
	}

	return lineBuffer;
    }



    public static void main(String[] args) {

	ListNodePorts lister;

	Node nodeService = null;
	Port[] ports = null;
        boolean showStats = false;

        //need to setSecurityManager 
        if ( System.getSecurityManager() == null )
            System.setSecurityManager(new SecurityManager());

	if (args.length < 1) {
	    System.err.println("Usage: nodeURL");
	    System.exit(1);
	}

        String nodeURL = NodeUtility.getNodeURL(args[0]);

        for(int i=1;i<args.length;i++){
            if(args[i].equals("-stats"))
               showStats = true;
        }
        
        lister = new ListNodePorts(showStats);

	try {
	    System.out.println("Looking for node service at " + nodeURL);

	    nodeService = (Node )Naming.lookup(nodeURL);

	    System.out.println("Got proxy for node service \"" + 
			       new String(nodeService.getName()) + "\"");

	}
	catch (Exception e) {
	    System.err.println("Caught exception: " + e.getMessage());
	    System.err.println("Couldn't get service at \"" + nodeURL + "\"");
	    System.exit(1);
	}

	try {
	    ports = nodeService.getPorts();
	    
	    System.out.println("Node has " + ports.length + " ports\n");

	    System.out.println(lister.header());

	    for (int i = 0; i < ports.length; i++) {

		System.out.println(lister.listPort(nodeService, ports[i]));
	    }
	    System.out.println("");
	}
	catch (RemoteException e) {
	    System.err.println("RemoteException: " + e.getMessage());
	}
	catch (Exception e) {
	    System.err.println("Got some exception: " + e.getMessage());
	    System.exit(1);
	}

	System.exit(0);
    }
}
