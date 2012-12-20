# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

# Parse turbidity event detector SBD message file
BEGIN {
  sampleIntervalSec = ARGV[1];
  ARGV[1] = "";
  FS = ",";
  msgFldNo = 0;

  timeCmdPrefix = "et2ut";

  printf "# Epoch    STA/LTA\n";
}

/TurbidityEventDetector.*triggered/ {
  for (i = 1; i <= NF; i++) {
    # printf "field %d, msgFldNo=%d: %s\n", i, msgFldNo, $i;
    if (index($i, "||") > 0) {

      inMsg = 1;
      msgFldNo = 1;
    }

    if (msgFldNo == 2) {
      time = $i;
    }
    else if (msgFldNo == 3) {
      ind = index($i, ":");
      state = substr($i, 24, ind-24);
    }
    else if (msgFldNo == 4) {
      ind = index($i, ":");
      staWidth = substr($i, ind+1);
      ### printf "# STA width: %d samples (%d sec)\n", staWidth, staWidth*sampleIntervalSec;
    }
    else if (msgFldNo == 5) {
      ind = index($i, ":");
      ltaWidth = substr($i, ind+1);
      ### printf "# LTA width: %d samples (%d sec)\n", ltaWidth, ltaWidth*sampleIntervalSec;
    }    
    else if (msgFldNo == 6) {
      # Print trigger ratio as comment
      ### printf "# %s\n", $i;
    }
    else if (msgFldNo == 7) {
      # Print de-trigger ratio as comment
      #### printf "# %s\n", $i;
    }
    else if (msgFldNo == 9) {
      sta = substr($i, 6);
    }
    else if (msgFldNo == 10) {
      lta = substr($i, 6);
    }
    else if (msgFldNo == 11) {
      # End of message - print
      printIt();
    }

    msgFldNo++;
  }
}

function printIt() {
  cmd = timeCmdPrefix " " time;
  cmd | getline timeString;
###  printf "epochSec %d\n", epochSec;
  close(cmd);

  printf "%d %f   # %s at %s\n", time, sta/lta, state, timeString;
}
