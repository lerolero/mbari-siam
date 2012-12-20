# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.


# Check args and print use message if invalid
if ( [[ -z $1 ]] || [[ -z $2 ]] ) then
echo usage: $0 port_name command
exit
fi

# Script executes moos.utils.CommIoTest.main()
j9 -cp:$JAVA_HOME/lib/jclMax/classes.zip:$JAVA_HOME/lib/prsnlmot.jar:$JAVA_HOME/lib/RXTXcomm.jar:$SIAM_HOME/classes \
-Dgnu.io.rxtx.SerialPorts=/dev/ttySX0:/dev/ttySX1:/dev/ttySX2:/dev/ttySX3:/dev/ttySX4:/dev/ttySX5:/dev/ttySX6:/dev/ttySX7:/dev/ttySA2 \
-Dsiam_home=$SIAM_HOME \
org.mbari.siam.utils.CommIoTest $1 $2


