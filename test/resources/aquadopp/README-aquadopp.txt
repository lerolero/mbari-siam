$Id: README-aquadopp.txt,v 1.1 2008/11/04 22:15:43 bobh Exp $

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Testing procedure for comparing aquadopp output with summary output
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In order to verify that the aquadopp summarizer is correctly generating 
averages, the following steps should be completed:

A. Generate aquadopp output files

   1. The first step is to turn the Aquadopp data stored in the SIAM logs into a 
   format that the Aquapro software can process. The class responsible for this
   is moos.devices.nortek.NortekFileBuilder. There is a script that wraps calls 
   this class; the following example demonstrates its use:
   
       util/buildNortekFile.sh 1474 test/resources/nortek-mse Aquadopp-1474.prf
   
   NOTE: Any binary configuration information form the Aquadopp found in the 
   SIAM logs will be written to the output file (e.g. Aquadopp-1474.prf) in the
   order that it is found in the SIAM logs. It's not clear if the Aquapro 
   software uses any configuration information other than the first one 
   encountered in the PRF file.
   
   2. The PRF file can be converted to text data using the Aquapro software.
   From the Aquapro menu select 'Deployment'->'Data Conversion' and add the PRF 
   file. Pressing done will generate a collection of text files, one for each 
   data type.
   
B. Generate summary data.

   1. There is a java class for generating summary data from a SIAM log file.
   The class, moos.util.SummaryGenerator, can be called as:
   
   utils/buildSummary.sh moos.devices.nortek.Aquadopp 1474 \
       test/resources/aquadopp/nortek-mse \
       amplitude-0 amplitude-1 amplitude-2 amplitude-3 \
       velocity-0 velocity-1 velocity-2 velocity-3 voltage > Aquadopp-1474.xml

