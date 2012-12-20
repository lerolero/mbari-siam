/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/*
 * Created on Mar 3, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.mbari.siam.devices.seahorse;

import java.util.Date;

/**
 * @author siamuser
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Connection {

	/**
	 * 
	 */
	private static final long EXPECTED_PERIOD = 15*60*1000;  // 15 Minute expected Period
	private static final long LEAD_TIME = 5*60*1000;  //Wake up 5 minutes early in the absense of better info.
	public static long AVERAGING_WINDOW = 6*60*60*1000;
	
	public long duration;
	public Date time_established;
	public Date expected_next_connection;
	public float BattVoltage;
	
	
	public Connection() {
		super();
		time_established = new Date();
		duration = -1;
		expected_next_connection = new Date(time_established.getTime()+EXPECTED_PERIOD-LEAD_TIME);
	}

	public void CloseConnection(){
		duration = System.currentTimeMillis()-time_established.getTime();
		expected_next_connection = new Date(time_established.getTime()+EXPECTED_PERIOD-LEAD_TIME-duration);
	}
}
