/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
// PuckVerifyUtil.h : main header file for the PUCKVERIFYUTIL application
//

#if !defined(AFX_PUCKVERIFYUTIL_H__E36D70DD_A99F_41B4_B33F_2D97314BAB28__INCLUDED_)
#define AFX_PUCKVERIFYUTIL_H__E36D70DD_A99F_41B4_B33F_2D97314BAB28__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#ifndef __AFXWIN_H__
	#error include 'stdafx.h' before including this file for PCH
#endif

#include "resource.h"		// main symbols

/////////////////////////////////////////////////////////////////////////////
// CPuckVerifyUtilApp:
// See PuckVerifyUtil.cpp for the implementation of this class
//

class CPuckVerifyUtilApp : public CWinApp
{
public:
	CPuckVerifyUtilApp();

// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(CPuckVerifyUtilApp)
	public:
	virtual BOOL InitInstance();
	//}}AFX_VIRTUAL

// Implementation

	//{{AFX_MSG(CPuckVerifyUtilApp)
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()
};


/////////////////////////////////////////////////////////////////////////////

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_PUCKVERIFYUTIL_H__E36D70DD_A99F_41B4_B33F_2D97314BAB28__INCLUDED_)
