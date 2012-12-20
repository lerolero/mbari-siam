// Copyright 2001 MBARI
package org.mbari.siam.distributed;

/**
 * Implementations of SensorDataHandler know how to parse, compress, print,
 * summarize sensor-specific data.
 * 
 * @author Tom O'Reilly
 */
public interface SensorDataHandler {

	public void setCalibration(Calibration calibration);

	public Calibration getCalibration();

	public void processPacket(DevicePacket packet);

	public String printPacket(DevicePacket packet);

	public String printSummary(long startDate, long endDate,
			DevicePacket[] packets);

	public String printShortSummary(long startDate, long endDate,
			int maxOutputChars, DevicePacket[] packets);
}