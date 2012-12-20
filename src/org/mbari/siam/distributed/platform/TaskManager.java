// Copyright 2001 MBARI
package org.mbari.siam.distributed.platform;

/**
   TaskManager "manages" a group of ManagedTask objects. The
   management might be from the standpoint of power, 
   communications, etc.
   @author Tom O'Reilly
*/
public interface TaskManager {

    /** Registers the ManagedTask */
    public void register(ManagedTask task);
}
