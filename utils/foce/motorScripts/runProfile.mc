#!/bin/bash

# Tips:
# - use sensibly-named constants for configuration
# - remember to enable the motor
# - use comments
# - use named constants for commands+options
# - prepend echo to command constants to test execution

#
# Description:
# Displays or runs motor test profile definitions from a file.
# Profiles have the following format:
# <profileName>,<portMmenonic>,(<portProfile>)[,< portMmenonic >,(<portProfile>)...],(<intervalProfile>)
# where
# name: profile name (string)
# portMmenonic: mnemonic for a SIAM  port 
# Valid portMnemoic values are 
#    FWD: forward thruster
#    AFT: aft thruster
#    ESW: ESW pump
#   FVAL: forward valve
#   AVAL: aft valve
#
# portProfile: space-separated speed values for the motor on < portMmenonic > (rpm)
# intervalProfile: space separated time interval values, applied modulo <number of profile values>
# 
# Examples (FWD=/dev/ttyS8, AFT=/dev/ttyS10, ESW=ESWpump
# foo,FWD,( -15 -20  -30 ),AFT,(  15  20  35 ),( 60 3540 )
#
# Profile name: "foo"
# runs as follows:
# /dev/ttyS8 @ -15 rpm, /dev/ttyS10 @ 15 rpm for 60 sec
# /dev/ttyS8 @ -20 rpm, /dev/ttyS10 @ 20 rpm for 3540 sec
# /dev/ttyS8 @ -30 rpm, /dev/ttyS10 @ 35 rpm for 60 sec (i.e., applied modulo 3)
# 
# baz,ESW,( -15 -25  -30 ),AFT,(  15  20  40 ),( 90 )
#
# Profile name: "baz"
# runs as follows:
# ESWpump @ -15 rpm, /dev/ttyS10 @ 15 rpm for 90 sec
# ESWpump @ -25 rpm, /dev/ttyS10 @ 20 rpm for 90 sec
# ESWpump @ -30 rpm, /dev/ttyS10 @ 40 rpm for 90 sec (i.e., applied modulo 3)
# 

# 
# Define script constants
#

# motor host
HOST=localhost

# default profile file
PROFILE_DAT="$SIAM_HOME/utils/foce/motorScripts/profile.csv"

# motor mnemonics
FWD=/dev/ttyS8
AFT=/dev/ttyS10
ESW=ESWpump
FVAL=/dev/ttyS19
AVAL=/dev/ttyS19
#PREFIX="md;mw=1000;me;"
DFLT_PREFIX=""
DFLT_POSTFIX=""
PREFIX=${DFLT_PREFIX}
POSTFIX=${DFLT_POSTFIX}

# TEST command prefix
# comment out to run commands
#TEST="echo"

# print (instead of running) if TRUE
# (-s option)
doSHOW="FALSE"
TARGET="all"

# enable verbose output if TRUE
# (-V option)
VERBOSE="FALSE"

# flag used by readProfile()
# indicates whether specified profile was found
PROFILE_NOT_FOUND="TRUE"

# Sign can be inverted to reverse a profile
let "MOTOR_SIGN=1"
###########################
# End Config
###########################

#
# Print current profile values
# args: e - empty line at end b - empty line before
#
showProfile(){
 if [ "$1" != "b" ] && [ "$2" != "b" ]
  then
    echo
  fi
	
    echo "profile    : ${PROFILE_NAME}"
    echo "ports      : ${#PORT_NAME[*]}"
	let i="0"
	while [ "$i" -lt "${#PORT_NAME[*]}" ]
	do
      echo "Port ${PORT_NAME[$i]} [${PORT_MNEM[$i]}] : [ ${VEL_PROFILE[$i][*]} ]"
	  let i="$i+1"
	done
	echo "Interval Profile:  [ ${INT_PROFILE[*]} ]"
	
	
 if [ "$1" != "e" ] && [ "$2" != "e" ]
  then
    echo
  fi
}

# read a named profile from a 
# file containing multiple profiles
# input: file, profile name
# output: sets PROFILE_NAME, FWD_PROFILE, AFT_PROFILE, INT_PROFILE
readProfile() {

 # preserve file separator
 OLD_IFS=$IFS
 PROFILE_NOT_FOUND="TRUE"

 # get the arguments
 INFILE="$1"
 PNAME="$2"
 SHOW="$3"

# read a line from the file
while read line 
do
 # remove leading whitespace, comments and empty lines
 quux="`echo $line|sed -e 's/^ *//'|sed -e '/#/d'|sed -e '/^$/d'`"

 # set file separator to record delimiter (',')
 IFS=","

 # assign the record fields into positional parameters
 set -- $quux

 # assign positional parameters 
 a=$1

 # restore file separator
 IFS="$OLD_IFS"
 
 # if line isn't empty...
 if [[ ! "$a" =~ ^$ ]]
 then
 if [ "$SHOW" == "TRUE" ]
 then
  # if we're just showing, start with empty arrays for each line
  PORT_NAME=( )
  VEL_PROFILE=( )
  INT_PROFILE=( )
  MOTOR_EN=( )
  fi
  
  # if the name matches or we're showing 
  if [ "$a" == "$PNAME" ] || [ "$SHOW" == "TRUE" ]
  then 

  let rfields="$#-1"
  let ipIndex="$#"
  #echo "============================="
  #echo "fields: $# rfields:$rfields ipIndex:$ipIndex"
  #echo "all args (${#}) [${*}]"
  #let z="0"
  #while [ ${z} -le "${#}" ]
  #do
  #   eval zzz=\${${z}}
  #   echo "arg[$z]=${zzz}"
  #	 let z="${z}+1"
  #done
   
   # assign profile name
   PROFILE_NAME=${a}

   let i="2"
   let count="0"
   while [ "$i" -le "$rfields" ]
   do  
	IFS=","
	set -- $quux
	let j="i+1"
	#echo "i:$i j:$j"
	# the brackets must be used for positional parameters>9
	eval thePort=\${$i}
	eval theProfile=\${$j}
	#echo "$thePort : $theProfile"

	klh=`echo $theProfile |sed -e 's/(//'|sed -e's/)//'`
	#echo port:$thePort 
	PORT_MNEM[$count]=${thePort}     
	eval PORT_NAME[$count]=\${${thePort}}
	IFS=' '
	set -- $klh
	XX=( ${*} )
	VEL_PROFILE[$count]=${XX[@]}    
	let i="i+2"
	let count="$count+1"
   done

	IFS=","
	set -- $quux
	eval iProfile=\${$ipIndex}
      klh=`echo $iProfile |sed -e 's/(//'|sed -e's/)//'`
      IFS=' '
      set -- $klh
      INT_PROFILE=( ${*} )

  if [ "$a" == "$PNAME" ]
  then 
   PROFILE_NOT_FOUND="FALSE"
  fi

   # if show output...
   if [ "$doSHOW" == "TRUE" ] && [ "$a" == "$PNAME" ]
   then
    showProfile e 
   elif [ "$PNAME" == "all" ]
   then
    showProfile e b
   fi

   if [ "$doSHOW" == "TRUE" ] && [ "$a" == "$PNAME" ] || [ "$PNAME" == "all" ]
   then
   # kick out one more empty line
   echo 
    if [ "$PNAME" != "all" ]
	then
	  break
	fi
   fi
  fi
 fi
 done < $INFILE

 # restore file separator
 IFS=$OLD_IFS
}

#
# print args if VERBOSE=TRUE
#
vout(){
 if [ "$VERBOSE" == "TRUE" ]
 then
 echo "$*"
 fi
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
echo "usage: `basename $0` [options...] <profileName>"
echo 
echo "  -f <file> : profile data file         [$PROFILE_DAT]"
echo "  -H <host> : hostname                  [$HOST]"
echo "  -i        : invert profile"
echo "  -R        : prefix commands"
echo "  -O        : postfix commands"
echo "  -s        : show one or all profiles  [$TARGET]"
echo "  -t        : test mode (echo, don't execute commands)"
echo "  -h        : print this help message"
echo "  -V        : verbose output            [$VERBOSE]"
echo
}

###################################
# Script main entry point
###################################

# init arrays
PORT_NAME=( )
VEL_PROFILE=( )
INT_PROFILE=( )
MOTOR_EN=( )


# issue use message for no arguments
if [ $# -lt 1 ]
then
printUsage
exit -1
fi

let "scount=0"
while getopts f:hH:iO:R:stV Option
do
    case $Option in
	f ) PROFILE_DAT="$OPTARG"
	    let "scount=$scount+2"
	;;
	H ) HOST="$OPTARG"
	    let "scount=$scount+2"
	;;
	i ) let "MOTOR_SIGN=-1"
	    let "scount=$scount+1"
	;;
	O ) POSTFIX="$OPTARG"
	    let "scount=$scount+2"
	;;
	R ) PREFIX="$OPTARG"
	    let "scount=$scount+2"
	;;
	s ) doSHOW="TRUE"
	    let "scount=$scount+1"
	;;
	t ) TEST="echo"
	    let "scount=$scount+1"
	;;
	V ) VERBOSE="TRUE"
	    let "scount=$scount+1"
	;;
	h)printUsage
	  exit 0
	;;
	*) # getopts outputs error message
          exit -1
	;;
    esac
done

# shift all of the 
# options out of the way to
# get to the profile
while [ $scount -gt 0 ]
do
 shift
 let "scount=$scount-1"
done

if [ "$1" == "" ]
then
TARGET="all"
else
TARGET="$1"
fi

if [ "$TARGET" == "all" ] && [ "$doSHOW" == "FALSE" ]
then
echo
echo "Error - must specify either profile or -s option"
printUsage
exit -2
fi

vout "HOST=$HOST"
vout "PROFILE_DAT=$PROFILE_DAT"
vout "TEST=$TEST"
vout "TARGET=$TARGET"
vout "doSHOW=$doSHOW"
vout "VERBOSE=$VERBOSE"
vout "MOTOR_SIGN=$MOTOR_SIGN"
vout "PREFIX=$PREFIX"
vout "POSTFIX=$POSTFIX"

# parse profile file, set active profile
# and optionally print values
readProfile $PROFILE_DAT $TARGET $doSHOW

# exit now if only showing profile(s)
if [ "$doSHOW" == "TRUE" ]
then
 exit 0
fi

# make sure profile found
if [ "$PROFILE_NOT_FOUND" == "TRUE" ]
then
echo 
echo "profile $TARGET not found in $PROFILE_DAT"
echo
exit -3
fi

# script command defintions
let i="0"
while [ "$i" -lt "${#PORT_NAME[*]}" ]
do
 if [ "${PORT_MNEM[$i]}" == "FWD" ] || [ "${PORT_MNEM[$i]}" == "AFT" ] 
 then
   MOTOR_EN[$i]="$TEST rmotor $HOST ${PORT_NAME[$i]}  me"
   vout "MOTOR_EN[$i]:${MOTOR_EN[$i]}"
 fi
 let i="$i+1"
done

SLEEP="$TEST sleep ${INTERVAL}"

# output active profile
showProfile

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
let i="0"
while [ "$i" -lt "${#PORT_NAME[*]}" ]
do
 vout "enabling motor on port ${PORT_NAME[$i]}"
 ${MOTOR_EN[$i]}
 let i="$i+1"
done

# execute profile
let "i=0"
let "isize=${#INT_PROFILE[*]}"
let "iindex=${i}%${isize}"
profile0=( ${VEL_PROFILE[0][*]} )
let nSteps="${#profile0[*]}"

while [ "$i" -lt "${nSteps}" ] 
do 
    let "j=0"
    while [ "$j" -lt "${#PORT_NAME[*]}" ] 
	do
	  # get fwd and aft velocities
	  vels=( ${VEL_PROFILE[$j][*]} )
	  if [ "${PORT_MNEM[$j]}" == "FVAL" ] 
	  then
	    VCMD=${vels[$i][$j]}
	    $TEST valve $HOST ${PORT_NAME[$j]} fwd ${VCMD}
	  elif [ "${PORT_MNEM[$j]}" == "AVAL" ] 
	  then
	    VCMD=${vels[$i][$j]}
	    $TEST valve $HOST ${PORT_NAME[$j]} aft ${VCMD}
	  else
	    let "VCMD=${vels[$i][$j]}*$MOTOR_SIGN"
	    # run profile step
	    $TEST rmotor $HOST ${PORT_NAME[$j]} "${PREFIX}tjo=${VCMD}${POSTFIX}"
	  fi
          let "j=$j+1"
 	done
 	    # wait profile step interval
	    $TEST sleep ${INT_PROFILE[$iindex]}
	 # increment counters
     let "i=$i+1"
        let "iindex=$i%$isize"

done
exit 0
