/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils;
import java.util.Iterator;

public interface TCPProtocol{
	public String  processInput(String inputLine) throws Exception;
	public Iterator terminators();	
	public String disconnectString();
	public String help();
}