# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

BEGIN {
  # Amount of bytes in a SIAM packet header
  PACKET_HEADER_BYTES = 72;
}


/devid=/ {
  # Now in packet records
  inPacket = 1;
  next;
}



# Byte count
/^nBytes=/ {
  totalBytes += substr($1, 8);
  nPackets++;
  inPacket = 0;
}




inPacket {
  totalBytes += length($0);  
}


# First column of LOG4J message is elapsed millisec
/DEBUG/ {
  elapsed = $1;
}


END {
  totalBytes += (nPackets * PACKET_HEADER_BYTES);

  printf "Retrieved %d SIAM bytes in %.1f sec\n", totalBytes, elapsed/1000.;

  days = elapsed/1000./3600./24.;

  dailyRate = totalBytes/days/1000000.;

  printf "Mean SIAM data rate = %.1f MBytes/day\n", dailyRate;
  print "(1 MByte = 1,000,000 bytes)";
}
