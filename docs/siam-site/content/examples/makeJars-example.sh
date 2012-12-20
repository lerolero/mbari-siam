#!/bin/bash

#########################################
# Name: makeJars-example.sh
#
# Summary: define and build target definitions for ALOHA instrument service JAR files
#
# Description:
# This file is used to configure SIAM instrument service 
# default parameters 
#
# Related SIAM utilities:
# portProperties, getMetadata
# restartPort, setProperty, cachePort
# Author:
#
# Copyright MBARI 2010
#
#########################################

# Notes: 
#  build SIAM classes using correct SIAM environment (SIAM_HOME, IS_UNIX, JAVA_DEV_ROOT)
#  make sure that puckxml is up to date
#  device IDs may be set in the script variable initialization section
#  service properties may be set using the appropriate mksiamjar invocation lines
#  by default, the device ID is used to name the service XML file (e.g. 9999.xml)

# Examples: 
#   to make all instrument jars: 
#      makeJars-example.sh all
#   to make ctd jars: 
#      makeJars-example.sh ctd


#########################################
# Script configuration defaults
# casual users should not need to change
# anything below this section
#########################################
X_ARG="defaultXValue"

#################################
# Script variable initialization
#################################
VERBOSE="FALSE"

# set device IDs here
ctdID=9995
ctdXML=${JAVA_DEV_ROOT}/docs/siam-site/content/examples/example.xml

ctd2ID=9996
ctd2XML=${JAVA_DEV_ROOT}/docs/siam-site/content/examples/example.xml

ictdID=9999
ictdXML=${JAVA_DEV_ROOT}/docs/siam-site/content/examples/example.xml

tripletID=9998
tripletXML=${JAVA_DEV_ROOT}/docs/siam-site/content/examples/example.xml

camID=9997
camXML=${JAVA_DEV_ROOT}/docs/siam-site/content/examples/example.xml
camUser="user"
camPW="passwd"

#################################
# Function Definitions
#################################

#################################
# name: sigTrap
# description: signal trap callback
# will interrupt 
# args: none
#################################
#sigTrap(){
#    exit 0;
#}

#################################
# name: printUsage
# description: print use message
# args: none
#################################
printUsage(){
    echo
	echo " Build one or more groups of SIAM instrument service JAR files"
	echo
    echo " usage: `basename $0` [-vH] <JAR group> [<JAR group>...]"
	echo
	echo "  options"
    echo "    -V  : verbose output          [$VERBOSE]"
    echo "    -h  : print this help message"
    echo 
    echo "    valid JAR groups:"
	echo "         all: all PUCK JAR files"
	echo "         ctd: SBE37SM serial CTD(s)"
	echo "        ictd: SBE37IM inductive CTD chain"
	echo "     triplet: WETLabs ECO fluorometer(s)"
	echo "         cam: Axis camera"
    echo
}

########################################
# name: vout
# description: print verbose message to stderr
# args:
#     msg: message
########################################
vout(){
    if [ "$VERBOSE" == "TRUE" ]
    then
	echo "$1" >&2
    fi
}

########################################
# name: exitError
# description: print use message to stderr
# args:
#     msg:        error message
#     returnCode: exit status to return
########################################
exitError(){
    echo >&2
    echo "$1" >&2
    echo >&2
    exit $2
}

##########################
# Script main entry point
##########################

# Argument processing
if [ "$#" -eq 0 ];then
    printUsage
    exit 0
fi

let "shiftCount=0"

while getopts hV Option
do
    case $Option in
	V ) VERBOSE="TRUE"
		let "shiftCount+=1"
	;;
	h)printUsage
	  exit 0
	;;
	*) exit 0 # getopts outputs error message
	;;
    esac
done

while [ $shiftCount -gt 0 ]
do
 shift
 let "shiftCount-=1"
done

# call sigTrap on INT,TERM or EXIT
# trap sigTrap INT TERM EXIT

# reset trapped signals
# trap - INT TERM EXIT

while [ "$#" -ge 1 ]
do
vout "processing target [$1]"
if [ "$1" == "all" ] || [ "$1" == "ctd" ]
then
	mksiamjar -service org.mbari.siam.devices.seabird.sbe37.SBE37 -jar ${JAVA_DEV_ROOT}/ports/Seabird37-${ctdID}.jar -base ${SIAM_HOME}/classes -classdir org/mbari/siam/devices/seabird/base; 
	mksiamjar -service org.mbari.siam.devices.seabird.sbe37.SBE37 -mnem 'Seabird SBE37' -id ${ctdID} -jar ${JAVA_DEV_ROOT}/ports/Seabird37-${ctdID}.jar -xml ${ctdXML} -add -classdir org/mbari/siam/devices/seabird/sbe37 \
	'sampleSchedule = 60' 'powerPolicy = always' 'commPowerPolicy = sampling' 'currentLimitMa = 3000' 'log = false' 'defaultSkipInterval = -1' 'safeSampleIntervalSec = 600' 'timeSynch=true' 'UUID = 00000000-0000-0000-0000-000000000000';
	# add other serial CTDs here
	mksiamjar -service org.mbari.siam.devices.seabird.sbe52mp.SBE52MP -jar ${JAVA_DEV_ROOT}/ports/Seabird52MP-${ctd2ID}.jar -base ${SIAM_HOME}/classes -classdir org/mbari/siam/devices/seabird/base; 
	mksiamjar -service org.mbari.siam.devices.seabird.sbe52mp.SBE52MP -mnem 'Seabird SBE52MP' -id ${ctd2ID} -jar ${JAVA_DEV_ROOT}/ports/Seabird52MP-${ctd2ID}.jar -xml ${ctd2XML} -add -classdir org/mbari/siam/devices/seabird/sbe52mp \
	'sampleSchedule = 60' 'powerPolicy = always' 'commPowerPolicy = sampling' 'currentLimitMa = 3000' 'log = false' 'defaultSkipInterval = -1' 'safeSampleIntervalSec = 600' 'timeSynch=true' 'UUID = 00000000-0000-0000-0000-000000000000';
fi
if [ "$1" == "all" ] || [ "$1" == "ictd" ]
then
	mksiamjar -service org.mbari.siam.devices.sbe37im.SeaBird37im -mnem 'Seabird Inductive CTD String' -id ${ictdID} -jar ${JAVA_DEV_ROOT}/ports/Seabird-IM-${ictdID}.jar -xml ${ictdXML} -base ${SIAM_HOME}/classes -classdir org/mbari/siam/devices/sbe37im \
	'sampleSchedule = 3600' 'powerPolicy=sampling' 'commPowerPolicy=sampling' 'sampleInterval=600' 'idMicroCATs = 0-3' 'UUID = 00000000-0000-0000-0000-000000000000'; 
# original mkpuck syntax
#	mkpuck moos.devices.sbe37im.SeaBird37im 'Seabird Inductive CTD String' ${ictdID} ${JAVA_DEV_ROOT}/ports/Seabird-IM-${ictdID}.jar ${ictdID} \
#	'sampleSchedule = 3600' 'powerPolicy=sampling' 'commPowerPolicy=sampling' 'sampleInterval=600' 'idMicroCATs = 0-3' 'UUID = 00000000-0000-0000-0000-000000000000'; 
fi
if [ "$1" == "all" ] || [ "$1" == "triplet" ]
then
	mksiamjar -service org.mbari.siam.devices.wetlabs.WetLabsTriplet -mnem 'WETLabs ECO-Triplet' -id ${tripletID} -jar ${JAVA_DEV_ROOT}/ports/WetLabsTriplet-${tripletID}.jar -xml ${tripletXML} -base ${SIAM_HOME}/classes -classdir org/mbari/siam/devices/wetlabs \
	'powerPolicy = always' 'commPowerPolicy = always' 'sampleSchedule = 10' 'diagnosticSampleInterval=60' 'timeSynch = true' 'samplesPerPacket = 5' 'averageCount = 20' 'summaryTriggerCount = 180' \
	'maxSummarySamples = 180' 'currentLimitMa = 3000' 'defaultSkipInterval = -1' 'logEnabled=false' 'UUID = 00000000-0000-0000-0000-000000000000' ;
	# add other triplets here

# original mkpuck syntax
#	mkpuck org.mbari.siam.devices.wetlabs.WetLabsTriplet 'WETLabs ECO-Triplet' ${tripletID} ${JAVA_DEV_ROOT}/ports/WetLabsTriplet-${tripletID}.jar ${tripletID} \
#	'powerPolicy = always' 'commPowerPolicy = always' 'sampleSchedule = 10' 'diagnosticSampleInterval=60' 'timeSynch = true' 'samplesPerPacket = 5' 'averageCount = 20' 'summaryTriggerCount = 180' \
#	'maxSummarySamples = 180' 'currentLimitMa = 3000' 'defaultSkipInterval = -1' 'logEnabled=false' 'UUID = 00000000-0000-0000-0000-000000000000' ;
fi

if [ "$1" == "all" ] || [ "$1" == "cam" ]
then
	mksiamjar -service org.mbari.siam.devices.axis.AxisCamera -id ${camID} -xml ${camXML} -jar ${SIAM_HOME}/ports/AxisCamera-${camID}.jar -base ${SIAM_HOME}/classes -classdir org/mbari/siam/devices/axis \
	'publisherHost=localhost' 'sampleSchedule=30' 'user=${camUser}' 'password=${camPW}' 'serviceName=Demo camera' 'advertiseService=true';
	# add other cameras here

fi

 shift

done

exit 0
