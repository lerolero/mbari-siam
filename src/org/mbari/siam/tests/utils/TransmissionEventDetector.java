/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.Date;
import java.text.SimpleDateFormat;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.mbari.siam.utils.StaLtaEventDetector;

public class TransmissionEventDetector 
    implements StaLtaEventDetector.Listener {

    protected SimpleDateFormat _dateFormatter = 
	new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    public void triggeredCallback(StaLtaEventDetector detector) {
	System.out.println("# Transition to TRIGGERED state at " + 
			   _dateFormatter.format(new Date(detector.getTransitionTime())) + 
			   " (nSample=" + detector.getNsamples() + ") ***");

	System.out.println(detector.toString());
    }

    public void detriggeredCallback(StaLtaEventDetector detector) {

	System.out.println("# Transition to DE-TRIGGERED state at " + 
			   _dateFormatter.format(new Date(detector.getTransitionTime())) + 
			   " (nSample=" + detector.getNsamples() + ") ***");

	System.out.println(detector.toString());
    }


    public static void main(String args[]) {
	
	PropertyConfigurator.configure(System.getProperties());
	PatternLayout layout = 
	    new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");

	BasicConfigurator.configure(new ConsoleAppender(layout));

	if (args.length != 7) {
	    System.err.println("usage: parameterName, STA-width LTA-width " + 
			       "triggerRatio deTriggerRatio " + 
			       "maxTriggeredSamples dataFile");

	    System.out.println("dataFile should consist of two columns.\n" + 
			       "column-1: millsec past epoch\n" + 
			       "column-2: %transmittance");

	    System.out.println("Lines beginning with '#' are ignored");

	    return;
	}

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

	// Print input parameters
	System.out.println("# parameter: " + parameterName);
	System.out.println("# staSampleWidth: " + staSampleWidth);
	System.out.println("# ltaSampleWidth: " + ltaSampleWidth);
	System.out.println("# triggerRatio:" + triggerRatio);
	System.out.println("# deTriggerRatio:" + deTriggerRatio);
	System.out.println("# maxTriggeredSamples:" + maxTriggeredSamples);

	// Create event detector
	StaLtaEventDetector eventDetector = null;

	try {
	    eventDetector = 
		new StaLtaEventDetector(parameterName, 
					staSampleWidth, ltaSampleWidth,
					triggerRatio, deTriggerRatio,
					maxTriggeredSamples);

	    eventDetector.addListener(new TransmissionEventDetector());
	}
	catch (Exception e) {
	    System.err.println("Error constructing detector: " + e);
	    return;
	}


	String fileName = args[6];

	BufferedReader reader = null;

	try {
	    reader = 
		new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
	}
	catch (Exception e) {
	    System.err.println("Error opening file " + fileName + ":\n" + e);
	    return;
	}


	int nLines = 0;
	while (true) {

	    try {
		String line = reader.readLine();
		if (line == null) {
		    // End of file
		    break;
		}

		nLines++;
		StringTokenizer tokenizer = new StringTokenizer(line);
		long epochMsec = 0;
		Double transmission = null;

		boolean parseError = false;

		int nToken;
		for (nToken = 0; tokenizer.hasMoreTokens(); nToken++) {
		    String token = tokenizer.nextToken();

		    if (nToken == 0) {
			try {
			    epochMsec = 1000 * Long.parseLong(token);
			}
			catch (Exception e) {
			    System.err.println("Invalid long value: " + token);
			    parseError = true;
			    break;
			}
		    }
		    else if (nToken == 1) {
			try {
			    transmission = Double.valueOf(token);
			}
			catch (Exception e) {
			    System.err.println("Invalid double value: " 
					       + token);
			    parseError = true;
			    break;
			}
		    }
		}

		if (nToken != 2) {
		    parseError = true;
		}

		if (parseError) {
		    System.err.println("Error parsing line " + nLines + 
				       ": \"" + line + "\"\n");
		    continue;
		}

		// Need to convert 
		Double attenuation = 
		    new Double(100. - transmission.doubleValue());

		eventDetector.addSample(attenuation, epochMsec);

		System.out.println(line + " " + nLines + " " + 
				   attenuation + " " + 
				   eventDetector.getSTA() + " " +
				   eventDetector.getLTA());
	    }
	    catch (Exception e) {
		System.err.println(e);
	    }
	}
    }
}
