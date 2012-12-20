#!/bin/sh
# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

ORIGINAL_JAVA_HOME=$JAVA_HOME
IS_UNIX=true
LOG4J_THRESHOLD=DEBUG
JAVA_DEV_ROOT=/Users/brian/workspace/siam
JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Versions/1.3/Home
JAVA=$JAVA_HOME/bin/java
PATH=$PATH:$JAVA_DEV_ROOT/utils
SIAM_HOME=$JAVA_DEV_ROOT
SIAM_CLASSPATH=$SIAM_LOG4J:$SIAM_HOME/classes
SIAM_LOG4J=$JAVA_DEV_ROOT/test/resources/log4j.properties
for each in $(ls $SIAM_HOME/jars/*.jar) 
do
    SIAM_CLASSPATH=$SIAM_CLASSPATH:$each
done

export LOG4J_THRESHOLD ORIGINAL_JAVA_HOME JAVA_DEV_ROOT JAVA_HOME IS_UNIX PATH SIAM_HOME JAVA SIAM_CLASSPATH SIAM_LOG4J

chmod u+x $JAVA_DEV_ROOT/utils/*
