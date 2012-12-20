# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

# Compute % time connected from shore portal host's /var/log/messages file
BEGIN {
  if (ARGC < 3) {
    # Must supply year, since /var/log/messages doesn't specify it!
    printf("Usage: year messageFile\n");
    error = 1;
    exit;
  }

  year = ARGV[1];
  ARGV[1] = "";

  cmdPrefix = "ut2et -format '%b %d %Y %H:%M:%S'";
  started = 0;
}

/Connect time/ {
  connectSec = ($8 * 1.) * 60;
  totalConnectSec += connectSec;
  nConnects++;
}

{
  if (!length($0) || NF == 0) {
    next;
  }

  # Strip leading spaces
  sub(/^ */, "", $0);

  cmd = cmdPrefix " '" $1 " " $2 " " year " " $3 "'";
###  printf "cmd: %s\n", cmd
  cmd | getline epochSec;
###  printf "epochSec %d\n", epochSec;
  close(cmd);
  if (!started) {
    epochSec0 = epochSec;
    startTime = $1 " " $2 " " year " " $3;
    started = 1;
  }
  endTime = $1 " " $2 " " year " " $3;
}


END {
  if (error) {
    exit;
  }

  durationSec = epochSec - epochSec0;
  printf "Start time: %s\n", startTime;
  printf "End time:   %s\n", endTime;
  printf "Test duration: %d sec\n", durationSec;
  printf "total connections: %d\n", nConnects;
  printf "total connection time: %d sec\n", totalConnectSec
  printf "%%connected: %.1f\n", 100. * totalConnectSec / durationSec;
}
