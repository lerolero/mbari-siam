package org.mbari.siam.utils;

/*%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

	Copyright (c) Non, Inc. 1999 -- All Rights Reserved

PACKAGE:	JavaWorld
FILE:		JarClassLoader.java

AUTHOR:		John D. Mitchell, Mar  3, 1999

REVISION HISTORY:
	Name	Date		Description
	----	----		-----------
	JDM	99.03.03   	Initial version.

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%*/


/**
 ** JarClassLoader provides a minimalistic ClassLoader which shows how to
 ** instantiate a class which resides in a .jar file.
<br><br>
 **  Based on code found in "Java Tip 70" at "Java World".
 **
 ** @author	John D. Mitchell, Non, Inc., Mar  3, 1999
 **
 ** @version 0.5
 **
 **/

public class JarClassLoader extends MultiClassLoader   {

    protected JarResources	jarResources;

    public JarClassLoader (String jarName) {
	// Create the JarResource and suck in the .jar file.
	jarResources = new JarResources (jarName);
    }

    protected byte[] loadClassBytes (String className) {

	// Support the MultiClassLoader's class name munging facility.
	className = formatClassName (className);

	// Attempt to get the class data from the JarResource.
	return (jarResources.getResource (className));
    }


}	// End of Class JarClassLoader.

