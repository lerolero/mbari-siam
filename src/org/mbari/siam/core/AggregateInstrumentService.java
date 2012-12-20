/**
* @Title AggregateInstrumentServices
* @author Martyn Griffiths
* @version 1.0
* @date 7/18/2003
*
* Copyright MBARI 2003
* 
* REVISION HISTORY:
* $Log: AggregateInstrumentService.java,v $
* Revision 1.2  2009/07/16 15:07:52  headley
* javadoc syntax fixes
*
* Revision 1.1  2008/11/04 22:17:48  bobh
* Initial checkin.
*
* Revision 1.1.1.1  2008/11/04 19:02:04  bobh
* Initial release of SIAM2, the release that was modularized to isolate hardware dependencies, and refactored to make most packages point to org.mbari.siam.
*
* Revision 1.13  2006/06/09 04:18:41  oreilly
* run() throws InterruptedException
*
* Revision 1.12  2006/06/03 19:11:42  oreilly
* extends PolledInstrumentService
*
* Revision 1.11  2006/04/21 04:45:57  headley
* converted System.x.println to log4j
*
* Revision 1.10  2006/03/06 19:43:10  oreilly
* *** empty log message ***
*
* Revision 1.9  2004/10/15 19:44:11  oreilly
* Changed name of framework variables/methods
*
* Revision 1.8  2004/04/16 23:44:38  headley
* changed validateSample to work correctly;
* now uses sendPacket() in acquireSample() instead of appendLog();
* added some debug messages
*
* Revision 1.7  2003/08/20 00:18:09  martyn
* Updated to use postSample() in framework
* Made debug strings in initializeInstrument and loadProperties consistent and more informative.
*
* Revision 1.6  2003/07/25 02:23:33  martyn
* Improved instrumentation
* Now checks timesync flag before setting time
*
* Revision 1.5  2003/07/24 21:18:27  martyn
* Darn stupid error!
*
* Revision 1.4  2003/07/24 02:33:16  martyn
* Cleanup after doing a full checkout and synching up with Kent's property changes
*
* Revision 1.3  2003/07/23 23:17:27  martyn
* Added documentation, removed redundant code.
*
* Close to operational version. Runs reliably on 3 MicroCATs.
*
* Revision 1.2  2003/07/23 02:35:38  martyn
* Changes: added sample validation, added sample concatenation and logging,
*                  improved comms recoverey, added comms stats dump using gp test method
*
* Revision 1.1  2003/07/19 04:23:11  martyn
* AggregateInstrumentService class for instruments with multiple sensors
* For example SeaBird MicroCAT
*
*
* 
*/


package org.mbari.siam.core;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Arrays;

import org.mbari.siam.distributed.Device;
import org.mbari.siam.distributed.InitializeException;
import org.mbari.siam.distributed.InvalidDataException;
import org.mbari.siam.distributed.NoDataException;
import org.mbari.siam.distributed.SensorDataPacket;
import org.mbari.siam.distributed.TimeoutException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;


/**
 * This class serves as a base class for instruments that are supervised by a
 * single controller (eg SeaBird37im.java). The 
 */
public abstract class AggregateInstrumentService 
    extends PolledInstrumentService {

    /** log4j logger */
    static Logger _log4j = Logger.getLogger(AggregateInstrumentService.class);
    
    static final int MAX_SAMPLE_LENGTH = 128;
    
    private int _sampleCount        = 0;
    private int _invalidDataCount   = 0;
    private int _timeoutCount       = 0;
    private int _retryExceededCount = 0;
    private int _badResponseCount   = 0;
    private int _totalSampleCount   = 0;
    private int _totalRetryExceededCount = 0;
    private int _totalInvalidDataCount = 0;
    private int _totalTimeoutCount = 0;
    private int _totalBadResponseCount = 0;

    protected int _numSensors;


    public AggregateInstrumentService() throws RemoteException {
        super();
        resetCommStats();
    }

    /**
     * Abstract method for setting the number of sensors (instruments)
     * <CODE>_numSensors</CODE> supported by sub-class.
     * 
     * @param numSensors Number ofinstruments supported by sub-class
     */
    protected abstract void setNumSensors(int numSensors);


    /**
     * Request a data sample from instrument module.
     * 
     * @param sensorRef reference for sub-class to determine which module to sample
     * 
     * @exception TimeoutException
     * @exception Exception
     */
    protected abstract void requestSample(int sensorRef) throws TimeoutException, Exception;
     


    /**
     * This is overridden but not implemented.
     * 
     * @exception TimeoutException
     * @exception Exception
     */
    protected void requestSample() throws TimeoutException, Exception {
        // Do nothing since we are overriding acquireSample
    }


    /**
     * 
     * 
     * @param logSample
     * 
     * @return sensor data packet
     * @exception NoDataException
     */
    public synchronized SensorDataPacket acquire(boolean logSample) 
	throws NoDataException {

        int maxTries = getMaxSampleTries();
        byte [] sampleBuffer = getSampleBuf();

        if (getStatus() == Device.SUSPEND) {
             throw new NoDataException("service is suspended");
         }
        
         setStatusSampling();

         // turn on communications/power
         managePowerWake();
         
         // instrumentation purposes only
         incSampleCount();

         //prepare device for sampling
         try
         {
             prepareToSample();
         }
         catch(Exception e)
         {
             setStatusError();
             throw new NoDataException(e.getMessage());
         }

         int tries;
         int nBytes;
         int offset = 0;
         byte [] tempBuf = new byte[MAX_SAMPLE_LENGTH];
         boolean thisSampleOK = false;

         Arrays.fill(sampleBuffer,(byte)0);
         for(int sensorRef=0;sensorRef<_numSensors;sensorRef++){
              for (tries = 0; tries < maxTries; tries++) {

                 try {
                     _log4j.debug("Base: calling requestSample(" + sensorRef +")");
                     
                     Arrays.fill(tempBuf,(byte)0);
                     
                     // Send sample request to instrument 
                     requestSample(sensorRef);

                     nBytes = readSample(tempBuf);
                     validateSample(tempBuf,nBytes);
                                    
                     // Concatanate '\r\n' to end of each sample
                     tempBuf[nBytes] = '\r'; tempBuf[nBytes+1] = '\n';
                     // Copy into main aggregate sample buffer
                     System.arraycopy(tempBuf,0,sampleBuffer,offset,nBytes+2);
                     offset += nBytes+2;

                     thisSampleOK = true;
                     break;
                 }
                 catch (TimeoutException e) {
                     _log4j.error(new String(getName()) + 
                                          ".acquireSample(): TimeoutException" + e);
                     incTimeoutCount();
                 }
                 catch (IOException e) {
                     _log4j.error(new String(getName()) + 
                                          ".acquireSample(): IOException" + e);
                     e.printStackTrace();
                 }
                 catch (InvalidDataException e)
                 {
                     _log4j.error(new String(getName()) + 
                                          ".acquireSample(): InvalidDataException" + e);
                     incInvalidDataCount();
                 }
                 catch (Exception e) {
                     _log4j.error(new String(getName()) + 
                                          ".acquireSample(): Exception" + e);
                     e.printStackTrace();
                 }
                 // other Exceptions fly past into the caller
                 Thread.yield();
             }//tries
             if(tries==maxTries) incRetryExceededCount();

         }//modulesRef
         
         if(thisSampleOK){ // If we got at least one sample then system working
             try{
                 SensorDataPacket dataPacket = processSample(sampleBuffer, offset);
                 if (logSample) {
                     logPacket(dataPacket);
                 }
                 // Do any post sampling clean up
                 postSample();
                 // turn off communications power
                 managePowerSleep();
                 setStatusOk();
                 return dataPacket;

             }catch (Exception e){
                 _log4j.error("acquireSample(): postAmble" + e);
             }
          }
         // Here if no data recovered
         setStatusError();
         throw new NoDataException();
    }


    /**
     * set communication statistic counters to 0
     */
    protected void resetCommStats(){
        _sampleCount        = _totalSampleCount = 0;
        _retryExceededCount = _totalRetryExceededCount = 0;
        _invalidDataCount   = _totalInvalidDataCount = 0;
        _timeoutCount       = _totalTimeoutCount = 0;
        _badResponseCount   = _totalBadResponseCount = 0;
    }

    /**
     * called to increment sample counter
     */
    protected void incSampleCount(){
        _sampleCount++;
    }


    /**
     * called to increment comms retry count
     */
    protected void incRetryExceededCount(){
        _retryExceededCount++;
    }

    /**
     * called to increment invalid data count
     */
    protected void incInvalidDataCount(){
        _invalidDataCount++;
    }

    /**
     * called to increment comms timeout count
     */
    protected void incTimeoutCount(){
        _timeoutCount++;
    }

    /**
     * called to increment bad response totalizer
     */
    protected void incBadResponseCount(){
        _badResponseCount++;
    }

    /**
     * Hack - triggers a comms stat dump through RMI
     * 
     * @return n/a
     */
    public int test() {
        _totalSampleCount += _sampleCount;
        _totalInvalidDataCount += _invalidDataCount;
        _totalTimeoutCount += _timeoutCount;
        _totalBadResponseCount += _badResponseCount;
        _totalRetryExceededCount += _retryExceededCount;

        System.out.println("[ Samples:\t\t" + _totalSampleCount + " : " + _sampleCount + " ]");
        System.out.println("[ Invalid data:\t\t" + _totalInvalidDataCount + " : " + _invalidDataCount + " ]");
        System.out.println("[ Timeouts:\t\t" + _totalTimeoutCount + " : " + _timeoutCount + " ]");
        System.out.println("[ Bad responses:\t" + _totalBadResponseCount + " : " + _badResponseCount + " ]");
        System.out.println("[ Retries exceeded:\t" + _totalRetryExceededCount + " : " + _retryExceededCount + " ]");

        _sampleCount        =  0;
        _invalidDataCount   =  0;
        _timeoutCount       =  0;
        _badResponseCount   =  0;
        _retryExceededCount =  0;

        return Device.OK; 
    }

}

