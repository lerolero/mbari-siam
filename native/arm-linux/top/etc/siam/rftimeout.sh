#!/bin/bash
#

if [ ! -n "$1" ] 
then
    echo "usage: rftimeout [seconds]"
    exit 1
fi

if [ ${1} -le 0 ]
then
    export -n TMOUT
else
    export TMOUT=${1}
fi

