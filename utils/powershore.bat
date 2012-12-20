#!/bin/bash
# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

#!/bin/bash
#

SET LOG4J_THRESHOLD=INFO

bash -c "monitorStreams foce3.shore.mbari.org 1648 1671 | tee /cygdrive/c/foce/siam/logs/power.`date +%%Y%%m%%d%%H%%M`"
