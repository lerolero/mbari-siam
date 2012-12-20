#!/bin/bash

# set up win32/cygwin SIAM build environment

# show help message if missing args
if [ "$#" -lt 2 ]
then
	echo "Usage: source `basename $0` <path to SIAM_HOME> <path to JAVA_HOME>"
	exit 0	
fi

# set SIAM home directory
export SIAM_HOME="$1"

# set Java development root 
export JAVA_DEV_ROOT="$SIAM_HOME"

# set JAVA_HOME
# e.g., C:/jsdk1.4.12
export JAVA_HOME="$2"

# define JAVA command
export JAVA="$JAVA_HOME/bin/java"

# define SIAM JAR directory for convenience
export SIAM_LIB="$SIAM_HOME/jars"

# define IS_UNIX=1 to indicate *NIX OS (mac, linux), unset for win32
#export IS_UNIX=0

# set SIAM classpath
SEP=";"
export SIAM_CLASSPATH="$SIAM_HOME/classes$SEP$SIAM_LIB/puck.jar$SEP$SIAM_LIB/rbnb.jar$SEP$SIAM_LIB/jakarta-regexp-1.5.jar$SEP$SIAM_LIB/RXTXcomm.jar$SEP$SIAM_LIB/jcommon.kh.jar$SEP$SIAM_LIB/javax.jar$SEP$SIAM_LIB/log4j-1.2.13.jar$SEP$SIAM_LIB/ssds-client-pub-bob.jar$SEP$SIAM_LIB/commons-logging.jar$SEP$SIAM_LIB/javax.servlet.jar$SEP$SIAM_LIB/org.mortbay.jetty-jdk1.2.jar$SEP$SIAM_LIB/jddac-common.jar$SEP$SIAM_LIB/jddac-probe-j2me.jar"

# set log4j level
export LOG4J_THRESHOLD=INFO

# add JAVA bin
echo $PATH|grep $JAVA_HOME/bin >& /dev/null
addPath="$?"
if [ "$addPath" -ne 0 ]
  then
  export PATH=`cygpath -u $JAVA_HOME/bin${SEP}$PATH`
fi

# add SIAM utils to path
echo $PATH|grep $SIAM_HOME/utils >& /dev/null
addPath="$?"
if [ "$addPath" -ne 0 ]
  then
  export PATH=`cygpath -u $SIAM_HOME/utils${SEP}$PATH`
fi

# add FOCE utils
echo $PATH|grep $SIAM_HOME/utils/foce >& /dev/null
addPath="$?"
if [ "$addPath" -ne 0 ]
  then
  export PATH=`cygpath -u $SIAM_HOME/utils/foce${SEP}$PATH`
fi

# create handy aliases
alias dbg="export LOG4J_THRESHOLD=DEBUG"
alias inf="export LOG4J_THRESHOLD=INFO"
alias err="export LOG4J_THRESHOLD=ERROR"

# show the environment
# show the environment
echo SIAM_HOME=$SIAM_HOME
echo JAVA_DEV_ROOT=$JAVA_DEV_ROOT
echo JAVA_HOME=$JAVA_HOME
echo SIAM_CLASSPATH=$SIAM_CLASSPATH
echo JUNIT_HOME=$JUNIT_HOME
