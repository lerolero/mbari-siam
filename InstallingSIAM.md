After you've retrieved SIAM, you can find very extensive documentation in the docs/siam-site directory. Following is a brief description to get you started in downloading, installing, and running SIAM on a SIAM "instrument host" computer (also known as a "SIAM node").

## Prerequisites ##

Java 1.5 or higher

SIAM has been tested with several Linux versions, Microsoft Windows running Cygwin, and MacOS 10.x.

SIAM deals primarily with instruments having an RS-232 interface. Hence your SIAM instrument 'host' computer should have one or more serial ports. Most modern laptops don't have 'native' serial ports, but you can use one or more USB-to-serial adapters, such as those made by Keyspan.

## Download and installation procedure ##
The following notes presume an instrument host computer running Linux, Unix, or Cygwin.

From your instrument host, clone the latest SIAM distribution with Mercurial, then build with 'ant':

```
cd /home/me/somedirectory
hg clone https://code.google.com/p/mbari-siam/
ant build
```


You should now see a subdirectory named 'siam' in your working directory. The siam directory contains a file called siam-env.sh that sets necessary variables in your environment. First edit siam-env.sh and set the value of SIAM\_HOME, e.g.

```
#!/bin/bash
# 
# Set SIAM_HOME to your SIAM "home" directory
export SIAM_HOME=/home/me/somedirectory/siam

```

Then 'source' siam-env.sh to setup your environment:

```
% . siam/siam-env.sh
```

More detail can be found by opening $SIAM\_HOME/docs/siam-site/index.html with a web browser.
To configure SIAM with instruments, please refer to the "SIAM Configuration" section in that document.