# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.


/INFO.*startSession\(\)\[/ {
##  ipName = substr($5, 16, index($5, "]")-16);
  ### printf "Got ipName: %s\n", ipName;
}

/node address and deviceID/ {

  ipName = substr($9, 1, index($9, "\/")-1);

  nodeID = $10;

# printf "DEBUG %s %s\n", ipName, nodeID; 

  entry = ipName ":" nodeID;

  for (i = 1; i <= nNodes; i++) {
    if (nodes[i] == entry) {
      # Already got this one
      next;
    }
  }
  nodes[++nNodes] = entry;
}


END {
  for (i = 1; i <= nNodes; i++) {
    printf "%s ", nodes[i];
    printf "\n";
  }
}

