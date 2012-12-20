# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

BEGIN {
  if (ARGC < 3) {
    printf("Usage: nodeName logfile\n");
    error = 1;
    exit;
  }

  # Name of node 
  node = ARGV[1];
  ARGV[1] = "";

  beginWakeupAttempt = "Send wakeup signal to .*" node;
  failedWakeupAttempt = "Exception trying to wake .*" node;
  successfulWakeupAttempt = "wakeup succeeded for .*" node;
  beginProbeAttempt = "Attempt to probe.*" node;
  successfulProbeAttempt = "NodeProbe returned true for .*" node;

}


# Look for wakeup attempt
$0 ~ beginWakeupAttempt {
  targetThread = $2;
  wakeupAttempts++;
  lastWakeupSucceeded = 0;
}


# Look for exception from wakeup
$0 ~ failedWakeupAttempt {
  wakeupFailed++;
}

# Look for successful wakeup
$0 ~ successfulWakeupAttempt {
  wakeupSuccess++;
  lastWakeupSucceeded = 1;
}


# Look for probe attempt 
$0 ~ beginProbeAttempt {
    probeAttempts++;
    lastProbeSucceeded = 0;
}

# Look for successful probe attempt
$0 ~ successfulProbeAttempt {
    probeSuccess++;
    lastProbeSucceeded = 1;
}



END {

  if (wakeupAttempts > 0) {

    printf "%d wakeup attempts, %d successful (%.0f%%): ", wakeupAttempts, wakeupSuccess, 100. * wakeupSuccess/wakeupAttempts;

    if (lastWakeupSucceeded) {
      print "Last wakeup attempt SUCCEEDED";
    }
    else {
      print "Last wakeup attempt FAILED";
    }

  }
  else {
    print "No wakeup attempts found";
  }


  if (probeAttempts > 0) {
    printf "%d probe attempts, %d successful (%.0f%%): ", probeAttempts, probeSuccess, 100. * probeSuccess/probeAttempts;

    if (lastProbeSucceeded) {
      print "Last probe attempt SUCCEEDED";
    }
    else {
      print "Last probe attempt FAILED";
    }
  }
  else {
    print "No probe attempts found";
  }


}

