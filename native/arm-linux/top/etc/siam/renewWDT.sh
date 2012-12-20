#!/bin/bash

WDT_FILE="/proc/watchdog"
DOG_FOOD="feed watchdog"
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
  echo "     -s <sleep>    : Sleep string       [$DOG_FOOD]"
  echo "     -v            : verbose output     [$VERBOSE]"
  echo
  echo "description:"
  echo "     Renews watchdog timer (WDT), i.e. 'feeds the dog'"
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
	-[s] ) 
	    shift
	    DOG_FOOD="$1"
	    echo "Sleep string set to $DOG_FOOD"
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

  if [ "$VERBOSE" == "true" ]
  then
     wdtState=`cat $WDT_FILE`
     echo $wdtState
  fi

  echo $DOG_FOOD > $WDT_FILE

  sleep $PERIOD

done

