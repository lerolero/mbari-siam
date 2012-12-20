// Copyright MBARI 2003
package org.mbari.siam.operations.utils;

import java.rmi.RemoteException;
import java.util.Date;
import java.text.DateFormat;
import java.rmi.Naming;
import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.PowerSwitch;
import org.mbari.siam.distributed.DeviceNotFound;


public class ListNodeSwitches extends NodeUtility {

    static final int SWITCHNAME_WIDTH = 18;
    static final int ID_WIDTH = 20;

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

	appendField(lineBuffer, "Switch name", SWITCHNAME_WIDTH);
	appendField(lineBuffer, "Attached ISI-ID", ID_WIDTH);

	StringBuffer lineBuffer2 = new StringBuffer(90);

	appendField(lineBuffer2, "---------", SWITCHNAME_WIDTH);
	appendField(lineBuffer2, "------", ID_WIDTH);

	return new String(lineBuffer + "\n" + lineBuffer2);
    }


    /** Print each power switch description */
    public void processNode(Node node) 
	throws Exception {

	PowerSwitch[] powerSwitch = node.getPowerSwitches();
	    
	System.out.println("Node has " + powerSwitch.length + 
			   " power switches\n");

	System.out.println(header());

	for (int i = 0; i < powerSwitch.length; i++) {


	    StringBuffer lineBuffer = new StringBuffer(90);

	    String switchName = new String(powerSwitch[i].getName());
	    appendField(lineBuffer, switchName, SWITCHNAME_WIDTH);

	    try {
		appendField(lineBuffer, 
			    Long.toString(powerSwitch[i].getSwitchedDeviceID()),
			    ID_WIDTH);
	    }
	    catch (DeviceNotFound e) {
		appendField(lineBuffer, "-", ID_WIDTH);
	    }

	    System.out.println(lineBuffer);
	}
    }



    public static void main(String[] args) {

	ListNodeSwitches lister = new ListNodeSwitches();
	lister.processArguments(args);
	lister.run();
    }


    /** Print usage message. */
    public void printUsage() {
	System.err.println("Usage: ListNodeSwitches nodeURL");
    }

    /** No custom options. */
    public void processCustomOption(String[] args, int index) 
	throws InvalidOption {
	throw new InvalidOption("Invalid option: " + args[index]);
    }
}
