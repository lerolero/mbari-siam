/** 
* @Title ProcessRunner
* @author Bob Herlien
* @version $Revision: 1.1 $
* @date $Date: 2008/11/04 22:17:53 $
*
* Copyright MBARI 2004
* 
* REVISION HISTORY:
* $Log: ProcessRunner.java,v $
* Revision 1.1  2008/11/04 22:17:53  bobh
* Initial checkin.
*
* Revision 1.1.1.1  2008/11/04 19:02:05  bobh
* Initial release of SIAM2, the release that was modularized to isolate hardware dependencies, and refactored to make most packages point to org.mbari.siam.
*
* Revision 1.8  2006/01/17 18:51:10  bobh
* Added getOutputString()
*
* Revision 1.7  2004/03/20 01:12:32  bobh
* Added waitFor() with timeout
*
* Revision 1.6  2004/03/12 23:39:35  mrisi
* got rid of System.out calls to see if this works with SIAM_AUTOSTART
*
* Revision 1.5  2004/02/24 18:16:38  bobh
* no message
*
* Revision 1.4  2004/02/20 23:42:10  bobh
* Initial Release
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
 * ProcessRunner provides a wrapper around the Java Runtime.getRuntime().exec()
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
 * ProcessRunner provides an intermediary "supervisor" thread between the
 * calling thread and the exec'd Process, which dumps the Process's stdout
 * and stderr to System.out and System.err.  This simulates having a terminal
 * attached for output, and prevents the exec'd Process from hanging due to
 * filling up its output buffer.
 *<p>
 * ProcessRunner also provides an addCallback() method, to allow the
 * caller to receive a callback when the child process finishes.
 *<p>
 * Note that this class doesn't resolve the input problem (this could be
 * done, but this implementation doesn't do it).  As a result, the scripts
 * and/or programs handed to ProcessRunner should <b>never</b> try to read
 * console input.
 * <p>
 * Also note that this class currently only implements two flavors of
 * Runtime.exec(): String and String[].
 */
public class ProcessRunner extends Thread
{
    Process	_process = null;
    Vector	_listeners = null;
    String	_command;
    int		_exitVal = ProcessRunnerCallback.NOT_COMPLETE;
    StringBuffer _outString = null;

    private static Logger _logger = Logger.getLogger(ProcessRunner.class);
    private final int POLL_DELAY = 500;

    /** Constructor emulates the Runtime.exec(String) method */
    public ProcessRunner(String command) throws IOException
    {
	_command = command;
	_listeners = new Vector();
	_outString = new StringBuffer();
	_process = Runtime.getRuntime().exec(command);
    }

    /** Constructor emulates the Runtime.exec(String[]) method */
    public ProcessRunner(String[] cmdarray) throws IOException
    {
	_command = cmdarray[0];
	_listeners = new Vector();
	_outString = new StringBuffer();
	_process = Runtime.getRuntime().exec(cmdarray);
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

    /** Add a Callback.  */
    public void addCallback(ProcessRunnerCallback listener)
    {
	_listeners.add(listener);
    }

    /** Protected method actually runs the command, throws IOException */
    protected void runIt() throws IOException
    {
        int	ch;
	InputStream procOutStream = _process.getInputStream();
	InputStream procErrStream = _process.getErrorStream();

	while (isRunning())
	{
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

	    StopWatch.delay(POLL_DELAY);
	}

    } /* runIt() */


    /** Run method just pipes Process's stdout to System.out and stderr to
	System.err.  Exits when Process is no longer running */
    public void run()
    {
	try
	{
	    runIt();
	} catch (IOException e) {
	    _logger.error("IOException: " + e.getMessage());
	    e.printStackTrace();
	}

	/* Get the exit value	*/
	try
	{
	    _exitVal = _process.exitValue();
	} catch (IllegalThreadStateException e) {
	    _exitVal = ProcessRunnerCallback.NOT_COMPLETE;
	}

	/* Notify anyone calling waitFor() with timeout */
	synchronized(this)
	{
	    notifyAll();
	}

	/* Call listener callbacks to let them know we're done */
	for (int i = 0; i < _listeners.size(); i++)
	{
	    ((ProcessRunnerCallback)_listeners.elementAt(i)).processCompleted(_exitVal);
	}

    } /* run() */


    /** Returns Process.exitValue() */
    public int exitValue() throws IllegalThreadStateException
    {
	return(_process.exitValue());
    }

    /** Returns Process.waitFor() */
    public int waitFor() throws InterruptedException
    {
	return(_process.waitFor());
    }

    /** Returns Process.waitFor() 
	@param timeout Timeout to wait in milliseconds
    */
    public int waitFor(long timeout)
	throws InterruptedException, IllegalThreadStateException
    {
	synchronized(this)
	{	
	    wait(timeout);
	}

	if (isRunning())
	{
	    _logger.warn("ProcessRunner timeout.  Killing " + _command);
	    _exitVal = ProcessRunnerCallback.NOT_COMPLETE;
	    destroyProcess();
	    throw new InterruptedException("Timeout running " + _command);
	}

	return(_process.exitValue());
    }

    /** Executes Process.destroy() */
    public void destroyProcess()
    {
	_process.destroy();
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

    /* For Testing	*/
    static class ProcessRunnerListener implements ProcessRunnerCallback
    {
	public void processCompleted(int exitVal)
	{
	    _logger.info("Got processCompleted callback, exitVal = " + exitVal);
	}
    }

    public static void main(String[] args) throws IOException
    {
	if (args != null)
	{
	    ProcessRunner p = new ProcessRunner(args);
	    ProcessRunnerListener l = new ProcessRunnerListener();

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

	    _logger.debug("ProcessRunner.main(): args.length=" + args.length);


	    p.addCallback(l);
	    p.start();

	    while (p.isAlive())
	    {
                _logger.info("Main process waiting...");
                StopWatch.delay(2000);
	    }

	    _logger.info("Main process shows child no longer running");
	    _logger.info("");
	    _logger.info("Now test with timeout = 5 seconds");
	    p = new ProcessRunner(args);
	    p.start();
	    try
	    {
		int retVal = p.waitFor(5000);
		_logger.info("Process exited normally, rtn code = "+retVal);
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
