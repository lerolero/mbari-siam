@echo off
CALL %SIAM_HOME%/utils/siam_setup
%JAVA% %JAVA_OPTIONS% -Djava.security.policy="%SIAM_HOME%/properties/policy" -cp %SIAM_CLASSPATH% org.mbari.siam.tests.socket.SockClient %1 %2 %3 %4
