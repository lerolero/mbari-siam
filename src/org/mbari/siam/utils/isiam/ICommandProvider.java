/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils.isiam;

import java.util.List;

/**
 * Interface for providers of commands and associated information.
 * 
 * @author carueda
 */
public interface ICommandProvider {

	/**
	 * Returns the list of ICommands provided by this provider.
	 */
	public List getCommands();

}
