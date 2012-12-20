// SerialPort.cpp : implementation file
//

#include "stdafx.h"
#include "SerialPort.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif


SerialPort::SerialPort(void)
{
	PortOpen = false;
	PortNum = -1;
    osWrite.Internal = 0;
    osWrite.InternalHigh = 0;
    osWrite.Offset = 0;
    osWrite.OffsetHigh = 0;
    osWrite.hEvent = CreateEvent(NULL, TRUE, FALSE, NULL);
    if ( osWrite.hEvent == NULL )
        MessageBox(NULL, "CreateEvent failed for osWrite", "SerialPort::SerialPort", MB_OK);

}

SerialPort::~SerialPort(void)
{
    CloseHandle(osWrite.hEvent);
    if ( PortOpen )
        CloseHandle(h_PortHandle);
}


int SerialPort::Open(int port, unsigned long baud_rate)
{
    char* comm_str_ptr;
    char temp_buff[80];
    char comm_1[] = "COM1";
    char comm_2[] = "COM2";
    char comm_3[] = "COM3";
    char comm_4[] = "COM4";

    DCB port_DCB;
    COMMTIMEOUTS comm_timeouts;

    if ( PortOpen )
    {		
        sprintf(temp_buff,"Port %d already open in this instance", PortNum);
        MessageBox(NULL, temp_buff, "SerialPort::Open", MB_OK);
        return -1; 
    }

	PortNum = port;

    switch ( PortNum )
    {
        case 1: comm_str_ptr = comm_1; break;
        case 2: comm_str_ptr = comm_2; break;
        case 3: comm_str_ptr = comm_3; break;
        case 4: comm_str_ptr = comm_4; break;
        default: goto ErrorExit;
    }

    switch ( baud_rate )
    {
        case CBR_110: break;
        case CBR_300: break;
        case CBR_600: break;
        case CBR_1200: break;
        case CBR_2400: break;
        case CBR_4800: break;
        case CBR_9600: break;
        case CBR_14400: break;
        case CBR_19200: break;
        case CBR_38400: break;
        case CBR_56000: break;
        case CBR_57600: break;
        case CBR_115200: break;
        case CBR_128000: break;
        case CBR_256000: break;
        default:         
            MessageBox(NULL, "Invalid baud rate specified", "SerialPort::Open", MB_OK);
            return -1;

    }

    h_PortHandle = CreateFile(  comm_str_ptr,                   // Pointer to the name of the port
                                GENERIC_READ | GENERIC_WRITE,   // Access (read-write) mode
                                0,                              // Share mode
                                NULL,                           // Pointer to the security attribute
                                OPEN_EXISTING,                  // How to open the serial port
                                FILE_FLAG_OVERLAPPED,           // Port attributes
                                NULL);                          // Handle to port with attribute to copy
    
    //if the port HANDLE is invalid, then we didn't open it
    if ( h_PortHandle == INVALID_HANDLE_VALUE )
    {
        sprintf(temp_buff,"Port %d failed to open.  May already be in use.", PortNum);
        MessageBox(NULL, temp_buff, "SerialPort::Open", MB_OK);
        return -1;
    }

    PortOpen = true;

    //Clear out DCB
    FillMemory(&port_DCB, sizeof(DCB), 0);
    //Set the DCBlength member
    port_DCB.DCBlength = sizeof(DCB);
    // Get the default port setting information.
    if ( !GetCommState(h_PortHandle, &port_DCB) )
        goto ErrorExit;

    //setup dcb for "baud_rate" and 8N1
    port_DCB.BaudRate = (DWORD)baud_rate;       // Current baud
    port_DCB.fBinary = TRUE;                    // Binary mode; no EOF check
    port_DCB.fParity = TRUE;                    // Enable parity checking
    port_DCB.fOutxCtsFlow = FALSE;              // No CTS output flow control
    port_DCB.fOutxDsrFlow = FALSE;              // No DSR output flow control
    port_DCB.fDtrControl = DTR_CONTROL_ENABLE;  // DTR flow control type
    port_DCB.fDsrSensitivity = FALSE;           // DSR sensitivity
    port_DCB.fTXContinueOnXoff = TRUE;          // XOFF continues Tx
    port_DCB.fOutX = FALSE;                     // No XON/XOFF out flow control
    port_DCB.fInX = FALSE;                      // No XON/XOFF in flow control
    port_DCB.fErrorChar = FALSE;                // Disable error replacement
    port_DCB.fNull = FALSE;                     // Disable null stripping
    port_DCB.fRtsControl = RTS_CONTROL_ENABLE;  // RTS flow control
    port_DCB.fAbortOnError = FALSE;             // Do not abort reads/writes on error
    port_DCB.ByteSize = 8;                      // Number of bits/byte, 4-8
    port_DCB.Parity = NOPARITY;                 // 0-4=no,odd,even,mark,space
    port_DCB.StopBits = ONESTOPBIT;             // 0,1,2 = 1, 1.5, 2

    //setup comm port
    if ( !SetCommState(h_PortHandle, &port_DCB) )
        goto ErrorExit;

    // Retrieve the time-out parameters for all read and write operations on the port.
    if ( !GetCommTimeouts(h_PortHandle, &comm_timeouts) )
        goto ErrorExit;

    // Change the COMMTIMEOUTS structure settings.
    comm_timeouts.ReadIntervalTimeout = MAXDWORD;
    comm_timeouts.ReadTotalTimeoutMultiplier = 0;
    comm_timeouts.ReadTotalTimeoutConstant = 0;
    comm_timeouts.WriteTotalTimeoutMultiplier = 10;
    comm_timeouts.WriteTotalTimeoutConstant = 3000;

    // Set the time-out parameters for all read and write operations on the port.
    if ( !SetCommTimeouts(h_PortHandle, &comm_timeouts) )
        goto ErrorExit;

    //Direct the port to perform extended functions SETDTR and SETRTS
    //SETDTR: Sends the DTR (data-terminal-ready) signal.
    //SETRTS: Sends the RTS (request-to-send) signal.
    if ( !EscapeCommFunction(h_PortHandle, SETDTR) )
        goto ErrorExit;

    if ( !EscapeCommFunction(h_PortHandle, SETRTS) )
        goto ErrorExit;

    //Set the enents to be monitiored by the comm port
    //EV_RXCHAR : A character was received and placed in the input buffer.
    //EV_CTS    : The CTS (clear-to-send) signal changed state.
    //EV_DSR    : The DSR (data-set-ready) signal changed state.
    //EV_RLSD   : The RLSD (receive-line-signal-detect) signal changed state.
    //EV_RING   : A ring indicator was detected.
    
    if ( !SetCommMask(h_PortHandle, EV_RXCHAR | EV_CTS | EV_DSR | EV_RLSD | EV_RING))
        goto ErrorExit;
    
    return 0;

ErrorExit:

   //try to close the port and return -1
   CloseHandle(h_PortHandle);
   sprintf(temp_buff,"Configuration of port %d failed.", PortNum);
   MessageBox(NULL, temp_buff, "SerialPort::Open", MB_OK);
   return -1;
}

int SerialPort::Close(void)
{
	int ret = -1;

    if ( PortOpen )
    {
        if ( CloseHandle(h_PortHandle) )
		{
			ret = 0;
			PortOpen = false;
		}
    }

    return ret;
}

int SerialPort::Read(char* bytes, int length)
{
    int ret;
    DWORD dwRead;
    OVERLAPPED osReader = {0};

    if ( !PortOpen )
        return -1;

    // Create the overlapped event.
    osReader.hEvent = CreateEvent(NULL, TRUE, FALSE, NULL);

    if ( osReader.hEvent == NULL )
        return -1;

    // Issue read operation.
    if ( !ReadFile(h_PortHandle, (LPVOID)bytes, (DWORD)length, &dwRead, &osReader) )
    {
        // ReadFile failed, but it isn't delayed. Report error and abort.
        if ( GetLastError() != ERROR_IO_PENDING )
        {
            ret = -1;
        }
        else
        {
            // Read is pending.
            if ( !GetOverlappedResult(h_PortHandle, &osReader, &dwRead, TRUE) )
                ret = -1;
            else
            {
                (char)bytes[dwRead] = '\0';
                ret = dwRead;
            }
        }
    }
    else
    {
        bytes[dwRead] = '\0';
        ret = dwRead;
    }
    //close the envent handle
    CloseHandle(osReader.hEvent);

    return ret;
}

int SerialPort::Write(char* bytes, int length)
{
    int ret;
    DWORD dwWritten = 0;
	DWORD last_error;

    if ( !PortOpen )
        return -1;

    if ( !WriteFile(h_PortHandle, (LPVOID)bytes, (DWORD)length, &dwWritten, &osWrite) )
    {
        // WriteFile failed, but it isn't delayed. Report error and abort.
		last_error = GetLastError();
        if (last_error != ERROR_IO_PENDING)
        {
            ret = -1;
        }
        
		//may want to use this code for a waiting write type function.
		//else
        //{
        //    // Write is pending.
        //    if ( !GetOverlappedResult(h_PortHandle, &osWrite, &dwWritten, TRUE) )
        //        ret = -1;
        //    else
        //        ret = dwWritten;
        //}
    }
    else
        ret = dwWritten;

    return ret;
}

int SerialPort::CharsPendingWrite(void)
{
    DWORD Errors;
    COMSTAT Stat;
    ClearCommError(h_PortHandle, &Errors, &Stat);
    
    return (int)Stat.cbOutQue;
}

bool SerialPort::PurgeBuffers(void)
{
	BOOL ret;
	ret = PurgeComm(h_PortHandle, PURGE_TXABORT | 
								  PURGE_RXABORT | 
								  PURGE_TXCLEAR | 
								  PURGE_RXCLEAR);

	if ( ret )
		return true;
	else 
		return false;
}

