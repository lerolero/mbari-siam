# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

# Convert times in NMCMooring log to date strings

/t=/ {
  # Find start of seconds 
  start = match($0, "t=[0-9]*");

  if (start > 0) {
    secs = substr($0, start+2, RLENGTH - 2) / 1000;

    printf "%s  UTC=", $0, secs;
    cmd = sprintf("et2ut %d", secs);
    system(cmd);
    next
  }
}

{ print }
