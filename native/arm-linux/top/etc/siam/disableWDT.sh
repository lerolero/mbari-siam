#!/bin/bash
#
# This script disables the SA1110 watchdog timer by disabling
# the interrupt generated when the OS timer and OSMR3 register 
# match. See OIER register bit 3.
#

OIER_FILE="/proc/cpu/registers/OIER"

  OIERVal="0x1"
  echo $OIERVal > $OIER_FILE
