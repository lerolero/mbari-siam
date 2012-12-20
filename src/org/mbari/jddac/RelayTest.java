/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
package org.mbari.jddac;

import net.java.jddac.common.type.ArgArray;
import net.java.jddac.common.meas.Measurement;
import net.java.jddac.common.meas.MeasAttr;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;

/**
 * Created by IntelliJ IDEA.
 * User: brian
 * Date: Apr 10, 2006
 * Time: 3:12:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class RelayTest {

    public static final String OpIdTest = "test";

    public RelayTest() throws Exception {
        run();
    }


    private void run() throws Exception {
        IFunction logFunction1 = new Log4jFunction(getClass().getName() + ".log1");
        IFunction logFunction2 = new Log4jFunction(getClass().getName() + ".log2");

        ArgArray f2 = FunctionFactory.createFunctionArg(OpIdTest, OpIdTest, logFunction1);
        ArgArray f3 = FunctionFactory.createFunctionArg(OpIdTest, OpIdTest, logFunction2);

        RelayBlock b1 = new RelayBlock();
        b1.addFunction(f2);

        RelayBlock b2 = new RelayBlock();
        b1.addChild(b2);
        b2.addFunction(f3);
        
        Measurement m = new Measurement();
        m.put(MeasAttr.NAME, "Foo");
        for(int i = 0; i < 100; i++) {
            m.put(MeasAttr.VALUE, new Integer(i));
            b1.perform(OpIdTest, m);
        }
    }

    public static void main(String[] args) throws Exception {
	/*
	 * Set up a simple configuration that logs on the console. Note that
	 * simply using PropertyConfigurator doesn't work unless JavaBeans
	 * classes are available on target. For now, we configure a
	 * PropertyConfigurator, using properties passed in from the command
	 * line, followed by BasicConfigurator which sets default console
	 * appender, etc.
	 */
	PropertyConfigurator.configure(System.getProperties());
	PatternLayout layout = new PatternLayout("%r %-5p %x %c{1} [%t]: %m%n");
	BasicConfigurator.configure(new ConsoleAppender(layout));
	//Logger.getRootLogger().setLevel((Level)Level.INFO);


        RelayTest t = new RelayTest();
        t.run();
    }
}
