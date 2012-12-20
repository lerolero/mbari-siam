/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils.isiam;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jline.History;
import jline.SimpleCompletor;

/**
 * Manages the jline history and some of the completors, and also other elements
 * that can be remembered from session to session (eg., the nodeUrls that have
 * been used).
 * 
 * @author carueda
 */
class SiamShellHistory {

	/**
	 * isiam resources kept under directory ~/.isiam/.
	 */
	private static final File _isiamDir = new File(System.getProperty("user.home",
			"./"),
			".isiam");

	/**
	 * JLine command history file, kept in ~/.isiam/cmd_history
	 */
	private static final File _jlineHistoryFile = new File(_isiamDir,
			"cmd_history");

	/**
	 * nodeUrl history file, kept in ~/.isiam/nodes
	 */
	private static final File _nodeUrlHistoryFile = new File(_isiamDir, "nodes");

	/**
	 * to print out any warning messages.
	 */
	private final PrintWriter _out;

	private final SimpleCompletor _siamNodeCompletor = new SimpleCompletor(new String[] { "localhost" });

	private final SimpleCompletor _portNameCompletor = new SimpleCompletor(new String[] {});

	/**
	 * list of nodeUrls, kept in sync with the nodes file.
	 */
	private final List _nodeUrls = new ArrayList();

	SiamShellHistory(PrintWriter out) {
		this._out = out;
		setUp();
	}

	private void setUp() {
		if (_isiamDir.exists()) {
			if (!_isiamDir.isDirectory()) {
				warning(_isiamDir + " exists but is not a directory.");
			}
		}
		else {
			if (!_isiamDir.mkdir()) {
				warning("cannot create directory " + _isiamDir);
			}
		}

		// initialize _siamNodeCompletor
		try {
			for (Iterator it = getNodeUrls().iterator(); it.hasNext();) {
				String nodeUrl = (String) it.next();
				_siamNodeCompletor.addCandidateString(nodeUrl);
			}
		}
		catch (IOException e) {
			_out.println(TC.red("WARNING: cannot nodeUrl history: "
					+ e.getMessage()));
		}

	}

	History loadJLineHistory() throws IOException {
		History history;
		if (_isiamDir.isDirectory()) {
			history = new History(_jlineHistoryFile);
		}
		else {
			history = new History();
		}
		return history;
	}

	String getJLineHistory() {
		// TODO
		return "## TODO (the command history is kept in " + _jlineHistoryFile
				+ ") ##";

	}

	SimpleCompletor getSiamNodeCompletor() {
		return _siamNodeCompletor;
	}

	SimpleCompletor getPortNodeCompletor() {
		return _portNameCompletor;
	}

	/**
	 * Returns the list of nodeUrls (Strings) that will be used to initialize
	 * the corresponding jline Completor for commands that accept this kind of
	 * argument.
	 */
	List getNodeUrls() throws IOException {
		if (_nodeUrls.isEmpty()) {
			loadNodeUrls();
		}
		return _nodeUrls;
	}

	private void loadNodeUrls() {
		if (!_nodeUrlHistoryFile.exists()) {
			/*
			 * just create an (empty) file:
			 */
			saveNodeUrls();
		}
		
		if (_nodeUrlHistoryFile.canRead()) {
			BufferedReader br = null;
			try {
				br = new BufferedReader(new InputStreamReader(new FileInputStream(_nodeUrlHistoryFile)));
				String line;
				while (null != (line = br.readLine())
						&& line.trim().length() > 0) {
					_nodeUrls.add(line.trim());
				}
			}
			catch (IOException e) {
				warning("error while reading " + _nodeUrlHistoryFile + ": "
						+ e.getMessage());
			}
			finally {
				if (br != null) {
					try {
						br.close();
					}
					catch (IOException ignore) {
					}
				}
			}
		}
		else {
			warning("cannot read " + _nodeUrlHistoryFile);
		}
	}

	/**
	 * Saves the given nodeUrl for this and future sessions.
	 */
	void rememberNodeUrl(String nodeUrl) {
		_siamNodeCompletor.addCandidateString(nodeUrl);
		if (!_nodeUrls.contains(nodeUrl)) {
			_nodeUrls.add(nodeUrl);
			saveNodeUrls();
		}
	}

	/**
	 * Port names are only remembered in the current session.
	 */
	void rememberPortName(String portName) {
		_portNameCompletor.addCandidateString(portName);
	}

	private void saveNodeUrls() {
		PrintWriter pw = null;
		try {
			// constructor PrintWriter(File) since java 1.5, so use alternate
			// java 1.3 constructor:
			// pw = new PrintWriter(_nodeUrlHistoryFile);
			pw = new PrintWriter(new FileOutputStream(_nodeUrlHistoryFile));
			for (Iterator it = _nodeUrls.iterator(); it.hasNext();) {
				String str = (String) it.next();
				pw.println(str);
			}
		}
		catch (IOException e) {
			warning("error while saving " + _nodeUrlHistoryFile + ": "
					+ e.getMessage());
		}
		finally {
			if (pw != null) {
				pw.close();
			}
		}
	}

	private void warning(String msg) {
		_out.println(TC.red("Warning: " + msg));
	}

}
