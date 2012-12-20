// Copyright MBARI 2003
package org.mbari.siam.distributed.devices;

import java.io.IOException;

/** DigitalInputBoard encapsulates a piece of hardware than contains digital
    input ports.
 * @author Bob Herlien
 */
public interface DigitalInputBoard
{
    /** Read DIO Port */
    public int readDIO(int port)
	throws IOException, NumberFormatException;

    /** Return name. */
    public String getName();
}
