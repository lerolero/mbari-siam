/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
#ifndef SERIALPORT_H
#define SERIALPORT_H

#include "windows.h"

class SerialPort
{
public:
    SerialPort();
    ~SerialPort();

    int Open(int port, unsigned long baud_rate);
    int Close();
    int Read(char* bytes, int length);
    int Write(char* bytes, int length);

    bool IsOpen(void) { return PortOpen; }
    int GetPortNum(void) { return PortNum; }
    int CharsPendingWrite(void);

	bool PurgeBuffers(void);

private:

    HANDLE h_PortHandle;
    OVERLAPPED osWrite;
    bool PortOpen;
    int PortNum;

};


#endif