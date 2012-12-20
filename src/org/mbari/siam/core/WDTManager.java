// MBARI copyright 2008
package org.mbari.siam.core;


import java.io.*;
import org.mbari.siam.utils.SyncProcessRunner;
import org.apache.log4j.Logger;

/**
WDTManager is a singleton object that manages initialization and strobing
of the watchdog timer. 

@author Brian Kieft
*/
public class WDTManager extends Thread
{
    /** Name of property indicating environmental service registry name */
    public static final String PROP_PREFIX = "WDTManager.";

    // Keep track of instances created - only one allowed
    protected static int _instanceCount = 0;
    protected static WDTManager _instance = null;
    private static Logger _log4j = Logger.getLogger(WDTManager.class);
    protected boolean _WDTManagerEnabled = true;

    protected NodeManager _nodeManager = null;
    protected NodeProperties _nodeProperties;
    protected boolean _debug = true;
 
    public WDTManager()
    {
      // Only allowed to create one instance
	    if (++_instanceCount > 1)
	    {
	      _log4j.error("Only ONE instance of WDTManager allowed!");
	      return;
    	}
	    _log4j.info("WDTManager constructor");
	    _instance = this;
	    _nodeManager = NodeManager.getInstance();
    	_nodeProperties = _nodeManager.getNodeProperties();
    	String enblString = _nodeProperties.getProperty(PROP_PREFIX+"enabled");

	    if (enblString != null)
	    {
	      _WDTManagerEnabled = enblString.equalsIgnoreCase("TRUE");
	    }
    }

    /** Method to get singleton instance. */
    public static synchronized WDTManager getInstance()
    {
	  if (_instanceCount < 1)
	    _instance = new WDTManager();
	    return _instance;
    }
    
    // Return true if sleep manager is enabled, else false.
    public boolean enabled() {
	    return _WDTManagerEnabled;
    }


    // Enable or disable
    public void set(boolean enable) {
	    _WDTManagerEnabled = enable;
    }


}


