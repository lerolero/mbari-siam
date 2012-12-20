/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
#ifndef PUCKIO_H
#define PUCKIO_H

#include "SerialPort.h"

class PuckIO
{
public:
    PuckIO();
    ~PuckIO();

    int initPuckIO(SerialPort );
    int memoryTest();
    int instrumentModeTest();
    int baudRateTest();
    int commandTest();
    int dataSheetTest();
	void regStatMsgCallback(void (*func)(char *)) { _statMsgCallback = func; }

private:

	char* _puckTermStr;
    SerialPort _serialPort;
	void (*_statMsgCallback)(char*);

	int writePuckPattern(long len);
	int readPuckPattern(long len);

    int sendPuckCmd(char* cmd, char* resp, int retries, int timeout);
    int readUntil(char* resp, int max_chars, long timeout, int* bytes_read);
	void statMsg(const char* format, ...);
};


#endif