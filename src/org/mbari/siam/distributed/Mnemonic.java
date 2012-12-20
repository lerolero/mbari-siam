/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

/**
 * A Mnemonic has "mnemonic string representations that can be converted to an
 * instance of the object.
 *  
 */
public interface Mnemonic extends Importable {

	/** Return array of valid string values. */
	public String[] validValues();
}
