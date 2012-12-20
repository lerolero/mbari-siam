@echo off

REM sddtest - Test program for BB232SDD16

"%JAVA%" %JAVA_OPTIONS% -Djava.security.policy="$SIAM_HOME/properties/policy" -Dlog4j.threshold=%LOG4J_THRESHOLD% -cp %SIAM_CLASSPATH% org.mbari.siam.devices.bbElec.BBSDDTest %1 %2
