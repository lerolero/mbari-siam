#!/bin/bash
# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

# Shell script to get state of the GPIO bits periodically
#

declare gpiofile="/proc/cpu/registers/GPLR"
declare logfile="/mnt/hda/siam/logs/gpio.out"
let mask=0x0c
let lastgpio=0
let lastgpiotime=0

if [ ! -e $gpiofile ]
then
    echo "No registers file.  Need to 'insmod registers'"
    exit 1
fi

if [ -e $logfile ]
then
    mv $logfile ${logfile}.bak
fi

while [ 1 ]
do
    let gpio=`cat $gpiofile`
    let gpiotime=`date +%s`
    declare timestring=`date "+%Y/%m/%d %H:%M:%S"`

    if [ $(($gpio&$mask)) -ne $(($lastgpio&$mask)) -o \
         $gpiotime -ge $(($lastgpiotime+1800)) ]
    then
        printf "%s %x\n" "$timestring" $gpio >>$logfile
	let lastgpiotime=gpiotime
        let lastgpio=gpio
    fi

    sleep 10
done
