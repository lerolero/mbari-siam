# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

@echo off
REM siam_setup.bat

SET SIAM_HOME=D:/SIAM/siam2
SET JAVA_DEV_ROOT=%SIAM_HOME%
SET SIAM_LIB=%SIAM_HOME%/jars
SET SIAM_CLASSPATH=%SIAM_HOME%/classes;%SIAM_HOME%/jars/log4j-1.2.13.jar;%SIAM_HOME/jars/instrument-proxy.jar;%SIAM_HOME/jars
SET SIAM_NODE_LOG=%SIAM_HOME%/logs/siam.nodelog
SET LOG4J_THRESHOLD=info
