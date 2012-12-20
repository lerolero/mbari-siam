#!/bin/bash

#########################################
# Name:
#
# Summary:
#
# Description:
#
# Author:
#
# Copyright MBARI
# Copyright 2000, Monterey Bay Aquarium Research Institute.
# Licensed for PUCK-enabled oceanographic instrumentation field-of-use.
# 
# Intellectual Property Disclaimer
# 
# MBARI MAKES NO REPRESENTATIONS OR WARRANTIES, EXPRESS OR IMPLIED. 
# BY WAY OF EXAMPLE, BUT NOT LIMITATION, MBARI MAKES NO REPRESENTATIONS 
# OR WARRANTIES OF MERCHANTABILITY OR FITNESS FOR ANY PARTICULAR PURPOSE 
# OR THAT THE USE OF THE LICENSED SOFTWARE COMPONENTS OR DOCUMENTATION 
# WILL NOT INFRINGE ANY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER RIGHTS. 
# MBARI SHALL NOT BE HELD LIABLE FOR ANY LIABILITY NOR FOR ANY DIRECT, 
# INDIRECT OR CONSEQUENTIAL DAMAGES WITH RESPECT TO ANY CLAIM BY RECIPIENT 
# OR ANY THIRD PARTY ON ACCOUNT OF OR ARISING FROM THIS AGREEMENT OR USE 
# OF THE SOFTWARE OR SPECIFICATION.
#
#########################################

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
    echo "usage: `basename $0`"
    echo "-x <option>    : argument with option    [$X_ARG]"
    echo "-V             : verbose output          [$VERBOSE]"
    echo "-h             : print this help message"
    echo ""
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

while getopts x:hV Option
do
    case $Option in
	x ) vout "b = $OPTARG"
	;;
	V ) VERBOSE="TRUE"
	;;
	h)printUsage
	  exit 0
	;;
	*) exit 0 # getopts outputs error message
	;;
    esac
done

# call sigTrap on INT,TERM or EXIT
# trap sigTrap INT TERM EXIT

# reset trapped signals
# trap - INT TERM EXIT

