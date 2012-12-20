#!/bin/bash

# set classpath (most SIAM apps just use SIAM_CLASSPATH)
HELLO_CLASSPATH=$SIAM_CLASSPATH:$SIAM_HOME/classes:$SIAM_HOME/jars/jserial.jar

# invoke the app
$JAVA -cp $HELLO_CLASSPATH org.mbari.siam.devices.fakeotron.FakeOTron "$@"
