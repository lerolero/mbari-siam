// Copyright MBARI 2004
package org.mbari.siam.operations.utils;

import org.mbari.siam.distributed.Node;
import org.mbari.siam.distributed.leasing.LeaseDescription;


public class Leases extends NodeUtility {

    boolean isPrimary=true;

/** Do not accept any "custom" arguments. */
   public void processCustomOption(String[] args, int index)
	throws InvalidOption {
       if(args[index].trim().equalsIgnoreCase("-aux"))
	   isPrimary=false;
       else
	   throw new InvalidOption("Invalid option: " + args[index]);
   }


/** Get and print lease(s) status. */
   public void processNode(Node node)
	throws Exception {
   	LeaseDescription[] leases = node.getLeases(isPrimary);
	System.out.println("\nFound " + leases.length + " Lease(s).\n");
	for(int i=0; i<leases.length; i++) {
	   System.out.println(leases[i]);
        }
   }

/** Print usage message. */
   public void printUsage() {
	System.err.println("");
	System.err.println("usage: Leases nodeURL [-aux]\n");
	System.err.println("nodeURL: valid node name[-aux]");
	System.err.println("aux:     use auxilliary link (e.g. redundant globalstar) ["+isPrimary+"]");
   }


/** Main method. */
   public static void main(String[] args) {
	Leases status = new Leases();
	status.processArguments(args);
	int stat = status.run();
	System.exit(stat);
   } //end of main

} //end of Leases class

