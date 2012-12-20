#!/bin/bash

WDT_FILE="/proc/watchdog"
SNARL_WARNING="watchdog status: snarling"
VERBOSE="false"
PERIOD=5

printUsage() {
  echo
  echo "usage: `basename $0` [options]"
  echo
  echo "options:"
  echo "     -h, --help    : this help message"
  echo "     -t <period>   : set polling period [$PERIOD]"
  echo "     -f <WDT file> : WDT file           [$WDT_FILE]"
  echo "     -w <snarl>    : Snarl warning text [$SNARL_WARNING]"
  echo "     -v            : verbose output     [$VERBOSE]"
  echo
  echo "description:"
  echo "     Monitors watchdog timer (WDT)"
  echo
  exit 1
}


while [ "$#" -gt "0" ]
do

    case "$1" in
	-h ) 
	    printUsage
    ;;
	--help ) 
	    printUsage
    ;;
	-[v] ) 
	    VERBOSE="true"
    ;;
	-[t] ) 
	    shift
	    let PERIOD="$1"
	    echo "period set to $PERIOD"
    ;;
	-[f] ) 
	    shift
	    WDT_FILE="$1"
	    echo "WDT file set to $WDT_FILE"
    ;;
	-[w] ) 
	    shift
	    SNARL_WARNING="$1"
	    echo "Snarl text set to $SNARL_WARNING"
    ;;
        -[v][t] )
	    VERBOSE="true"
	    shift
	    let PERIOD="$1"
	    echo "period set to $PERIOD"
    ;;
    esac

 shift
done


while [ 1 ]
do
    wdtState=`cat $WDT_FILE`

    if [ "$VERBOSE" == "true" ]
    then
	echo $wdtState
    fi
    if [ "$wdtState" == "$SNARL_WARNING" ]
    then
	echo "uh oh...wdt is snarling...RUN!"
    fi
    sleep $PERIOD

done

