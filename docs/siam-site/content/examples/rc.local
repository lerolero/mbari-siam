#!/bin/sh

#
#****** This file is specific to MMC ****** 
#
# Called at boot time to configure MMC node appropriately.
#
# This is the default installation version of this file
# Normally it is CREATED AUTOMATICALLY by the configuration tool
# DO NOT MODIFY
# To set defaults, change $SIAM_HOME/properties/rclocal.base.template
# and run the configuration tool

# This script will be executed *after* all the other init scripts.
# You can put your own initialization stuff in here if you don't
# want to do the full Sys V style init stuff.

# Check the compact flash file system
# we do this here instead of using ide.opts
# because of a race between card manager
# and the SIAM application.

# Mount everything
echo "mounting filesystems"
mount -avt notmpfs,noproc,nodevpts 

# Start log size management utilities. Args are:
#          logfile       maxBytes  checkInterval (sec)
/etc/siam/manageLog /var/log/syslog 50000000 60 &
/etc/siam/manageLog /var/log/messages 50000000 60 &

# Set groups and permissions for RXTX Java serial port package
chgrp lock /var/lock
chmod 775 /var/lock

# Get time from Ricoh 
/root/ricohRTC -s

# NOTE: Don't try to synchronize time with shore yet,
# as network link to NTP server may not be present yet
# /usr/sbin/ntpdate -b ocean.shore.mbari.org

# Not sure why this is here...looks like it's setting up
# the MSP430 port, but seems to work without it
# /bin/stty -F /dev/ttySX15 9600

# Enable packet-forwarding
echo '1' > /proc/sys/net/ipv4/ip_forward

# Set PWER
# This sets the bits for what can wake us out of sleep
# The current bits set are:
# 0x80000000 = SA1100 RTC (/root/suspend needs this)
# 0x8 and 0x4 = Exar UARTs (/dev/ttySX{0-15})
# 0x1        = MSP430
# 0x10       = SPI Interrupt (needed to wake up on broadcast packets)
# See the Sidearm4 documentation on the the assignment
# of GPIO bits.  Set additional bits as necessary.
#
/root/setPWER 80000011
