// MBARI copyright 2008
package org.mbari.siam.moos.deployed;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import java.io.*;

import org.mbari.siam.utils.SyncProcessRunner;
import org.mbari.siam.core.WDTManager;

import org.apache.log4j.Logger;

/**
MOOSWDTManager extends the default implementation of the WDTManager
with hardware specific implementation

@author Brian Kieft
*/
public class MOOSWDTManager extends WDTManager
{
    /** Name of property indicating environmental service registry name */
    private static int OSCLK = 3686400;                                // Clocks per second
    private static int TIMEOUT_SEC = 60;                               // Time in seconds before next WDT strobe needs to take place.
    private static final int WDT_FREQ_MS = ((TIMEOUT_SEC / 4) * 1000); // Duration of sleep for this thread
    private static final int PROC_RUNNER_TIMEOUT_MS = 500;                // Timeout in ms for process runner to wait
    private static int WME = 1;         // OWER bit 0
    private static int M3 = 8;          // OSSR bit 3
    private static String _writeRegisterString = "/root/writeRegister ";

    // Keep track of instances created - only one allowed
    protected static int _instanceCount = 0;
    private static Logger _log4j = Logger.getLogger(MOOSWDTManager.class);
    protected Thread _testThread = null;

    protected boolean _WDTManagerEnabled = true;

    protected boolean _debug = true;
    protected static SyncProcessRunner _procRunner = null;

    public static final String RESET_REASON_REGISTER = 
	"/proc/cpu/registers/RCSR";

    protected MOOSWDTManager()
    {
    	// Only allowed to create one instance
	    if (++_instanceCount > 1)
	    {
	      _log4j.error("Only ONE instance of MOOSWDTManager allowed!");
	      return;
    	}
	    _log4j.info("MOOSWDTManager constructor");
      _procRunner = new SyncProcessRunner();
    }
    
    
    /** Method to get singleton instance. */
    public static synchronized WDTManager getInstance()
    {
	  if (_instanceCount < 1)
	    _instance = new MOOSWDTManager();
	    return _instance;
    }    
  
   // Run - initialize WDT and then run forever to strobe it
    public void run()
    {
	  _log4j.debug("WDTManager thread starting, enabled=" + 
		_WDTManagerEnabled + ", NodeManager = " + 
		_nodeManager.toString());
		
    _log4j.info("WDTManager initializing watchdog timer");
    initializeWDT();

	  for (; ;) 
	  {
	    if (_WDTManagerEnabled) {
		    try {
			    // Sleep 
			    Thread.sleep( WDT_FREQ_MS );
		    }
		    catch (InterruptedException e) {
		    }
      
      _log4j.debug("WDTManager strobing WDT");       
		  strobeWDT(); 
		    
		  }
		  else
		  {
		  	_log4j.info("WDTManager disabled. Not strobing watchdog");
		  }
	  } 
    } 


    protected void disableWDT() {
    	_log4j.info("Disabling Watchdog Timer Interrupt");    	
      int OIERVal = readProcRegister("/proc/cpu/registers/OIER");      
      OIERVal = OIERVal & 0x7; // disable channel 3 interrupt. 
      // A match between OSMR3 and OSCR will result in interrupt bit M3 in OSSR
      writeProcRegister("/proc/cpu/registers/OIER", OIERVal);    	
      OIERVal = readProcRegister("/proc/cpu/registers/OIER");      
      if( (OIERVal & 0x8) != 0x8 ) {
        _log4j.info("Watchdog is disabled");   
      }
      else {
        _log4j.error("Disabling Watchdog Timer Failed");   
      }
    }

    /** Read contents of specified register */
    public static int readProcRegister(String procFilename) {
      int retVal = -1;
      Long l;
    	try {
        FileReader procFile = new FileReader(procFilename);
        BufferedReader procFileReader = new BufferedReader(procFile);	  
        l = Long.decode( procFileReader.readLine() );
        retVal = l.intValue();
        procFile.close();
    	} 
    	catch (Exception e) {
    	  _log4j.error("exception attempting to read proc filesystem");
    	  _log4j.error(e.getMessage());
    	}
      return retVal;
    }
    

    private synchronized static void writeProcRegister(String procFilename, int value) {
	    try
	    {
	      // Execute the write via the writeRegister executable
	      _procRunner.exec( (_writeRegisterString + procFilename + " 0x" + Integer.toHexString(value)) );
	      _procRunner.waitFor(PROC_RUNNER_TIMEOUT_MS);
	      
	      // Check for errors returned from writeRegister
	      String pro=_procRunner.getOutputString();
	      if(pro.length() > 1)
	      {
		      _log4j.error("WDTManager error calling writeRegister:" + pro);
		    }
	    } catch (Exception e) {
	      _log4j.error("WDTManager exception: ", e);
	    }
    }    
        
    
    protected static void initializeWDT() {
    	// First, grab register values from RCSR, OWER, and OIER
    	int RCSRVal = readProcRegister("/proc/cpu/registers/RCSR"); 
      int OWERVal = readProcRegister("/proc/cpu/registers/OWER");
      int OIERVal = readProcRegister("/proc/cpu/registers/OIER");
      
      OWERVal = OWERVal | WME; // Enable the WDT
      OIERVal = OIERVal | 0x8; // Enable channel 3 interrupt. 
      // A match between OSMR3 and OSCR will result in interrupt bit M3 in OSSR
      
      // Check for a watchdog reset - bit 2 - and report it
      if( (RCSRVal & 0x4) == 0x4 ) 
      {
      	_log4j.error("!!! Reboot due to watchdog reset !!!");
      	writeProcRegister("/proc/cpu/registers/RCSR", 4); // Clear bit 2 WDR
      }
      // Check for a hardware reset - bit 0 - and report it
      if( (RCSRVal & 0x1) == 0x1 ) 
      {
      	_log4j.info("Reboot due to hardware reset");
      	writeProcRegister("/proc/cpu/registers/RCSR", 1); // Clear bit 0 WDR
      }
      
      // Get the current value of OSCR to generate the new value for the compare register (OSMR3)
      int OSCRVal = readProcRegister("/proc/cpu/registers/OSCR");  
      int OSMR3Val = OSCRVal + ( TIMEOUT_SEC * OSCLK);
      
      // Write the registers
      writeProcRegister("/proc/cpu/registers/OSMR3", OSMR3Val);
      writeProcRegister("/proc/cpu/registers/OWER", OWERVal);
      writeProcRegister("/proc/cpu/registers/OIER", OIERVal);

      // Finally, verify that the initialization was successful
      if( ((readProcRegister("/proc/cpu/registers/OWER") & 0x1) != 1) || ((readProcRegister("/proc/cpu/registers/OIER") & 0x8) != 8)  ) {
        _log4j.error("WDTManager failed to initialize watchdog timer");
      }
      else {
      	_log4j.info("WDTManager watchdog initialization successful");
      }
      
    }


    public static void setOWER() {
    	// First, grab register value from OWER
      int OWERVal = readProcRegister("/proc/cpu/registers/OWER");
      
      OWERVal = OWERVal | WME; // Enable the WDT
      
      // Write the register
      writeProcRegister("/proc/cpu/registers/OWER", OWERVal);

      // Finally, verify that the initialization was successful
      if( ((readProcRegister("/proc/cpu/registers/OWER") & 0x1) != 1) ) {
        _log4j.error("WDTManager failed to initialize watchdog timer after sleep");
      }
      else {
      	_log4j.debug("WDTManager watchdog initialization successful");
      }
      
    }


    public static boolean clrOSSR() {
    	// OSSR bits 0-3 values are unknown at reset (including a sleep reset)
    	// Here we clear bit 3 so as not to incur a false WDT reset

      // Write the register
      writeProcRegister("/proc/cpu/registers/OSSR", M3);

      _log4j.debug("OSSR value is:" + readProcRegister("/proc/cpu/registers/OSSR"));

      if( (readProcRegister("/proc/cpu/registers/OSSR") & 0x8) != 0)
      {
      	_log4j.error("Post suspend failed to clear OSSR bit");
      	return false;
      }
      else
      {
      	return true;
      }
    }
    
    private static void strobeWDT() {
    	// Get the current value of OSCR to generate the new value for the compare register (OSMR3)
      int OSCRVal = readProcRegister("/proc/cpu/registers/OSCR");  
      int OSMR3Val = OSCRVal + ( TIMEOUT_SEC * OSCLK);
      
      writeProcRegister("/proc/cpu/registers/OSMR3", OSMR3Val);	
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


