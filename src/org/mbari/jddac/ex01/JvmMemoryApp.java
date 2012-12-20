/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
/*
 * JvmMemoryApp.java
 *
 * Created on March 24, 2006, 10:58 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.mbari.jddac.ex01;

import net.java.jddac.common.fblock.Entity;
import net.java.jddac.common.type.ArgArray;
import net.java.jddac.common.type.TimeRepresentation;
import net.java.jddac.common.meas.Measurement;
import net.java.jddac.common.meas.MeasAttr;
import org.apache.log4j.Logger;
import org.mbari.jddac.*;

import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author brian
 */
public class JvmMemoryApp {

    public final String OpIdLog = "Log";
    public final String OpIdRelay = "Relay";
    public final String OpIdCalc = Entity.PerformResult;
    public final String OpIdEnd = "End";

    private static final Logger log = Logger.getLogger(JvmMemoryApp.class);

    private Timer timer = new Timer();

    private RelayBlock relayBlock = new RelayBlock();


    /**
     * Creates a new instance of JvmMemoryApp
     */
    public JvmMemoryApp() {
        init();
    }

    private void init() {

        /*
        * Commands chain:
        *
        * OpIdRelay->[RelayFunction]->OpIdCalc->[MeanStatsFunction]->OpIdLog->[Log4jFunction]
        *                  |
        *                  |
        *                  `->AggregationBlock.OpIdAddArray->[AggregationBlock]
        *
        * FBlocks:
        *                [SampleCountFilter]
        *                         |
        *                         |
        * [RelayBlock]-->[AggregationBlock]-->[RelayBlock]
        *      |                   |                 |
        *      V                   |                 V
        * [RelayFunction]  [MeanStatsFunction]  [Log4jFunction]
        */

        /*
        * The RelayBlock is the start of the funciton chain. This fblock does 2 things when it obtains a new
        * Measurement: 1) It passes values to be stored in the AggregationBlock 2) It triggers the MeanStatsFunction
        * in the aggregation block.
        */
        IFunction function = new RelayFunction();
        ArgArray f1 = FunctionFactory.createFunctionArg(OpIdRelay, OpIdCalc, function);
        ArgArray f4 = FunctionFactory.createFunctionArg(OpIdRelay, AggregationBlock.OpIdAddArgArray, function);
        relayBlock.addFunction(f1);   // This is equivalent to relayBlock.process(RelayBlock.OpIdAddFunction, f1)
        relayBlock.addFunction(f4);

        /*
        * Add an aggregation block that that averages the last 10 samples.
        */
        AggregationBlock meanBlock = new AggregationBlock();
        meanBlock.addFilter(new SampleCountFilter(meanBlock, 10));
        ArgArray f2 = FunctionFactory.createFunctionArg(OpIdCalc, OpIdLog, new MeanStatsFunction(meanBlock));
        meanBlock.addFunction(f2);
        relayBlock.addChild(meanBlock);

        /*
        * Add a block to log the results.
        */
        RelayBlock logBlock = new RelayBlock();
        meanBlock.addChild(logBlock);
        ArgArray f3 = FunctionFactory.createFunctionArg(OpIdLog, OpIdEnd, new Log4jFunction("JVMMonitor"));
        logBlock.addFunction(f3);


        /*
        * Create a thread to sample at a regular interval
        */
        TimerTask timerTask = new TimerTask() {
            public void run() {
                Measurement m = read();
                try {
                    relayBlock.perform(OpIdRelay, m);
                } catch (Exception e) {
                    log.error("Read failed!!", e);
                }
            }
        };
        timer.scheduleAtFixedRate(timerTask, 0, 1000);
    }


    private Measurement read() {
        TimeRepresentation timestamp = new TimeRepresentation();
        Measurement m = new Measurement();
        m.put(MeasAttr.TIMESTAMP, timestamp);
        m.put(MeasAttr.SHORT_NAME, "JVM Free Memory");
        m.put(MeasAttr.UNITS, "Bytes");
        m.put(MeasAttr.VALUE, new Long(Runtime.getRuntime().freeMemory()));
        return m;
    }



    public static void main(String[] args) {
        new JvmMemoryApp();
    }



}
