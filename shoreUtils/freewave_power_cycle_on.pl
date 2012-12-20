#!/usr/bin/perl -w

# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

#
# Script to drive Agilent E363xA power supply
# This script is to be run upon a PPP disconnect
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
my $randOnLimit = 1800; # High limit of random time (e.g. 1020 sec = between 0-17 min additional time added to timeOn) - Now set to 30 minutes per Tom O.'s request.
###########

my $numcycles = 1;
my $rand_num;

#Initialize power supply
$ob->write("SYST:REM\n\r"); # remote mode
sleep 1;
$ob->write("DISP:TEXT 'PWR ON'\n\r");
sleep 1;
$ob->write("OUTP ON\n\r"); # Turn on output
sleep 1;
$ob->write("VOLT:RANGE P20V\n\r"); # set high voltage range
sleep 1;
$ob->write("DISP:TEXT:CLEAR\n\r"); # clear screen
sleep 1;

#Display Random number and time to off-state.
$rand_num = ($randOnLimit*rand); #Create total time in rand_num

while ($rand_num > 0)
{
$ob->write("DISP:TEXT 'SLEEPING " . sprintf("%.0f", $rand_num) . "'\n\r"); # Output text to power supply
$rand_num = $rand_num-1; # Decrement Random Number
sleep 1;
}

$ob->write("DISP:TEXT:CLEAR\n\r"); # clear screen
sleep 1;

  $ob->write("SYST:BEEP\n\r"); # Beep
  sleep 1;
  $ob->write("APPL 12.0, 0.5\n\r"); # Turn power on and wait
  sleep 1;
  $ob->write("SYST:REM\n\r"); # remote mode
  sleep 1;

undef $ob;

exit 0;

