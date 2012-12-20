// PuckIO.cpp : implementation file
//

#include "stdafx.h"
#include "Puck.h"
#include "errors.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

static void trimString(char* buff);

PuckIO::PuckIO(void)
{
    _puckTermStr = "RDY\r";
	_statMsgCallback = NULL;
}

PuckIO::~PuckIO(void)
{

}

int Puck::initPuck(int port)
{
    //if a serial port is set, close that port
    if ( _serialPort.IsOpen() )
        _serialPort.Close();

	if ( port != 0 )
		return _serialPort.Open(port, 9600);
	
	return 0;
}

#define MIN_PUCK_MEM		96
#define READONLY_DATASHEET	0x01
int Puck::memoryTest()
{
	int retVal;
    long memSize = 0;
	int testSize = 2048;
	unsigned int puckType = 0;
	unsigned int memStart = 0;
	char respBuff[80];

	//Query PUCK mem size
	retVal = sendPuckCmd("PUCKSZ\r", respBuff, 3, 1000); 

    if ( retVal )
    {
		statMsg("ERR: Memory test failed on PUCKSZ command\r\n");
		return retVal;
	}

	sscanf(respBuff, "%ld", &memSize);

	if ( memSize < 96 )
    {
		statMsg("ERR: Only %ld memory available, ", memSize);
		statMsg("PUCK requires at least %ld bytes\r\n", MIN_PUCK_MEM);
		return retVal;
	}

	statMsg("STATUS: PUCK memory size is %ld bytes\r\n", memSize);

	//determine if datasheet is readonly
	retVal = sendPuckCmd("PUCKTY\r", respBuff, 3, 1000); 

    if ( retVal )
    {
		statMsg("ERR: Memory test failed on PUCKTY command\r\n");
		return retVal;
	}

	sscanf(respBuff, "%04X", &puckType);

	if ( puckType & READONLY_DATASHEET )
	{
		statMsg("STATUS: found PUCK with read only datasheet\r\n");
//need to determine size from datasheet, don't assume it's 96 bytes		
		memStart = 96;
	}

	//erase PUCK
	statMsg("STATUS: erasing PUCK memory\r\n");
	retVal = sendPuckCmd("PUCKEM\r", respBuff, 3, 10000); 

    if ( retVal )
    {
		statMsg("ERR: Memory test failed on PUCKEM command\r\n");
		return retVal;
	}

	statMsg("STATUS: PUCK memory erased\r\n");

	//write a 2K byte pattern or PUCK size, which ever is smaller
	if ( (memSize - memStart) < 2048 )
		testSize = memSize - memStart;

	retVal = writePuckPattern(testSize);

    if ( retVal )
    {
		statMsg("ERR: Memory test failed while writing ");
		statMsg("bit pattern to PUCK memory\r\n");
		return retVal;
	}

	//powerdown the PUCK

	//powerup the PUCK

	//read back pattern and compare to pattern stored
	retVal = readPuckPattern(testSize);

    if ( retVal )
    {
		statMsg("ERR: Memory test failed while reading ");
		statMsg("bit pattern from PUCK memory\r\n");
		return retVal;
	}
	
	return ERR_SUCCESS;
}

int Puck::instrumentModeTest()
{
    //?????
	
	return ERR_SUCCESS;
}

int Puck::baudRateTest()
{
    //probe for baud rates
	// 300, 1200, 2400, 4800, 9600, 19200, 38400, 57600, 115200

	//switch to all available bauds and perfrom PUCKVR command

	//restore PUCK default baud
	
	return ERR_SUCCESS;
}

int Puck::commandTest()
{
	int retVal;
    char respBuff[80];

    //check PUCKVR and PUCKTY
	retVal = sendPuckCmd("PUCKVR\r", respBuff, 3, 1000); 

    if ( retVal )
    {
		statMsg("ERR: Command test failed on PUCKVR command\r\n");
		return retVal;
	}

    //trim of the CR\LF if it's there
	trimString(respBuff);
	statMsg("STATUS: PUCKVR command returned '%s'\r\n", respBuff);

	retVal = sendPuckCmd("PUCKTY\r", respBuff, 3, 1000);

    if ( retVal )
    {
		statMsg("ERR: Command test failed on PUCKTY command\r\n");
		return retVal;
	}
	
    //trim of the CR\LF if it's there
	trimString(respBuff);
    statMsg("STATUS: PUCKTY command returned '%s'\r\n", respBuff);

    return ERR_SUCCESS;
}

int Puck::dataSheetTest()
{
    //read datasheet and display if it makes sense.
	
	return ERR_SUCCESS;
}

int Puck::writePuckPattern(long len)
{
	return ERR_FAILURE;
}

int Puck::readPuckPattern(long len)
{
	return ERR_FAILURE;
}

int Puck::sendPuckCmd(char* cmd, char* resp, int retries, int timeout)
{
	int bytesRead;

    if ( !_serialPort.IsOpen() )
	{
		statMsg("ERR: failed to send command to the PUCK, ");
		statMsg("no serial port selected.\r\n");
        return ERR_PUCK_PORT_CLOSED;
	}
    
    for (int i = 0; i < retries; ++i)
    {
        //send the command
        _serialPort.Write(cmd, strlen(cmd));
            
        //get the response
        if ( readUntil(resp, 100, timeout, &bytesRead) == ERR_SUCCESS )
			return ERR_SUCCESS;
    }

	statMsg("ERR: exceeded max retries while communicating with PUCK\r\n");
	return ERR_SEND_CMD_RETRIES;
}

int Puck::readUntil(char* resp, int max_chars, long timeout, int* bytes_read)
{
    //total bytesRead less the terminator length
    int bytesRead = 0;
    //elapsed time out the read
    long elapsed = 0;
    // index into terminator
    int termIndex = 0;
    // char var
    char c;
	// length of term string
	int termLen = strlen(_puckTermStr);
	//capture the start time of readUntil
    DWORD t0 = GetTickCount();

    //read until we receive terminator, exceed outbuf, or time out
    for (;;)
    {
        if (_serialPort.Read(&c, 1) == 1) 
        {
            //store the char if you have a valid resp buffer
            if (resp != NULL)
                resp[bytesRead++] = c;
			else
				++bytesRead;
                    
            // check for the PUCK term string
			if (c == _puckTermStr[termIndex] )
            {
                termIndex++;
            }
            else
            {
                termIndex = 0;

                //this may be the start of the terminator as well
                if (c == _puckTermStr[0])
                    termIndex++;
            }
                     
            //if the termIndex is the same size as the 
            //terminator you got it 
            if (termIndex == termLen)
            {
                if ( (resp != NULL) )
                    resp[bytesRead - termLen] = '\0';

				*bytes_read = bytesRead - termLen;

				return ERR_SUCCESS;
            }
                    
            if (bytesRead >= max_chars)
			{
				statMsg("ERR: bytes read from PUCK exceeded ");
				statMsg(" bytes expected\r\n");
                return ERR_READ_OVERRUN;
			}
        }
        else
        {
            //didn't get a byte, take a break
			Sleep(50);
        }

        elapsed = GetTickCount() - t0;

        if (elapsed > timeout)
		{
			statMsg("ERR: timed out waiting for PUCK response\r\n");
            return ERR_READ_TIMEOUT;
		}
    }
}

void Puck::statMsg(const char* format, ...)
{
	va_list args;
	char msgBuff[128];
	
	// prepare the arguments
	va_start(args, format);	
	
	//only attempt to post the msg is there is 
	//a valid puckVerify dialog
	if (_statMsgCallback != NULL)
	{
		//format the message for output
		vsprintf(msgBuff, format, args);
		_statMsgCallback(msgBuff);    
	}
	
	// clean the stack
	va_end(args);
}

void trimString(char* buff)
{
	int i = 0;

	while ( buff[++i] != '\0' )
	{
		if ( buff[i] == '\r' )
		{
			buff[i] = '\0';
			return;
		}
	}
}