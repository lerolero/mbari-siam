# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

# Get sleep statistics of specified node from portal log file
BEGIN {

  if (ARGC < 3) {
    printf("Usage: nodeID portalLogfile\n");
    error = 1;
    exit;
  }

  # Name of node 
  target = ARGV[1];
  ARGV[1] = "";

  FS=",";
  nCycles = 0;
}


/devid=/ {
  if (index($0, target) > 0) {
    inTarget = 1;
  }
  else {
    inTarget = 0;
  }
}

/sleep.*pred=.*act=.*/ {

  if (!inTarget) {
    # Not the node we're looking for
    next;
  }

  nCycles++;

  cmd = sprintf("ut2et -format '%%D %%r' '%s'", $2);
  cmd | getline startTime;
  close(cmd);
  if (firstTime == 0) {
    firstTime = startTime;
  }
  lastTime = startTime;
  i = index($4, "=");
  duration = substr($4,i+1);
  avgDuration += duration;
  printf("%d  %s\n", startTime, duration);
}


END {
  if (error) {
    exit;
  }

  if (nCycles == 0) {
    printf("Target %s did not sleep!\n", target);
  }
  else {

    if (lastTime != firstTime) {
      printf("Avg freq=%d cycles/hr, ", \
	     nCycles/(lastTime - firstTime) * 3600);
    }

    printf("Avg duration=%d sec, nCycles=%d\n", avgDuration/nCycles, nCycles);
  }
}
