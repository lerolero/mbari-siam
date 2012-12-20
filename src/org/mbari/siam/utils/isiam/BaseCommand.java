/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils.isiam;


/**
 * A convenient base ICommand implementation.
 * 
 * @author carueda
 */
public abstract class BaseCommand implements ICommand {

	protected final String _name;
	
	protected String _nodeUrlArgument;
	protected String _portNameArgument;
	
	
	BaseCommand(String name) {
		this._name = name;
	}

	public String getName() {
		return _name;
	}

	public String getNodeUrlArgument() {
		return _nodeUrlArgument;
	}

	public String getPortNameArgument() {
		return _portNameArgument;
	}
	
}
