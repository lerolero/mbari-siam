#!/bin/bash
#

SET LOG4J_THRESHOLD=INFO

bash -c "monitorStreams foce3.pl.mbari.org 1648 1671 | tee /cygdrive/c/foce/siam/logs/power.`date +%%Y%%m%%d%%H%%M`"
