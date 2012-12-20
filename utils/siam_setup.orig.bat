# Copyright 2013 MBARI, all rights reserved. 
# For license and copyright details, see COPYRIGHT.TXT in the SIAM project
# home directory.

@echo off
REM siam_setup.bat

SET SIAM_HOME=U:\foce\siam
SET JAVA_DEV_ROOT=%SIAM_HOME%
SET SIAM_LIB=%SIAM_HOME%\jars
SET SIAM_JARS="%SIAM_LIB%\ajile.jar";"%SIAM_LIB%\bcel-5.1.jar";"%SIAM_LIB%\bsh-1.2b3.jar";"%SIAM_LIB%\comm.jar";"%SIAM_LIB%\commons-logging.jar";"%SIAM_LIB%\ftp.jar";"%SIAM_LIB%\gnu-regexp-1.1.4.jar";"%SIAM_LIB%\gnu_getopt.jar";"%SIAM_LIB%\IBM_ivj_vc_utilitybeans.jar";"%SIAM_LIB%\IBM_jvm_io.jar";"%SIAM_LIB%\javax.jar";"%SIAM_LIB%\javax.servlet.jar";"%SIAM_LIB%\jcommon-1.0.0-pre2.jar";"%SIAM_LIB%\jcommon.kh.jar";"%SIAM_LIB%\jddac-common.jar";"%SIAM_LIB%\jddac-probe-j2me.jar";"%SIAM_LIB%\jfreechart-1.0.0-pre2.jar";"%SIAM_LIB%\jfreechart.jar";"%SIAM_LIB%\jini-core.jar";"%SIAM_LIB%\jini-ext.jar";"%SIAM_LIB%\jrdebug.jar";"%SIAM_LIB%\jrendezvous.jar";"%SIAM_LIB%\junit.jar";"%SIAM_LIB%\kxml2-2.2.2.jar";"%SIAM_LIB%\log4j-1.2.13.jar";"%SIAM_LIB%\Makefile.jfreechart.jar";"%SIAM_LIB%\nanoxml-2.0.jar";"%SIAM_LIB%\org.mortbay.jetty-jdk1.2.jar";"%SIAM_LIB%\primetime.jar";"%SIAM_LIB%\RXTXcomm.jar";"%SIAM_LIB%\serviceUI.jar";"%SIAM_LIB%\ssds-client-pub-bob.jar";"%SIAM_LIB%\sun-util.jar";"%SIAM_LIB%\XModem.jar"
SET SIAM_CLASSPATH="%SIAM_HOME%\dist\siam.jar";%SIAM_JARS%
SET SIAM_NODE_LOG=%SIAM_HOME%\logs\siam.nodelog
