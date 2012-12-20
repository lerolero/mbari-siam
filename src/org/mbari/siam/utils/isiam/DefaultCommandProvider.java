/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils.isiam;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * A provider of commands to dispatch NodeUtility subclasses.
 * 
 * These commands are kept in the ~/isiam/nucmds file (where ~ represents the
 * user's home directory). Each line in this files should be the full name of a
 * subclass of NodeUtility. The contents of the nucmds file is initialized
 * automatically with all the classes under the org.mbari.siam.operations.utils
 * package that extend SIAM's NodeUtility.
 * 
 * Note that the file can be edited manually. The automatic initialization
 * occurs only if the file does not exist.
 * 
 * @author carueda
 */
public class DefaultCommandProvider implements ICommandProvider {

	private static Logger _log = Logger.getLogger(DefaultCommandProvider.class);

	/**
	 * iSiam resources are kept under directory ~/.isiam/, which is created
	 * automatically if non-existent.
	 */
	private static final File _isiamDir = new File(System.getProperty("user.home",
			"./"),
			".isiam");

	private static final ClassLoader _classLoader = DefaultCommandProvider.class.getClassLoader();

	private static Class _nodeUtilityBaseClass = null;
	static {
		String nodeUtilityBaseClassName = SiamShellConstants.NODE_UTILITY_CLASS_NAME;
		try {
			_nodeUtilityBaseClass = _classLoader.loadClass(nodeUtilityBaseClassName);
		}
		catch (ClassNotFoundException e) {
			// normally it should not happen
			_log.error("error: couldn't load " + nodeUtilityBaseClassName
					+ ": " + e.getMessage());
		}
	}

	/**
	 * NodeUtility commands file, kept in ~/.isiam/nucmds
	 */
	private static final File _nuNamesFile = new File(_isiamDir, "nucmds");

	private final Set/* <String> */_nuNames = new HashSet();

	private final List/* <NodeUtilityCommand> */_cmds = new ArrayList();

	public List getCommands() {
		if (_cmds.isEmpty()) {
			prepareNuNames();
			createCmds();
		}
		return _cmds;
	}

	/**
	 * Prepares the list of NodeUtility class names.
	 */
	private void prepareNuNames() {
		if (!_nuNamesFile.exists()) {
			/*
			 * initialize the file with dynamically scanned NodeUtility
			 * subclasses:
			 */
			if (_nodeUtilityBaseClass == null) {
				// log message should have been written out already.
				return;
			}

			if (_log.isDebugEnabled()) {
				_log.debug("Initializing set of node utility commands...");
			}

			PrintWriter pw = null;

			try {
				/*
				 * get the classes
				 */
				Collection/* <Class> */classes = getNuClasses(SiamShellConstants.NODE_UTILITIES_PACKAGE,
						true);
				if (_log.isInfoEnabled()) {
					_log.info("Scanned " + classes.size()
							+ " NodeUtility subclasses.");
				}

				/*
				 * and save the names in the nucmd file:
				 */
				pw = new PrintWriter(new FileOutputStream(_nuNamesFile));
				for (Iterator it = classes.iterator(); it.hasNext();) {
					Class clazz = (Class) it.next();
					String className = clazz.getName();
					pw.println(className);
					_nuNames.add(className);
				}
			}
			catch (FileNotFoundException e) {
				_log.warn("error while writing initial contents to "
						+ _nuNamesFile + ": " + e.getMessage(), e);
			}
			catch (Exception e) {
				_log.warn("An exception was thrown while loading classes", e);
			}
			finally {
				if (pw != null) {
					pw.close();
				}
			}
		}

		else if (_nuNamesFile.canRead()) {
			BufferedReader br = null;
			try {
				br = new BufferedReader(new InputStreamReader(new FileInputStream(_nuNamesFile)));
				String line;
				while (null != (line = br.readLine())
						&& line.trim().length() > 0) {
					_nuNames.add(line.trim());
				}
			}
			catch (IOException e) {
				_log.warn("error while reading " + _nuNamesFile + ": "
						+ e.getMessage(), e);
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
			_log.warn("cannot read " + _nuNamesFile);
		}
	}

	/**
	 * Creates and add the {@link NodeUtilityCommand" instances to the _cmds
	 * list for the names in _nuNames.
	 */
	private void createCmds() {
		for (Iterator it = _nuNames.iterator(); it.hasNext();) {
			String nuName = (String) it.next();
			_cmds.add(new NodeUtilityCommand(nuName));
		}
	}

	/**
	 * Dynamically gets the list of classes under the given package and
	 * subapckages that extend SIAM's NodeUtility class, that is, that can be
	 * used to initialize the set of commands.
	 * 
	 * (adapted from http://snippets.dzone.com/posts/show/4831)
	 * 
	 * @param packageName
	 * @param includeSubPackages
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @return the collection of classes
	 */
	private static Collection/* <Class> */getNuClasses(String packageName,
			boolean includeSubPackages) throws IOException,
			ClassNotFoundException {

		String path = packageName.replace('.', '/');
		Enumeration resources = _classLoader.getResources(path);
		List dirs = new ArrayList();
		if (_log.isDebugEnabled()) {
			_log.debug("Resources in path: " + path);
		}
		while (resources.hasMoreElements()) {
			URL resource = (URL) resources.nextElement();
			if (_log.isDebugEnabled()) {
				_log.debug("  " + resource);
			}
			dirs.add(new File(resource.getFile()));
		}
		Collection classes = new HashSet();
		for (Iterator it = dirs.iterator(); it.hasNext();) {
			File directory = (File) it.next();
			loadNuClasses(directory, packageName, includeSubPackages, classes);
		}
		return classes;
	}

	/**
	 * Recursive method used to find the desired subclasses of NodeUtility in
	 * the given directory and subdirectories (subpackages).
	 * 
	 * @param directory
	 *            The base directory
	 * @param packageName
	 *            The package name for classes found inside the base directory
	 * @param includeSubPackages
	 * @param classes
	 *            the class collection to update
	 * @throws ClassNotFoundException
	 */
	private static void loadNuClasses(File directory, String packageName,
			boolean includeSubPackages, Collection classes)
			throws ClassNotFoundException {

		if (!directory.exists()) {
			return;
		}
		File[] files = directory.listFiles();
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			if (file.isDirectory() && includeSubPackages) {
				loadNuClasses(file,
						packageName + "." + file.getName(),
						includeSubPackages,
						classes);
			}
			else if (file.getName().endsWith(".class")) {
				String className = packageName
						+ '.'
						+ file.getName().substring(0,
								file.getName().length() - ".class".length());

				try {
					/*
					 * load the class and verify that it is not abstract and
					 * that extends NodeUtility:
					 */
					Class nodeUtilityClass = _classLoader.loadClass(className);
					if (!Modifier.isAbstract(nodeUtilityClass.getModifiers())) {
						if (_nodeUtilityBaseClass.isAssignableFrom(nodeUtilityClass)) {
							classes.add(nodeUtilityClass);
						}
					}
				}
				catch (ClassNotFoundException e) {
					_log.warn("warning: couldn't load class: " + className
							+ ": " + e.getMessage());
				}
			}
		}
	}
}
