/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

import java.io.Serializable;

/**
 * PowerPolicy specifies how power should be applied to a device or to the
 * device's communication channel.
 */
public class PowerPolicy implements Mnemonic, Serializable {

	int _value;

	/**
	 * Correlation between PowerPolicy and internal value is kept private.
	 */
	private PowerPolicy(int value) {
		_value = value;
	}

	/**
	 * Public no-argument constructor (needed to be able to parse from
	 * properties file with Reflection...)
	 *  
	 */
	public PowerPolicy() {
		_value = 3;
	}

	/** Device is self-powered, no need to apply power */
	public static PowerPolicy NEVER = new PowerPolicy(0);

	/** Apply power when accessing device */
	public static PowerPolicy WHEN_SAMPLING = new PowerPolicy(1);

	/** Apply power at startup and leave it on */
	public static PowerPolicy ALWAYS = new PowerPolicy(2);

	/**
	 * This object is necessary so that we can invoke methods of the Mnemonic
	 * interface on it.
	 */
	public static PowerPolicy UNDEFINED = new PowerPolicy(3);

	/** Generate mnemonic string for this power policy. */
	public String toString() {

		String retVal = null;

		switch (_value) {

		case 0:
			retVal = "NEVER";
			break;

		case 1:
			retVal = "SAMPLING";
			break;

		case 2:
			retVal = "ALWAYS";
			break;

		default:
			retVal = "UNKNOWN";
		}
		return retVal;
	}

	/** Return array of valid string values. */
	public String[] validValues() {
		String[] values = new String[] {"NEVER", "SAMPLING", "ALWAYS"};
		return values;
	}
	
	/** Generate PowerPolicy object from mnemonic string. */
	public Object fromString(String string) throws InvalidPropertyException {

		if (string.equalsIgnoreCase("ALWAYS")) {
			return ALWAYS;
		} else if (string.equalsIgnoreCase("SAMPLING")) {
			return WHEN_SAMPLING;
		} else if (string.equalsIgnoreCase("NEVER")) {
			return NEVER;
		} else {
			throw new InvalidPropertyException(string);
		}
	}
}