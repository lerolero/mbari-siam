# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

BEGIN {

  if (ARGC < 3) {
    printf("Usage: nodeID portalLogfile\n");
    error = 1;
    exit;
  }

  # Node's ISI ID
  target = ARGV[1];
  ARGV[1] = "";

  FS=",";
  nCycles = 0;
  # printf("packet#   total     free     used\n");
}

/devid=/ {
  if (index($0, target) == 0) {
    # Not our target
    inTarget = 0;
    next;
  }
  inTarget = 1;

  t = substr($2, 4)/1000;

  #  printf "t = %d\n", t;

  if (t0 == 0) {
    t0 = t;
  }

  elapsed = t - t0;
}

/devid=.*, t=.*/ {
  packetHeader = $0;
}

/jvmthreads:/ {
  if (!inTarget) {
    next;
  }
  # Get timestamp from packet header
  ind = index(packetHeader, "t=") + 2;
  timeStampSec = substr(packetHeader, ind, 14)/1000.;

  i = index($0, ":");
  nThreads = substr($0, i+1);

###  printf "%.1f  %s\n", elapsed/3600, nThreads;
  printf "%.0f  %s\n", timeStampSec, nThreads;
}

