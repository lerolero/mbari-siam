#!/bin/bash

# Tips:
# - use sensibly-named constants for configuration
# - remember to enable the motor
# - use comments
# - use named constants for commands+options
# - prepend echo to command constants to test execution

#
# Description:
# Runs both motors in opposite directions
# over a profile of motor speeds, spending a
# prescribed amount of time at each speed
# the motors are stopped between each
# speed cycle.
#

# 
# Define script constants
#

# motor host
HOST=localhost
FWDPORT=/dev/ttyS8
AFTPORT=/dev/ttyS10
PROFILE_DAT=test.pfl

# TEST command prefix
# comment out to run commands
#TEST="echo"

doSHOW="FALSE"

PROFILE_NAME=( ) 
FWD_PROFILE=( )
AFT_PROFILE=( )
INT_PROFILE=( )

# read a named profile from a 
# file containing multiple profiles
# input: file, profile name
# output: sets PROFILE_NAME, FWD_PROFILE, AFT_PROFILE, INT_PROFILE
readProfile() {
OLD_IFS=$IFS
IFS=','

INFILE="$1"
PNAME="$2"

# remove leading whitespace, comments and empty lines
cat $INFILE|sed -e 's/^ *//'|sed -e '/#/d'|sed -e '/^$/d'| while read a b c d
do

if [[ "$a" =~ ^$ ]]
then
echo [blank]
else
if [ "$a" == "$PNAME" ]
then
echo found requested profile
PROFILE_NAME=${a}
echo profile: ${PROFILE_NAME} 
foo=`echo $b|sed -e 's/(//'|sed -e's/)//'`
bar=`echo $c|sed -e 's/(//'|sed -e's/)//'`
baz=`echo $d|sed -e 's/(//'|sed -e's/)//'`
IFS=' '
set -- $foo
FWD_PROFILE=( ${*} )
echo FWD_PROFILE:  ${FWD_PROFILE[*]}
set -- $bar
AFT_PROFILE=( ${*} )
echo AFT_PROFILE: ${AFT_PROFILE[*]} 
set -- $baz
INT_PROFILE=( ${*} )
echo INT_PROFILE: ${INT_PROFILE[*]}
echo
fi
IFS=','
fi

done 
IFS=$OLD_IFS
}

# sequence profile definitions
 PN_1="s1"
 FP_1=(  15  25  35  45  35  25  15 -15 -25 -35 -45 -35 -25 -15 )
 AP_1=( -15 -25 -35 -45 -35 -25 -15  15  25  35  45  35  25  15 )
 MI_1=( 3600 )

 PN_2="s2"
 FP_2=( 15 25 35 45 35 25 15 -15 -25 -35 -45 -35 -25 -15 0  0  0  0  0  0  0  0   0   0   0   0   0   0   0)
 AP_2=( 0  0  0  0  0  0  0  0   0   0   0   0   0   0   15 25 35 45 35 25 15 -15 -25 -35 -45 -35 -25 -15 0)
 MI_2=( 900 )

 PN_3="s3"
 FP_3=( 0  15  25  35  45  35  25  15 0 -15 -25 -35 -45 -35 -25 -15 )
 AP_3=( 0 -15 -25 -35 -45 -35 -25 -15 0  15  25  35  45  35  25  15 )
 MI_3=( 900 )

 PN_4="s4"
 FP_4=( 0  15  25  35  45  55  45  35  25  15 0 -15 -25 -35 -45 -55 -45 -35 -25 -15 )
 AP_4=( 0 -15 -25 -35 -45 -55 -45 -35 -25 -15 0  15  25  35  45  55  45  35  25  15 )
 MI_4=( 900 )

 # DCOSS Conference demo
 PN_5="dcoss"
 FP_5=(  15  25  35  45  35  25  15 0 )
 AP_5=( -15 -25 -35 -45 -35 -25 -15 0 )
 MI_5=( 140 )

 # Kick motors (gently) once an hour
 PN_6="kick"
 FP_6=( -15 -20  )
 AP_6=(  15  20  )
 MI_6=( 60 3540 )

 # Kick motors (a little harder) once an hour
 PN_7="kick2"
 FP_7=( -15 -20 -25 )
 AP_7=(  15  20  25 )
 MI_7=( 60 60 3480 )

 # Kick motors (aw, go on, give a boot) once an hour
 PN_8="kick3"
 FP_8=( -15 -20 -25 -30 -35 )
 AP_8=(  15  20  25  30 35 )
 MI_8=( 60 60 60 60 3360 )

printUsage(){
echo
echo "usage: `basename $0` [-hs][-f <profileDat>] <sequenceName>"
echo
}

# issue use message for no arguments
if [ $# -lt 1 ]
then
printUsage
exit -1
fi

while getopts f:hsV Option
do
    case $Option in
	f ) PROFILE_DAT="$OPTARG"
	    shift
	    shift
	;;
	s ) doSHOW="TRUE"
	    shift
	;;
	V ) VERBOSE="TRUE"
	    shift
	;;
	h)printUsage
	  exit 0
	;;
	*) # getopts outputs error message
          exit -1
	;;
    esac
done

if [ "$TARGET" == "" ]
then
TARGET="all"
else
TARGET="$1"
fi

echo PROFILE_DAT=$PROFILE_DAT
echo TARGET=$TARGET
echo doSHOW=$doSHOW
echo VERBOSE=$VERBOSE
exit 0

# show defined profiles for "show" option
if [ "$doSHOW" == "TRUE" ] || [ "$TARGET" == "all" ]
then
 echo
 echo "profile $PN_1"
 echo "fwd profile ${FP_1[*]}"
 echo "aft profile ${AP_1[*]}"
 echo "interval ${MI_1[*]}" 
 echo 
 echo "profile $PN_2"
 echo "fwd profile ${FP_2[*]}"
 echo "aft profile ${AP_2[*]}"
 echo "interval ${MI_2[*]}" 
 echo 
 echo "profile $PN_3"
 echo "fwd profile ${FP_3[*]}"
 echo "aft profile ${AP_3[*]}"
 echo "interval ${MI_3[*]}" 
 echo 
 echo "profile $PN_4"
 echo "fwd profile ${FP_4[*]}"
 echo "aft profile ${AP_4[*]}"
 echo "interval ${MI_4[*]}" 
 echo 
 echo "profile $PN_5"
 echo "fwd profile ${FP_5[*]}"
 echo "aft profile ${AP_5[*]}"
 echo "interval ${MI_5[*]}" 
 echo 
 echo "profile $PN_6"
 echo "fwd profile ${FP_6[*]}"
 echo "aft profile ${AP_6[*]}"
 echo "interval ${MI_6[*]}" 
 echo 
 echo "profile $PN_7"
 echo "fwd profile ${FP_7[*]}"
 echo "aft profile ${AP_7[*]}"
 echo "interval ${MI_7[*]}" 
 echo 
 echo "profile $PN_8"
 echo "fwd profile ${FP_8[*]}"
 echo "aft profile ${AP_8[*]}"
 echo "interval ${MI_8[*]}" 
 echo 
exit -1
fi


# set the sequence
seq=$1

# set script profile
if [ "$seq" == $PN_1 ] 
then
 FWD_PROFILE=( ${FP_1[*]} )
 AFT_PROFILE=( ${AP_1[*]} )
 INTERVAL=( ${MI_1[*]} )
elif [ "$seq" == $PN_2 ] 
then
 FWD_PROFILE=( ${FP_2[*]} )
 AFT_PROFILE=( ${AP_2[*]} )
 INTERVAL=( ${MI_2[*]} )
elif [ "$seq" == $PN_3 ] 
then
 FWD_PROFILE=( ${FP_3[*]} )
 AFT_PROFILE=( ${AP_3[*]} )
 INTERVAL=( ${MI_3[*]} )
elif [ "$seq" == $PN_4 ] 
then
 FWD_PROFILE=( ${FP_4[*]} )
 AFT_PROFILE=( ${AP_4[*]} )
 INTERVAL=( ${MI_4[*]} )
elif [ "$seq" == $PN_5 ] 
then
 FWD_PROFILE=( ${FP_5[*]} )
 AFT_PROFILE=( ${AP_5[*]} )
 INTERVAL=( ${MI_5[*]} )
elif [ "$seq" == $PN_6 ] 
then
 FWD_PROFILE=( ${FP_6[*]} )
 AFT_PROFILE=( ${AP_6[*]} )
 INTERVAL=( ${MI_6[*]} )
elif [ "$seq" == $PN_7 ] 
then
 FWD_PROFILE=( ${FP_7[*]} )
 AFT_PROFILE=( ${AP_7[*]} )
 INTERVAL=( ${MI_7[*]} )
elif [ "$seq" == $PN_8 ] 
then
 FWD_PROFILE=( ${FP_8[*]} )
 AFT_PROFILE=( ${AP_8[*]} )
 INTERVAL=( ${MI_8[*]} )
else
 echo "sequence [$seq] not found"
 exit -1
fi

# script command defintions
FWD_MOTOR_EN="$TEST rmotor $HOST $FWDPORT  me"
AFT_MOTOR_EN="$TEST rmotor $HOST $AFTPORT  me"
SLEEP="$TEST sleep ${INTERVAL}"

echo
echo "  FWD profile     : [ ${FWD_PROFILE[@]} ]"
echo "  AFT profile     : [ ${AFT_PROFILE[@]} ]"
echo "  INTERVAL profile: [ ${INTERVAL[@]} ]"
echo

#
# Script Main Entry Point
#

# throw error if SIAM_HOME not defined
if [ ! "$SIAM_HOME" ]
then
echo "SIAM_HOME not defined"
exit 1
fi

# enable motors
$FWD_MOTOR_EN
$AFT_MOTOR_EN

# execute profile
let "i=0"
let "isize=${#INTERVAL[*]}"
let "iindex=${i}%${isize}"

while [ "$i" -lt "${#FWD_PROFILE[*]}" ]
do

 # get fwd and aft velocities
 let "VFWD=${FWD_PROFILE[$i]}"
 let "VREV=${AFT_PROFILE[$i]}"

 # run profile step
 $TEST rmotor $HOST $FWDPORT tjo=${VFWD}
 $TEST rmotor $HOST $AFTPORT tjo=${VREV}

 # wait profile step interval
 $TEST sleep ${INTERVAL[$iindex]}

 # increment counters
 let "i=$i+1"
 let "iindex=$i%$isize"
done
exit 0
