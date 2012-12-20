// Copyright MBARI 2003
package org.mbari.siam.distributed.devices;

import java.io.IOException;

/** AnalogBoard encapsulates a piece of hardware than can do A/D conversions
 * @author Bob Herlien
 */
public interface AnalogBoard
{
    /** Set up one or more analog channels */
    public void analogSetup(int chan, int range, int polarity, int gain)
	throws IOException;

    /** Convert one A/D channel to voltage */
    public double analogSample(int chan)
	throws IOException, NumberFormatException;

    /** Convert multiple A/D channels to voltages */
    public double[] analogScan(int chan, int nchans)
	throws IOException, NumberFormatException;

    /** Close this board		*/
    public void close() throws IOException;

    /** Return number of channels per board */
    public int numChans();
    
    /** Return name. */
    public String getName();
}
