#!/bin/bash
#

SET LOG4J_THRESHOLD=INFO

bash -c "monitorStreams foce3.pl.mbari.org 1646 | tee /cygdrive/c/foce/siam/logs/pH.`date +%%Y%%m%%d%%H%%M`"
