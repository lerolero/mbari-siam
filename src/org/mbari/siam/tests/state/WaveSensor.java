/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.state;

import org.mbari.siam.distributed.InstrumentServiceAttributes;
import org.mbari.siam.distributed.AttributeChecker;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.InvalidPropertyException;

public class WaveSensor extends AttrTest {

	public class Attributes extends InstrumentServiceAttributes {

		/** Required constructur with service argument */
		Attributes(AttrTest service) {
			super(service);
		}

		float xOffset;

		float yOffset;

		float zOffset;
		
		String company = "ACME";
		String serialNo = "fribbish123";


		int sampleMinutes;

		boolean timeSync;

		int coeffs[];

		/**
		 * Throw MissingPropertyException if specified attribute is mandatory.
		 */
		public void missingAttributeCallback(String fieldName)
				throws MissingPropertyException {

			if (fieldName.equals("serialNo")) {
				throw new MissingPropertyException(fieldName);
			}
		}

		/**
		 * Throw InvalidPropertyException if specified attribute has invalid
		 * value.
		 */
		public void setAttributeCallback(String attributeName, String valueString)
				throws InvalidPropertyException {
			// Don't care....
		}

		/** Throw InvalidPropertyException if any invalid values */
		public void checkValues() throws InvalidPropertyException {

			if (sampleMinutes < 5 || sampleMinutes > 35) {
				throw new InvalidPropertyException("sampleMinutes="
						+ sampleMinutes + ": must " + "be in range 5 to 35");

			}
		}
	}

	// Working attributes
	Attributes attributes = null;

	public WaveSensor() {
		System.out.println("Construct WaveSensor attributes...");
		attributes = new Attributes(this);
	}

}

