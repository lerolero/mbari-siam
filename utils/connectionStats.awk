# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

BEGIN {
  # Unfortunately, year doesn't appear in input timestrings - need to assume
  # current year
  dateCmd = "date +%Y";
  dateCmd | getline year;
  close(dateCmd);

  cmdPrefix = "ut2et -format '%b %d %Y %H:%M:%S'";

  minDurationSec = 9999999;
  maxDurationSec = 0;

  minReconnectSec = 9999999;
  maxReconnectSec = 0;
}


/pppd.*started by/ {
  nAttempts++;
}

/RSSI/ {
  nRSSI++;
}


/local.*IP address/ {
  # Convert timestring to epoch seconds
  startSec = epochSec();

  if (nConnect > 0) {
    reconnectSec = startSec - endSec;
    if (reconnectSec < minReconnectSec) {
      minReconnectSec = reconnectSec;
    }
    if (reconnectSec > maxReconnectSec) {
      maxReconnectSec = reconnectSec;
    }
    meanReconnectSec += reconnectSec;
  }

  if (prevStartSec > 0) {
    meanIntervalSec += (startSec - prevStartSec);
  }
  prevStartSec = startSec;
}


/Connect time.*minutes/ {
  endSec = epochSec();
  if (startSec > 0) {

    if (firstStartSec == 0) {
      # Keep track of very first connection 
      firstStartSec = startSec;
    }

    nConnect++;
    ### printf "%s <--- (%.1f minutes)\n", $0, (endSec - startSec)/60.;
    duration = endSec - startSec;
    if (duration < minDurationSec) {
      minDurationSec = duration;
    }
    if (duration > maxDurationSec) {
      maxDurationSec = duration;
    }

    meanDurationSec += duration;
  }
}


END {

  printf "\nTest start: %s\n", strftime("%m/%d/%Y  %H:%M:%S", firstStartSec);
  printf "Test end: %s\n", strftime("%m/%d/%Y  %H:%M:%S", endSec);

  meanReconnectSec /= nConnect;
  totalConnectedSec = meanDurationSec;
  meanDurationSec /= nConnect;
  printf "\nTotal connection attempts: %d\n", nAttempts;
  printf "Successful connections: %d (%.1f %% successful)\n", nConnect, 100.*nConnect/nAttempts;
  printf "Connected for %.1f %% of the time\n", 100.*totalConnectedSec/(endSec - firstStartSec);
  nMissed = nAttempts - nRSSI;
  printf "Missed RSSI points: %d (%.1f %%)\n", nMissed, 100.*nMissed/nAttempts;
 printf "\nMean connect establish time: %d sec\n", meanReconnectSec;
  printf "Min connect establish time: %d sec\nMax connect establish time: %d sec\n", minReconnectSec, maxReconnectSec;
  printf "\nMean connection duration: %d sec\n", meanDurationSec;
  printf "Min duration: %d sec\nMax duration: %d sec\n", minDurationSec, maxDurationSec;

  printf "\nMean connect interval: %d sec\n", meanIntervalSec/nConnect;


}


function epochSec() {
  cmd = cmdPrefix " " "'" $1 " " $2 " " year " " $3 "'";
  ### printf "cmd: %s\n", cmd; 
  cmd | getline eSec;
  close(cmd);
  return eSec;
}  
