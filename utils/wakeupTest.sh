#!/bin/bash
# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

target=$1

pingTries=1
wait=5

while [ 1 ] ; do

  # Wakeup the target node
  echo "Send wakeup signal to target"
  # wakeup localhost target

  # Ping the target node
  ping -c $pingTries $target

  echo "Wait $wait sec..."
  sleep $wait

done
