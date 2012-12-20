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

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.mbari.siam.distributed.InstrumentServiceAttributes;

/**
 * @author oreilly
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class AttributeSerializer  {

	public void serialize1() throws IOException {
		
		FileOutputStream fileOut = new FileOutputStream("byteArray.out");
		ObjectOutputStream out = new ObjectOutputStream(fileOut);
		ByteArrayTest byteArrayTest = new ByteArrayTest();
		System.out.println("writing ByteArrayTest object");
		out.writeObject(byteArrayTest);
		System.out.println("OK");
		out.flush();
		out.close();
		fileOut.flush();
		fileOut.close();	
	
		fileOut = new FileOutputStream("embeddedObject.out");
		out = new ObjectOutputStream(fileOut);
		System.out.println("writing EmbeddedObjectTest object");
		out.writeObject(new EmbeddedObjectTest());
		out.flush();
		out.close();
		fileOut.flush();
		fileOut.close();
	}
	
	
	public void run(String outputName) throws IOException {
		
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(byteOut);
		System.out.println("Writing first object...");
		InstrumentServiceAttributes attributes = 
			InstrumentServiceAttributes.getAttributes();
		
		System.out.println(attributes.getHelp());
		System.out.println(attributes.toString());
		
		out.writeObject(attributes);
		System.out.println("Writing second object...");
		out.writeObject(InstrumentServiceAttributes.getAttributes());
		System.out.println("Writing third object...");
		out.writeObject(InstrumentServiceAttributes.getAttributes());

		out.flush();
        out.close();
        byteOut.flush();
		byteOut.close();
		byte[] content = byteOut.toByteArray();
		
		System.out.println("serialized " + content.length + " bytes.");
	}
	
	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("usage: outputFile");
			return;
		}
		AttributeSerializer serializer = new AttributeSerializer();
		try {
			serializer.serialize1();
			System.out.println("Done.");
		}
		catch (Exception e) {
			System.err.println(e);
		}
	}
}

class ByteArrayTest implements Serializable {
	byte[] properties;
	
	ByteArrayTest() {
		properties = 
			InstrumentServiceAttributes.getAttributes().toString().getBytes();
	}
}

class EmbeddedObjectTest implements Serializable {
	InstrumentServiceAttributes attributes = 
		InstrumentServiceAttributes.getAttributes();
}

