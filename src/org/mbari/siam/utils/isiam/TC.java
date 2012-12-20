/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils.isiam;

/**
 * Simple utility to generate colored output with sequences understood by
 * typical terminals. (TC stands for "terminal color.") Control sequences are
 * actually output if the static boolean member useColor is true. This member is
 * initialized to true only if the value of the environment variable "tcolor" is
 * "y" or if the value of the system property "tcolor" is "y".
 * 
 * <p>
 * Note that under java1.3, System.getenv will throw java.lang.Error:
 * "getenv no longer supported", but such a call is again valid in later java
 * releases. In this case, the exception is simply ignored and just the system
 * property "tcolor" is taken into account.
 * 
 * <p>
 * For color sequences, see for example <a
 * href="http://tldp.org/HOWTO/Bash-Prompt-HOWTO/x329.html">here</a>.
 * 
 * @author carueda
 */
public class TC {
	/**
	 * Initialized with true only if environment variable or system property
	 * "tcolor" is defined with value "y". Value can be changed at any time.
	 */
	public static volatile boolean useColor = false;

	static {
		try {
			useColor = "y".equals(System.getenv("tcolor"));
		}
		catch (Throwable ignore) {
			/*
			 * surely we are under an old JVM version -- just ignore and only
			 * use system property.
			 */
		}

		if (!useColor) {
			useColor = "y".equals(System.getProperty("tcolor", "n"));
		}
	}

	private static final String RED = "\u001b[0;31m";
	private static final String GREEN = "\u001b[0;32m";
	private static final String BLUE = "\u001b[0;34m";
	private static final String YELLOW = "\u001b[1;33m";
	private static final String DEFAULT = "\u001b[1;00m";

	public static String red(String s) {
		return useColor ? RED + s + DEFAULT : s;
	}

	public static String green(String s) {
		return useColor ? GREEN + s + DEFAULT : s;
	}

	public static String blue(String s) {
		return useColor ? BLUE + s + DEFAULT : s;
	}

	public static String yellow(String s) {
		return useColor ? YELLOW + s + DEFAULT : s;
	}

	private TC() {
	}
}
