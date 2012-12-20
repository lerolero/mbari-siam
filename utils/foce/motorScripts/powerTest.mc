#!/bin/bash

# Wraps runProfile.mc
# to run motor power test

# A number of profiles (mt0, mt20, mt25, etc.) are
# defined in profile.csv. Each profile defines a single
# (3600 s) cycle running at a particular speed. mt0 is defined as a 900s cycle.

# This script calls runProfile.mc repeatedly
# to run each profile so that the commands are re-issued 
# every 30 min for 25 hours.


# Script directory
SCRIPT_HOME=$SIAM_HOME/utils/foce/motorScripts/

# runProfile script
RUN_PROFILE=$SCRIPT_HOME/runProfile.mc

# uncomment to run in test mode
#TEST="-t"

# doProfile() - runs n cycles of a specified profile
#
# $1: profile
# $2: cycles
# $3: options (-i)
#
doProfile(){
let "i=0"
while [ "$i" -lt $2 ]
do
$RUN_PROFILE $TEST $3 $1
let "i=$i+1"
echo "n=$i"
done
}

#
# print use message
#
printUsage(){
echo
echo "#"
echo "# Display or run motor test profile definitions from a file"
echo "#"
echo
echo "usage: `basename $0` [-h -t ]"
echo 
echo "  -t        : test mode (echo but do not execute commands)"
echo "  -h        : print this help message"
echo
}

# check args
while getopts ht Option
do
    case $Option in
	t ) TEST="-t"
	;;
	h)printUsage
	  exit 0
	;;
	*) # getopts outputs error message
          exit -1
	;;
    esac
done

cd $SCRIPT_HOME

# 25h @ 0 rpm
doProfile mt0 100

# 24.75h @ 20 rpm
doProfile mt20 99 
# 0.25h @ 0 rpm
doProfile mt0 1

# 24.75h @ -20 rpm
doProfile mt20 99 -i
# 0.25h @ 0 rpm
doProfile mt0 1

# 24.75h @ 25 rpm
doProfile mt25 99
# 0.25h @ 0 rpm
doProfile mt0 1

# 24.75h @ -25 rpm
doProfile mt25 99 -i
# 0.25h @ 0 rpm
doProfile mt0 1

# 24.75h @ 30 rpm
doProfile mt30 99 
# 0.25h @ 0 rpm
doProfile mt0 1

# 24.75h @ -30 rpm
doProfile mt30 99 -i
# 0.25h @ 0 rpm
doProfile mt0 1

# 24.75h @ 35 rpm
doProfile mt35 99
# 0.25h @ 0 rpm
doProfile mt0 1

# 24.75h @ -35 rpm
doProfile mt35 99 -i
# 0.25h @ 0 rpm
doProfile mt0 1

# 24.75h @ 15 rpm
doProfile mt15 99
# 0.25h @ 0 rpm
doProfile mt0 1

# 24.75h @ -15 rpm
doProfile mt15 99 -i
# 0.25h @ 0 rpm
doProfile mt0 1

# 24.75h @ 40 rpm
doProfile mt40 99
# 0.25h @ 0 rpm
doProfile mt0 1

# 24.75h @ -40 rpm
doProfile mt40 99 -i
# 0.25h @ 0 rpm
doProfile mt0 1

# 25h @ 0 rpm
doProfile mt0 100
