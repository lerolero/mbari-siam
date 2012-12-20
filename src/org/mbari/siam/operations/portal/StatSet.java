/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.portal;

import java.io.*;
import java.util.Date;
import java.util.Arrays;
import java.util.AbstractCollection;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.NoSuchElementException;
import java.text.NumberFormat;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Level;


public abstract class StatSet{

    // CVS revision 
    private static String _versionID = "$Revision: 1.2 $";

    long _count=0L;
    double _min=Double.MAX_VALUE;
    double _max=Double.MIN_VALUE;
    double _average;
    double _sum;
    double _stdev;
    String _delimiter=";";
    File _outFile;

    public StatSet(){
    }

    public StatSet(String outFile){
	this();
	_outFile=new File(outFile);
    }

    public void setDelimiter(String delimiter){
	_delimiter=delimiter;
    }

    /** Override BufferedWriter's newLine() method, which
	varies the newline character(s) according to 
	platform. ConfigFiles should always use UNIX (\n)
	newline.
    */
    public void newLine(BufferedWriter bw)throws IOException{
	//bw.newLine();
	    
	bw.write('\n');
    }

    public void writeRecord(BufferedWriter bw,String x){
	try{
	    bw.write(x);
	    newLine(bw);
	    bw.flush();
	    return;
	}catch(IOException i){
	    i.printStackTrace();
	}
    }
    public double min(){return _min;}
    public double max(){return _max;}
    public double average(){return _average;}
    public double stdev(){return _stdev;}
    public double sum(){return _sum;}
    public long count(){return _count;}
    public abstract void process(Object value);

    public String toString(){
	String s=_count+this._delimiter+
	    _min+this._delimiter+
	    _max+this._delimiter+
	    _average+this._delimiter+
	    _sum+this._delimiter+
	    _stdev;
	return s;
    }

    public void export(){
	// default do nothing
	//_log4j.debug("StatSet.export");
    }


}
