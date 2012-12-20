// MBARI copyright 2003
package org.mbari.siam.moos.deployed;

import org.mbari.siam.core.SleepManager;

import org.apache.log4j.Logger;


/**
MOOSSleepManager extends the default implementation of the SleepManager
by adding some hardware re-initialization after sleep.

@author Bob Herlien
*/
public class MOOSSleepManager extends SleepManager
{
	private static Logger _log4j = Logger.getLogger(MOOSSleepManager.class); //*** DEBUG
  protected static int _instanceCount = 0;



    protected MOOSSleepManager()
    {
    	// Only allowed to create one instance
	    if (++_instanceCount > 1)
	    {
	      _log4j.error("Only ONE instance of MOOSSleepManager allowed!");
	      return;
    	}
	    _log4j.info("MOOSSleepManager constructor");
    }
    
    
    /** Method to get singleton instance. */
    public static synchronized SleepManager getInstance()
    {
	  if (_instanceCount < 1)
	    _instance = new MOOSSleepManager();
	    return _instance;
    }    


    /** Do any necessary cleanup after returning from suspend */
    public void postSuspend()
    {
    _log4j.debug("MOOSSleepManager - sending SPI initialization string in postSuspend"); //*** DEBUG    	
	// SPI is broken after sleep/wakeup - give it new 
	// initialization string.
	// This is the WRONG WAY to fix this problem.  
	// We need to fix the SPI driver

	SpiMaster spiMaster = SpiMaster.getInstance();

	if (spiMaster != null)
	    spiMaster.writeSpi("10W0G100S0L0I0P");

  // First, clear bit 3 of the OSSR register. Then,
  // RE-initialize the match enable register since bits are cleared after sleep
  MOOSWDTManager _moosWDTManager = (MOOSWDTManager)MOOSWDTManager.getInstance();
  boolean regCleared = _moosWDTManager.clrOSSR();	
  if(regCleared)
  {
    _moosWDTManager.setOWER();		    
  }
  else
  {
  	_log4j.error("OWER register not set. NO WATCHDOG");
  }

	super.postSuspend();
    }
}


