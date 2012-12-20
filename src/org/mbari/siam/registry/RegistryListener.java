/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.registry;

/** RegistryListener is an interface that must
    be implemented by objects that wish a callback when
    the InstrumentRegistry adds or deletes an entry.
 */


public interface RegistryListener
{
    /** Action performed when service installed */
    public void newRegistrantCallback(RegistryEntry entrant);

    /** Action performed when service is removed */
    public void removeRegistrantCallback(RegistryEntry entrant);

} /* class RegistryListener */
