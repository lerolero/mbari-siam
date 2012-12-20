# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

BEGIN {

  FS = ", ";
}



/serviceName=/ {
  # Instrument service name
  name = substr($1, 13);
}

/ipAddress=/ {
  # "Name" of node
  name = substr($1, 11);
}

/devid=.*t=.*seqNo=.*/ {
  devid = $1;
  timestamp = $2;
}


END {
  if (error) {
    exit;
  }

  deviceID = substr(devid, 7);

  # Epoch milliseconds
  lastTime = substr(timestamp, 3);

  # Epoch seconds
  lastTime = lastTime / 1000;

  # Compute age
  ageHours = (systime() - lastTime) / 3600;

  printf "ID %d (%s): age=%.1f hours\n", deviceID, name, ageHours, totalBytes;
}


