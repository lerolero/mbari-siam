# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.


/INFO/ {
  # Need to save this, as some INFO lines don't have time in first column
  timeTag = $1;
}

/DEBUG/ {
  # Need to save this, as some DEBUG lines don't have time in first column
  timeTag = $1;
}

/Portal got connection/ {
  startTime = $1;
  print "CONNECTED";
  if (nConnects > 0) {
    connectInterval = (startTime - prevStartTime)/1000;
    avgConnectInterval += connectInterval;
    downTime = (startTime - prevFinishTime)/1000;
    avgDownTime += downTime;
  }
  prevStartTime = startTime;
  nConnects++;
}

/startSession.*mse.*done/ {
  printf "GOT END\n";
  downloadTime = (timeTag - startTime)/1000;
  prevFinishTime = timeTag;
  avgDownloadTime += downloadTime;
  nCompleteSessions++;
  printf "Download time: %d sec\n", downloadTime; 
}

END {
  printf "\n";
  printf "Total connections: %d\n", nConnects;
  printf "Total downloads: %d\n", nCompleteSessions;
  if (nCompleteSessions > 0) {
    printf "Avg download time: %d sec\n", avgDownloadTime / nCompleteSessions;
  }
  else {
    printf "No downloads found!\n";
  }
  if (nConnects > 0) {
    printf "Avg connect interval: %d sec\n", avgConnectInterval / nConnects;
  }
  else {
    printf "No connections found!\n";
  }

}
