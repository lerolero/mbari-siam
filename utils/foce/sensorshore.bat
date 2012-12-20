#!/bin/bash
#

SET LOG4J_THRESHOLD=INFO

bash -c "monitorStreams foce3.shore.mbari.org 1642 1647 1649 | tee /cygdrive/c/foce/siam/logs/sensors.`date +%%Y%%m%%d%%H%%M`"
