// MBARI copyright 2003
package org.mbari.siam.core;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

/**
PowerManager is a singleton object that manages the system power
for the MOOS Mooring Controller.  Any load (instruments, comm devices,
etc) must request power from the PowerManager before it's allowed
to turn on its power .

@author Bob Herlien
*/
public class PowerManager {

    /** log4j logger */
    static Logger _log4j = Logger.getLogger(PowerManager.class);

    /** Name of property indicating environmental service registry name */
    private static final int POWERTHREAD_PERIOD = 1000;

    // Keep track of instances created - only one allowed
    private static int _instanceCount = 0;

    /** Constructor just ensures one instance and creates PowerThread */

    PowerManager() {
      // Only allowed to create one instance
      if (++_instanceCount > 1) {
	_log4j.error("Only ONE instance of PowerManager allowed!");
	System.exit(1);
      }
      _log4j.debug("PowerManager constructor");
    }

    /** Returns name of PowerManager's class. */
    public final byte[] getName() {
	return (this.getClass().getName()).getBytes();
    }

} /* PowerManager */
