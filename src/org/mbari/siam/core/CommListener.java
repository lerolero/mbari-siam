/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.core;

import java.util.EventListener;

/** CommListener is an interface that must
    be implemented by objects that wish to intercept
    CommEvents.
 */

public interface CommListener extends EventListener {

    /** Action performed when service installed */
    //public void someCommEvent(CommEvent e);

} // end class CommListener


