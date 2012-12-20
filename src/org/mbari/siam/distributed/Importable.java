/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

/**
 * An Importable object is capable of being input from a string...
 *  
 */
public interface Importable {

	/** Generate object from mnemonic string. */
	public Object fromString(String string) throws InvalidPropertyException;

}
