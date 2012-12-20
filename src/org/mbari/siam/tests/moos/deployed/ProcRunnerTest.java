/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.moos.deployed;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

import org.mbari.siam.utils.SyncProcessRunner;


public class ProcRunnerTest {

    SyncProcessRunner _procRunner = new SyncProcessRunner();
    long _timeoutMsec = 30000;
    long _sleepMsec = 10000;

    void run(String cmd1, String cmd2) throws Exception {

	while (true) {
	    System.out.println("executing " + cmd1);
	    _procRunner.exec(cmd1);
	    int retVal = _procRunner.waitFor(_timeoutMsec);
	    System.out.println("\"" + cmd1 + "\" returned " + retVal + ":");
	    System.out.println("output: " + _procRunner.getOutputString());

	    Thread.sleep(1000);

	    System.out.println("executing " + cmd2);
	    _procRunner.exec(cmd2);
	    retVal = _procRunner.waitFor(_timeoutMsec);
	    System.out.println("\"" + cmd2 + "\" returned " + retVal + ":");
	    System.out.println("output: " + _procRunner.getOutputString());


	    System.out.println("sleep for " + _sleepMsec/1000 + " sec:");
	    Thread.sleep(_sleepMsec);
	}
    }


    public static void main(String[] args) {

	if (args.length != 2) {
	    System.err.println("usage: command1 command2");
	    return;
	}

	PropertyConfigurator.configure(System.getProperties());
	PatternLayout layout = new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");
	BasicConfigurator.configure(new ConsoleAppender(layout));

	ProcRunnerTest test = new ProcRunnerTest();
	try {
	    test.run(args[0], args[1]);
	}
	catch (Exception e) {
	    System.err.println(e);
	}
    }

}
