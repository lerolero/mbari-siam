# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

@echo off

REM bcastc - broadcast test client
REM Sends a UDP broadcast packet to a waiting bcast server (see bcasts)
REM and waits for it to return the packet.
REM Use 'bcastc --help' for options
CALL siam_setup

"%JAVA%" %JAVA_OPTIONS% -Djava.security.policy="$SIAM_HOME/properties/policy" -Dlog4j.threshold=%LOG4J_THRESHOLD% -cp %SIAM_CLASSPATH% org.mbari.siam.tests.bcast.bcastcli %1 %2 %3 %4 %5 %6 %7 %8 %9
