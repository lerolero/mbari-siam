/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

import java.util.EventListener;

/** PowerListener is an interface that must
    be implemented by objects that wish to intercept
    PowerEvents.
 */

public interface PowerListener extends EventListener {

    /** Action performed when system is shutting down */
    public void shutdown(PowerEvent e);

    /** Action performed when power failure is detected */
    public void failureDetected(PowerEvent e);

} // end class PowerListener


