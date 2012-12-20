# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

@echo off
CALL siam_setup
echo Starting node [ Log Level @ %LOG_THRESHOLD% ]

cd %SIAM_HOME%

"%JAVA%" %JAVA_OPTIONS% -Djava.rmi.server.codebase="%SIAM_CODEBASE%" -Djava.security.policy="$SIAM_HOME/properties/policy" -Dlog4j.threshold=%LOG4J_THRESHOLD% -cp %SIAM_CLASSPATH% org.mbari.siam.core.NodeMain %1 %2 %3 %4 %5 %6 %7 %8 %9

