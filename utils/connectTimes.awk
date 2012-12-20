# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.


BEGIN {
  target = "10.1.36.2";
  max = 0;
  min = 9999999;
}

/remote IP address/ {

  if (index($0, target) > 0) {
    inSession = 1;
    nSessions++;
    print;
    next;
  }
}



  
  
/Connect time/ {
  if (inSession) {
    print;

    # accumulate for mean
    mean += $8;

    # Track maximum connect time
    if ($8 > max) {
      max = $8;
      maxDate = $1 " " $2 " " $3;
    }

    if ($8 < min) {
      min = $8;
      minDate = $1 " " $2 " " $3;
    }

    # Done with this session
    inSession = 0;
  }
}


END {
  mean = mean / nSessions;
  printf "Avg time: %.1f\n", mean;
  printf "Min time: %.1f, on %s\n", min, minDate;
  printf "Max time: %.1f, on %s\n", max, maxDate;
}
