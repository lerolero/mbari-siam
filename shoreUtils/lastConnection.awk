# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

# Print start time of last connection to specified node, and duration of 
# the connection.
BEGIN {
  if (ARGC < 2) {
    printf("Usage: rfAddress");
    error = 1;
    exit;
  }

  rfAddress = ARGV[1];
  ARGV[1] = "";
  startConnect = "IP-UP.*" rfAddress;
  endConnect = "IP-DOWN.*" rfAddress;
}


$0 ~ startConnect {
  nConnects++;
  startTime = $2;
  connected = 1;
}

$0 ~ endConnect {
  duration = $2 - startTime;
  connected = 0;
}



END {
  if (error) {
    exit;
  }

  if (nConnects > 0) {
    # Print last connect time and duration
    printf "Last connection from %s at %s ", rfAddress, strftime("%m/%d/%Y %H:%M:%S", startTime);

    if (connected) {
      printf "(Now connected?)\n";
    }
    else {
      printf "(connected for %d sec)\n", duration;
    }
  }
}


