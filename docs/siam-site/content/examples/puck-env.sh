#!/bin/bash
#
# Sample PUCK Environment Configuration
#
# Description: Sets up environment variables for building PUCK.
# Use:         This file may be sourced from .bash_profile:
#
# -------- snip ---------
# source puck environment if it exists
# if [ -e "${HOME}/.puck-env" ] ;  then
#   source "${HOME}/.puck-env"
# fi
# -------- snip ---------

# NOTE: SOME PROGRAMS OPERATING UNDER CYGWIN DO NOT WORK CORRECTLY 
#       IF THEIR FILE PATHS CONTAIN WHITESPACE CHARACTERS
#
# It is best to install PRDK so that relavant pathnames contain
# no whitespace characters. For example , programs like Ant or 
# Java SDK may be installed in 'C:\Program Files', which may cause 
# problems finding classes or executables.
# Possible solutions include
#
#  1) Re-install the program in a location with no spaces in the path, e.g.
#       'c:\bin\Java'
#
#  2) Use cygwin's 'mount' utility to link the location to a 
#     pathname with no embedded spaces. For example, link 
#     'c:\Program Files' to '/programFiles':
#
#        mount -bsf "c:/Program Files" /programFiles
#
# 

# Example: Mount Program Files folder to directory name w/o whitespace
# Mount Windows 'C:\Program Files' (path contains whitespace)
# to '/programfiles' (path contains no whitespace)
# mount -bsf "C:/Program Files" /programFiles

# Specify home directories - DO NOT USE WHITESPACE IN FILENAME ELEMENTS!

export JAVA_HOME=`/System/Library/Frameworks/JavaVM.framework/Versions/Current/Commands/java_home`
#export ANT_HOME=/usr/bin
export PUCK_HOME=/Users/joeUser/cvs/puck

# Set LOG4J_THRESHOLD to "DEBUG", "INFO", "WARN", or "ERROR"
export LOG4J_THRESHOLD=INFO

###################################################
# DO NOT MODIFY BELOW THIS LINE!
###################################################
# Convert HOME variables to Windows-style (or mixed) filename format, for 
# incorporation into classpath variables
#export MX_PUCK_HOME=`cygpath -m $PUCK_HOME`

export JAVA=$JAVA_HOME/bin/java

# NOTE: PUCK_CLASSPATH must be expressed in Windows-style or Mixed-style filename format
#       Also note escaped path separator "\;"
export PUCK_CLASSPATH=$PUCK_HOME/java/classes:$PUCK_HOME/java/jars/log4j-1.2.13.jar

# Prepend elements for PUCK, Ant and Java to PATH variable
#export PATH=$PUCK_HOME/utils:$JAVA_HOME/bin:$ANT_HOME/bin:$PATH
export PATH=$PUCK_HOME/utils:$JAVA_HOME/bin:$PATH
