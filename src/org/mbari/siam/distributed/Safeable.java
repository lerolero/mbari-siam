/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;


/** 
Safeable interface is implemented by instruments that can operate in a 
resource-restricted environment ("safe mode") as well as the nominal 
environment ("normal mode"). A Safeable instrument is restored to 
"normal" operation by re-starting or re-scanning the service.
*/
public interface Safeable {

    /** Enter mode for resource-restricted environement. */
    public void enterSafeMode() throws Exception;
}
