#!/bin/bash
#
# Script to set ownership and privs of various files needed by SIAM
# NOTE - You must be root to execute this script
#

cd /root
chown root:root suspend
if [ $? -ne 0 ]; then
    echo "You must be root to run this script!"
    exit 1
fi
chmod 4755 suspend

chown root:root suspend.sh
chmod 4755 suspend.sh

chown root:root setPWER
chmod 4755 setPWER

chown root:root ricohRTC
chmod 4755 ricohRTC

cd /etc
chown root:root auxTelem.sh
chmod 775 auxTelem.sh 

chown root:root hosts
chmod 664 hosts

chown root:root hostname
chmod 664 hostname

chown root:root profile
chmod 644 profile

cd /etc/init.d
chown root:root siamapp
chmod 755 siamapp

chown root:root rc.local
chmod 775 rc.local

cd /etc/siam
chown root:root siamEnv
chmod 775 siamEnv

chown root:root manageLog
chmod 755 manageLog

cd /etc/ppp/peers
chown root:root longhaul

cd /usr/sbin
chown root:lock ntpdate
chmod 4755 ntpdate

chown root:lock pppd
chmod 4755 pppd

chown root:root pon
chmod 755 pon

chown root:root poff
chmod 755 poff

chown root:root plog
chmod 755 plog

chown root:root plogfast
chmod 755 plogfast

chown root:root plogslow
chmod 755 plogslow
