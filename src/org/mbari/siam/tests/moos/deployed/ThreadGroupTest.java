/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.moos.deployed;

import java.lang.Thread;
import java.lang.ThreadGroup;

/**
   This code demonsrates the use of ThreadGroups; these
   are used in NodeService to manage the group of SafeWorker
   threads used to put the instruments in safe mode; NodeService
   pends on the SafeWorker thread group before exiting.
*/

public class ThreadGroupTest extends Thread{

    String _name="CrinkleWinkieGroup";
    int _threads=10;
    long _maxWaitSec=7L;

    ThreadGroupTest(String groupName, int threads, long maxWaitSec){
	_name=groupName;
	_threads=threads;
	_maxWaitSec=maxWaitSec;
    }

    public void run(){
	ThreadGroup group=new ThreadGroup(_name);
	System.out.println("\nmain: Spinning up threads...\n");
	for(int i=1;i<=_threads;i++){
	    System.out.println("thread "+i+" timeout "+i+" sec");
	    GroupWorker gw=new GroupWorker(group,i,i*1000L);
	    gw.start();
	}
	System.out.println("\nmain: waiting "+_maxWaitSec+" sec for threads to exit...\n");
	long now=System.currentTimeMillis();
	while(group.activeCount()>0 && 
	      (System.currentTimeMillis()-now)<_maxWaitSec*1000L){
	    try{
		Thread.sleep(100);
	    }catch(InterruptedException e){}
	}
	if(group.activeCount()>0){
	    System.out.println("\nmain: done w/ threads still running");
	}else{
	    System.out.println("\nmain: done w/ threads complete");
	}
	System.exit(0);

    }

    class GroupWorker extends Thread{
	int _id=-1;
	long _timeout=0L;

	public GroupWorker(ThreadGroup group, int id, long timeoutMSec){
	    super(group,(Thread)null);
	    _id=id;
	    _timeout=timeoutMSec;
	}

	public void run(){
	    try{
		System.out.println("thread "+_id+" in group "+
				   this.getThreadGroup().getName()+
				   ": sleeping for "+
				   _timeout+" ms");
		
		Thread.sleep(_timeout);
		System.out.println("thread "+_id+": time's up");
	    }catch(InterruptedException e){
		
	    }
	}
    }

    public static void main(String[] args) {
	long maxWaitSec=7L;
	if(args.length>0)
	    maxWaitSec=Long.parseLong(args[0]);

	ThreadGroupTest tgt=new ThreadGroupTest("Quux",10,maxWaitSec);
	tgt.start();
    }
}
