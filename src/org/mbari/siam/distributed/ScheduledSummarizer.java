/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.distributed;

import java.rmi.RemoteException;


/**
   A ScheduledSummarizer generates summaries on a specified schedule. 
 */
public interface ScheduledSummarizer 
    extends Summarizer {

    public void setSummarySchedule(ScheduleSpecifier schedule) 
	throws RemoteException;

}

