# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

# Convert 'j9counter' output to format suitable for gnu 'graph'

{
  timestring = substr($0,5);

  gsub("\)", "", timestring);

###  cmd = "\"ut2et -format '%a %b %d %H:%M%S UTC %Y\"" "\"" timestring "\"";

  cmd = sprintf("ut2et -format '%%a %%b %%d %%H:%%M:%%S UTC %%Y' '%s'", timestring);

##  system(cmd);
  
  cmd | getline secs;
  close(cmd);

  if (startSecs == 0) {
    startSecs = secs;
  }

  printf "%d  %d\n", secs - startSecs, $1;
}


