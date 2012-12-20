/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.utils.filter;

import org.mbari.siam.utils.Filter;
import org.mbari.siam.utils.FilterInput;
import org.mbari.siam.utils.BoxcarFilter;

public class TestBoxcarFilter{
	
	public static void printUsage(){
		
		System.out.println("");
		System.out.println("bctest [-d <filterDepth>] [-b <begin>] [-e <end>] [-i <increment>]");
		System.out.println("");
	}
	
	/** main */
    public static void main(String args[])
    {
		int depth=10;
		double begin=0.0;
		double end=20.0;
		double inc=1.0;
		
		if(args.length>0){
			for(int i=0;i<args.length;i++){
			if(args[i].equals("-h") || args[i].equals("--help")){
				printUsage();
				System.exit(0);
			}else if(args[i].equals("-d")){
				depth=Integer.parseInt(args[i+1]);
			}else if(args[i].equals("-b")){
				begin=Double.parseDouble(args[i+1]);
			}else if(args[i].equals("-e")){
				end=Double.parseDouble(args[i+1]);
			}else if(args[i].equals("-i")){
					inc=Double.parseDouble(args[i+1]);
				}
			}
		}
		
		System.out.println("BoxcarFilter test");
		BoxcarFilter filter=new BoxcarFilter("BoxcarTest",Filter.DEFAULT_ID,depth);
		FilterInput finput=null;
		try{
			finput=new FilterInput(FilterInput.DEFAULT_NAME,FilterInput.DEFAULT_ID);
			filter.addInput(finput);
		}catch(Exception e){
			e.printStackTrace();
		}
		
		for(double i=begin;i<end;i+=inc){
			finput.put((double)i);
			System.out.println("i:"+i+" output="+filter.doubleValue());		
		}
		System.out.println(filter);
		System.out.println(finput);
	}		
}