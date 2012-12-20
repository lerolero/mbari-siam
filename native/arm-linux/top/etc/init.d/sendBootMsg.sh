#!/bin/bash
# Send a message indicating time of node boot to shore via shore messaging 
# service

logfile=/tmp/sendBootMsg.out

echo "running sendBootMsg.sh" >> $logfile

# Get SIAM environment
export SIAM_HOME=/mnt/hda/siam
. /etc/siam/siamEnv

# Keep this node awake for a bit
/etc/siam/cpuAwake 90

# Shore messaging service host name is hard-coded!
serviceHost=node21

# Wake up nodes
$SIAM_HOME/utils/bcastc

# Make sure that shore msg service host is reachable, to avoid potentially 
# long timeouts
ping -c 3 -i 3 $serviceHost

if [ $? -ne 0 ] 
then
  # Couldn't reach serviceHost
  echo "Couldn't ping $serviceHost - not connected?" | /usr/bin/tee -a $logfile
  exit 1
fi

# Create a small message file to send to shore messaging service
echo `hostname` " node rebooted at " `date`  > /tmp/reboot.txt
echo -n 'RCSR register: " >> /tmp/reboot.txt
cat /proc/cpu/register/RCSR >> /tmp/reboot.txt

# Dispatch the message file
$SIAM_HOME/utils/shoremsgclient $serviceHost -qf /tmp/reboot.txt | /usr/bin/tee -a $logfile

exit 0



