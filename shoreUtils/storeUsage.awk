# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

# Print storage use as function of elapsed time (hours), based on node 
# log input
/devid=.*t=.*seqNo=/ {

  if (nPackets == 0) {
    # First packet - this is the 'base time'
    baseTime = substr($2, 3, 13);
  }

  nPackets++;
  timeTag = $2;
}


/\/dev\/hda1/ {

  t = substr(timeTag, 3, 13);
  elapsedHours = (t - baseTime) / 1000 / 3600 ;

  MbytesAvailable = $4 / 1024;
  printf "%.1f  %d\n", elapsedHours, MbytesAvailable;

  nFound++;
  if (nFound == 1) {
    Mbytes0 = MbytesAvailable;
  }

}

END {
  printf "Storage rate: %.1f Mbytes/day\n", 24 * (Mbytes0 - MbytesAvailable)/elapsedHours;
  
}

