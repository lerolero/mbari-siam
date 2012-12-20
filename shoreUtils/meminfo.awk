# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

# Determine JVM memory usage on specified node.
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
  if (index($1, target) == 0) {
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



/jvmmem/ {
  if (!inTarget) {
    next;
  }

  # Parse total and free memory, calculate used memory
  start = index($0, "total=") + 6;
  stop = index($0, ", ");
  total = substr($0, start, stop - start);
  start = index($0, "free=") + 5;
  stop = index($0, "(bytes)") - 1;
  free = substr($0, start, stop - start);
#  printf("%d,    %s,  %s,  %d\n", nPacket++, total, free, total - free);

  printf "%.1f  %d  %d  %d\n", elapsed/3600, total, free, total-free;
 
}

/jvmthreads/ {
  if (!inTarget) {
    next;
  }
  ### print;
}


