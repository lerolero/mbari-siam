/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.moos.utils.nodeGUI;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.mbari.siam.distributed.DevicePacket;

public class SamplerTest implements Application {

    public SamplerTest() {
	System.out.println("SamplerTest constructor");
    }


    /** Set "sample starting" indicator. */
    public void sampleStartCallback() {
    }

    /** Set "sample ended" indicator. */
    public void sampleEndCallback() {
    }


    /** Set "sample error" indicator. */
    public void sampleErrorCallback(Exception e) {
    }

    public void processSample(DevicePacket packet) {
	System.out.println("processSample()...");
    }

    /** Start sampling at specified interval */
    public void startSampling(int millisec) {
	System.out.println("startSampling()...");
    }


    public static void main(String[] args) {

	// Configure log4j
	PropertyConfigurator.configure(System.getProperties());
	BasicConfigurator.configure();

	SamplerTest app = new SamplerTest();

	System.out.println("Construct sampler thread");
	// Create sampler thread
	Thread thread = new Thread(new InstrumentSampler(null, 5000, app));

	System.out.println("Start sampler thread");
	thread.start();

	while (true) {
	    System.out.println("main sleep");
	    try {
		Thread.sleep(5000);
	    }
	    catch (InterruptedException e) {
	    }
	}
    }
}


