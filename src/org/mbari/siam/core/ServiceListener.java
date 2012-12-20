/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

import java.util.EventListener;

/** ServiceListener is an interface that must
    be implemented by objects that wish to intercept
    ServiceEvents.
 */

public interface ServiceListener extends EventListener {

    /** Action performed when service installed */
    public void serviceInstalled(ServiceEvent e);

    /** Action performed when service removed */
    public void serviceRemoved(ServiceEvent e);    

    /** Action performed when service request complete */
    public void serviceRequestComplete(ServiceEvent e);    

} // end class ServiceListener


