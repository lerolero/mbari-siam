/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils.isiam;

/**
 * Interface for the commands that iSiam can run.
 * 
 * @author carueda
 */
interface ICommand {

	/**
	 * A short name to identify the command
	 */
	public String getName();

	/**
	 * Runs the command
	 */
	public void run(String[] args) throws Exception;

	/**
	 * Upon a successful completion of run, this will be called by iSiam 
	 * to update an internal list of 'nodeUrl' arguments.
	 */
	public String getNodeUrlArgument();

	/**
	 * Upon a successful completion of run, this will be called by iSiam 
	 * to update an internal list of 'portName' arguments.
	 */
	public String getPortNameArgument();
}
