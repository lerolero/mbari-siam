/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/*
 * Created on Oct 13, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.mbari.siam.tests.state;

import junit.framework.TestCase;

import org.mbari.siam.distributed.InstrumentServiceAttributes;

/**
 * @author oreilly
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class MnemonicTest extends TestCase {

	public void testHelp() {
		InstrumentServiceAttributes attributes = 
			InstrumentServiceAttributes.getAttributes();
		
		String help = attributes.getHelp();
		System.out.println(help);
	}
}
