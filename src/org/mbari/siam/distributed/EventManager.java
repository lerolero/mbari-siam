// Copyright 2001 MBARI
package org.mbari.siam.distributed;

import java.rmi.Remote;

/**
 * Waits for and responds to asynchronous events from its Platform's components,
 * including Devices. Performs fault-handling. Contains time-tagged records of
 * events (Sensor and Platform state changes, etc.).
 * 
 * @author Tom O'Reilly
 */
public interface EventManager extends Remote {

}