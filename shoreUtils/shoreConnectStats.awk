# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

BEGIN {
  minStartSec = ARGV[1];
  ARGV[1] = "";


  minDurationSec = 99999;
  maxDurationSec = -99999;
}



{
  if ($2 < minStartSec) {
    next;
  }
}



/IP-UP/ {

  startSec = $2;

  if (firstStartSec == 0) {
    # Record first start time
    firstStartSec = startSec;
  }
  else {
    meanIntervalSec += (startSec - prevStartSec);
  }
  prevStartSec = startSec;
}


/IP-DOWN/ {

  if (firstStartSec == 0) {
    # Need a preceding IP-UP...
    next;
  }
  endSec = $2;
  durationSec = endSec - startSec;

  if (durationSec < minDurationSec) {
    minDurationSec = durationSec;
  }

  if (durationSec > maxDurationSec) {
    maxDurationSec = durationSec;
  }

  meanDurationSec += durationSec;
  nConnect++;
}




END {

  printf "\nStatistics start: %s\n", strftime("%m/%d/%Y  %H:%M:%S", firstStartSec);
  printf "Statistics end: %s\n", strftime("%m/%d/%Y  %H:%M:%S", endSec);

  totalConnectedSec = meanDurationSec;
  meanDurationSec /= nConnect;
  printf "Connections: %d\n", nConnect;
  printf "Connected for %.1f %% of the time\n", 100.*totalConnectedSec/(endSec - firstStartSec);

  printf "Mean connection duration: %d sec\n", meanDurationSec;
  printf "Min duration: %d sec\nMax duration: %d sec\n", minDurationSec, maxDurationSec;

  printf "Mean connect interval: %d sec\n", meanIntervalSec/nConnect;
}


function epochSec() {
  cmd = cmdPrefix " " "'" $1 " " $2 " " year " " $3 "'";
  ### printf "cmd: %s\n", cmd; 
  cmd | getline eSec;
  close(cmd);
  return eSec;
}  
