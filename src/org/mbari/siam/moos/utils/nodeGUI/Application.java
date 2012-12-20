/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.moos.utils.nodeGUI;

import org.mbari.siam.distributed.DevicePacket;

/** Interface for node GUI applications. */
interface Application {

    /** Process the input packet. */
    public void processSample(DevicePacket packet);

    /** Sampling about to start. */
    public void sampleStartCallback();

    /** Finished sampling */
    public void sampleEndCallback();

    /** Error when sampling */
    public void sampleErrorCallback(Exception e);

    /** Start sampling at specified interval */
    public void startSampling(int millisec);
}
