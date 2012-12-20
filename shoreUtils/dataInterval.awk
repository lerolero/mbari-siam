# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

# Compute interval between adjacent sensor data packets. Takes output of 
# 'logView' as input.

BEGIN {
##  FS = ", ";
  nPackets = 0;
}

/SensorDataPacket/ {
  sensorData = 1;
  line = 1;
  ### print "# " $0;
  timeTag = $0;
  next;
}

{
  line++;
  if (line == 3 && sensorData) {
    sec = substr($2, 3, 13)/1000.;
    if (nPackets > 0) {
      interval = sec - prev_sec;
      printf "%d %.1f  # %s\n", sec, interval, timeTag;
      x1 += interval;
      x2 += interval * interval;
    }
    nPackets++;
    prev_sec = sec;
    sensorData = 0;  # Done with this packet
  }
}

END {
  mean = x1 / nPackets;
  x2 = x2 / nPackets;
  sigma = sqrt(x2 - mean * mean);
  if (nPackets > 1) {
    std_err = sigma / sqrt(nPackets - 1);
  }
  printf "# Mean interval: %.1f,  sigma: %.1f, stderr: %.1f\n", mean, sigma, std_err;
}
