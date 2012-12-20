/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.operations.utils;
import java.io.File;
import com.topcoder.file.split.FileJoiner;

public class FileJoinerUtil {

    public static void main(String[] args) {

	if (args.length < 1) {
	    System.err.println("usage: firstSegment");
	    return;
	}

	File firstSegment = new File(args[0]);

	FileJoiner joiner = new FileJoiner(firstSegment);

	try {
	    File joinedFile = joiner.join();
	    System.out.println("Joined file " + joinedFile.getName() + 
			       " - " + joinedFile.length() + " bytes");
	}
	catch (Exception e) {

	    e.printStackTrace();
	}

	return;
    }

}
