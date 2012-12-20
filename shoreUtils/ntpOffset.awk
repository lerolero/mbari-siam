# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

BEGIN {

  if (ARGC < 3) {
    printf("Usage: nodeName portalLogfile\n");
    error = 1;
    exit;
  }

  # Name of node 
  target = ARGV[1];
  ARGV[1] = "";

  startTime = 0;
}

/Command Response/ {
  if (index($0, target) > 0) {
    if (!startTime) {
      startTime = $1/1000;
    }
    t = $1/1000 - startTime;
    printNext = 1;
  }
}

/ntpdate.*offset/ {
  if (printNext) {
    i = index($0, "offset");
    val = substr($0, i+7) / 1;
    printf "%.1f   %s\n", t/3600, val;
    printNext = 0;
  }
}
