#!/usr/bin/perl

use Time::Local;

#
# Script reads in log files and outputs files for GNU plot.
#

#
# Variables
#

# Variables derived from the profile log
$sessDate = "";                      # Date in human readable
$sessID = 0;                         # Date in EPOCH
$sessSummDurationSec = 0;
$sessSummBytes = 0;
$sessSummPackets = 0;
$sessSummNodes = 0;
$sessSummDevices = 0;               
$sessVolume = 0;
$cumulativeVolume = 0; 		     # Cumulative Volume 
$sessThroughput = 0;
$sessCountTotal = 0;                 # Counts the number of total sessions
$totalSessTimeExecuted = 0;          # Amount of the session time used for event execution

# Variables for events from the profile log
@eventArray = (["connectNode",0,0]); # Array to store event names, time, and how many times called
$eventName = "";                     # Name of the event
$eventExecutionTime = 0;             # How long it took the event to execute
$totalEvents = 0;                    # Total of all events combined
$totalExecutionTime = 0;             # Total execution time of all events combined

# Totals used for calculating averages
$durationsTotal = 0;
$throughputTotal = 0;
$volumeTotal = 0; # Volume just for Profiled Time Duration.
$cumVolumeTotal = 0; # Cumulative - Volume for entire Portal Run.
$pppThroughputTotal = 0;
$dailyMBperDay = 0;
$totalTimeSync = 0; # DEBUG
$totalPortalRunTimeHour = 0;
$totalPortalVolume = 0;

# Averages 
$avgThroughput = 0;           
$avgVolume = 0;
$avgDuration = 0;
$avgPPPThroughput = 0;

# Variables from /var/log/messages
$pppVolume = 0;                      # Volume calculated from /var/log/messages
$totalPPPVolume = 0;                 # Sum of each session volume from /var/log/messages
$pppThroughput = 0;                  # Throughput for the entire PPP connection

# Variables from ppp.log
$pppLogUp = 0;
$pppLogDown = 0;

# New Variables to help with errors writing to .tmp files
$pppLogSessMatch = true;	     # Boolean for determining first session time sync
$varLogSessMatch = true;	     # Boolean for determining first session time sync

# Other variables
$then = 0;                           # Time in EPOCH time to go back to in the log
$firstSessIDAcq = false;             # Boolean for receiving the first TOTAL session ID (time EPOCH)
$firstSessID    = 0;                 # Variable to store the first TOTAL session ID (time EPOCH)
$firstProfiledSessIDAcq = false;     # Boolean for receiving the first session ID (time EPOCH)
$firstProfiledSessID    = 0;	     # Variable to store the first session ID (time EPOCH)
$portalName = "";                    # Optional command line arg to pass name of portal to output

############################                             
#  Begin the main program  #
############################

# Fire up log file
open (mainLog, ">/home/ops/siam/shoreUtils/portalStats/PortalStats.log") or die "$! Error creating log file";
print mainLog "Begin Portal Calculations: " . localtime() . "\n";


# Read in command line in order to parse config file
if (@ARGV) {
	print "Processing " . @ARGV[0] . "\n";
} 
else {
	print "Error! Usage: perl CalculatePortalStats.pl <path/CONFIG FILE> [optional: <hours back> <name of portal> ] \n";
}

#
# Config File Variables get set
#
open confFile, @ARGV[0] or die "Cannot open config File:" . @ARGV[0] . "\n";
while (<confFile>) {
                      # remove:
	chomp;              # newline chars
	s/#.*//;            # comments
	s/^\s+//;           # leading whitespace
	s/\s+$//;           # trailing whitespace
	next unless length; # anything else?
	
	my ($var, $value) = split(/\s*=\s*/, $_, 2); # assign value to variable
	no strict 'refs';   # create var name exactly how it is in the conf file
	$$var = $value; 	  # make assignment
	print mainLog $var . " = " . $value . "\n";
} # end of config file variable assignments
close(confFile);

#
# Use command line for hours to go back if it exists. Hours on command line OVERRIDE hours in config file!
#
if ($ARGV[1]) {
  print mainLog "Using command line for duration hours back in log from present";
  $hoursBack = $ARGV[1];
}

# Add optional portal name from command line
if ($ARGV[2]) {
	$portalName = $ARGV[2];
}

# Figure out how far back to go
$then = ((time() - ($hoursBack * 3600)) * 1000); # convert hours to seconds and multiply by 1000 for epoch msec. Add 8 hrs for GMT
print mainLog "Going back to: " . $then . "\n";

#
# Open html output file
#
$htmlFilename = "autometrics_" . $hoursBack . ".html"; # Name the file with how many hours back in the logs it goes
open (htmlOut, ">/home/ops/siam/shoreUtils/portalStats/$htmlFilename") or die "$! Error creating html file";
print htmlOut "<html>\n<head>\n<title>Script Automation</title>\n</head>\n<body>Begin $portalName Portal Calculations: " . localtime() . "<br>";


#
# Open the input log file(s) read only
#
open profile_FH, $profileLog or die "Cannot open log file:" . $profileLog . "\n";
open portal_FH, $portalLog or die "Cannot open log file:" . $portalLog . "\n";
open varLog_FH, $varLog or die "Cannot open log file:" . $varLog . "\n";
open pppLog_FH, $pppLog or die "Cannot open log file:" . $pppLog . "\n";

#
# Open data files read/write for gnuplot
# 
open (duration_Data, ">/home/ops/siam/shoreUtils/portalStats/statFiles/duration_Data.dat") or die "$! Error creating duration_Data";
open (throughput_Data, ">/home/ops/siam/shoreUtils/portalStats/statFiles/throughput_Data.dat") or die "$! Error creating throughput_Data";
open (volume_Data, ">/home/ops/siam/shoreUtils/portalStats/statFiles/volume_Data.dat") or die "$! Error creating volume_Data";




############################                             
# Begin To Parse Log Files #
############################
#
# Begin processing the profile log
#
while (defined ($line = <profile_FH>)) {
	# Parse session summary data
	if ($line =~/(^\d+.+UTC);.+SessionSummary.+id;(\d+);.+durationSec;(\d+);nodes;(\d+);devices;(\d+);packets;(\d+);bytes;(\d+)$/) {
		  $sessDate              = $1;
		  $sessID                = $2;
		  $sessSummDurationSec   = $3; 
		  $sessSummNodes         = $4;
      		  $sessSummDevices       = $5;
      		  $sessSummPackets       = $6;
      		  $sessSummBytes         = $7;

    if ($firstSessIDAcq eq false) {  # Cumulative - Total profile stats since start.
		$firstSessID = $sessID;
	        $firstSessIDAcq = true;
		}

# Calculate total Portal Volume
      if ( $sessSummBytes > 0 ) {
      $cumulativeVolume = (($sessSummBytes)/1024);
      }
      else {
      $cumulativeVolume = 0;
      }

      $cumVolumeTotal += $cumulativeVolume;



    if ($then <= $sessID) {

#DEBUG print mainLog "Profile: " . $line . "\n";  #DEBUG

	if ($firstProfiledSessIDAcq eq false) {  # Daily profile stats since command line $then.
		$firstProfiledSessID = $sessID;
	        $firstProfiledSessIDAcq = true;
		}

      # Do Calculations
      $sessCountTotal += 1; # Increment count of total sessions
      $durationsTotal += $sessSummDurationSec; # Sum total durations for log (or log period examined) 

      if ($sessSummDurationSec == 0) {
        $sessSummDurationSec = 1;
      }    

      $sessThroughput = (($sessSummBytes * 10) / $sessSummDurationSec); # 10 bits/byte divded by duration
      $throughputTotal += $sessThroughput;

# Calculate Volume Profiled Sessions
      if ( $sessSummBytes > 0 ) {
      $sessVolume = (($sessSummBytes)/1024);
      }
      else {
      $sessVolume = 0;
      }

      $volumeTotal += $sessVolume;
    
      # Print Results
#      print mainLog $line;
#		  print mainLog "Session Date: " . $sessDate . "\n";
#		  print mainLog "Session ID: " . $sessID . "\n";
#		  print mainLog "sessSummDurationSec: " . $sessSummDurationSec . "\n";
		  #DEBUGprint mainLog "sessSummNodes: " . $sessSummNodes . "\n";
		  #DEBUGprint mainLog "sessSummDevices: " . $sessSummDevices . "\n";
		  #DEBUGprint mainLog "sessSummPackets: " . $sessSummPackets . "\n";
		  #DEBUGprint mainLog "sessSummBytes: " . $sessSummBytes . "\n";
		  #DEBUGprint mainLog "sessThroughput bps: " . $sessThroughput . "\n";
		  #DEBUGprint mainLog "sessVolume kbytes: " . $sessVolume . "\n";
		  
#
# Process the ppp log while inside the profile log loop
#
#DEBUG$/ = "CONNECT"; # change line character to grab up until next "CONNECT" for easier parse of both lines in log file

# If couldn't sync PPP Log last time, try, try again.
if ($pppLogSessMatch eq false) {
      close (pppLog_FH);
      open pppLog_FH, $pppLog or die "Cannot open log file:" . $pppLog . "\n";
}
else {
    $pppLogSessMatch = false;
}
      while (defined ($pppLogLine = <pppLog_FH>) ) {

	if ($pppLogLine =~/.*\n?IP-UP\s+(\d+).*$/){
		      $pppLogLine = pppLog_FH.readline();
	if ($pppLogLine =~/IP-UP\s+(\d+).+\n?$\n?IP-DOWN\s+(\d+).+$/) {
		      $pppLogUp    = $1;
		      $pppLogDown  = $2;
		      
		      $pppLogUp = $pppLogUp * 1000; # Convert to EPOCH msec
		      $pppLogDown = $pppLogDown * 1000;		      

		      if ( ($pppLogUp >= ($sessID - 5000)) && ( $pppLogUp <= ($sessID+($sessSummDurationSec*1000)) ) ) { # Check that it's within the correct time in the profile log
                      $pppLogSessMatch = true;
		      

		      # Write duration data
		      print duration_Data $sessDate . ";" . $sessSummDurationSec . ";" . (($pppLogDown - $pppLogUp)/1000) . "\n"; 
            last;
          }
	}                
      }
    }
#      $/ = "\n"; # set the line character back to newline
# End of PPP log processing 


#
# Process the var/log/messages log while inside the profile log loop
#
      # Convert names to numbers
      my %months;
      @months{qw/Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec/} = (0 .. 11);

# If couldn't sync VARLog last time, try, try again.
if ($varLogSessMatch eq false) {
      close (varLog_FH);
      open varLog_FH, $varLog or die "Cannot open log file:" . $varLog . "\n";
}
else {
    $varLogSessMatch = false;
}
      while (defined ($varLine = <varLog_FH>)) {
        if ($varLine =~/(^\w+)\s+(\d+)\s+(\d+):(\d+):(\d+)\s+.*Sent\s+(\d+)\s+bytes, received (\d+)\s+.*/ ) {
		      $varMonth  = $1;
		      $varMonth = $months{$varMonth}; # convert to number
		      $varDay    = $2;
		      $varHour   = $3;
		      $varMin    = $4;
		      $varSec    = $5;
		      $varSent   = $6;
		      $varRev    = $7;
		      $varEpoch = timegm($varSec, $varMin, $varHour, $varDay, $varMonth, (localtime)[5]);

####################################################
###### GMT TIME CHANGE - PACIFIC TIME TO GMT #######
####################################################
          $varEpoch += 25200; # Convert to GMT (Currently 7 HOUR offset; 8 hour = 28800)
          #print mainLog "varEpoch:$varEpoch\n";

          # if this epoch is > sessId and < sessID + duration + 50 sec, then we have a time sync
          if ( (($varEpoch*1000) >= ($sessID)) &&  (($varEpoch*1000) <= ($sessID + 50000 + ($sessSummDurationSec*1000))) ) { 
$totalTimeSync += 1;
#DEBUG print mainLog "varLog: " . $varLine . "\n";  #DEBUG
                      $varLogSessMatch = true;
#	              print mainLog $varLine;
#	              print mainLog "epoch:" . $varEpoch*1000 . "\n\n";
	          
	          #DEBUGprint mainLog "varMonth:" . $varMonth . "\n";
	          #DEBUGprint mainLog "varDay:" . $varDay . "\n";
	          #DEBUGprint mainLog "varHour:" . $varHour . "\n";
	          #DEBUGprint mainLog "varMin:" . $varMin . "\n";
	          #DEBUGprint mainLog "varSec:" . $varSec . "\n";
	          #DEBUGprint mainLog "varSent:" . $varSent . "\n";
	          #DEBUGprint mainLog "varRev:" . $varRev . "\n";
	          #DEBUGprint mainLog "varEpoch:" . $varEpoch . "\n";

	    
	          # Do calculations
	          $pppVolume = (($varSent + $varRev)/1024);
	          $totalPPPVolume += $pppVolume;
            $pppThroughput = ((($varSent + $varRev) * 10) / $sessSummDurationSec); # 10 bits/byte divded by duration
            $pppThroughputTotal += $pppThroughput;
            
	    
	          # Write throughput data
		  print throughput_Data $sessDate . ";" . $sessThroughput . ";" . $pppThroughput . "\n"; 
		        
		        # Write volume data
      		  print volume_Data $sessDate . ";" . $sessVolume . ";" . $pppVolume . "\n";   

	          
	          last;
	    
          } # the time is right
        } # line contains sent / received bytes
      } # end of var log messages log loop
# End /var/log/messages log processing


    } # Session ID valid in profile log loop
  } # Line contains summary data in profile log loop


	# Parse event data in the profile log
  if ($then <= $sessID) {
	  if ($line =~/^\d+.+"event";\d+;(\w+);\d+;\d+;(\d+).*$/) {
		  $eventName            = $1;
		  $eventExecutionTime   = $2;		  
		  
		  #Do Calculations
		  $totalEvents += 1;
		  $totalExecutionTime += $eventExecutionTime;
		  
#DEBUG		   print mainLog $line;
#		   print mainLog "eventName: " . $eventName . "\n";
#	   print mainLog "eventExecutionTime (msec): " . $eventExecutionTime . "\n";
		   
		   for (my $i=0; $i <= $#eventArray; $i++) {
		     if ($eventArray[$i][0] eq $eventName) {
		       $eventArray[$i][1] += 1; # increment the count
		       $eventArray[$i][2] += $eventExecutionTime; # increment the execution time total 
		       last; # match found so break out of loop
		     }
		     elsif ($i >= $#eventArray) { # Otherwise it's a new event so add it to the array
		       my @tmp = ($eventName,1,$eventExecutionTime);
		       push(@eventArray, [@tmp]);	 
		       print mainLog "ADD EVENT: " . $eventName . "\n";
		     } 	
		  }
	  
		}
  } # session ID within range  

} # end of profile log loop






### DATE CONVERSION DELETE THIS EVENTUALLY
#print system("\ndate -d 'Feb  1 16:35:54' +%s");
#print "END\n";
#print system("date -d 'Feb  1 2007 16:35:54' +%s");

############################                             
#    Final Calculations    # 
############################
# Estimate daily volume throughput.
$dailyMBperDay = (($volumeTotal / 1000) * (86400 / ((($sessID + $sessSummDurationSec) - $firstProfiledSessID) / 1000)));
$totalPortalRunTimeHour = (((($sessID + $sessSummDurationSec) - $firstSessID)/1000)/3600);
$totalPortalVolume = ($cumVolumeTotal / 1000);

# Log final numbers

# Cumulative Portal Statistics
print htmlOut "<br><b><u>Cumulative Statistics Since Portal Start:</u></b><br>";
print htmlOut "Total Portal Run Time: <b>" . sprintf("%i", (($sessID + $sessSummDurationSec) - $firstSessID)/1000) . " seconds &nbsp[" . sprintf("%.2f", ((($sessID + $sessSummDurationSec) - $firstSessID)/1000)/3600) . " hour(s)]</b><br>";
print htmlOut "Total Portal Volume: <b>" . sprintf("%.2f",  ($cumVolumeTotal / 1000)) . " MB</b><br>";
print htmlOut "Average SIAM Telemetry Rate: <b>" . sprintf("%.2f", (($totalPortalVolume / $totalPortalRunTimeHour) * 24)) . " MB / day</b><br>";

# Daily Profiled Portal Statistics
print htmlOut "<br><b><u>Profiled Statistics for last " . sprintf("%.0f", ((($sessID + $sessSummDurationSec) - $firstProfiledSessID)/1000)/3600) . " hour(s):</u></b></br>";
print htmlOut "Profiled Duration of all Sessions: <b>" . $durationsTotal . " seconds &nbsp[" . sprintf("%.2f", ($durationsTotal/3600)) . " hour(s)]</b><br>";
print htmlOut "Profiled Portal Run Time: <b>" . sprintf("%i", (($sessID + $sessSummDurationSec) - $firstProfiledSessID)/1000) . " seconds &nbsp[" . sprintf("%.2f", ((($sessID + $sessSummDurationSec) - $firstProfiledSessID)/1000)/3600) . " hour(s)]</b><br>";
print htmlOut "Total Sessions: <b>" . $sessCountTotal . "</b><br>";
if ( $totalPPPVolume == 0 ) {  #Making sure totalPPPVolume doesn't = 0 for first run session summary in Profile.out
  $totalPPPVolume = 1;
}
print htmlOut "Percent Time Connected: <b>" . sprintf("%.2f", ($durationsTotal / ((($sessID + $sessSummDurationSec) - $firstProfiledSessID) / 1000)) * 100) . "%</b><br>";
print htmlOut "Average Utility (Volume/PPP Volume): <b>" . sprintf("%.1f", (($volumeTotal / $totalPPPVolume) * 100) ) . "%</b><br>";
print htmlOut "Volume of Profiled Sessions: <b>" . sprintf("%.2f",  ($volumeTotal / 1000)) . " MB</b><br>";
print htmlOut "Projected Total SIAM Data Volume/Day (from Profile log): <b>" . sprintf("%.2f",  $dailyMBperDay) . " MB</b><br>";

#DEBUGprint "Total Time Sync = " . $totalTimeSync . "\n";

$avgDuration = ($durationsTotal / $sessCountTotal);
$avgThroughput = $throughputTotal / $sessCountTotal;
$avgVolume = $volumeTotal / $sessCountTotal;

# Print out the graphs
print htmlOut "<br><br><hr>Average Application Duration Sec. (As reported by profile log): <b>" . sprintf("%.1f", $avgDuration) . "</b>";
print htmlOut "<br><IMG SRC=\"./Connection_Duration.jpg\"><br><br><hr>";

print htmlOut "<br>Average Data Throughput BPS (As reported by profile log): <b>" . sprintf("%.1f", $avgThroughput) . "</b>";
print htmlOut "<br><IMG SRC=\"./Connection_Throughput.jpg\"><br><br><hr>";

print htmlOut "<br>Average Data Volume Kbytes (As reported by profile log): <b>" . sprintf("%.1f", $avgVolume) . "</b>";
print htmlOut "<br><IMG SRC=\"./Connection_Volume.jpg\"><br><br><hr>";

# Print event array data
print htmlOut "<br><br><b>EVENT SUMMARIES:</b><br>\n";
for (my $x=0; $x <= $#eventArray; $x++) {
  print htmlOut "<br>Event: <b>" . $eventArray[$x][0] . "</b> executed: " . $eventArray[$x][1] . " times<br>";

if ( $totalEvents == 0 ) {  #Making sure totalEvents doesn't = 0 for first run session summary in Profile.out
  $totalEvents = 1;
}

  print htmlOut "Percent of total events: " . sprintf("%.1f", (($eventArray[$x][1] / $totalEvents)*100) ) . "%<br>";

if ( $totalExecutionTime == 0 ) { #Making sure totalExecutionTime doesn't = 0 for first run session summary in Profile.out
  $totalExecutionTime =1;
}
  print htmlOut "Percent of total execution time: " . sprintf("%.1f", (($eventArray[$x][2] / $totalExecutionTime)*100) ) . "%<br>";
  print htmlOut "Percent of total session time: " . sprintf("%.1f", (($eventArray[$x][2] / ($durationsTotal*1000))*100) ) . "%<br><br>";  
  $totalSessTimeExecuted += (($eventArray[$x][2] / ($durationsTotal*1000))*100);
}
  print htmlOut "<b>Unknown</b> total percent of session time: " . (sprintf("%.1f", (100 - $totalSessTimeExecuted))) . "%<br>";

  print htmlOut "<b>Log Sync Accuracy: " . sprintf("%.2f", (($totalTimeSync * 100) / $sessCountTotal)) . "%</b><br>";
 
		  #Do Calculations
		  $totalEvents += 1;
		  $totalExecutionTime += $eventExecutionTime;



# Prepare to graph and exit
print mainLog "\nClosing files and exiting... " . localtime() . "\n";

# Close input files
close (profile_FH);
close (portal_FH);
close (varLog_FH);
close (pppLog_FH);

# Close output data files
close (duration_Data);
close (throughput_Data);
close (volume_Data);



# Call graphing routines to make graphs
&graphDurations;
&graphThroughput;
&graphVolume;

# Close html file
print htmlOut "</body>\n</html>";
close (htmlOut);

# Copy files over to webserver
system("scp /home/ops/siam/shoreUtils/portalStats/statFiles/*.jpg ops\@abalone:/var/www/html/siam2/mseAutoMetrics/");
system("scp /home/ops/siam/shoreUtils/portalStats/autometrics_$hoursBack.html ops\@abalone:/var/www/html/siam2/mseAutoMetrics/");
# Close log file
close (mainLog);

#
# Subroutine Definitions
#
sub graphDurations {
	print mainLog "graphing durations\n";
  system("/usr/local/bin/gnuplot '/home/ops/siam/shoreUtils/portalStats/statFiles/duration.tmp'");
}

sub graphThroughput {
	print mainLog "graphing throughput\n";
  system("/usr/local/bin/gnuplot '/home/ops/siam/shoreUtils/portalStats/statFiles/throughput.tmp'");
}

sub graphVolume {
	print mainLog "graphing volume\n";
  system("/usr/local/bin/gnuplot '/home/ops/siam/shoreUtils/portalStats/statFiles/volume.tmp'");
}

# Run Back_ups of files - Currently performed by crontab -e
#system("/home/ops/mse/siam/shoreUtils/portalStats/mkBackups.sh");

exit 0;

