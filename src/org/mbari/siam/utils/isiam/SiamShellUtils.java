/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils.isiam;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Some supporting utilities, especially related with java1.3 friendly functions
 * that actually have available support in java1.4 or newer.
 * 
 * @author carueda
 */
public class SiamShellUtils {

	static String array2string(String[] args) {
		StringBuffer sb = new StringBuffer("[");
		String comma = "";
		for (int i = 0; i < args.length; i++) {
			sb.append(comma + args[i]);
			comma = ", ";
		}
		return sb.toString() + "]";
	}

	/**
	 * Name without the package.
	 */
	static String getSimpleName(String className) {
		int idx = className.lastIndexOf('.');
		if (idx >= 0) {
			className = className.substring(idx + 1);
		}
		return className;
	}

	static String[] tokenize(String line) {
		// java >= 1.4: return line.split("\\s+");

		List toks = new ArrayList();
		StringTokenizer st = new StringTokenizer(line);
		while (st.hasMoreTokens()) {
			toks.add(st.nextToken());
		}

		return (String[]) toks.toArray(new String[toks.size()]);
	}

	/**
	 * Removes the first element of the given array.
	 */
	static String[] tail(String[] toks) {
		String[] args = new String[toks.length - 1];
		System.arraycopy(toks, 1, args, 0, args.length);
		return args;
	}

}
