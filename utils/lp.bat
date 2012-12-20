# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

@echo off
CALL siam_setup
:repeat
%JAVA% %JAVA_OPTIONS% -Djava.security.policy="%SIAM_HOME%/properties/policy" -Dlog4j.threshold=%LOG4J_THRESHOLD% -cp %SIAM_CLASSPATH% org.mbari.siam.operations.utils.PortLister foce3.pl.mbari.org -stats
sleep 5
goto repeat
