/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.utils.filter;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

import org.mbari.siam.utils.Filter;
import org.mbari.siam.utils.FilterInput;
import org.mbari.siam.utils.RangeValidator;
import org.mbari.siam.utils.RangeFilter;

public class TestRangeFilter{
	/** Log4j logger */
    protected static Logger _log4j = Logger.getLogger(TestRangeFilter.class);
	
	
	public static void printUsage(){
		
		System.out.println("");
		System.out.println("rftest [-n <avgN>] [-lb <lowerBound>] [-ub <upperBound>] [-c <constant>] [-ri] [-ro] [-il] [-iu] [-rp <R|L|A>]");
		System.out.println("");
	}
	
	/** main */
    public static void main(String args[])
    {
		PropertyConfigurator.configure(System.getProperties());
		PatternLayout layout = new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");
		BasicConfigurator.configure(new ConsoleAppender(layout));
		
		int region=RangeValidator.RANGE_INSIDE;
		int rejectPolicy=RangeValidator.REJECT;
		int averageN      =  1;
		double lowerBound = -1;
		double upperBound =  1;
		boolean includeLower=false;
		boolean includeUpper=false;
		double constant=0.0;
		RangeValidator validator=null;

		if(args.length>0){
			for(int i=0;i<args.length;i++){
				if(args[i].equals("-h") || args[i].equals("--help")){
					printUsage();
					System.exit(0);
				}else if(args[i].equals("-n")){
					averageN=Integer.parseInt(args[i+1]);
				}else if(args[i].equals("-lb")){
					lowerBound=Double.parseDouble(args[i+1]);
				}else if(args[i].equals("-ub")){
					upperBound=Double.parseDouble(args[i+1]);
				}else if(args[i].equals("-ri")){
					region=RangeValidator.RANGE_INSIDE;
				}else if(args[i].equals("-ro")){
					region=RangeValidator.RANGE_OUTSIDE;
				}else if(args[i].equals("-c")){
					constant=Double.parseDouble(args[i+1]);
				}else if(args[i].equals("-il")){
					includeLower=true;
				}else if(args[i].equals("-iu")){
					includeUpper=true;
				}else if(args[i].equals("-rp")){
					if(args[i+1].equalsIgnoreCase("R")){
						rejectPolicy=RangeValidator.REJECT;
					}else if(args[i+1].equalsIgnoreCase("L")){
						rejectPolicy=RangeValidator.USE_LAST_VALID;
					}else if(args[i+1].equalsIgnoreCase("A")){
						rejectPolicy=RangeValidator.USE_AVERAGE;
					}else if(args[i+1].equalsIgnoreCase("C")){
						rejectPolicy=RangeValidator.USE_CONSTANT;
					}
				}
			}
		}
		
		System.out.println("RangeFilter test");
		FilterInput input=new FilterInput("rfi",FilterInput.DEFAULT_ID);
		try{
			switch(rejectPolicy){
				case RangeValidator.REJECT:
					validator=RangeValidator.getRejectingValidator( region,  lowerBound,  upperBound,  includeLower,  includeUpper );
					break;
				case RangeValidator.USE_LAST_VALID:
					validator=RangeValidator.getLastValidValidator( region,  lowerBound,  upperBound,  includeLower,  includeUpper );
					break;
				case RangeValidator.USE_AVERAGE:
					validator=RangeValidator.getAveragingValidator( region,  lowerBound,  upperBound,  includeLower,  includeUpper,  averageN );
					break;
				case RangeValidator.USE_CONSTANT:
					validator=RangeValidator.getConstantValidator( region,  lowerBound,  upperBound,  includeLower,  includeUpper,  constant);
					break;
			}
			RangeFilter filter=new RangeFilter("foo",Filter.DEFAULT_ID,input,validator);
			
			double test=lowerBound-1.0;
			input.put(test);
			System.out.println("input:"+test);
			System.out.println(filter);
			//System.out.println(input+"\n");
			
			test=upperBound+1.0;
			input.put(test);
			System.out.println("input:"+test);
			System.out.println(filter);
			//System.out.println(input+"\n");
			
			test=lowerBound;
			input.put(test);
			System.out.println("input:"+test);
			System.out.println(filter);
			//System.out.println(input+"\n");
			
			test=upperBound;
			input.put(test);
			System.out.println("input:"+test);
			System.out.println(filter);
			//System.out.println(input+"\n");

			test=(upperBound+lowerBound)/2.0;
			input.put(test);
			System.out.println("input:"+test);
			System.out.println(filter);
			//System.out.println(input+"\n");
			
			test=lowerBound-1.0;
			input.put(test);
			System.out.println("input:"+test);
			System.out.println(filter);
			//System.out.println(input+"\n");
			
			test=upperBound+1.0;
			input.put(test);
			System.out.println("input:"+test);
			System.out.println(filter);
			//System.out.println(input+"\n");
			
		}catch (Exception e) {
			e.printStackTrace();
		}		
	}		
}