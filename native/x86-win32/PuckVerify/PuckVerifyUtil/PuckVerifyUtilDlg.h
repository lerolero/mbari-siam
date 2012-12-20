/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
// PuckVerifyUtilDlg.h : header file
//

#if !defined(AFX_PUCKVERIFYUTILDLG_H__EAA8EEC5_26BA_4D27_AA69_C6E196208B78__INCLUDED_)
#define AFX_PUCKVERIFYUTILDLG_H__EAA8EEC5_26BA_4D27_AA69_C6E196208B78__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

/////////////////////////////////////////////////////////////////////////////
// CPuckVerifyUtilDlg dialog

class CPuckVerifyUtilDlg : public CDialog
{
// Construction
public:
	CPuckVerifyUtilDlg(CWnd* pParent = NULL);	// standard constructor

// Dialog Data
	//{{AFX_DATA(CPuckVerifyUtilDlg)
	enum { IDD = IDD_PUCKVERIFYUTIL_DIALOG };
	CButton	m_AbortButton;
	CButton	m_StartButton;
	CEdit	m_TestStatusMsg;
	BOOL	m_MemoryTest;
	BOOL	m_InstModeTest;
	BOOL	m_InstDsTest;
	BOOL	m_CmdTest;
	BOOL	m_BaudTest;
	//}}AFX_DATA

	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(CPuckVerifyUtilDlg)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);	// DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation

protected:
	HICON m_hIcon;

	// Generated message map functions
	//{{AFX_MSG(CPuckVerifyUtilDlg)
	virtual BOOL OnInitDialog();
	afx_msg void OnSysCommand(UINT nID, LPARAM lParam);
	afx_msg void OnPaint();
	afx_msg HCURSOR OnQueryDragIcon();
	afx_msg void OnChangeEditStatus();
	afx_msg void OnButtonStart();
	afx_msg void OnButtonAbort();
	afx_msg void OnFileExit();
	afx_msg void OnSerialCom1();
	afx_msg void OnSerialCom2();
	afx_msg void OnSerialCom3();
	afx_msg void OnSerialCom4();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

	void statMsg(const char* format, ...);
	void writeStatusMsg(char* msg);

private:
    Puck _puck;
	friend void printStatusMsg(char* msg);
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_PUCKVERIFYUTILDLG_H__EAA8EEC5_26BA_4D27_AA69_C6E196208B78__INCLUDED_)
