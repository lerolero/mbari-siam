/** 
* @Title SyncProcessRunner
* @author Bob Herlien
* @version $Revision: 1.1 $
* @date $Date: 2008/11/04 22:17:53 $
*
* Copyright MBARI 2004
* 
* REVISION HISTORY:
* $Log: SyncProcessRunner.java,v $
* Revision 1.1  2008/11/04 22:17:53  bobh
* Initial checkin.
*
* Revision 1.1.1.1  2008/11/04 19:02:05  bobh
* Initial release of SIAM2, the release that was modularized to isolate hardware dependencies, and refactored to make most packages point to org.mbari.siam.
*
* Revision 1.3  2006/03/11 05:26:33  oreilly
* Added debugs
*
* Revision 1.2  2006/01/17 18:51:10  bobh
* Added getOutputString()
*
* Revision 1.1  2004/03/24 23:44:24  bobh
* Initial version, based on (but not inherited from) ProcessRunner
*
*
*/
package org.mbari.siam.utils;

import java.lang.Runtime;
import java.lang.Process;
import java.lang.IllegalThreadStateException;
import java.lang.InterruptedException;
import java.util.Vector;
import java.io.IOException;
import java.io.InputStream;
import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;

/**
 * SyncProcessRunner provides a wrapper around the Java Runtime.getRuntime().exec()
 * methods.
 *<p>
 * The problem with Runtime.exec() is that the created Process does not have
 * its own terminal or console.  All standard I/O is read/written to the
 * streams created with the Process, which by default have no readers or
 * writers.  This is in contrast with how Linux shells (bash etc) deal with
 * stdio -- the child process inherits the controlling terminal.
 *<p>
 * As a result, if you Runtime.exec() to a shell script, and that shell
 * script does any I/O, it will hang after filling the output buffer.
 *<p>
 * SyncProcessRunner is similar to ProcessRunner, but is synchronous.
 * That is, it does not introduce a supervisor thread, as does ProcessRunner.
 * Rather, the caller MUST (immediately) call the waitFor() or exitValue()
 * methods, either of which waits (synchronously) for the child process to
 * finish, and then returns its exit value.  While waiting, these functions
   capture the process' stdout and stderr and save them to a string.  Thus,
   this class uses the thread context of the caller for supervising the
 * process.
 *<p>
 * Note that this class doesn't resolve the input problem (this could be
 * done, but this implementation doesn't do it).  As a result, the scripts
 * and/or programs handed to ProcessRunner should <b>never</b> try to read
 * console input.
 * <p>
 * Also note that this class currently only implements two flavors of
 * Runtime.exec(): String and String[].
 */
public class SyncProcessRunner
{
    Process	_process = null;
    String	_command = null;
    int		_exitVal = ProcessRunnerCallback.NOT_COMPLETE;
    StringBuffer _outString = null;

    private static Logger _logger = Logger.getLogger(SyncProcessRunner.class);
    private final int POLL_DELAY = 500;

    /** exec method emulates the Runtime.exec(String[]) method */
    public void exec(String[] cmdarray)
	throws IOException, IllegalThreadStateException
    {
	if ((_process != null) && isRunning()) {
	    _logger.error("exec() - IllegalThreadStateException");
	    throw new IllegalThreadStateException("Process still running");
	}
	_command = cmdarray[0];
	_outString = new StringBuffer();
	_process = Runtime.getRuntime().exec(cmdarray);
    }


    /** exec() method emulates the Runtime.exec(String) method */
    public void exec(String command)
	throws IOException, IllegalThreadStateException
    {
	if ((_process != null) && isRunning()) {
	    _logger.error("exec() - IllegalThreadStateException");
	    throw new IllegalThreadStateException("Process still running");
	}
	_command = command;
	_outString = new StringBuffer();
	_logger.debug("exec() - Runtime.getRunTime().exec(" + command + ")");
	_process = Runtime.getRuntime().exec(command);
	_logger.debug("exec() - done");
    }


    /** Similar to Thread.isAlive(), but returns true if wrapped Process is
	still running */
    public boolean isRunning()
    {
	try
	{
	    int val = _process.exitValue();
	} catch (IllegalThreadStateException e) {
	    return(true);
	}
	return(false);
    }


    /** Protected method actually runs the command, throws IOException
     * @param timeout - timeout in milliseconds.  <=0 means no timeout
     */
    protected void runIt(long timeout) throws IOException
    {
        int	ch;
	long startTime = System.currentTimeMillis();
	InputStream procOutStream = _process.getInputStream();
	InputStream procErrStream = _process.getErrorStream();

	while (isRunning())
	{
	    StopWatch.delay(POLL_DELAY);

	    while (procOutStream.available() > 0)
	    {
	        ch = procOutStream.read();
	        _outString.append((char)ch);
		System.out.write(ch);
	    }

	    while (procErrStream.available() > 0)
	    {
	        ch = procErrStream.read();
	        _outString.append((char)ch);
		System.out.write(ch);
	    }

	    if ((timeout > 0) && 
		(System.currentTimeMillis() >= startTime + timeout))
		return;
	}

    } /* runIt() */


    /** Returns Process.waitFor() 
	@param timeout Timeout to wait in milliseconds
    */
    public int waitFor(long timeout) throws IOException, InterruptedException
    {
	_logger.debug("waitFor() - runIt(" + timeout + ")");

	runIt(timeout);

	_logger.debug("waitFor() - done with runIt(" + timeout + ")");

	if (isRunning())
	{
	    _logger.warn("SyncProcessRunner timeout.  Killing " + _command);
	    _exitVal = ProcessRunnerCallback.NOT_COMPLETE;
	    _process.destroy();
	    _logger.debug("waitFor() - throwing InterruptedException");
	    throw new InterruptedException("Timeout running " + _command);
	}

	try		/* Get the exit value	*/
	{
	    _logger.debug("waitFor() - try to return ");
	    return(_process.exitValue());
	} catch (IllegalThreadStateException e) {
	    _logger.debug("waitFor() - return not complete");
	    return(ProcessRunnerCallback.NOT_COMPLETE);
	}
    }


    /** Returns Process.waitFor() */
    public int waitFor() throws IOException
    {
	runIt(0);

	try		/* Get the exit value	*/
	{
	    return(_process.exitValue());
	} catch (IllegalThreadStateException e) {
	    return(ProcessRunnerCallback.NOT_COMPLETE);
	}
    }


    /** Returns Process.exitValue() */
    public int exitValue() throws IOException
    {
	return(waitFor());
    }

    /** Returns the output that the Process had sent to stdout and stderr,
	as a String.  The normal sequence of calls is exec(), waitFor(), and
	then getOutputString().  That is, it doesn't make much sense to
	call getOutputString() until after the call has completed; though
	it's not illegal to call it sooner.
    */
    public String getOutputString()
    {
        return(_outString.toString());
    }

    /** Main routine, for testing only */
    public static void main(String[] args) throws IOException
    {
	if (args != null)
	{
	/* Stolen from NodeTest.java				*/
	/* Set up a simple configuration that logs on the console.
	   Note that simply using PropertyConfigurator doesn't work
	   unless JavaBeans classes are available on target. 
	   For now, we configure a PropertyConfigurator, using properties
	   passed in from the command line, followed by BasicConfigurator
	   which sets default console appender, etc.
	*/
	    PropertyConfigurator.configure(System.getProperties());
	    PatternLayout layout = 
		new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");

	    BasicConfigurator.configure(new ConsoleAppender(layout));

	    _logger.debug("SyncProcessRunner.main(): args[0]=" + args[0]);

	    SyncProcessRunner p = new SyncProcessRunner();
	    p.exec(args);
	    int rtn = p.waitFor();

	    _logger.info("SyncProcessRunner returns " + rtn);
	    _logger.info("Command response was:\n\"" +
			     p.getOutputString() + "\"");
	    _logger.info("");
	    _logger.info("Now test with timeout = 5 seconds");

	    p.exec(args);
	    try
	    {
		rtn = p.waitFor(5000);
		_logger.info("Process exited normally, rtn code = " + rtn);
		_logger.info("Command response was:\n\"" +
			     p.getOutputString() + "\"");
	    } catch (InterruptedException e) {
		_logger.warn("Process timed out; got exception: " + e);
	    }
	}
    } /* main() */

/* The following bash script is useful for testing
#!/bin/bash
# processRunnerTest.sh

let numloops=10
if [ $# -gt 0 ]
then
    let numloops=$1
fi

let exitcode=0
if [ $# -gt 1 ]
then
    let exitcode=$2
fi

let i=0
while [ $i -lt $numloops ]
do
    echo "$0 loop $i"
    let i=i+1
    sleep 1
done

exit $exitcode
*/

} /* class ProcessRunner */
