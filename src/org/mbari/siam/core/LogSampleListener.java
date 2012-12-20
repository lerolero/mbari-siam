/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

import java.util.EventListener;

/** LogSampleListener is an interface that must
    be implemented by objects that wish to intercept
    LogSampleServiceEvents.
 */

public interface LogSampleListener extends EventListener {

    /** Action performed when service logs a device sample */
    public void sampleLogged(LogSampleServiceEvent e);    

} // end class LogSampleListener


