// Copyright 2003 MBARI
package org.mbari.siam.core;

import java.io.IOException;
import java.io.InputStream;

import org.mbari.siam.distributed.InitializeException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

public class NullPuck implements Puck
{
    /** log4j logger */
    static Logger _log4j = Logger.getLogger(NullPuck.class);
    
    /** initialize the puck */
    public void initialize() throws InitializeException
    {
        return;
    }

    /** switch the puck to sensor mode */
    public void setSensorMode() throws IOException
    {
        return;
    }

    /** perform any necessary puck operations on wake-up */
    public void managePowerWake(int comms_policy, int inst_policy)
        throws IOException
    {
        
        _log4j.debug("NullPuck - managePowerWake()"); 
        return;
    }

    /** perform any necessary puck operations before entering powering down */
    public void managePowerSleep(int comms_policy, int inst_policy)
        throws IOException
    {
        _log4j.debug("NullPuck - managePowerSleep()"); 
        return;
    }
    
    /** call when device service is resumed */
    public void resume()
    {
        return;
    }

    /** call when device service is suspended */
    public void suspend()
    {
        return;
    }
    
    /** get an InputStream to the puck*/
    public InputStream getInputStream() throws IOException
    {
        return null;
    }
}
