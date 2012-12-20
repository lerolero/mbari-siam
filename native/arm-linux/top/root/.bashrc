#/bin/bash
# User specific aliases and functions

# Source global definitions
if [ -f /etc/bashrc ]; then
        . /etc/bashrc
fi

# Get SIAM environment
. /home/ops/.bashrc

# set prompt
PS1="\u@\h:\w\\$ "

alias java=j9
alias ls='ls -F'
alias ll='ls -l'
