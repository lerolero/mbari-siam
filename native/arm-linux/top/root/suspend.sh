#!/bin/bash
#
# Shell script to put CPU into sleep mode
#

# Flush the file system
/bin/sync

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
# According to Tim, it is not necessary to call setPWER here each time
# the sidearm is suspended; it would be sufficient to do it once at
# initialization (in rc.local, e.g.). Also, note that a node that is not
# sleep managed will never call suspend.sh, and therefore the register set
# by setPWER will never be explicitly set. Moving this call to rc.local.
# 08/12/05 k. headley
# /root/setPWER 8000001d

#Suspend a minimum of 6 seconds (3 to wait for flush, 3 for suspend)
if [ $1 -gt 6 ]
then
# Do a sleep (busy-wait) for 3 seconds to allow CFM to flush
    sleep 3
    echo "PWER register: " 
    cat /proc/cpu/registers/PWER

# Now put the CPU into sleep
# Uncomment the ifdown/iup lines if you want ethernet after sleep
# But note that ifup can take many seconds.
# --> ifdown is not necessary with cs8900 driver
#    ifdown eth0
    let sleepTime=$1-3
    /root/suspend -v $sleepTime
    /root/ricohRTC -s
#    ifup eth0

# --> below is not necessary with cs8900 driver!
    # This extra toggle of eth0 seems needed for reliable recovery
#    sleep 1
#    ifdown eth0
#    ifup eth0
fi
