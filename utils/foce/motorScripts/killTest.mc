#!/bin/bash
let "i=1"
while [ 1 ]
do
echo "still running...[$i]"
let "i=$i+1"
sleep 5
done
