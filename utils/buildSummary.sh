#!/bin/bash
# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

# $Id: buildSummary.sh,v 1.3 2012/12/17 23:27:14 oreilly Exp $

$JAVA -cp $SIAM_CLASSPATH \
-Djava.security.policy=$SIAM_HOME/properties/policy \
-Dlog4j.threshold=$LOG4J_THRESHOLD \
-Dsiam.log4j=$SIAM_LOG4J \
moos.utils.SummaryGenerator $*
