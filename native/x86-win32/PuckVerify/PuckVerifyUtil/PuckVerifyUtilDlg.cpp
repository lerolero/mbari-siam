// PuckVerifyUtilDlg.cpp : implementation file
//

#include "stdafx.h"
#include "PuckVerifyUtil.h"
#include "PuckVerifyUtilDlg.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
// CAboutDlg dialog used for App About

class CAboutDlg : public CDialog
{
public:
	CAboutDlg();

// Dialog Data
	//{{AFX_DATA(CAboutDlg)
	enum { IDD = IDD_ABOUTBOX };
	//}}AFX_DATA

	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(CAboutDlg)
	//}}AFX_VIRTUAL

// Implementation
protected:
	//{{AFX_MSG(CAboutDlg)
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()
};

CAboutDlg::CAboutDlg() : CDialog(CAboutDlg::IDD)
{
	//{{AFX_DATA_INIT(CAboutDlg)
	//}}AFX_DATA_INIT
}

BEGIN_MESSAGE_MAP(CAboutDlg, CDialog)
	//{{AFX_MSG_MAP(CAboutDlg)
		// No message handlers
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// CPuckVerifyUtilDlg dialog

/////////////////////////////////////////////////////////////////////////////
//The following cod is to work around the fact that I 
//could not register the CPuckVerifyUtilDlg::showStatusMsg 
//member function directly with other objects ???
static CPuckVerifyUtilDlg* puckVerifyDlg = NULL;

static void printStatusMsg(char* msg)
{
	if ( puckVerifyDlg != NULL) 
		puckVerifyDlg->writeStatusMsg(msg);
}

/////////////////////////////////////////////////////////////////////////////

CPuckVerifyUtilDlg::CPuckVerifyUtilDlg(CWnd* pParent /*=NULL*/)
	: CDialog(CPuckVerifyUtilDlg::IDD, pParent)
{
	//{{AFX_DATA_INIT(CPuckVerifyUtilDlg)
	m_MemoryTest = FALSE;
	m_InstModeTest = FALSE;
	m_InstDsTest = FALSE;
	m_CmdTest = FALSE;
	m_BaudTest = FALSE;
	//}}AFX_DATA_INIT
	// Note that LoadIcon does not require a subsequent DestroyIcon in Win32
	m_hIcon = AfxGetApp()->LoadIcon(IDR_MAINFRAME);
	//assign the file pointer (see above work around)
	puckVerifyDlg = this;
}

void CPuckVerifyUtilDlg::DoDataExchange(CDataExchange* pDX)
{
	CDialog::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(CPuckVerifyUtilDlg)
	DDX_Control(pDX, IDC_BUTTON_ABORT, m_AbortButton);
	DDX_Control(pDX, IDC_BUTTON_START, m_StartButton);
	DDX_Control(pDX, IDC_EDIT_STATUS, m_TestStatusMsg);
	DDX_Check(pDX, IDC_CHECK_MEM, m_MemoryTest);
	DDX_Check(pDX, IDC_CHECK_INST, m_InstModeTest);
	DDX_Check(pDX, IDC_CHECK_DSHEET, m_InstDsTest);
	DDX_Check(pDX, IDC_CHECK_CMD, m_CmdTest);
	DDX_Check(pDX, IDC_CHECK_BAUD, m_BaudTest);
	//}}AFX_DATA_MAP
}

BEGIN_MESSAGE_MAP(CPuckVerifyUtilDlg, CDialog)
	//{{AFX_MSG_MAP(CPuckVerifyUtilDlg)
	ON_WM_SYSCOMMAND()
	ON_WM_PAINT()
	ON_WM_QUERYDRAGICON()
	ON_EN_CHANGE(IDC_EDIT_STATUS, OnChangeEditStatus)
	ON_BN_CLICKED(IDC_BUTTON_START, OnButtonStart)
	ON_BN_CLICKED(IDC_BUTTON_ABORT, OnButtonAbort)
	ON_COMMAND(ID_FILE_EXIT, OnFileExit)
	ON_COMMAND(ID_SERIAL_COM1, OnSerialCom1)
	ON_COMMAND(ID_SERIAL_COM2, OnSerialCom2)
	ON_COMMAND(ID_SERIAL_COM3, OnSerialCom3)
	ON_COMMAND(ID_SERIAL_COM4, OnSerialCom4)
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// CPuckVerifyUtilDlg message handlers

BOOL CPuckVerifyUtilDlg::OnInitDialog()
{
	CDialog::OnInitDialog();
	// Add "About..." menu item to system menu.

	// IDM_ABOUTBOX must be in the system command range.
	ASSERT((IDM_ABOUTBOX & 0xFFF0) == IDM_ABOUTBOX);
	ASSERT(IDM_ABOUTBOX < 0xF000);

	CMenu* pSysMenu = GetSystemMenu(FALSE);
	if (pSysMenu != NULL)
	{
		CString strAboutMenu;
		strAboutMenu.LoadString(IDS_ABOUTBOX);
		if (!strAboutMenu.IsEmpty())
		{
			pSysMenu->AppendMenu(MF_SEPARATOR);
			pSysMenu->AppendMenu(MF_STRING, IDM_ABOUTBOX, strAboutMenu);
		}
	}
	// Set the icon for this dialog.  The framework does this automatically
	//  when the application's main window is not a dialog
	SetIcon(m_hIcon, TRUE);			// Set big icon
	SetIcon(m_hIcon, FALSE);		// Set small icon
	
	//register status msg function with the Puck object
	_puck.regStatMsgCallback(printStatusMsg);
	
	return TRUE;  // return TRUE  unless you set the focus to a control
}

void CPuckVerifyUtilDlg::OnSysCommand(UINT nID, LPARAM lParam)
{
	if ((nID & 0xFFF0) == IDM_ABOUTBOX)
	{
		CAboutDlg dlgAbout;
		dlgAbout.DoModal();
	}
	else
	{
		CDialog::OnSysCommand(nID, lParam);
	}
}

// If you add a minimize button to your dialog, you will need the code below
//  to draw the icon.  For MFC applications using the document/view model,
//  this is automatically done for you by the framework.

void CPuckVerifyUtilDlg::OnPaint() 
{
	if (IsIconic())
	{
		CPaintDC dc(this); // device context for painting

		SendMessage(WM_ICONERASEBKGND, (WPARAM) dc.GetSafeHdc(), 0);

		// Center icon in client rectangle
		int cxIcon = GetSystemMetrics(SM_CXICON);
		int cyIcon = GetSystemMetrics(SM_CYICON);
		CRect rect;
		GetClientRect(&rect);
		int x = (rect.Width() - cxIcon + 1) / 2;
		int y = (rect.Height() - cyIcon + 1) / 2;

		// Draw the icon
		dc.DrawIcon(x, y, m_hIcon);
	}
	else
	{
		CDialog::OnPaint();
	}
}

// The system calls this to obtain the cursor to display while the user drags
//  the minimized window.
HCURSOR CPuckVerifyUtilDlg::OnQueryDragIcon()
{
	return (HCURSOR) m_hIcon;
}

void CPuckVerifyUtilDlg::OnChangeEditStatus() 
{
	// TODO: If this is a RICHEDIT control, the control will not
	// send this notification unless you override the CDialog::OnInitDialog()
	// function and call CRichEditCtrl().SetEventMask()
	// with the ENM_CHANGE flag ORed into the mask.
	
	// TODO: Add your control notification handler code here

	//MessageBox("test");
	
}

void CPuckVerifyUtilDlg::OnButtonStart() 
{
	CMenu* pMenu = GetMenu();
	int commPort = 0;

	
	//if the button is disabled, bail out as things are going on
	if ( !m_StartButton.IsWindowEnabled() )
		return;

//Call Puck::StartTests(MEM_TEST | INST_MODE_TEST | etc)
// you can check

	//update the data before accessing the checkbox variables
	UpdateData(TRUE);

	m_StartButton.EnableWindow(FALSE);


	if ( m_MemoryTest )
    {
	    statMsg("STATUS: performing Memory Test\r\n");
        _puck.memoryTest();
    }

	if ( m_InstModeTest )
    {
	    statMsg("STATUS: performing Instrument Mode Test\r\n");
        _puck.instrumentModeTest();
    }

	if ( m_CmdTest )
    {
	    statMsg("STATUS: performing Command Test\r\n");
        _puck.commandTest();
    }

	if ( m_BaudTest )
    {
	    statMsg("STATUS: performing Baud Rate Test\r\n");
        _puck.baudRateTest();
    }

    if ( m_InstDsTest )
    {
	    statMsg("STATUS: performing Datasheet Test\r\n");
        _puck.dataSheetTest();
    }

	m_StartButton.EnableWindow(TRUE);

}

void CPuckVerifyUtilDlg::statMsg(const char* format, ...)
{
	va_list args;
	char msgBuff[128];
	
	// prepare the arguments
	va_start(args, format);	
	
	//format the message for output
	vsprintf(msgBuff, format, args);
	writeStatusMsg(msgBuff);    

	// clean the stack
	va_end(args);
}

void CPuckVerifyUtilDlg::writeStatusMsg(char* msg) 
{
    int lastChar;
    int startChar;
    int endChar;

    //as the message boxs fills remove the top lines that have
    //already scrolled out of view
    if (m_TestStatusMsg.GetLineCount() > 30) 
    {
        //find the last hidden char
        lastChar = m_TestStatusMsg.LineIndex(m_TestStatusMsg.GetFirstVisibleLine() - 1);
        m_TestStatusMsg.SetSel(0, lastChar);
        m_TestStatusMsg.ReplaceSel("");
        m_TestStatusMsg.SetSel(0, -1);
        m_TestStatusMsg.GetSel(startChar, endChar);
        m_TestStatusMsg.SetSel((endChar - 1), endChar);
        m_TestStatusMsg.ReplaceSel("\r\n");
    }

    m_TestStatusMsg.ReplaceSel(msg);
}


void CPuckVerifyUtilDlg::OnButtonAbort() 
{
	// TODO: Add your command handler code here
}

void CPuckVerifyUtilDlg::OnFileExit() 
{
	// TODO: Add your command handler code here
}

void CPuckVerifyUtilDlg::OnSerialCom1() 
{
	CMenu* pMenu = GetMenu();
	UINT menuState;
    int errCode;

	menuState = pMenu->GetMenuState(ID_SERIAL_COM1, MF_BYCOMMAND);

	if ( menuState & MF_CHECKED )
	{
        pMenu->CheckMenuItem(ID_SERIAL_COM1, MF_UNCHECKED);
		pMenu->EnableMenuItem(ID_SERIAL_COM2, MF_ENABLED | MF_BYCOMMAND);
		pMenu->EnableMenuItem(ID_SERIAL_COM3, MF_ENABLED | MF_BYCOMMAND);
		pMenu->EnableMenuItem(ID_SERIAL_COM4, MF_ENABLED | MF_BYCOMMAND);

		//initializing a PUCK obj with port zero closes any serial ports if open
		_puck.initPuck(0);
    }
	else
	{
        errCode = _puck.initPuck(1);
    
        if ( errCode )
        {
            statMsg("ERR: Failed to open COM 1 to PUCK\r\n");
            return;
        }
        else
        {
            statMsg("STATUS: Successfully opened COM 1 to PUCK\r\n");
        }

		pMenu->CheckMenuItem(ID_SERIAL_COM1, MF_CHECKED);
		pMenu->EnableMenuItem(ID_SERIAL_COM2, MF_GRAYED | MF_BYCOMMAND);
		pMenu->EnableMenuItem(ID_SERIAL_COM3, MF_GRAYED | MF_BYCOMMAND);
		pMenu->EnableMenuItem(ID_SERIAL_COM4, MF_GRAYED | MF_BYCOMMAND);
	}
}

void CPuckVerifyUtilDlg::OnSerialCom2() 
{
	CMenu* pMenu = GetMenu();
	UINT menuState;
    int errCode;

	menuState = pMenu->GetMenuState(ID_SERIAL_COM2, MF_BYCOMMAND);

	if ( menuState & MF_CHECKED )
	{
        pMenu->EnableMenuItem(ID_SERIAL_COM1, MF_ENABLED | MF_BYCOMMAND);
		pMenu->CheckMenuItem(ID_SERIAL_COM2, MF_UNCHECKED);
		pMenu->EnableMenuItem(ID_SERIAL_COM3, MF_ENABLED | MF_BYCOMMAND);
		pMenu->EnableMenuItem(ID_SERIAL_COM4, MF_ENABLED | MF_BYCOMMAND);

		//initializing a PUCK obj with port zero closes any serial ports if open
		_puck.initPuck(0);
	}
	else
	{
        errCode = _puck.initPuck(2);
    
        if ( errCode )
        {
            statMsg("ERR: Failed to open COM 2 to PUCK\r\n");
            return;
        }
        else
        {
            statMsg("STATUS: Successfully opened COM 2 to PUCK\r\n");
        }

		pMenu->EnableMenuItem(ID_SERIAL_COM1, MF_GRAYED | MF_BYCOMMAND);
		pMenu->CheckMenuItem(ID_SERIAL_COM2, MF_CHECKED);
		pMenu->EnableMenuItem(ID_SERIAL_COM3, MF_GRAYED | MF_BYCOMMAND);
		pMenu->EnableMenuItem(ID_SERIAL_COM4, MF_GRAYED | MF_BYCOMMAND);
	}	

}

void CPuckVerifyUtilDlg::OnSerialCom3() 
{
	CMenu* pMenu = GetMenu();
	UINT menuState;
    int errCode;

	menuState = pMenu->GetMenuState(ID_SERIAL_COM3, MF_BYCOMMAND);

	if ( menuState & MF_CHECKED )
	{
        pMenu->EnableMenuItem(ID_SERIAL_COM1, MF_ENABLED | MF_BYCOMMAND);
		pMenu->EnableMenuItem(ID_SERIAL_COM2, MF_ENABLED | MF_BYCOMMAND);
		pMenu->CheckMenuItem(ID_SERIAL_COM3, MF_UNCHECKED);
		pMenu->EnableMenuItem(ID_SERIAL_COM4, MF_ENABLED | MF_BYCOMMAND);

		//initializing a PUCK obj with port zero closes any serial ports if open
		_puck.initPuck(0);
	}
	else
	{
        errCode = _puck.initPuck(3);
    
        if ( errCode )
        {
            statMsg("ERR: Failed to open COM 3 to PUCK\r\n");
            return;
        }
        else
        {
            statMsg("STATUS: Successfully opened COM 3 to PUCK\r\n");
        }

		pMenu->EnableMenuItem(ID_SERIAL_COM1, MF_GRAYED | MF_BYCOMMAND);
		pMenu->EnableMenuItem(ID_SERIAL_COM2, MF_GRAYED | MF_BYCOMMAND);
		pMenu->CheckMenuItem(ID_SERIAL_COM3, MF_CHECKED);
		pMenu->EnableMenuItem(ID_SERIAL_COM4, MF_GRAYED | MF_BYCOMMAND);
	}	
}

void CPuckVerifyUtilDlg::OnSerialCom4() 
{
	CMenu* pMenu = GetMenu();
	UINT menuState;
    int errCode;

	menuState = pMenu->GetMenuState(ID_SERIAL_COM4, MF_BYCOMMAND);

	if ( menuState & MF_CHECKED )
	{
        pMenu->EnableMenuItem(ID_SERIAL_COM1, MF_ENABLED | MF_BYCOMMAND);
		pMenu->EnableMenuItem(ID_SERIAL_COM2, MF_ENABLED | MF_BYCOMMAND);
		pMenu->EnableMenuItem(ID_SERIAL_COM3, MF_ENABLED | MF_BYCOMMAND);
		pMenu->CheckMenuItem(ID_SERIAL_COM4, MF_UNCHECKED);

		//initializing a PUCK obj with port zero closes any serial ports if open
		_puck.initPuck(0);
	}
	else
	{
        errCode = _puck.initPuck(4);
    
        if ( errCode )
        {
            statMsg("ERR: Failed to open COM 4 to PUCK\r\n");
            return;
        }
        else
        {
            statMsg("STATUS: Successfully opened COM 4 to PUCK\r\n");
        }

		pMenu->EnableMenuItem(ID_SERIAL_COM1, MF_GRAYED | MF_BYCOMMAND);
		pMenu->EnableMenuItem(ID_SERIAL_COM2, MF_GRAYED | MF_BYCOMMAND);
		pMenu->EnableMenuItem(ID_SERIAL_COM3, MF_GRAYED | MF_BYCOMMAND);
		pMenu->CheckMenuItem(ID_SERIAL_COM4, MF_CHECKED);
	}	
}
