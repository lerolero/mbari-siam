// Copyright 2003 MBARI
package org.mbari.siam.core;

import java.io.IOException;
import java.io.InputStream;

import org.mbari.siam.distributed.InitializeException;


public interface Puck
{
    /** initialize the puck */
    public void initialize() throws InitializeException;

    /** switch the puck to sensor mode */
    public void setSensorMode() throws IOException;

    /** perform any necessary puck operations on wake-up */
    public void managePowerWake(int comms_policy, int inst_policy) 
        throws IOException;

    /** perform any necessary puck operations before entering powering down */
    public void managePowerSleep(int comms_policy, int inst_policy)
        throws IOException;

    /** call when device service is resumed */
    public void resume();

    /** call when device service is suspended */
    public void suspend();

    /** get an InputStream to the puck*/
    public InputStream getInputStream() throws IOException;
}
