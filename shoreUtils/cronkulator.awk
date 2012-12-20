# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

# Estimate generation rate of telemetry for each device specified in
# input.
#
# Each input line contains the following semicolon-delimited fields:
#
# Field
# 1      device ID
# 2      device name
# 3      bytes per packet
# 4      sample interval (seconds)
# 5      fraction of day when generation is active
# 6      filtering skip interval (0 = no filter, 1 = every other packet, etc)
# 7      effective telemetry bit rate
#
# Comments are preceded by the '#' character
BEGIN {
  # Hardcoded for now!!!
  effectiveTelemBps = 7800;
  FS="; ";
}

/^#/ {
  # Comment
  next;
}

{
  if (NF == 0) {
    # Skip blank input lines
    next;
  }

  if (NF != 6) {
    printf "Got %d fields in input line %d; expecting 5 fields\n", NF, NR;
    next;
  }

  bytesPerPacket = $3;
  sampleInterval = $4 / 3600.;
  skip = $5;
  activeFraction = $6;

  bytesPerDay = bytesPerPacket * 24./sampleInterval * 1./(1. + skip) * activeFraction;

  airTime = bytesPerDay * 8. / effectiveTelemBps;

  totalBytesPerDay += bytesPerDay;
  totalAirTime += airTime;
## printf "%d", bytesPerPacket, sampleInterval, skip, activeFraction
  printf "%d ; %20s; %8d bytes/day; %d sec/day\n", $1, $2, bytesPerDay, airTime;
}

END {

// Empirical factor to adjust to observed air times
fudgeFactor = 2.90;


printf "\nTotal:   bytes/day=%d   airtime=%.1f min/day\n", totalBytesPerDay, totalAirTime/60.;

printf "'Corrected' airtime=%.1f min/day (adjust factor=%.2f)\n", totalAirTime/60*fudgeFactor, fudgeFactor;

}

