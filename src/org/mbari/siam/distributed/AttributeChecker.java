/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

/**
 * AttributeChecker has callbacks that are invoked as attributes are parsed.
 */
public interface AttributeChecker {

	/**
	 * Called when specified attribute was not found. Throw
	 * MissingPropertyException if specified attribute is mandatory.
	 * 
	 * @param attributeName
	 *            name of missing attribute
	 */
	public void missingAttributeCallback(String attributeName)
			throws MissingPropertyException;

	/**
	 * Called when specified attribute has been found. Throw
	 * InvalidPropertyException if specified attribute has invalid value.
	 * 
	 * @param attributeName
	 *            name of parsed attribute
	 */
	public void setFieldCallback(String attributeName, String valueString)
			throws InvalidPropertyException;

	/**
	 * Called when all attributes have been parsed. Throw
	 * InvalidPropertyException if any invalid attribute values found
	 */
	public void checkXXXXValues() throws InvalidPropertyException;
}