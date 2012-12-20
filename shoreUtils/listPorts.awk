# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

BEGIN {

  if (ARGC < 3) {
    printf("Usage: nodeID portalLogfile\n");
    error = 1;
    exit;
  }

  # Node's ISI ID
  target = ARGV[1];
  ARGV[1] = "";

  FS = ",";


 header = sprintf("%-12s %5s %34s %6s %6s %6s %6s", "port", "ID", "Descr", "OK", "Error", "Retry", "Status");

  # Device status; all is well
 stateMnem[0] = "OK";
 stateMnem[1] = "ERR";
 stateMnem[2] = "INIT";
 stateMnem[3] = "DOWN";
 stateMnem[4] = "SUSP";
 stateMnem[5] = "SMPL";
 stateMnem[6] = "SLEEP";
 stateMnem[7] = "SAFE";
 stateMnem[-1] = "UNK";
}


/devid=.*t=.*seqNo=.*mdref=/ {
  if (index($1, "devid="target) == 0) {
    # Not our target
    inTarget = 0;
    next;
  }
  inTarget = 1;

  t = substr($2, 4)/1000;

  #  printf "t = %d\n", t;

  if (t0 == 0) {
    t0 = t;
  }

  elapsed = t - t0;
  timestamp = t;
}


/dev.*ID=/ && NF == 3 {
  
  id = substr($3, 5);
  descr[id] = $2;
  if (length(descr[id]) > 33) {
    tmp = substr(descr[id], 1, 33);
    descr[i] = tmp;
  }
}

/Node started at/ {
  if (inTarget) {
    nodeStartTime = $0;
  }

}


/port=.*id=.*samples=.*/ {

  if (!inTarget) {
    next;
  }

  if (!inBlock) {
    nlines = 0;
    deleteArray(status);
    inBlock = 1;
  }

  if (index($0, "|") > 0) {
    # New format; "|" delimiter
    split($0, fields, "|");
    port = substr(fields[1], 6);
    id = substr(fields[2], 4);
    description = substr(fields[3], 6);
    nSamples = substr(fields[4], 9);
    nErrors = substr(fields[5], 5);
    nRetries = substr(fields[6], 9);
    state = substr(fields[7], 8);
  }
  else {
    # Old format; "," delimiter
    port = substr($1, 6);
    id = substr($2, 4);
    nSamples = substr($3, 9);
    nErrors = substr($4, 5);
    nRetries = substr($5, 9);
    state = substr($6, 8);
    description = descr[id];
  }
    status[++nlines] = sprintf("%-12s %5d %34s %6d %6d %6d %5s", port, id, description, nSamples, nErrors, nRetries, stateMnem[state]);

  next;
}


{
  inBlock = 0;
}


END {
  # Print the latest status 
  printf "%s UTC\n", nodeStartTime;
  printf "%s\n", strftime("Instrument service status as of %b %d %Y, %H:%M:%S %Z:\n", timestamp);
  printf "%s\n", header;
  for (i = 1; i <= nlines; i++) {
    print status[i];
  }
}


function deleteArray(array, i) {
  for (i in array) {
    delete array[i];
  }
}
