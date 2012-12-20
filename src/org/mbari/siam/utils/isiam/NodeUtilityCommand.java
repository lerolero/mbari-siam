/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.utils.isiam;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Command for NodeUtility classes.
 * 
 * <p>
 * Note: the current implementation is fully reflection-based (ie. there are no
 * compile dependencies on SIAM classes). This does not have to be the case but,
 * along with a new class loader for each invocation, it was the mechanism I
 * explored while trying to make the execution of each NodeUtility "separated"
 * from other executions, ie., in some kind of sandbox at each invocation (so
 * repeated executions won't interfere with each other). This "sandbox"
 * approach, however, is not enough and actually more appropriate changes would
 * have to be done in the NodeUtility classes themselves (rather than in this
 * class) so they purposely allow multiple executions during the same JVM
 * instance. For the moment, I'm leaving the current reflection-based mechanism
 * in this class as it should not hurt (but it can be simplified later on).
 * 
 * @author carueda
 */
class NodeUtilityCommand extends BaseCommand {

	/** Name of the concrete NodeUtility subclass */
	private final String _nodeUtilityClassName;

	/**
	 * Creates a command for the NodeUtility subclass of the given full name.
	 * 
	 * @param className
	 */
	NodeUtilityCommand(String className) {
		super(SiamShellUtils.getSimpleName(className));
		_nodeUtilityClassName = className;
	}

	public void run(String[] args) throws Exception {

		_nodeUrlArgument = null;
		_portNameArgument = null;

		Support classes = new Support(_nodeUtilityClassName);
		Object nuObj = classes._nodeUtilityClass.newInstance();

		doRun(classes, nuObj, args);

		updateArguments(classes, nuObj, args);
	}

	/**
	 * calls the NodeUtil methods using reflection
	 */
	private void doRun(Support classes, Object nuObj, String[] args)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {

		classes._processArgumentsMethod.invoke(nuObj, new Object[] { args });
		classes._runMethod.invoke(nuObj, null);
	}

	/**
	 * TODO: The following strategy to keep track of the node and port arguments
	 * is very ad hoc -- can be improved.
	 */
	private void updateArguments(Support classes, Object nuObj, String[] args) {
		/*
		 * assume the command expects a nodeUrl as the first argument
		 */
		if (args.length > 0) {
			_nodeUrlArgument = args[0];
		}

		/*
		 * If it was a PortUtility, assume the second argument is the port
		 */
		if (args.length > 1 && classes._portUtilityClass.isInstance(nuObj)) {
			_portNameArgument = args[1];
		}
	}

	/**
	 * Loads needed SIAM classes by using a new class loader and prepares
	 * handles to needed methods.
	 * 
	 * <p>
	 * The new class loader is intended to faciliate that any resources
	 * allocated by SIAM while processing the NodeUtility program get released
	 * when the program is completed.
	 * 
	 * The goal is to make each program state-less, that is, the class is always
	 * loaded/initialized at each invocation of the program.
	 * 
	 * <p>
	 * Update: this "sandbox" approach won't be enough; see javadoc for the
	 * whole class {@link NodeUtilityCommand}.
	 */
	private static class Support {

		final Class _nodeUtilityClass;
		final Class _portUtilityClass;

		final Method _processArgumentsMethod;
		final Method _runMethod;

		/**
		 * Creates a supporting object to execute the run method associated with
		 * the class by the given name.
		 */
		Support(String nodeUtilityClassName) throws ClassNotFoundException,
				SecurityException, NoSuchMethodException {

			// use a new class loader for all the support:
			ClassLoader classLoader = new ClassLoader() {
			};

			// get the base NodeUtility class:
			Class _nodeUtilityBaseClass = classLoader.loadClass(SiamShellConstants.NODE_UTILITY_CLASS_NAME);

			// get the particular NodeUtility subclass:
			_nodeUtilityClass = classLoader.loadClass(nodeUtilityClassName);

			// get the base PortUtility class:
			_portUtilityClass = classLoader.loadClass(SiamShellConstants.PORT_UTILITY_CLASS_NAME);

			if (!_nodeUtilityBaseClass.isAssignableFrom(_nodeUtilityClass)) {
				throw new ClassCastException(nodeUtilityClassName
						+ " is not a subclass of " + SiamShellConstants.NODE_UTILITY_CLASS_NAME);
			}

			// get the processArguments method:
			_processArgumentsMethod = _nodeUtilityClass.getMethod("processArguments",
					new Class[] { String[].class });

			// get the run method:
			_runMethod = _nodeUtilityClass.getMethod("run", new Class[0]);
		}
	}
}
