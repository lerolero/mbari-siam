/** 
* @Title SystemCall.java
* @author Bob Herlien
* @version $Revision: 1.2 $
* @date $Date: 2009/07/16 22:02:09 $
*
* Copyright MBARI 2004
* 
* REVISION HISTORY:
* $Log: SystemCall.java,v $
* Revision 1.2  2009/07/16 22:02:09  headley
* javadoc syntax fixes for 1.5 JDK
*
* Revision 1.1  2008/11/04 22:17:53  bobh
* Initial checkin.
*
* Revision 1.1.1.1  2008/11/04 19:02:05  bobh
* Initial release of SIAM2, the release that was modularized to isolate hardware dependencies, and refactored to make most packages point to org.mbari.siam.
*
* Revision 1.1  2004/03/16 22:58:33  bobh
* Java wrapper for JNI call to the C run-time system() call
*
*
*/
package org.mbari.siam.utils;
import java.io.IOException;
import org.apache.log4j.Logger;
	
/**
 * SystemCall is a simple JNI interface to the C runtime "system()" call.
 *<p>
 * The problem with Runtime.exec() is that the created Process does not have
 * its own terminal or console.  All standard I/O is read/written to the
 * streams created with the Process, which by default have no readers or
 * writers.  This causes problems in running shell scripts or running
 * in background mode
 */

public class SystemCall
{
    private static boolean libIsLoaded = false;
    private static Logger _logger = Logger.getLogger(SystemCall.class);

    static
    {
	try
	{
	    System.loadLibrary("systemCall");
	    libIsLoaded = true;
	} catch (UnsatisfiedLinkError le) {
	    libIsLoaded = false;
	    _logger.error("UnsatisfiedLinkError trying to load systemCall");
	} catch (Exception e) {
	    libIsLoaded = false;
	    _logger.error("Exception in SystemCall: " + e);
	}
    }


    /** Call the system() C run-time call 
	@param callString String to pass to the system() call
    */
    public static native int call(String callString);


    /** Return true if System.loadLibrary() call succeeded,
	false if not.
    */
    public static boolean isLoaded()
    {
	return(libIsLoaded);
    }


    /** Main routine, for unit testing only
	@param args - args[0] is passed to system()
	To test a call that itself has arguments, enclose the
	entire string in quotes; e.g. 
	java moos.utils.SystemCall "echo this is a test"
     */
    public static void main(String[] args) throws IOException
    {
	if (args != null)
	{
	    System.out.println("Calling " + args[0]);
	    int rtn = SystemCall.call(args[0]);
	    System.out.println("Return value was " + rtn);
	}

    } /* main() */

} /* class SystemCall */
