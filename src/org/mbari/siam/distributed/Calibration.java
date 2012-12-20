// Copyright 2001 MBARI
package org.mbari.siam.distributed;

import java.io.Serializable;

/**
 * Sensor calibration information. Contains calibration coefficients.
 * 
 * @author Tom O'Reilly
 */
public class Calibration implements Serializable {

	public static class Coefficient {
		public String _name;

		public double _value;
	}

	public Calibration(long deviceID, long startDate, long endDate) {
		_deviceID = deviceID;
		_startDate = startDate;
		_endDate = endDate;
	}

	public long getDeviceID() {
		return _deviceID;
	}

	public long getStartDate() {
		return _startDate;
	}

	public long endDate() {
		return _endDate;
	}

	Coefficient[] getCoefficients() {
		return _coefficients;
	}

	private long _startDate;

	private long _endDate;

	private long _deviceID;

	private Coefficient _coefficients[] = new Coefficient[10];
}