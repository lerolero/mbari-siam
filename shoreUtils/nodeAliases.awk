# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

BEGIN {
  if (ARGC < 3) {
    printf("Usage: name hosts-file\n");
    error = 1;
    exit;
  }

  # Name of node 
  trueName = ARGV[1];
  ARGV[1] = "";

  target = "^" trueName "$";
  ## printf "target: %s\n", target;
}

{
  # Trim comments
  if ((commentStart = index($0, "#")) != 0) {
    line = substr($0, 1, commentStart-1);
    $0 = line;
  }

  gotIt = 0;
  for (i = 1; i <= NF; i++) {
    if ($(i) ~ target) {
      gotIt = 1;
    }
  }
  if (!gotIt) {
    next;
  }

  aliases = "";
  nAliases = 0;
  for (i = 2; i <= NF; i++) {
    if ($(i) != trueName) {
      if (nAliases == 0) {
        aliases = $(i);
      }
      else {
        tmp = aliases ", " $(i);
        aliases = tmp;
      }
      nAliases++;
    }
  }
}


END {
  printf "aka %s\n", aliases;
}

