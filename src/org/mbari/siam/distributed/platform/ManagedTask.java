// Copyright 2001 MBARI
package org.mbari.siam.distributed.platform;

/**
   ManagedTasks register with and are managed by a TaskManager.
   @author Tom O'Reilly
*/
public interface ManagedTask {

    /** Task states */
    public static final int UNINITIALIZED = -1;
    public static final int BUSY = 0;
    public static final int IDLE = 1;

    /** Return task state */
    public int taskState();

    /** Go to BUSY state? */
    public boolean startTask();

    public boolean stopTask();

    /** Allow task to go to IDLE state? */
    public boolean quiesceTask();
}
