/** 
* @Title adcp2oasis
* @author Kent Headley
*
* Copyright MBARI 2003
* 
* REVISION HISTORY:
*/
package org.mbari.siam.utils;

import java.util.*;
import java.lang.Integer;
import java.lang.Long;
import java.text.NumberFormat;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
/**
 * Quick and dirty hack to convert ASCII hex ADCP data 
 * from SSDS website to OASIS format for QC by NPS (Fred Bahr).
 *
 * The OASIS record consists of a timestamp (decimal day of year) followed by 
 * the ASCII hex adcp data, separated by whitespace.
 *
 * Sends data to standard out; for piping and redirection to output file 
 * or other processes
 *  infile path to input data file
 *  timestampField Field index of timestamp in input data record
 *  dataField Field index of data in input data record 
 * @see OasisTimestamp
 */
public class Adcp2Oasis{

    /** Constructor */
    private Adcp2Oasis(){
        // singleton
    }

    /** This is the processing loop. 
     * @param infile path to input data file
     * @param tsField Field index of timestamp in input data record
     * @param dataField Field index of data in input data record
     * @see OasisTimestamp
     */
    public void process(String infile, int tsField, int dataField){

	try{
	    // Get a BufferedReader to get the data one line at a time
	    BufferedReader br=null;
	    br = new BufferedReader(new FileReader(infile));

	    // Get lines of data one by one
	    String line="";
	    while( (line = br.readLine())!=null){
		// get a StringTokenizer to chop up the line
		StringTokenizer st = new StringTokenizer(line,",");

		String tsString = null;
		String dataString = null;

		// iterate through the tokens and save the timestamp and data
		// stop when you've got them both
		for(int t=0;st.hasMoreTokens();t++){
		    String nextToken = st.nextToken();
		    if(t==tsField)
			tsString = nextToken;
		    if(t==dataField)
			dataString = nextToken;
		if(dataString!=null && tsString!=null)
		    break;
		}

		// if both timestamp and data were found...
		if(dataString!=null && tsString!=null){
		    OasisTimestamp ots = new OasisTimestamp(Long.parseLong(tsString));
		    System.out.println(ots+" "+dataString);
		}else{
		    System.out.println("## Warning: invalid line ");
		}
	    }
	    // close the input file
	    br.close();
	}catch(IOException e){
	    System.err.println(e);
	    e.printStackTrace();
	    System.exit(-1);
	}
    }
    public static void main(String args[]){

	// print use message if incorrect number of args
	if(args.length<3){
	    System.err.println("usage: adcp2oasis file timestampColumn dataColumn");
	    System.exit(0);
	}

	// extract args
	String infile = args[0];
	int tsCol = Integer.parseInt(args[1]);
	int dataCol = Integer.parseInt(args[2]);

	// make a new Adcp2Oasis and run it
	Adcp2Oasis x = new Adcp2Oasis();
	x.process(infile,tsCol,dataCol);
    }

}







