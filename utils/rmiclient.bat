# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

@echo off
CALL siam_setup
SET CLIENT_CLASSES="%SIAM_HOME%\build\testrmiclient"
SET TEST_RESOURCES="%SIAM_HOME%\test\resources"
SET SERVER_CODEBASE="file:///Documents and Settings/brian/workspace/siam/classes"

%JAVA% %JAVA_OPTIONS% -cp %TEST_RESOURCES%;%SIAM_JARS%;%CLIENT_CLASSES% -Djava.rmi.server.codebase=%SERVER_CODEBAE% -Djava.security.policy="%SIAM_HOME%/properties/policy" -Dsiam_home="%SIAM_HOME%" -Dlog4j.threshold=%LOG4J_THRESHOLD% org.mbari.siam.operations.utils.RMIRun org.mbari.siam.tests.linkBenchmarks.client.Benchmark1Test %1
