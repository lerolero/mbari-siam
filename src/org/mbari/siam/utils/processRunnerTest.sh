#!/bin/bash

let numloops=10
if [ $# -gt 0 ]
then
    let numloops=$1
fi

let exitcode=0
if [ $# -gt 1 ]
then
    let exitcode=$2
fi

let i=0
while [ $i -lt $numloops ]
do
    echo "$0 loop $i"
    let i=i+1
    sleep 1
done

exit $exitcode

