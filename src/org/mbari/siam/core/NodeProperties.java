/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.mbari.siam.distributed.CommsMode;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.PowerPort;

/**
   NodeProperties is responsible for parsing the Node properties file
   (typically siamPort.cfg) in order to configure the Node.
 */
public class NodeProperties extends SiamProperties {

    /** log4j logger */
    static Logger _log4j = Logger.getLogger(NodeProperties.class);

    public static final String NODEID_KEY = "nodeID";
    public static final String PORTNAMES_KEY = "platformSerialPorts";
    public static final String SERVICEJAR_DIRECTORY_KEY = "jarLocation";
    public static final String DEVICELOG_DIRECTORY_KEY = "logLocation";
    public static final String CODEBASE_DIRECTORY_KEY = "codeBaseLocation";
    public static final String ADVERTISE_SERVICE_KEY = "advertiseService";
    public static final String USE_PUCK_PAYLOAD_KEY = "usePuckPayload";
    public static final String COMMS_MODE_KEY  = "commsMode";
    public static final String HTTP_PORT_KEY = "httpPort";
    public static final String SERIAL_PORT_KEY = "serialPort";
    public static final String POWER_PORT_KEY  = "powerPort";
    public static final String SERVICE_JAR_KEY = "serviceJar";
    public static final String PORTNAME_KEY = "portName";
    public static final String MAX_PORTS_KEY = "maxPorts";
    public static final String DATA_SHELF_LIFE = "dataShelfLifeHours";
    public static final String REASSERT_COMMLINK = "CommsManager.reassert";
    public static final String MAX_REASSERT_COMMLINK_TRIES = "CommsManager.maxReassertTries";
    public static final String NVT_SERIAL_PORT_KEY = "NVTserialPort";
    public static final String MISC_PORT_KEY = "miscPort";
    public static final String PORT_ALIAS_KEY = "portAlias";

    /** maximum number of ports to check for in siamPort.cfg */
    private static final int MAX_NODE_PORTS = 100;

    /** Hashmap of port aliases */
    HashMap _portAliases = new HashMap();
    HashMap _reversePortAliases = new HashMap();

    public static final String NODE_DIAGNOSTIC_INTERVAL_KEY = 
	"diagnosticIntervalSeconds";

    /** Get names of node serial ports. */
    public String getSerialPortNames() 
	throws MissingPropertyException, InvalidPropertyException {
	return getRequiredProperty(PORTNAMES_KEY);
    }

    /** Get name of service jar directory. */
    public String getServiceJarDirectory() 
	throws MissingPropertyException, InvalidPropertyException {
	return getRequiredProperty(SERVICEJAR_DIRECTORY_KEY);
    }

    /** Get name of sensor log directory. */
    public String getDeviceLogDirectory() 
	throws MissingPropertyException, InvalidPropertyException {
	return getRequiredProperty(DEVICELOG_DIRECTORY_KEY);
    }

    /** Get name of codebase directory. */
    public String getCodebaseDirectory() 
	throws MissingPropertyException, InvalidPropertyException {
	return getRequiredProperty(CODEBASE_DIRECTORY_KEY);
    }

    /** Get node diagnostic interval (seconds). */
    public int getDiagnosticInterval() 
	throws InvalidPropertyException {
	int value = 0;
	try {
	    value = getIntegerProperty(NODE_DIAGNOSTIC_INTERVAL_KEY);
	}
	catch (MissingPropertyException e) {
	    return 3600;
	}

	if (value < 10) {
	    throw new InvalidPropertyException("Invalid " + 
					       NODE_DIAGNOSTIC_INTERVAL_KEY + 
					       ": must be 10 or greater");
	}

	return value;
    }

    /** Get node diagnostic interval (seconds). */
    public int getMaxPorts()
    {
	return(getIntegerProperty(MAX_PORTS_KEY, MAX_NODE_PORTS));
    }

    /** getPort() is responsible for parsing siamPort.cfg and creating
	the DevicePorts specified there.
	@param index - the index number of the DevicePort.  The entries in
	siamPort.cfg should end with this index number.
    */
    public DevicePort getPort(int index) 
	throws MissingPropertyException {

	PowerPort pwrPort = getPowerPort(POWER_PORT_KEY + index);

	// Look for "portAliasN = "
	String portAlias = getProperty(PORT_ALIAS_KEY + index);
	_log4j.debug("port #" + index + " alias: " + portAlias);

	String jarName = getProperty((SERVICE_JAR_KEY + index));
	if (jarName == null) {
	    jarName = "";
	}

	String commsMode = getProperty((COMMS_MODE_KEY + index));
	CommsMode cm=CommsMode.RS232;
	if (commsMode != null) {
	    try{
		cm=(CommsMode)cm.fromString(commsMode);
	    }catch(InvalidPropertyException e){
		_log4j.warn(e);
	    }
	}

	// Look for "httpPortN ="
	String url = getProperty(HTTP_PORT_KEY + index);
	if (url != null && url.length() > 0) {
	    if (portAlias != null) {
		_log4j.debug("set alias for " + url + " = " + portAlias);
		_portAliases.put(url, portAlias);
		_reversePortAliases.put(portAlias, url);
	    }
	    return getHttpDevicePort(index, url, pwrPort, jarName);
	}


	// Look for "serialPortN = "
	String serialName = getProperty((SERIAL_PORT_KEY + index));

	if ((serialName != null) && (serialName.length() > 0)) {
	    if (portAlias != null) {
		_portAliases.put(serialName, portAlias);
		_reversePortAliases.put(portAlias, serialName);
	    }
	    return(getSerialDevicePort(index, serialName, pwrPort, 
				       jarName, cm));
	}

	// Look for "NVTserialPortN = "
	serialName = getProperty((NVT_SERIAL_PORT_KEY + index));

	if ((serialName != null) && (serialName.length() > 0)) {
	    if (portAlias != null) {
		_portAliases.put(serialName, portAlias);
		_reversePortAliases.put(portAlias, serialName);
	    }
	    _log4j.debug("Try to create NVTDevicePort for " + serialName);
	    return(getNVTDevicePort(index, serialName, pwrPort, jarName, cm));
	}

	String miscPort = getProperty((MISC_PORT_KEY + index));
	if (miscPort != null)
	{
	    String portName = getProperty((PORTNAME_KEY + index), miscPort);
	    if (portAlias != null) {
		_portAliases.put(portName, portAlias);
		_reversePortAliases.put(portAlias, portName);
	    }
	    return(new MiscDevicePort(index, portName, pwrPort, jarName, 
				      null, miscPort));
	}


	// If none of the above work, check for platform-specific syntax
	DevicePort port = getPlatformPort(index, pwrPort, jarName, cm);
	if (port != null) {
	    if (portAlias != null) { 
		_portAliases.put(portAlias, port.getPortName());
	    }
	    return(port);
	}

	// If still no match, throw exception
	throw new MissingPropertyException("No port " + index);

    }


    /** Look for the power port.  This implementation
	just returns a NullPowerPort.  Should be overridden by the 
	platform-specific subclass of NodeProperties.
	@param key - Property key for the PowerPort.
    */
    public PowerPort getPowerPort(String key) throws MissingPropertyException
    {
	return(new NullPowerPort());
    }



    /** Create HttpDevicePort */
    protected HttpDevicePort getHttpDevicePort(int index, String url,
					       PowerPort powerPort, 
					       String jarName) {

	return new HttpDevicePort(index, url, powerPort, jarName, null);
    }

					       
    /** Create a SerialDevicePort (or subclass).
	May be overridden by platform-specific subclass of NodeProperties.
    */
    protected SerialDevicePort getSerialDevicePort(int index, String portName,
						   PowerPort powerPort, 
						   String jarName,
						   CommsMode commsMode)
    {
	return(new SerialDevicePort(index, portName, powerPort, jarName, 
				    null, commsMode));
    }


    /** Create an NVTDevicePort.
    */
    protected SerialDevicePort getNVTDevicePort(int index, String portId,
						PowerPort powerPort, 
						String jarName,
						CommsMode commsMode)
    {
	String portName = getProperty((PORTNAME_KEY + index), portId);
	return(new NVTDevicePort(index, portId, portName, powerPort, 
				 jarName, null, commsMode));
    }


    /** Look for platform-specific DevicePorts.  Called
	after base class didn't find standard device ports.  
	Default implementation
	just returns throws a MissingPropertyException.
	@param index - the index number of the DevicePort.
    */
    protected DevicePort getPlatformPort(int index, PowerPort powerPort,
					 String jar, CommsMode commsMode)
	throws MissingPropertyException
    {
	throw new MissingPropertyException("No port " + index);
    }


    /** Get Node ID. */
    public long getNodeID() 
	throws MissingPropertyException, InvalidPropertyException {
	String idString = getRequiredProperty(NODEID_KEY);
	long id;
	try{
	    id = Long.parseLong(idString);
	    if(id<0L) {
		throw new InvalidPropertyException("getNodeID:Invalid Node ID; must be > 0");
	    }
	}catch(NumberFormatException e){
		throw new InvalidPropertyException("getNodeID:Invalid Node ID; must be long, > 0");
	}
	return id; 
    }

    /** Get data shelf-life (default is unlimited shelf-life) */
    public float getDataShelfLifeHours() throws InvalidPropertyException {
	String string = getProperty(DATA_SHELF_LIFE, "-1");

	try {
	    return Float.parseFloat(string);
	}
	catch(NumberFormatException e){
	    throw new InvalidPropertyException("Invalid " + DATA_SHELF_LIFE + ": must be floating point");	
	}
    }


    /** Return true (default) if node should reassert prematurely broken comms link */
    public boolean reassertCommsLink() throws InvalidPropertyException {

	Boolean value = Boolean.valueOf(getProperty(REASSERT_COMMLINK, "true"));
	return value.booleanValue();
    }


    /** Return true (default) if node should reassert prematurely broken comms link */
    public int maxReassertCommLinkTries() throws InvalidPropertyException {
	boolean error = false;
	int maxTries = 25;
	try {
	    maxTries = Integer.parseInt(getProperty(MAX_REASSERT_COMMLINK_TRIES, "25"));
	    if (maxTries < 0) {
		error = true; 
	    }
	}
	catch (NumberFormatException e) {
	    error = true;
	}
	if (error) {
	    throw new InvalidPropertyException("Invalid " + MAX_REASSERT_COMMLINK_TRIES + 
					       ": must be non-negative integer");
	}
	return maxTries;
    }
}

