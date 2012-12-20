#!/usr/bin/perl -w

# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

#
# Script to drive Agilent E363xA power supply
# This script is to be run upon a PPP Connect.
# It will cycle power off randomly based on the
# variables timeOff and randOffLimit
#

# Serial port being used 
$SP = "/dev/ttyUSB3";

use Device::SerialPort 0.12; # Package from CPAN 

# Setup serial port
# Writes to $ob are followed by a sleep for flushing
#
$ob = Device::SerialPort->new ($SP) || die "Can't Open $SP: $!";
$ob->baudrate(9600)    || die "failed setting baudrate";
$ob->parity("none")    || die "failed setting parity";
$ob->databits(8)       || die "failed setting databits";
$ob->handshake("none") || die "failed setting handshake";
$ob->write_settings    || die "no settings";

###########
## Adjust variables here
my $timeOff = 10;  # Base for how soon power could be turned off (in seconds)
my $randOffLimit = 3600; # High limit of random time (e.g. 1020 sec = between 0-17 min additional time added to timeOff) - Now set to 30 minutes per Tom O.'s request.

###########

my $numcycles = 1;
my $rand_num;

#Initialize power supply
#$ob->write("SYST:REM\n\r"); # remote mode
#sleep 1;
#$ob->write("DISP:TEXT 'FRWAVE TEST'\n\r");
#sleep 1;
#$ob->write("OUTP ON\n\r"); # Turn on output
#sleep 1;
#$ob->write("VOLT:RANGE P20V\n\r"); # set high voltage range
#sleep 1;
#$ob->write("DISP:TEXT:CLEAR\n\r"); # clear screen
#sleep 1;


 $rand_num = ($timeOff+($randOffLimit*rand)); #Create total time in rand_num
 sleep $rand_num; # Delay for total time

  $ob->write("SYST:BEEP\n\r"); # Beep
  sleep 1;
  $ob->write("DISP:TEXT 'PWR OFF'\n\r");
  sleep 1;
  $ob->write("APPL 0.0, 0.0\n\r"); # Turn power off

#  sleep 60; # sleep for 60 sec and turn power back on for next ppp session (increased from 35sec).

#  $ob->write("DISP:TEXT 'PWR ON'\n\r");
#  sleep 1;  
#  $ob->write("SYST:BEEP\n\r");
#  sleep 1;
#  $ob->write("APPL 12.0, 0.5\n\r"); #Turn on power
#  sleep 1;  

#undef $ob;

exit 0;

