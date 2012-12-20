/****************************************************************************/
/* Copyright 2003 MBARI.                                                    */
/* Monterey Bay Aquarium Research Institute Proprietary Information.        */
/* All rights reserved.                                                     */
/****************************************************************************/
package org.mbari.siam.core;
import java.text.DateFormat;
import java.util.Date;

import org.mbari.siam.distributed.leasing.LeaseRefused;
import org.mbari.siam.distributed.leasing.LeaseManager;

import org.apache.log4j.Logger;

/**
   CommsSchedulerTask is based on the old LeaseMaintenanceTask (deprecated),
   and is responsible for periodically acquiring a lease for the Node's
   communications medium.  CommsManager is responsible for informing the
   Portal that the link is up/down.
   <p> The purpose of this task is to keep the comms link active for
   <leaseInterval> seconds every <leaseRenewalInterval> seconds (typically
   2 minutes every 1/2 hour).  Thus, we never call LeaseManager.terminate() --
   the lease is allowed to expire.
*/
class CommsSchedulerTask extends SiamTimerTask {

    long _leasePeriod = 0;
    long _leaseRenewalInterval = 0;
    LeaseManager _leaseManager = null;
    DateFormat _dateFormatter = null;
    private static Logger _logger = Logger.getLogger(CommsSchedulerTask.class);

    public CommsSchedulerTask(LeaseManager leaseManager, 
			      long leasePeriod, long leaseRenewalInterval)
    {
	_leasePeriod = leasePeriod;
	_leaseRenewalInterval = leaseRenewalInterval;
	_leaseManager = leaseManager;
	_dateFormatter = DateFormat.getDateTimeInstance();
    }

    /** */
    public void run() {

        long now = System.currentTimeMillis();
	_logger.info("CommsSchedulerTask.run(): " +
		     _dateFormatter.format(new Date(now)));

	try {
	  _leaseManager.establish(_leasePeriod, now + _leaseRenewalInterval,
				  "comms scheduler");
	  
	} catch (LeaseRefused e) {
	    _logger.error("CommsSchedulerTask caught LeaseRefused: " + e);
	} catch (Exception e) {
	    _logger.error("CommsSchedulerTask exception: " + e);
	}
    }
}
