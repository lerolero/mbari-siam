# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

BEGIN {
  print "<title>Overview of portal events</title>";
  print "<body>";
  _printSeconds = 1;

  if (_printSeconds) {
    printf "<br>Elapsed time and (+delta) in seconds\n";
  }
  else {
    printf "<br>Elapsed time and (+delta) in milliseconds\n";
  }

  PPP_CALLBACK = "pppCallback";
  CONNECTION_WORKER = "ConnectionWorker";
  WAKEUP_WORKER = "WakeupWorker";
  UNKNOWN_THREAD = "Unknown";
  SURFACE_CONNECTED = "SurfaceConnected";
  MAIN = "Main";

  _color[MAIN] = "black";
  _color[PPP_CALLBACK] = "red";
  _color[CONNECTION_WORKER] = "green";
  _color[WAKEUP_WORKER] = "blue";
  _color[SURFACE_CONNECTED] = "maroon";

  print "<br>Thread color code: ";
  print "<br><font color=\"" _color[MAIN] "\">" MAIN "</font>";
  print "<br><font color=\"" _color[SURFACE_CONNECTED] "\">" SURFACE_CONNECTED "</font>";
  print "<br><font color=\"" _color[CONNECTION_WORKER] "\">" CONNECTION_WORKER "</font>";
  print "<br><font color=\"" _color[WAKEUP_WORKER] "\">" WAKEUP_WORKER "</font>";
  print "<br><font color=\"" _color[PPP_CALLBACK] "\">" PPP_CALLBACK "</font>";
  print "<br>";
}


{
  if ($2 == "INFO" || $2 == "DEBUG" || $2 == "WARN" || $2 == "ERROR") {
    _elapsedMsec = $1;
  }
}

{
  if (match($4, "main")) {
    _thread = MAIN;
  }
  else if (match($4, "RMI")) {
    _thread = PPP_CALLBACK;
  }
  else if (match($4, "_connectionWorker")) {
    _thread = CONNECTION_WORKER;
  }
  else if (match($4, "_wakeupWorker")) {
    _thread = WAKEUP_WORKER;
  }
  else if (match($4, "Thread-")) {
    _thread = SURFACE_CONNECTED;
  }
  else {
    _thread = UNKNOWN_THREAD;
  }
}


{
  if ($2 == "ERROR") {
    $1 = $2 = $3 = $4 = "";
    printLine("ERROR: " $0 "\n");
  }
}

/Portal got connection from/ {
  printLine("Received connection msg from " $9);
}



/Send wakeup signal to subnode/ {
  printLine("Send wakeup signal to " $12);
}

/NodeProbe returned/ {
  printLine("NodeProbe returned " $7 " for " $9);
}


/startSession.*get node proxy/ {
  printLine("Get proxy from "  substr($5, 16, length($5)-16) "\n");
}


/Lease established for / {
  printLine("Established lease on " $11 "\n");
}


/INFO.*Command Response:/ {
  printLine("Ready to download from " $5 "\n");
}

/INFO.*startSession\(\)/ {
  $1 = $2 = $3 = $4 = "";
  printLine($0);
}

/INFO.*Retrieving data from/ {
  printLine("Retrieving data from device " $9);
}

/Portal.nodeLinkNotify.*ON/ {
  pppOnMsec = $1;
  printLine("PPP is ON");
}

/Portal.nodeLinkNotify.*OFF/ {
  if (pppOnMsec != 0) {
    durationMin = ($1 - pppOnMsec)/60000.;
    printLine(sprintf("PPP is OFF (connect time = %.1f min)", durationMin));
  }
  else {
    printLine("PPP is OFF" );
  }
}

/normal connection termination/ {
  printLine($(NF-4) " normal connection termination");
}

/disconnectNode\(\).*Trying to terminate all leases/ {
  printLine("Completed downloads: trying to terminate leases\n");
}

/terminateLease.*terminated lease/ {
  printLine("Terminated lease " $(NF-2) " on " $(NF) "\n");
}


/abrupt connection termination/ {
  printLine($(NF-4) ": Abrupt connection termination\n");

}

/already connected.*Don't launch another worker/ {
  printLine("\"already connected? Don't launch another worker\" - UNUTILIZED CONNECTION!");
}



END {
  print "</body>";
}

function printLine(line) {
  print "<br><font color=\"" _color[_thread] "\">" timestamp() " [" _thread "]: " line "</font>";
  _prevMsec = _elapsedMsec;
}

function timestamp() {

  if (_printSeconds) {
    return sprintf("%.3f (%+.3f)", _elapsedMsec/1000., (_elapsedMsec - _prevMsec)/1000.);
  }
  else {
    return sprintf("%d (%+d)", _elapsedMsec, _elapsedMsec - _prevMsec);
  }


}
