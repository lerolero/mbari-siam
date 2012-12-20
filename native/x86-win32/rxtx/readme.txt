This document describes steps necessary to build and install a slightly modified version of rxtx-2.1-6 for Win32.

7-15-2003 - Mike Risi, mrisi@mabri.org

* The files

In the CVS siam module under siam\native\x86-win32\rxtx you will find four binary files and this text file.  The files are

rxtx-2.1-6.tar.gz       - the original rxtx sources downloaded from www.rxtx.org
rxtx-2.1-6.win32.tar.gz - part of the original rxtx sources with slight modifications
RXTXcomm.jar            - the rxtx classes necessary to use rxtx on a Win32 machine
rxtxSerial.dll          - a dynamic link library necessary to use rxtx on a Win32 machine
readme.txt              - this file

* Installation instructions

If you just want to install the binaries place rxtxSerial.dll in your JDK bin directory.  For example

C:\jdk1.3.1\bin

and place RXTXcomm.jar in your JDK lib directory.  For example

C:\jdk1.3.1\lib

If you use the CLASSPATH environment variable make sure there are no references to Sun's comm.jar in it and add RXTXcomm.jar to it.  If you specify the CLASSPATH from the command line make sure you include the RXTXcomm.jar and remove any references to Sun's comm.jar.  Also make sure the JDK bin directory is in your PATH.

Once this is done you should be ready to use RXTX.

* Building rxtx for Win32

The modifications to rxtx-2.1-6 were necessary to fix a bug that did not allow the select() system call to function.  Changes were also made to select() to increase the performance of rxtx when sending or receiving large amounts of serial data.  

To build rxtx for Win32 you will need to install MingW32.  You can download MingW32 from http://www.mingw.org/.  After installing the MingW32 tools and libraries untar the rxtx-2.1-6.win32.tar.gz onto your machine.  For Example

tar -zxvf rxtx-2.1-6.win32.tar.gz

Open the Makefile located in the 
 
rxtx-2.1-6.win32\build\Makefile

and modify all the variables between the comments

######################
#  user defined variables
######################

and

######################
#  End of user defined variables
######################

to point to the necessary locations on your machine.  Also make sure the CLASSPATH variable inside the Makefile is set correctly.  Once these changes have been made you should be able to use make from the 

rxtx-2.1-6.win32\build\

directory to build rxtx for Win32.  It is necessary to perform a 'make clean' or remove the appropriate .o file if you modify any of the files in rxtx-2.1-6.win32\src as the dependencies for the Makefile are not working correctly.