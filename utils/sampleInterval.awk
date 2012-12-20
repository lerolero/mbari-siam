# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

# Compute and sampling intervals. 
# Input file is 'logView' output

/MetadataPacket/ {
  inData = 0;
}


/SensorDataPacket/ {
  inData = 1;
}

/devid=[0-9].*t=[0-9].*/ {
  if (!inData) {
    # Not in data packet
    next;
  }

  # Compute seconds
  sec = substr($2, 3)/1000;

  if (nSamples++ > 0) {
    interval = sec - prevSec;
    avgInterval += interval;
    print nSamples, interval;
  }

  prevSec = sec;
}


END {
  printf "#samples: %d\n", nSamples;
  printf "avg sample interval: %f\n", avgInterval/(nSamples-1);
}

