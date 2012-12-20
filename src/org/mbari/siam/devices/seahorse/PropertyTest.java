/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/*
 * Created on Feb 14, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.mbari.siam.devices.seahorse;

import java.util.Properties;

/**
 * @author siamuser
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class PropertyTest {

	/**
	 * 
	 */
	public PropertyTest() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public static void main(String[] arg){
		Properties systemProperties = System.getProperties();
		String siamHome = systemProperties.getProperty("SIAM_HOME").trim();
	}

}
