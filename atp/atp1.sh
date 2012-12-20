#!/bin/bash
# Perform the following functions on each port of the specified node:
#    
#    shutdown
#    scan
#    annotate
#    suspend
#    resume
# 
# After each operation, check to verify that the command had the
# desired affect on service status or presence.
#

if [ "$#" -ne "1" ]
then
  echo "usage: $0 nodeHost"
  echo "e.g.: "
  echo "  % $0 sidearm14"
  exit 1
fi

AWK=awk

node="//$1/node"

echo node=$node

# Set temporary awk file
awkFile="/tmp/atp1_$$.awk"

echo awkFile=$awkFile

cat > $awkFile <<END_AWK

BEGIN {
  inPorts = 0;

  node = ARGV[1];
  ARGV[1] = "";
  printf "**** NODE=%s\n", node
}

/^-----/ {
  inPorts = 1;
  next;
}

NF == 0 {
  # Blank line
  next;
}

{
  if (!inPorts) {
    next;
  }

  port = \$1;
  if (\$2 ~ /-.*/) {
    printf "No service on port %s - skip\n", port;
    next;
  }
  printf "Annotating port %s...\n", port;
  status = system("annotatePort " node " " port " 'Running ATP on port'");
  if (status != 0) {
    print "ERROR!";
  }
  printf "Shutting down service on port %s...\n", port;
  system("shutdownPort " node " " port);
  system("listPorts " node);
  printf "Should now be no service now on port %s.\n", port;
  system("sleep 5");
  printf "Scanning port %s...\n", port;
  system("scanPort " node " " port);
  system("listPorts " node);
  printf "Service should now be running on port %s.\n", port;
  system("sleep 5");
  printf "Suspending service on port %s...\n", port;
  system("suspendPort " node " " port);
  system("listPorts " node);
  printf "Service on port %s should now be SUSPENDED\n", port;
  system("sleep 5");
  printf "Resuming service on port %s...\n", port;
  system("resumePort " node " " port);
  system("listPorts " node);
  printf "Service on port %s should now be OK or SAMPLING\n", port;
  system("sleep 5");
  printf "Sampling instrument %s on port %s...\n", \$2, port;
  printf "------------------------------------------------------\n";
  system("samplePort " node " " port);
  printf "Sampling complete for instrument %s.\n", \$2
  printf "------------------------------------------------------\n";
  printf "Get device metadata %s from instrument on port %s...\n", \$2, port;
  printf "------------------------------------------------------\n";
  system("getMetadata " node " " port " i");
  printf "------------------------------------------------------\n";
  system("sleep 5");
  printf "Annotating port %s...\n", port;
  system("annotatePort " node " " port " 'ATP completed on port'");
}
END_AWK

echo "Starting all services at $node..."
scanPorts $node

echo "Starting tests..."
echo "Show schedules"
showSchedule $node
sleep 5
echo "List power switches"
listSwitches $node
sleep 5
listPorts $node | $AWK -f $awkFile $node
echo "Test complete."


