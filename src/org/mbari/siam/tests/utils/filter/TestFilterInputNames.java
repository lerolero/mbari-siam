/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.utils.filter;

import org.mbari.siam.utils.Filter;
import org.mbari.siam.utils.UnityFilter;
import org.mbari.siam.utils.FilterInput;

public class TestFilterInputNames{
	
	public static void printUsage(){
		
		System.out.println("");
		System.out.println("fntest");
		System.out.println("");
	}
	
	/** main */
    public static void main(String args[])
    {
		/*
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
		*/
		System.out.println("FilterName test");
		UnityFilter filter=new UnityFilter("testFilter",Filter.DEFAULT_ID);

		try{
			System.out.println("adding input "+FilterInput.DEFAULT_NAME+"...");
			FilterInput fie=new FilterInput(FilterInput.DEFAULT_NAME,FilterInput.DEFAULT_ID);
			FilterInput foo=new FilterInput("foo",FilterInput.DEFAULT_ID,2.0,0.0);
			FilterInput bar=new FilterInput("bar",FilterInput.DEFAULT_ID,1.5,100);
			filter.addInput(fie);
			System.out.println("adding input foo...");
			filter.addInput(foo);
			System.out.println("adding input bar...");
			filter.addInput(bar);
			System.out.println("adding input foo (should generate exception)...");
			filter.addInput(foo);
		}catch(Exception e){
			e.printStackTrace();
		}
		int i=filter.inputCount();
		System.out.println("inputs:"+i);		
		System.out.println("default:"+filter.indexOf(FilterInput.DEFAULT_NAME));		
		System.out.println("foo:"+filter.indexOf("foo"));		
		System.out.println("bar:"+filter.indexOf("bar"));	
		System.out.println("baz:"+filter.indexOf("baz"));	
		
		System.out.println(filter);

		FilterInput dflt=filter.getInput("default");
		FilterInput foo=filter.getInput("foo");
		System.out.println("putting 1 to default...");		
		dflt.put(1);
		System.out.println(filter);
		System.out.println("putting 1 to foo...");		
		foo.put(1);
		System.out.println(filter);
	}		
}