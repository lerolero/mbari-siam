/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.moos.deployed;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.BasicConfigurator;

import org.mbari.siam.utils.SyncProcessRunner;

public class ProcessRunnerTest implements Runnable {
	
	String _cmd;
	int _timeoutMsec;
	String _name;
	
	SyncProcessRunner _processRunner = new SyncProcessRunner();
	
	public ProcessRunnerTest(String name, String cmd, int timeoutMsec) {
		_name = name;
		_cmd = cmd;
		_timeoutMsec = timeoutMsec;
	}

	public void run() {
	    try {
		    System.out.println("Calling SyncProcessRunner(\"" 
				+ _cmd + "\")");
		    _processRunner.exec(_cmd);
		    System.out.println("wait for " + _timeoutMsec/1000 + " sec");
		    int ret = _processRunner.waitFor(_timeoutMsec);
		    System.out.println("waitFor() returned " + ret);

		    String output=_processRunner.getOutputString();
		    if(output!=null) {
			  System.out.println(output);
		    }
		} catch (Exception e) {
		    System.err.println("Exception in run(): " + e);
		}

	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BasicConfigurator.configure();

		int timeoutMsec = 240000;
		if (args.length != 2) {
			System.err.println("Usage: command1 command2");
			return;
		}
		
		ProcessRunnerTest test1 = new ProcessRunnerTest("test1", args[0], timeoutMsec);
		ProcessRunnerTest test2 = new ProcessRunnerTest("test2", args[1], timeoutMsec);
		
		Thread thread1 = new Thread(test1, "thread1");
		Thread thread2 = new Thread(test2, "thread2");
		
		thread1.start();
		try {
		    Thread.sleep(5000);
		}
		catch (Exception e) {
		}

		thread2.start();
				
		while (true) {
			try {
				System.out.println("sleep 2000");
			  Thread.sleep(2000);
			}
			catch (Exception e) {
	
			}
		}
	}
}
