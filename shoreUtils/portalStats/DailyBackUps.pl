#!/usr/bin/perl

use Time::Local;
#print system("\ndate +%y%m%d_%k%M");
#
# Script copies autometrics logs, graphs, etc to a server for backups.
#

# Variables
#
# Set up the home directory on the portal.
$HOME = "/home/ops/mse/siam/shoreUtils/portalStats";
$SIAM_HOME = "/home/ops/mse/siam";

# Set up the portal name (i.e., siam-portal-1 or siam-portal-3
$PORTAL = "siam-portal-1";

# Get the local Date and Time to name generated TAR files.
($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst) = localtime(time); 
$year = sprintf("%02d", $year % 100);
$mon = sprintf("%02d", $mon + 1); 
$min = sprintf("%02d", $min);
$hour = sprintf("%02d", $hour); 
$mday = sprintf("%02d", $mday);
$date = $year . $mon . $mday . "_" . $hour . $min;
print "----------------------------------------------\n";
print "\nBacking up " . $PORTAL . " autometrics and portal files.....\n";
print "\nNOTE: Current TAR file will be named " . $date . ".tar.gz";
      

# Begin backups
#
system("mkdir $HOME/tempDir");
system("cp $HOME/statFiles/*.jpg $HOME/tempDir/");
print "\n\nCopying JPGs.\n";
system("cp $SIAM_HOME/logs/*.gif $HOME/tempDir/");
print "\n\nCopying GIFs.\n";
system("cp $SIAM_HOME/logs/*.png $HOME/tempDir/");
print "\n\nCopying PNGs.\n";
system("cp $HOME/*.html $HOME/tempDir/");
system("cp $SIAM_HOME/logs/summary.html $HOME/tempDir/");
print "Copying HTMLs.\n";
system("cp $SIAM_HOME/logs/portal.out $HOME/tempDir/");
print "Copying portal.out.\n";
system("cp $SIAM_HOME/logs/profile.out $HOME/tempDir/");
print "Copying profile.out.\n";
print "\nFinished copying files.\n\n";

#
# Tar up the files
#
print "\nTARing the following files for Backup.\n";
print "----------------------------------------\n";
$tarString = "tar czvf " . $date . ".tar.gz *";
system("cd $HOME/tempDir;" . $tarString); 


#
# Copying files over to backup server
#
print "\n\nCopying TAR file to ABALONE server.\n";
system("scp $HOME/tempDir/*.tar.gz ops\@abalone:/home/ops/logs/portalStats/daily_TARs/$PORTAL/");


#
# Removing unwanted files.
#
print "\nPerforming cleanup of temporary files.\n";
system("rm $HOME/tempDir/*.jpg");
system("rm $HOME/tempDir/*.html");
system("rm $HOME/tempDir/*.out");
system("mv $HOME/tempDir/*.tar.gz $HOME/TAR_archives/");
print "\nMoving " . $date . ".tar.gz to /home/ops/mse/siam/shoreUtils/portalStats/TAR_archives on $PORTAL.\n\n\n";
system("rm -rf $HOME/tempDir/");