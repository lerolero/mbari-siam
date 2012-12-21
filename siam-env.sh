#!/bin/bash
# Set up SIAM environmnent

export SIAM_HOME=/Users/oreilly/siam2
alias gosiam="cd $SIAM_HOME"
alias inf="export LOG4J_THRESHOLD=INFO"
alias dbg="export LOG4J_THRESHOLD=DEBUG"

export SIAM_CLASSPATH=$SIAM_HOME/classes:$SIAM_HOME/jars/log4j-1.2.13.jar:$SIAM_HOME/jars/javax.jar:$SIAM_HOME/jars/ssds-client-pub.jar:$SIAM_HOME/jars/jddac-common.jar:$SIAM_HOME/jars/jddac-probe-j2me.jar:$SIAM_HOME/jars/RXTXcomm.jar:/Applications/RBNB/bin/rbnb.jar:$SIAM_HOME/jars/puck.jar


export PATH=$SIAM_HOME/utils:$SIAM_HOME/shoreUtils:$PATH

