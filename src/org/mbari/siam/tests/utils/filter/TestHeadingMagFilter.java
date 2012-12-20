/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.utils.filter;

import java.util.Vector;
import java.text.NumberFormat;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

import org.mbari.siam.utils.Filter;
import org.mbari.siam.utils.FilterInput;
import org.mbari.siam.utils.HeadingFilter;
import org.mbari.siam.utils.MagnitudeFilter;


public class TestHeadingMagFilter{
	static protected Logger _log4j = Logger.getLogger(TestHeadingMagFilter.class);  
	
	double PI=Math.toRadians(180.0);
	
	double duration=86400;
	double inc=600;
	double a=2.0;
	double b=1.0;
	double T1=86400;
	double T2=T1/2.0;
	
	public TestHeadingMagFilter(){
	}
	public TestHeadingMagFilter(String[] args){
		configure(args);
	}
	
	/** Print help message */
	public void printUsage(){
		
		System.out.println("");
		System.out.println(" hmtest [-a <mag>] [-b <mag>] [-t1 <periodSec>] [-t2 <periodSec>] [-d <durationSec>] [-i <incSec>]");
		System.out.println("");
		System.out.println("  a <mag>      : max amplitude          ["+a+"]");
		System.out.println("  b <mag>      : min amplitude          ["+b+"]");
		System.out.println(" t1 <period>   : primary period (sec)   ["+T1+"]");
		System.out.println(" t2 <period>   : secondary period (sec) ["+T2+"]");
		System.out.println("  d <duration> : test cycle (sec)       ["+duration+"]");
		System.out.println("  i <inc>      : test increment (sec)   ["+inc+"]");
		System.out.println("");
		System.out.println(" Test simulates a duration sec period in increments of inc seconds.");
		System.out.println(" Calculates heading and magnitude for portion of elipse (a,b,T1) and ");
		System.out.println(" compound elipse (a,b,T1,T2) traced in duration sec");
		System.out.println("");
	}
	
	/** Delay for the specified number milliseconds in the calling thread */
    public static void delay(long millisecs)
    {
        try 
        {
            Thread.sleep( millisecs );
        } 
        catch ( Exception e ) 
        {
            _log4j.error("wait(...) failed: " + e);
        }
    }
	
	/** configure this instance from command line args */
	public void configure(String[] args){
		if(args.length>0){
			for(int i=0;i<args.length;i++){
				if(args[i].equals("-h") || args[i].equals("--help")){
					printUsage();
					System.exit(0);
				}else if(args[i].equals("-t1")){
					T1=Double.parseDouble(args[i+1]);
				}else if(args[i].equals("-t2")){
					T2=Double.parseDouble(args[i+1]);
				}else if(args[i].equals("-a")){
					a=Double.parseDouble(args[i+1]);
				}else if(args[i].equals("-b")){
					b=Double.parseDouble(args[i+1]);
				}else if(args[i].equals("-d")){
					duration=Long.parseLong(args[i+1]);
				}else if(args[i].equals("-i")){
					inc=Long.parseLong(args[i+1]);
				}
			}
		}
		
	}
	
	/** run the test*/
	public void run(){
		try{		
			_log4j.debug("HeadingMagFilter test");
			_log4j.debug("a:"+a+" b:"+b);
			_log4j.debug("T1:"+T1+" T2:"+T2);
			_log4j.debug("duration:"+duration+" inc:"+inc);
			
			FilterInput xInput=new FilterInput("xIn",FilterInput.DEFAULT_ID  ,1.0,0.0);
			FilterInput yInput=new FilterInput("yIn",FilterInput.DEFAULT_ID+1,1.0,0.0);
			
			HeadingFilter heading=new HeadingFilter("Heading",Filter.DEFAULT_ID,xInput,yInput,HeadingFilter.OUTPUT_DEGREES);
			
			Vector inputs=new Vector();
			inputs.add(xInput);
			inputs.add(yInput);
			MagnitudeFilter magnitude=new MagnitudeFilter("Magnitude",Filter.DEFAULT_ID,inputs);
			
			NumberFormat nf=NumberFormat.getInstance();
			nf.setMaximumFractionDigits(3);
			nf.setMinimumFractionDigits(3);
			nf.setMinimumIntegerDigits(1);
			nf.setGroupingUsed(false);
			
			_log4j.debug("t,x,y,mag,dir");	
			
			for(double t=0;t<=duration;t+=inc){
				double theta=2*PI*t*(1/T1);
				// elipse
				double r=Math.sqrt( (a*a*b*b) / ((a*a*Math.sin(theta)*Math.sin(theta))+(b*b*Math.cos(theta)*Math.cos(theta))));
				double x=r*Math.cos(theta);
				double y=r*Math.sin(theta);
				
				xInput.put(x);
				yInput.put(y);
				System.out.println(nf.format(t)+","+nf.format(x)+","+nf.format(y)+","+nf.format(magnitude.doubleValue())+","+nf.format(heading.doubleValue()));	
			}
			for(double t=0;t<=duration;t+=inc){
				// parametric equation theta=wt=2*PI*t/T
				double alpha=2*PI*t*(1/T1);
				double beta=2*PI*t*(1/T2);
				// driven by two combined ellipses w/ different periods
				double r1=Math.sqrt( (a*a*b*b) / ((a*a*Math.sin(alpha)*Math.sin(alpha))+(b*b*Math.cos(alpha)*Math.cos(alpha))));
				double r2=Math.sqrt( (a*a*b*b) / ((a*a*Math.sin(beta)*Math.sin(beta))+(b*b*Math.cos(beta)*Math.cos(beta))));
				double x=r1*Math.cos(alpha)+r2*Math.cos(beta);
				double y=r1*Math.sin(alpha)+r2*Math.sin(beta);
				xInput.put(x);
				yInput.put(y);
				System.out.println(nf.format(t)+","+nf.format(x)+","+nf.format(y)+","+nf.format(magnitude.doubleValue())+","+nf.format(heading.doubleValue()));	
			}
		}catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/** main */
    public static void main(String args[]){
		
		PropertyConfigurator.configure(System.getProperties());
		PatternLayout layout = new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");
		BasicConfigurator.configure(new ConsoleAppender(layout));

		TestHeadingMagFilter test=new TestHeadingMagFilter(args);
		test.run();
	}		
}