# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

BEGIN {
  if (ARGC < 3) {
    printf("Usage: node-ISI-ID logfile\n");
    error = 1;
    exit;
  }

  # Name of node 
  node = ARGV[1];
  ARGV[1] = "";

  target = "Node.*" node " has.*subnodes";
}


# Look for subnode list
$0 ~ target {

  # Blank out leading fields; just preserve actual list
  $1 = $2 = $3 = $4 = $5 = $6 = $7 = $8 = $9 = "";
  subnodes = $0;
}



END {
  # Print latest ("current") subnode list
  printf "subnodes: %s\n", subnodes;
}

