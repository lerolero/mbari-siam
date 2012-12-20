# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

# Convert ThreadUtility.printThreads() output to format suitable for
# gnu 'graph'
/ERROR/ || /INFO/ || /DEBUG/ {
  elapsed = $1 / 1000;
}

/includes about/ {
  printf "%d  %d\n", elapsed, $5;
}
