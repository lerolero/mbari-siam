/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.utils.filter;

import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

import org.mbari.siam.utils.Filter;
import org.mbari.siam.utils.FilterInput;
import org.mbari.siam.utils.WeightedAverageFilter;


public class TestCascadedFilter{
	static protected Logger _log4j = Logger.getLogger(TestCascadedFilter.class);  

	public static void printUsage(){
		
		System.out.println("");
		System.out.println("cascadeFilterTest [-b <begin>] [-e <end>] [-i <increment>]");
		System.out.println("");
	}
	
	/** main */
    public static void main(String args[])
    {
		PropertyConfigurator.configure(System.getProperties());
		PatternLayout layout = new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");
		BasicConfigurator.configure(new ConsoleAppender(layout));
		
		double begin=0.0;
		double end=10.0;
		double inc=1.0;
		if(args.length>0){
			for(int i=0;i<args.length;i++){
				if(args[i].equals("-h") || args[i].equals("--help")){
					printUsage();
					System.exit(0);
				}else if(args[i].equals("-b")){
					begin=Double.parseDouble(args[i+1]);
				}else if(args[i].equals("-e")){
					end=Double.parseDouble(args[i+1]);
				}else if(args[i].equals("-i")){
					inc=Double.parseDouble(args[i+1]);
				}
			}
		}
		
				
		System.out.println("CascadedFilter test");
		try{
		FilterInput input1_1x=new FilterInput("f1.1x",FilterInput.DEFAULT_ID,1.0,0.0);
		FilterInput input1_2x=new FilterInput("f1.2x",FilterInput.DEFAULT_ID,2.0,0.0);
		Vector inputs=new Vector();
		inputs.add(input1_1x);
		inputs.add(input1_2x);
		WeightedAverageFilter filter1=new WeightedAverageFilter("Filter1",Filter.DEFAULT_ID,inputs);

		FilterInput input2_1x=new FilterInput("f2.1x",FilterInput.DEFAULT_ID,1.0,0.0);
		Vector inputs2=new Vector();
		inputs2.add(input2_1x);
		WeightedAverageFilter filter2=new WeightedAverageFilter("Filter2",Filter.DEFAULT_ID,inputs2);
		filter1.attach(input2_1x);
		
		for(double i=begin;i<end;i+=inc){
			input1_1x.put((double)i);
			input1_2x.put((double)i);
			System.out.println(filter1);
			System.out.println(filter2);
		}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}		
}