/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils.isiam;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jline.ArgumentCompletor;
import jline.Completor;
import jline.ConsoleReader;
import jline.History;
import jline.MultiCompletor;
import jline.NullCompletor;
import jline.SimpleCompletor;

import org.apache.log4j.Logger;

/**
 * Interactive command line interface to SIAM.
 * 
 * <p>
 * NOTE: Preliminary design/implementation.
 * 
 * <p>
 * See <a href=
 * "https://oceana.mbari.org/confluence/display/~carueda/Interactive+SIAM">this
 * page</a>.
 * 
 * @author carueda
 */
public class SiamShell {

	/*
	 * Note: The original base for this code was in a separate project (related
	 * with OOI-CI) where java1.5 features (generics, in particular) and some
	 * post-java1.3 library functions (eg., String.replaceAll) were used. Here I
	 * simply replaced them with corresponding java1.3 friendly constructs.
	 */

	private static Logger _log = Logger.getLogger(SiamShell.class);

	/**
	 * The InteractiveSiam program, still preliminary.
	 * 
	 * @param args
	 *            ignored for the moment.
	 */
	public static void main(String[] args) throws Exception {
		Thread.currentThread().setName("main-isiam");

		SiamShell isiam = new SiamShell(new DefaultCommandProvider());
		isiam.goInteractive();
	}

	// TODO assign version from some properties file
	private static final String VERSION = "0.0.0.1 (experimental)";

	/**
	 * Nice iSiam logo generated by using http://www.network-science.de/ascii/
	 * with the smslant font.
	 */
	private static final String ASCII_ART = "   _ _____          \n"
			+ "  (_) __(_)__ ___ _ \n" + " / /\\ \\/ / _ `/  ' \\\n"
			+ "/_/___/_/\\_,_/_/_/_/";

	private static final String LOGO_AND_VERSION = ASCII_ART + "  " + VERSION;

	private final SiamShellHistory _siamShellHistory;

	private final Completor _nullCompletor = new NullCompletor();

	private final ICommandProvider _commandProvider;
	private final PrintWriter _out;

	private ConsoleReader _consoleReader;

	private volatile boolean _keepRunning;
	private volatile boolean _shutdown;

	/** Initialized to true to faciliate ongoing tests */
	private boolean _printTiming = true;

	private final List _nodeUtilCmds = new ArrayList();
	private final List _otherCmds = new ArrayList();
	private final Map _allCmds = new HashMap();

	/**
	 * Constructs an instance.
	 */
	public SiamShell(ICommandProvider commandProvider) {
		_commandProvider = commandProvider;
		_out = new PrintWriter(System.out, true);
		_siamShellHistory = new SiamShellHistory(_out);
		setUpEnvironment();
		setUpCommands();
	}

	/**
	 * The main method.
	 * 
	 * @throws IOException
	 */
	public void goInteractive() throws IOException {
		createConsoleReader();
		printPreamble();
		setShutdownHook();
		loop();
		shutdown();
	}

	/**
	 * Sets the JVM environment is a way that mitigates some issues due to
	 * limitations in the SIAM implementation, including lack of proper support
	 * for multiple execution of SIAM utilities, and prevent System.exit calls
	 * in those utilities from actually exiting the application.
	 */
	private void setUpEnvironment() {

		if (_log.isInfoEnabled()) {
			_log.info("Setting rmi socket factory...");
		}
		String warning = SiamShellSocketFactory.setUp();
		if (warning != null) {
			warning(warning);
		}

		if (_log.isInfoEnabled()) {
			_log.info("Setting security manager...");
		}
		warning = ExitSecurityManager.setUp();
		if (warning != null) {
			warning(warning);
		}
		if (_log.isInfoEnabled()) {
			_log.info("jvm setup done.");
		}
	}

	/**
	 * Sets up the commands understood by the program, which include those given
	 * by the command provider and a few others.
	 */
	private void setUpCommands() {
		setUpProvidedCommands();
		setUpOtherCommands();
	}

	private void setUpProvidedCommands() {
		List cmds = _commandProvider.getCommands();
		for (Iterator it = cmds.iterator(); it.hasNext();) {
			ICommand cmd = (ICommand) it.next();
			addCommand(_nodeUtilCmds, cmd);
		}
	}

	private void setUpOtherCommands() {
		addCommand(_otherCmds, new BaseCommand("help") {
			public void run(String[] args) throws Exception {
				printHelp();
			}
		});

		addCommand(_otherCmds, new BaseCommand("version") {
			public void run(String[] args) throws Exception {
				_out.println(TC.green("\n" + getVersion() + "\n"));
			}
		});

		addCommand(_otherCmds, new BaseCommand("memory") {
			public void run(String[] args) throws Exception {
				_out.println(TC.green(String.valueOf(Runtime.getRuntime()
						.freeMemory())));
			}
		});

		addCommand(_otherCmds, new BaseCommand("time") {
			public void run(String[] args) throws Exception {
				_printTiming = !_printTiming;
				_out.println(TC.green("print command timing toggled to: "
						+ _printTiming));

			}
		});

		addCommand(_otherCmds, new BaseCommand("gc") {
			public void run(String[] args) throws Exception {
				Runtime.getRuntime().gc();
			}
		});

		addCommand(_otherCmds, new BaseCommand("tcolor") {
			public void run(String[] args) throws Exception {
				TC.useColor = !TC.useColor;
			}
		});

		addCommand(_otherCmds, new BaseCommand("history") {
			public void run(String[] args) throws Exception {
				_out.println(TC.green(_siamShellHistory.getJLineHistory()));
			}
		});

		addCommand(_otherCmds, new BaseCommand("quit") {
			public void run(String[] args) throws Exception {
				_keepRunning = false;
			}
		});
	}

	/**
	 * Adds a command to the allCmds map and to the given list.
	 * 
	 * @param list
	 * @param cmd
	 */
	private void addCommand(List list, ICommand cmd) {
		String name = cmd.getName();
		_allCmds.put(name, cmd);
		list.add(name);
	}

	/**
	 * Prepares the jline.ConsoleReader instance, sets up the completors and
	 * prepares the history.
	 */
	private void createConsoleReader() throws IOException {
		_consoleReader = new jline.ConsoleReader();
		setUpCompletors();
		loadJLineHistory();
	}

	private void setUpCompletors() {

		String[] nuCmdNames = (String[]) _nodeUtilCmds.toArray(new String[_nodeUtilCmds.size()]);
		List argCompletor1 = new LinkedList();
		argCompletor1.add(new SimpleCompletor(nuCmdNames));
		argCompletor1.add(_siamShellHistory.getSiamNodeCompletor());
		argCompletor1.add(_siamShellHistory.getPortNodeCompletor());
		argCompletor1.add(_nullCompletor);

		/*
		 * TODO actually some commands accept less or more arguments.
		 */

		String[] otherCmdNames = (String[]) _otherCmds.toArray(new String[_otherCmds.size()]);
		List argCompletor2 = new LinkedList();
		argCompletor2.add(new SimpleCompletor(otherCmdNames));
		argCompletor2.add(_nullCompletor);

		// List<Completor> completors = new LinkedList<Completor>();
		List completors = new LinkedList();
		completors.add(new ArgumentCompletor(argCompletor1));
		completors.add(new ArgumentCompletor(argCompletor2));

		_consoleReader.addCompletor(new MultiCompletor(completors));
	}

	private void loadJLineHistory() {
		try {
			History history = _siamShellHistory.loadJLineHistory();
			_consoleReader.setHistory(history);
		}
		catch (IOException e) {
			warning("cannot read history: " + e.getMessage());
		}
	}

	/**
	 * Main interactive loop.
	 */
	private void loop() throws IOException {
		_keepRunning = true;
		while (_keepRunning) {
			String line = _consoleReader.readLine(getPrompt());
			if (line == null) {
				break;
			}
			line = line.trim();
			if (line.length() == 0) {
				continue;
			}

			String[] toks = SiamShellUtils.tokenize(line);

			String cmdName = toks[0];
			ICommand cmd = (ICommand) _allCmds.get(cmdName);

			if (cmd == null) {
				_out.println(TC.green("command not found: " + cmdName
						+ ". Press TAB or enter \"help\""));
				continue;
			}

			String[] args = SiamShellUtils.tail(toks);
			if (_log.isDebugEnabled()) {
				_log.debug("Arguments: " + SiamShellUtils.array2string(args));
			}
			_out.println(TC.yellow("Arguments: "
					+ SiamShellUtils.array2string(args)));

			if (args.length > 0) {
				SiamShellSocketFactory.setServerHost(args[0]);
			}

			final long iniTime = System.currentTimeMillis();
			long endTime = 0;
			try {
				try {
					cmd.run(args);
					endTime = System.currentTimeMillis();
				}
				catch (InvocationTargetException e) {
					Throwable t = e.getTargetException();
					throw (t != null) ? t : e;
				}

				if (_printTiming) {
					_out.println(TC.yellow("\n\t## Command took "
							+ (endTime - iniTime) + " ms ##\n"));
				}

				updateCompletors(cmd);
			}
			catch (ExitException e) {
				_out.println(TC.red("\n-- intercepted System.exit(" + e.status
						+ ") call by command " + cmdName + " --\n"));
			}
			catch (Throwable e) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				_out.println(TC.red(sw.toString()));
			}
		}
	}

	/**
	 * Once a command has been executed, this method is called to update any
	 * completors depending of the arguments used by the command.
	 */
	private void updateCompletors(ICommand cmd) {
		String nodeUrl = cmd.getNodeUrlArgument();
		if (nodeUrl != null) {
			_siamShellHistory.rememberNodeUrl(nodeUrl);
		}

		String portName = cmd.getPortNameArgument();
		if (portName != null) {
			_siamShellHistory.rememberPortName(portName);
			/*
			 * TODO some mechanism to associate the port to the corresponding
			 * node.
			 */
		}
	}

	private synchronized void shutdown() {
		if (!_shutdown) {
			_out.println();
			ExitSecurityManager.remove();
			SiamShellSocketFactory.remove();
		}
		_shutdown = true;
	}

	private void printHelp() {
		_out.println(TC.green(getHelp()));
	}

	/**
	 * Provides a general help message.
	 * 
	 * TODO this is not implemented yet (it just returns a simple message). It
	 * can be implemented by reading from a resource, or something like that.
	 */
	private String getHelp() {
		return "## help TODO ;)  press TAB ##";
	}

	/**
	 * Returns a descriptive version of this tool.
	 */
	private String getVersion() {
		return LOGO_AND_VERSION;
	}

	private void printPreamble() {
		_out.println(TC.green(getVersion() + "\n"
				+ "Welcome to iSiam. Press TAB or enter \"help\""));
	}

	private String getPrompt() {
		return TC.blue("isiam" + "> ");
	}

	/**
	 * For a graceful termination upon ^C. TODO: this is just a basic
	 * termination only related with the main loop, but not about other
	 * potential aspects related with any commands that may still be running.
	 */
	private void setShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread("shutdown") {
			public void run() {
				if (!_shutdown) {
					_out.println("\nStopping iSiam ...");
					shutdown();
				}
			}
		});
	}

	private void warning(String msg) {
		_out.println(TC.red("Warning: " + msg));
	}

}
