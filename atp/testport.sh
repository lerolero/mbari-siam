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

if [ "$#" -ne "2" ]
then
  echo "usage: $0 nodeHost portName"
  echo "e.g.: "
  echo "  % $0 sidearm14 /dev/ttySX1"
  exit 1
fi

node="//$1/node"
port="$2"

echo node=$node
echo port=$port

listPorts $node -stats

annotatePort $node $port "ATP test initiated `date` on $node $port"

echo -n "Press any key to sample instrument on port " $port ":"
read resp
samplePort $node $port 

echo -n "Press any key to get most recent sample from instrument on port " $port ":"
read resp
getLastSample $node $port 

echo -n "Press any key to suspend service on port " $port ":"
read resp
suspendPort $node $port
listPorts $node -stats
echo "Service hould now be SUSPENDED on port " $port

echo -n "Press any key to resume service on port " $port ":"
read resp
resumePort $node $port
listPorts $node -stats
echo "Service hould now be OK/SAMPLING on port " $port

echo -n "Press any key to shutdown service on port " $port ":"
read resp
shutdownPort $node $port
listPorts $node -stats
echo "Should now be no service on port " $port


echo -n "Press any key to load service on port " $port ":"
read resp
scanPort $node $port
listPorts $node -stats
echo "Should now be service running on port " $port

echo -n "Press any key to get instrument metadata from port " $port ":"
read resp
getMetadata $node $port 


echo -n "Press any key to show schedule for instrument on " $port ":"
read resp
showSchedule $node $port 

annotatePort $node $port "ATP test complete `date` on $node $port"

echo "Test complete."



