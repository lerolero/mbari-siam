// MBARI copyright 2002
package org.mbari.siam.core;

/**
Interface for objects that may be scheduled.
@author Kent Headley, Tom O'Reilly
*/
public interface Schedulable{
    /** Execute the scheduled task */
    public void execute();
}
