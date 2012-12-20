# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

/Process.*pon globalstar/ {
  nConnects++;
  startTime = $1;
  if (nSessions > 0) {
    connectInterval = (startTime - prevStartTime)/1000;
    avgConnectInterval += connectInterval;
  }
  prevStartTime = startTime;

  error = 0;
}

/pon globalstar failed/ {
  error = 1;
}

/Process.*poff globalstar/ {
  endTime = $1;
  sessionDuration = (endTime - startTime)/1000;
  avgSessionDuration += sessionDuration;
  prevEndTime = endTime;
  nSessions++;
}

END {
  printf "Total connection attempts: %d\n", nConnects;
  printf "Avg session duration: %d sec\n", avgSessionDuration / nSessions;
  printf "Avg connect attempt interval: %d sec\n", avgConnectInterval / nConnects;
}


