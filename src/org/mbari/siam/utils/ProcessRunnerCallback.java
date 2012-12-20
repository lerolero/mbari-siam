/** 
* @Title ProcessRunnerCallback
* @author Bob Herlien
* @version $Revision: 1.1 $
* @date $Date: 2008/11/04 22:17:53 $
*
* Copyright MBARI 2004
* 
* REVISION HISTORY:
* $Log: ProcessRunnerCallback.java,v $
* Revision 1.1  2008/11/04 22:17:53  bobh
* Initial checkin.
*
* Revision 1.1.1.1  2008/11/04 19:02:05  bobh
* Initial release of SIAM2, the release that was modularized to isolate hardware dependencies, and refactored to make most packages point to org.mbari.siam.
*
* Revision 1.1  2004/02/20 21:36:43  bobh
* Added ProcessRunner and ProcessRunnerCallback
*
*/
package org.mbari.siam.utils;

/**
 * ProcessRunnerCallback provides the interface to allow the parent thread to
 * get a callback when the ProcessRunner (and hence the exec'd process) has
 * completed.
 */
public interface ProcessRunnerCallback
{
    /** Value for exitVal if process is not yet finished.  This should
	never happen, as the callback is supposed to happen after
	process completion */
    static final int NOT_COMPLETE = -2;

    /** Called when ProcessRunner has completed */
    public void processCompleted(int exitVal);

} /* class ProcessRunnerCallback */
