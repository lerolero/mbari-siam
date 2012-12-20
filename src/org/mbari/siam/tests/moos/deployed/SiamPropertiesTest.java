/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.siam.tests.moos.deployed;

import java.lang.*;
import java.util.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.AssertionFailedError;
import org.mbari.siam.distributed.MissingPropertyException;
import org.mbari.siam.distributed.InvalidPropertyException;
import org.mbari.siam.core.SiamProperties;

/**
JUnit test harness for testing SiamProperties class.
@author Karen A. Salamy & Kent Headley
 */


// Creating a subclass of TestCase
public class SiamPropertiesTest extends TestCase
{

	public SiamPropertiesTest(String methodName) {
		super(methodName);
	}


    // Test method to assert expected results on the object under test
	public void testEmptyCollection() {
	    Collection collection = new ArrayList();
	    assertTrue(collection.isEmpty());
	}

    // Test whitespace trailing and leading trimming
    	public void getProperty(){
	    SiamProperties properties = new SiamProperties(); //Create SiamProperties object
	    properties.setProperty("Foo"," 4  "); // Creates a property object (name value pair)
	    String value = properties.getProperty("Foo"); //Retrieve the value of Foo
	    assertTrue(value.equals("4")); //test to see if it is really equal to 4


	    //Will it fail?
	    //  properties.setProperty("Foo","quux");
	    //value = properties.getProperty("Foo");
	    //assertTrue(value.equals("quux")); 
	    //System.out.println(value);

	    //Default value
	    // properties.setProperty("Foo", null); // Creates a property object (name value pair)
	    value = properties.getProperty("Bar","doh"); //If no value give this
	    assertTrue(value.equals("doh")); 
    	}

    // Test whether a required property is provided, otherwise throw a MissingPropertyException
    public void getRequiredProperty()
            throws MissingPropertyException{
	    SiamProperties properties = new SiamProperties();
	    properties.setProperty("Kent", "100");
	    String value = properties.getRequiredProperty("Kent");
	    assertTrue(value.equals("100")); //Equals value of the property "Kent"
	    
	    try { //Success case
		//	properties.setProperty("Karen", "100");

		value = properties.getRequiredProperty("Karen"); //should throw missing Prop exception
	    } catch (MissingPropertyException e) {return;}
	    assertTrue(false); //If it doesn't catch missing exception then false - should not get to this point however

    }

    // Testing the InvalidProperty and MissingProperty Exceptions for the getIntegerProperty method.
     public void getIntegerProperty()
	 //throws InvalidPropertyException,MissingPropertyException
    {
		SiamProperties properties = new SiamProperties();
		properties.setProperty("test", "100"); // Set valid property
		int value=0; // Declare return value
		try{
		    value = properties.getIntegerProperty("test"); //test valid property
		}catch (InvalidPropertyException e) {assertTrue(false); }
		catch (MissingPropertyException j) {assertTrue(false); }
		assertTrue(value==100); //Check return value


		// Put range tests here (same as above) -NOTE - WILL ONLY TAKE INT VALUES NOT LONG!!!
		properties.setProperty("test", "2147483647");
		value=0;
		try {
		    value = properties.getIntegerProperty("test");
		}catch (InvalidPropertyException a) {assertTrue(false); }
		catch (MissingPropertyException b) {assertTrue(false); }
		assertTrue(value==2147483647);


	        properties.setProperty("test", "-2147483648");
		value=0;
		try{
		    value = properties.getIntegerProperty("test");
		} catch (InvalidPropertyException c) {assertTrue(false); }
		catch (MissingPropertyException d) {assertTrue(false); }
		assertTrue(value==-2147483648);

		// Hex values? Double? exponential notation    


		properties.setProperty("test", "!#"); // Set an invalid property "Should throw InvalidPropertyException"
		try {  //Success case1
		    value = properties.getIntegerProperty("test"); //Set value equal to value of test which is !#
		} catch (InvalidPropertyException e) {
		    try { //Test to see if value = !# will throw an InvalidPropertyException
			value = properties.getIntegerProperty("Karen"); //Next set value equal to "Karen" which is an undeclared Property
		    } catch (MissingPropertyException i) {
			return; //Verify that all exceptions are thrown, otherwise print the two lines below.
		    }catch (InvalidPropertyException k) {
			System.out.println("Hey1"); } //Should never get here
		}catch (MissingPropertyException j) {
		    System.out.println("Hey2");}	 //Should never get here.	
		assertTrue(false); 

    }

    // Testing the getIntegerProperty DEFAULT method.
    public void getIntegerPropertyDefault()
    {
	// Tested:  Correctly returns correct value for valid input.
	SiamProperties properties = new SiamProperties();
	properties.setProperty("goodInt", "100");
	int value=0;
	value = properties.getIntegerProperty("goodInt", 50);
	assertTrue(value==100);

	// Tested:  Correctly returns the default value if property does not exist.
	properties.setProperty("undefined", " ");
	value=0;
	value = properties.getIntegerProperty("undefined", 50);
	assertTrue(value==50);

	// Tested:  Correctly returns the default value if property is not a valid INT.
	properties.setProperty("badInt", "!#");
	value=0;
	value = properties.getIntegerProperty("badInt", 50);
	assertTrue(value==50);

	// Tested: Returns the default value if property is a Decimal. 
	//Does Not accept Decimal values!
	properties.setProperty("decimal", "5.2");
	value=0;
	value = properties.getIntegerProperty("decimal", 50);
	assertTrue(value==50);

	// Tested:  Returns the default value if property is a Hexidecimal.
	//Does Not accept Hexidecimal values!
	properties.setProperty("hexidecimal", "2A");
	value=0;
	value = properties.getIntegerProperty("hexidecimal", 50);
	assertTrue(value==50);

	// Tested:  Returns the default value if property is represented in Scientific Notation.
	//Does Not accept values in Scientific Notation!
	properties.setProperty("sciNote", "2E10");
	value=0;
	value = properties.getIntegerProperty("sciNote", 50);
	assertTrue(value==50);

	// Tested:  Returns a correct NEGATIVE integer value input
	//Accepts NEGATIVE integer values.
	properties.setProperty("negValue", "-100");
	value=0;
	value = properties.getIntegerProperty("negValue", 50);
	assertTrue(value==-100);

	// Tested:  Returns the default value if POSITIVE Integer value is represented with a "+" symbol.
	//Does NOT accept "+" symbols to represent positive integer values.
	properties.setProperty("negValue", "+100");
	value=0;
	value = properties.getIntegerProperty("negValue", 50);
	assertTrue(value==50);

	// Tested:  Returns the default value if POSITIVE LONG number value input
	//Does NOT accept positive long numbers.
 	properties.setProperty("posLongValue", "2147483650");
	value=0;
	value = properties.getIntegerProperty("posLongValue", 50);
	assertTrue(value==50);

	// Tested:  Returns the default value if NEGATIVE LONG number value input
	//Does NOT accept negative long numbers.
 	properties.setProperty("negLongValue", "-2147483650");
	value=0;
	value = properties.getIntegerProperty("negLongValue", 50);
	assertTrue(value==50);
    }

     

 // Testing the InvalidProperty and MissingProperty Exceptions for the getLongProperty method.
     public void getLongProperty()
	 //throws InvalidPropertyException,MissingPropertyException
    {
		SiamProperties properties = new SiamProperties();
		properties.setProperty("test", "2147483650"); // Set valid property
	        long value=0; // Declare return value
		try{
		    value = properties.getLongProperty("test"); //test valid property
		}catch (InvalidPropertyException e) {assertTrue(false); }
		catch (MissingPropertyException j) {assertTrue(false); }
		assertTrue(value==2147483650L); //Check return value


		// Put range tests here.
		properties.setProperty("test", "9223372036854775807");
		value=0;
		try {
		    value = properties.getLongProperty("test");
		}catch (InvalidPropertyException a) {assertTrue(false); }
		catch (MissingPropertyException b) {assertTrue(false); }
		assertTrue(value==9223372036854775807L);


	        properties.setProperty("test", "-9223372036854775807");
		value=0;
		try{
		    value = properties.getLongProperty("test");
		} catch (InvalidPropertyException c) {assertTrue(false); }
		catch (MissingPropertyException d) {assertTrue(false); }
		assertTrue(value==-9223372036854775807L);
       

		properties.setProperty("test", "!#"); // Set an invalid property "Should throw InvalidPropertyException"
		try {  //Success case1
		    value = properties.getLongProperty("test"); //Set value equal to value of test which is !#
		} catch (InvalidPropertyException e) {
		    try { //Test to see if value = !# will throw an InvalidPropertyException
			value = properties.getLongProperty("Karen"); //Next set value equal to "Karen" which is an undeclared Property
		    } catch (MissingPropertyException i) {
			return; //Verify that all exceptions are thrown, otherwise print the two lines below.
		    }catch (InvalidPropertyException k) {
			System.out.println("Hey1"); } //Should never get here
		}catch (MissingPropertyException j) {
		    System.out.println("Hey2");}	 //Should never get here.	
		assertTrue(false); 

    }

    // Testing the getIntegerProperty DEFAULT method.
    public void getLongPropertyDefault()
    {
	// Tested:  Correctly returns correct value for valid input.
	SiamProperties properties = new SiamProperties();
	properties.setProperty("goodInt", "1000000000000");
	long value=0;
	value = properties.getLongProperty("goodInt", 5000000000000L);
	assertTrue(value==1000000000000L);

	// Tested:  Correctly returns the default value if property does not exist.
	properties.setProperty("undefined", " ");
	value=0;
	value = properties.getLongProperty("undefined", 50000000000000L);
	assertTrue(value==50000000000000L);

	// Tested:  Correctly returns the default value if property is not a valid LONG.
	properties.setProperty("badInt", "!#");
	value=0;
	value = properties.getLongProperty("badInt", 50000000000000L);
	assertTrue(value==50000000000000L);

	// Tested: Returns the default value if property is a Decimal. 
	//Does Not accept Decimal values!
	properties.setProperty("decimal", "5.2");
	value=0;
	value = properties.getLongProperty("decimal", 50000000000000L);
	assertTrue(value==50000000000000L);

	// Tested:  Returns the default value if property is a Hexidecimal.
	//Does Not accept Hexidecimal values!
	properties.setProperty("hexidecimal", "2A");
	value=0;
	value = properties.getLongProperty("hexidecimal", 50000000000000L);
	assertTrue(value==50000000000000L);

	// Tested:  Returns the default value if property is represented in Scientific Notation.
	//Does Not accept values in Scientific Notation!
	properties.setProperty("sciNote", "2E10");
	value=0;
	value = properties.getLongProperty("sciNote", 50000000000000L);
	assertTrue(value==50000000000000L);

	// Tested:  Returns a correct NEGATIVE integer value input
	//Accepts NEGATIVE integer values.
	properties.setProperty("negValue", "-1000000000000");
	value=0;
	value = properties.getLongProperty("negValue", 50000000000000L);
	assertTrue(value==-1000000000000L);

	// Tested:  Returns the default value if POSITIVE Long value is represented with a "+" symbol.
	//Does NOT accept "+" symbols to represent positive integer values.
	properties.setProperty("negValue", "+100");
	value=0;
	value = properties.getLongProperty("negValue", 50000000000000L);
	assertTrue(value==50000000000000L);
    } 

// Implement a suite() method that uses reflection to dynamically create a test suite containing all the testXXX() methods
	public static Test suite() {
	    TestSuite suite = new TestSuite();
	    suite.addTest(new SiamPropertiesTest("getProperty"));
	    suite.addTest(new SiamPropertiesTest("getRequiredProperty"));
	    suite.addTest(new SiamPropertiesTest("testEmptyCollection"));
	    suite.addTest(new SiamPropertiesTest("getIntegerProperty"));
	    suite.addTest(new SiamPropertiesTest("getIntegerPropertyDefault"));
	    suite.addTest(new SiamPropertiesTest("getLongProperty"));
	    suite.addTest(new SiamPropertiesTest("getLongPropertyDefault"));


	    return suite;

	}

} //end class
