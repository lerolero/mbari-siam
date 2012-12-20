/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

import java.io.Serializable;

/**
 * CommsMode specifies the commumincations mode to be applied to a 
 * device's communication channel.
 */
public class CommsMode implements Mnemonic, Serializable {

	int _value;

	/**
	 * Correlation between CommsMode and internal value is kept private.
	 */
	private CommsMode(int value) {
		_value = value;
	}

	/**
	 * Public no-argument constructor (needed to be able to parse from
	 * properties file with Reflection...)
	 *  
	 */
	public CommsMode() {
		_value = 3;
	}

	/** Device is self-powered, no need to apply power */
	public static CommsMode RS232 = new CommsMode(0);

	/** Apply power when accessing device */
	public static CommsMode RS485 = new CommsMode(1);

	/** Apply power at startup and leave it on */
	public static CommsMode RS422 = new CommsMode(2);

	/**
	 * This object is necessary so that we can invoke methods of the Mnemonic
	 * interface on it.
	 */
	public static CommsMode UNDEFINED = new CommsMode(3);

	/** Generate mnemonic string for this power policy. */
	public String toString() {

		String retVal = null;

		switch (_value) {
		case 0:
			retVal = "RS232";
			break;
		case 1:
			retVal = "RS485";
			break;
		case 2:
			retVal = "RS422";
			break;
		default:
			retVal = "UNKNOWN";
		}
		return retVal;
	}

	/** Return array of valid string values. */
	public String[] validValues() {
		String[] values = new String[] {"RS232", "RS485", "RS422"};
		return values;
	}
	
	/** Generate CommsMode object from mnemonic string. */
	public Object fromString(String string) throws InvalidPropertyException {

	    if (string.equalsIgnoreCase("RS232")) {
		return RS232;
	    } else if (string.equalsIgnoreCase("RS485")) {
		return RS485;
	    } else if (string.equalsIgnoreCase("RS422")) {
			return RS422;
	    } else {
		throw new InvalidPropertyException(string);
	    }
	}
}
