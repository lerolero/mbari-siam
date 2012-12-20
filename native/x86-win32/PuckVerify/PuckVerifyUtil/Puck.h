/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
#ifndef PUCK_H
#define PUCK_H

#include "SerialPort.h"

class Puck
{
public:
    Puck();
    ~Puck();

    int initPuck(int commPort);
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

	//vars and member funcs for PUCK writing
	long _writeAddress;
    int _writeBufferIndex;
    bool _writeEnabled;
#define WRITE_BUFFER_SIZE	32
    char _writeBuffer[WRITE_BUFFER_SIZE];

    int startWriting(long address);
    int endWriting();
    int write(char c);
    int writePacket();

	//vars and member funcs for PUCK reading
    long _readAddress;
    int _readBufferIndex;
    bool _readEnabled;
#define READ_BUFFER_SIZE	256
    char _readBuffer[READ_BUFFER_SIZE];
    
    int startReading(long address);
    int endReading();
    int readAvailable();
    int read(char& c);
    int readPacket();
};


#endif