#!/bin/bash
#
# Startup script to control power to the Short Haul RF Link
#
# This script is normally called from an entry in /etc/inittab, e.g.
# T13:23:respawn:/etc/auxTelem.sh &>/dev/null
#
# Usage:  auxTelem.sh <-d device> <-b baud> <-t timeout> <-y term_emulation> \
#		<-p powerBit> <-s sleepTime>
# where
# "-d device"	refers to the ttyxxx device to perform a getty on.
#		Leave off the leading "/dev/"
# "-b baud"	is the baud rate to talk to the serial device (radio)
# "-t timeout"	is the timeout passed to getty.  This is the amount of
#		time the radio will be on, allowing the user to log in
# "-y term_emulation" - This is the value, passed through getty, that
#		        eventually is put into your TERM variable
# "-p powerBit"	Radio power bit, passed to "rfpower" to turn on radio
#               -1 means don't do rfpower
# "-s sleepTime" Amount of time script goes to sleep after getty times out.
#		 This, then, becomes the radio "off" time.
#

source /etc/siam/siamEnv

declare device="ttySX13"
let baud=115200
let timeout=30
declare term="vt100"
let powerBit=0
let sleepTime=300

#Process command-line options
while getopts "b:d:p:s:t:y:" OPTION
do
    case $OPTION in
        b) let baud=$OPTARG
	;;
        d) declare device=$OPTARG
	;;
        p) if [ $OPTARG -gt 1 ]
	   then
	       echo "Bad value for Power Bit (-p)"
	   else
	       let powerBit=$OPTARG
	   fi
	;;
        s) let sleepTime=$OPTARG
	;;
        t) let timeout=$OPTARG
	;;
        y) declare term=$OPTARG
	;;
    esac
done

# Keep us awake
let awakeTime=timeout+6
/etc/siam/cpuAwake -q -r 2 0 $awakeTime

# Turn on RF Power
if [ $powerBit -ge 0 ]; then
    /etc/siam/rfpower $powerBit ON
fi

# Let Radio data-link connect before getty tries to use it
sleep 3

# Run getty with timeout, prints login prompt to user
/sbin/getty -t $timeout $device $baud $term

# Let radio send last characters before cutting off power
sleep 2

# Turn off RF power
if [ $powerBit -ge 0 ]; then
    /etc/siam/rfpower $powerBit OFF
fi

# This 'sleep' gives us our "off" time before this script exits
# and inittab reruns it from the top.
# First, make sure we set a wakeup for the next time
/etc/siam/cpuAwake -q -r 2 $sleepTime $awakeTime

# The Linux 'sleep' command gets lost if CPU is in sleep mode
# (Ironic, isn't it?)
#sleep $sleepTime

# Instead, sleep in small catnaps until the time we want to finish
let end=`date +%s`+$sleepTime

while [ `date +%s` -lt $end ]
do
    sleep 1
done
