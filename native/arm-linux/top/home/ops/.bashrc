# .bashrc

# User specific aliases and functions

# Source global definitions
if [ -f /etc/bashrc ]; then
        . /etc/bashrc
fi

# Set the shell prompt
export PS1='\h:\w\$ '
export SIAM_HOME=/mnt/hda/siam
. /etc/siam/siamEnv

alias ll='ls -AlF'
alias info='export LOG4J_THRESHOLD=INFO'
alias debug='export LOG4J_THRESHOLD=DEBUG'
alias listp='while [ 1 ]; do listPorts localhost -stats; sleep 4; done'

# command aliases
alias ftp='echo "Use scp instead"'

alias a='cpuAwake'
alias wrpuck='writePuck'
alias rdpuck='readPuck'
alias catpuck='catPuck'

alias catjar='catJar'
alias puckjar='puckJar'

alias listports='listPorts'
alias shdport='shutdownPort'
alias scport='scanPort'
alias scports='scanPorts'
alias smplport='samplePort'
alias susport='suspendPort'
alias puport='powerUpPort'
alias pdport='powerDownPort'
alias rsport='resumePort'
alias conport='conPort'
alias port485='portTo485'
alias portstat='portStatus'
alias portprop='portProperties'

alias gleases='getLeases'
alias getleases='getLeases'

alias getmeta='getMetadata'
alias getlast='getLastSample'

alias addsch='addSchedule'
alias shsch='showSchedule'
alias rssch='resumeSchedule'
alias rmsch='removeSchedule'
alias susssch='suspendSchedule'
alias setsch='setSchedule'

alias dpaview='dpaView'

alias logpub='logPublish'
alias logview='logView'

alias exitnode='exitNode'
alias nodestat='nodeStatus'
alias listsub='listSubnodes'
alias listsw='listSwitches'
alias siamhelp='alias'
alias killnode='killBIN'

alias rstturns='resetTurns'
alias swhv='switchHiVoltage'

alias nodeid='showconf | grep nodeID'
alias sleepstat='showconf | grep "SleepManager\.enabled"'

alias inf='export LOG4J_THRESHOLD=INFO'
alias dbg='export LOG4J_THRESHOLD=DEBUG'

#alias static=ifswitch-to-static
alias dhcp=ifswitch-to-dhcp
