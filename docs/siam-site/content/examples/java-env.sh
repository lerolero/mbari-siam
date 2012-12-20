#!/bin/bash
#
# Sample JAVA Environment Configuration
#
# Description: Sets up environment variables for JAVA.
# Use:         This file may be sourced from .bash_profile:
#
# -------- snip ---------
# source java environment if it exists
# if [ -e "${HOME}/.java-env" ] ;  then
#   source "${HOME}/.java-env"
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

# Specify home directories - DO NOT EMBED SPACES IN FILENAME ELEMENTS!
#export JAVA_HOME=/usr
export JAVA_HOME=`/System/Library/Frameworks/JavaVM.framework/Versions/Current/Commands/java_home`

###################################################
# DO NOT MODIFY BELOW THIS LINE!
###################################################

export JAVA=$JAVA_HOME/bin/java

# Prepend elements for PRDK, Ant and Java to PATH variable
# (also need to include path to mspgcc)

# add JAVA bin
echo $PATH|grep $JAvA_HOME/bin >& /dev/null
addPath="$?"
if [ "$addPath" -ne 0 ]
  then
  export PATH=$JAvA_HOME/bin${SEP}$PATH
fi
