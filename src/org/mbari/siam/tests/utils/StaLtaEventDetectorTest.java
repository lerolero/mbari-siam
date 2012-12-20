/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;

import org.mbari.siam.utils.StaLtaEventDetector;

public class StaLtaEventDetectorTest {

    protected static Logger _log4j = 
	Logger.getLogger(StaLtaEventDetectorTest.class);

    public static void main(String[] args) {

	if (args.length != 6) {
	    System.err.println("usage: parameterName, STA-width LTA-width " + 
			       "triggerRatio deTriggerRatio " + 
			       "maxTriggeredSamples");
	    return;
	}

	PropertyConfigurator.configure(System.getProperties());
	PatternLayout layout = new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");
	BasicConfigurator.configure(new ConsoleAppender(layout));


	String parameterName;
	int staSampleWidth, ltaSampleWidth;
	float triggerRatio, deTriggerRatio;
	int maxTriggeredSamples;

	try {
	    parameterName = args[0];
	    staSampleWidth = Integer.parseInt(args[1]);
	    ltaSampleWidth = Integer.parseInt(args[2]);
	    triggerRatio = Float.parseFloat(args[3]);
	    deTriggerRatio = Float.parseFloat(args[4]);
	    maxTriggeredSamples = Integer.parseInt(args[5]);
	}
	catch (Exception e) {
	    System.err.println("Error parsing paramters: " + e);
	    return;
	}


	StaLtaEventDetector eventDetector = null;

	try {
	    eventDetector = 
		new StaLtaEventDetector(parameterName, 
					staSampleWidth, ltaSampleWidth,
					triggerRatio, deTriggerRatio,
					maxTriggeredSamples);
	}
	catch (Exception e) {
	    System.err.println("Error constructing detector: " + e);
	    return;
	}

	BufferedReader stdInput = 
	    new BufferedReader(new InputStreamReader(System.in));

	while (true) {

	    // Prompt user for data sample
	    System.out.print("sample (just rtn to quit): " );

	    try {
		String line = stdInput.readLine();

		if (line.length() == 0) {
		    break;
		}

		eventDetector.addSample(new Double(line),
					System.currentTimeMillis());

	    }
	    catch (Exception e) {
		_log4j.error("Exception reading user input: ", e);
	    }
	}
    }
}
