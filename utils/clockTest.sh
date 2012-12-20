#!/bin/bash
# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

# Periodically suspend the sidearm cpu; periodically synchronize with NTP

echo "NOTE: SIAM should NOT be running during this test"

# NTP host machine
ntpHost=shore

# Interval between calls to suspend CPU
suspendInterval=60

# Suspend CPU for this many seconds
suspendDuration=50

# Interval (seconds) between NTP time synchronization
syncInterval=300

# Compute (roughly) time required to traverse main loop
let loopTime=$suspendDuration+$suspendInterval

# Start by synchronizing clock

runNtpDate shore



while [ 1 ]; do
  let count=$count+1
  echo "count=$count"
  echo "Wait $suspendInterval sec before suspend"
  sleep $suspendInterval; 
  echo "Suspend now for $suspendDuration sec" 
  
  /root/suspend.sh $suspendDuration
  echo "Back from suspend"

  echo "Initializing SPI"
  echo "10W0G100S0L0I0P" > /dev/spi

  let elapsed=$count*$loopTime
  echo "elapsed=$elapsed, syncInterval=$syncInterval"
  if [ $elapsed -ge $syncInterval ]
  then
    echo "Wait a few secs..."
    sleep 3
    # Turn on comms link to shore, to exercise LDD functions
    echo "Turn on ppp link"
    pon longhaul
    echo "call runNtpDate"
    runNtpDate $ntpHost
    echo "Turn off ppp link"
    poff longhaul

    count=0   # Reset counter
  fi

done
