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
  printf "ricohOffset - look for node %s\n", target;
  ARGV[1] = "";

  startTime = 0;
}



/Command Response:/ {
  if (index($0, target) > 0) {
    inTarget = 1;
    printf "ricohOffset - found target %s\n", target;
  }
  if (inTarget) {
    if (!startTime) {
      startTime = $1/1000;
    }
    t = $1/1000 - startTime;
  }
}

/RicohRTC offset/ {
  if (inTarget) {
    printf "%.1f   %s\n", t/3600, $7;
    inTarget = 0;
  }
}
