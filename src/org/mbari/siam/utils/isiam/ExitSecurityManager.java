/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils.isiam;

import java.security.Permission;

/**
 * A security manager that intercepts calls to System.exit to throw an
 * ExitException, which is then caught and handled by iSiam. This helps prevent
 * the tool from exiting when a utility makes such a call.
 * 
 * @author carueda
 */
class ExitSecurityManager extends SecurityManager {
	private static final long serialVersionUID = 1L;

	private static SecurityManager _saveSecurityManager;
	private static boolean _securityManagerSaved = false;
	private static boolean _securityManagerSet = false;

	/**
	 * Sets an instance of this class as the JVM security manager, to prevent
	 * System.exit calls from actually exiting the application.
	 * 
	 * @return null if everything went well; otherwise a message that can be
	 *         logged.
	 */
	static String setUp() {
		StringBuffer sb = new StringBuffer();
		try {
			_saveSecurityManager = System.getSecurityManager();
			_securityManagerSaved = true;
		}
		catch (Throwable e) {
			sb.append("WARNING: cannot get security manager: " + e.getMessage());
		}

		try {
			SecurityManager sm = new ExitSecurityManager();
			System.setSecurityManager(sm);
			_securityManagerSet = true;
		}
		catch (Throwable e) {
			sb.append("WARNING: cannot set security manager to intercept exit calls: "
					+ e.getMessage());
		}
		if (sb.length() > 0) {
			return sb.toString();
		}
		else {
			return null; // OK
		}
	}

	/**
	 * Removes the installed security manager and restores the previous one, if
	 * any.
	 */
	static void remove() {
		if (_securityManagerSaved && _securityManagerSet) {
			System.setSecurityManager(_saveSecurityManager);
		}
		_securityManagerSaved = _securityManagerSet = false;
	}

	/**
	 * Does nothing, ie., allows the given permission. We are only interested in
	 * {@link #checkExit(int)}.
	 */
	// @Override
	public void checkPermission(Permission perm) {
		// simply allow by doing nothing.
	}

	/**
	 * Does nothing, ie., allows the given permission. We are only interested in
	 * {@link #checkExit(int)}.
	 */
	// @Override
	public void checkPermission(Permission perm, Object context) {
		// simply allow by doing nothing.
	}

	/**
	 * System.exit(status) has been called so throw a corresponding
	 * {@link ExitException}.
	 */
	// @Override
	public void checkExit(int status) {
		super.checkExit(status);
		throw new ExitException(status);
	}
}

/**
 * The exception that our security manager throws when intercepting a
 * System.exit call.
 * 
 * @author carueda
 */
class ExitException extends SecurityException {
	private static final long serialVersionUID = 1L;

	/**
	 * The status passed to System.exit for information purposes.
	 */
	public final int status;

	/**
	 * Creates an instance of this exception.
	 * 
	 * @param status
	 *            The status passed to System.exit for information purposes.
	 */
	public ExitException(int status) {
		this.status = status;
	}
}
