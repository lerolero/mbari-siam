#!/bin/bash
# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

# Script executes moos.deployed.NodeTest.main()
$JAVA -cp $SIAM_CLASSPATH \
-Djava.security.policy=$SIAM_HOME/properties/policy \
-Dlog4j.threshold=$LOG4J_THRESHOLD \
-Dsiam.log4j=$SIAM_LOG4J \
org.mbari.siam.devices.nortek.NortekFileBuilder $*
