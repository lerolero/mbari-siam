/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.utils.filter;

import org.mbari.siam.utils.Filter;
import org.mbari.siam.utils.FilterInput;
import org.mbari.siam.utils.WeightedAverageFilter;
import java.util.Vector;

public class TestWeightedAvgFilter{
	
	public static void printUsage(){
		
		System.out.println("");
		System.out.println("watest [-b <begin>] [-e <end>] [-i <increment>]");
		System.out.println("");
	}
	
	/** main */
    public static void main(String args[])
    {
		double begin=0.0;
		double end=10.0;
		double inc=2.0;
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
		
				
		System.out.println("WeightedAverageFilter test");
		try{
		FilterInput dflt=new FilterInput(FilterInput.DEFAULT_NAME,FilterInput.DEFAULT_ID,1.0,0.0);
		FilterInput foo=new FilterInput("foo",FilterInput.DEFAULT_ID+1,2.0,0.0);
		FilterInput bar=new FilterInput("bar",FilterInput.DEFAULT_ID+2,4.0,0.0);
		Vector inputs=new Vector();
		inputs.add(dflt);
		inputs.add(foo);
		inputs.add(bar);
		WeightedAverageFilter filter=new WeightedAverageFilter("WAFilterTest",Filter.DEFAULT_ID,inputs);
		
		FilterInput input=filter.getInput(FilterInput.DEFAULT_NAME);
		for(double i=begin;i<end;i+=inc){
			dflt.put((double)i);
			foo.put((double)i);
			bar.put((double)i);
			System.out.println("i:"+i+" output="+filter.doubleValue());		
			System.out.println(filter);
		}
		foo.setInhibit(true);
		for(double i=begin;i<end;i+=inc){
			dflt.put((double)i);
			foo.put((double)i);
			bar.put((double)i);
			System.out.println("i:"+i+" output="+filter.doubleValue());		
			System.out.println(filter);
		}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}		
}