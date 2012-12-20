// Sample.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"
#include "stdlib.h"
#include "math.h"

#define PD_MAX_DEPLOYNAME           6
#define PD_MAX_COMMENTS             180
#define PD_MAX_VELTABLE             90
#define PD_MAX_SERIALNO             14
#define PD_MAX_HEADSERIALNO         12
#define PD_MAX_FWVERSION            4
#define PD_MAX_BEAMS                3
#define PD_MAX_BINS						128

#define PI      3.14159265358979323846
#define DEGTORAD(x)  ((x)/(180.0/PI))


#pragma pack(push)
#pragma pack(1)      // 1 byte struct member alignment

//////////////////////////////////////////////////////////////////////////////
// Clock data (6 bytes)  NOTE! BCD format

typedef struct {
   unsigned char  cMinute;       // minute
   unsigned char  cSecond;       // second
   unsigned char  cDay;          // day
   unsigned char  cHour;         // hour
   unsigned char  cYear;         // year
   unsigned char  cMonth;        // month
} PdClock;


//////////////////////////////////////////////////////////////////////////////
// Instrument configuration (512 bytes)

typedef struct {
   unsigned char  cSync;         // sync = 0xa5
   unsigned char  cId;           // identification = 0x00
   unsigned short hSize;         // total size of structure (words)
   unsigned short hT1;           // transmit pulse length (counts)
   unsigned short hT2;           // blanking distance (counts)
   unsigned short hT3;           // receive length (counts)
   unsigned short hT4;           // time between pings (counts)
   unsigned short hT5;           // time between burst sequences (counts)
   unsigned short nPings;        // number of beam sequences per burst
   unsigned short hAvgInterval;  // average interval in seconds
   unsigned short nBeams;        // number of beams
   unsigned short hTimCtrlReg;   // timing controller mode
                                 //    bit 0:  profile (0=single, 0=continuous)
                                 //    bit 1:  profile (0=single, 1=continuous)
                                 //    bit 2:  mode (0=burst, 0=continuous)      (not used ?)
                                 //    bit 3:  mode (0=burst, 1=continuous)      (not used ?)
                                 //    bit 3:  multiplex transmit beam onto receive beam 3 (0=normal, 1=receive from transmit xducer) (Vectrino)
                                 //    bit 4:  not used
                                 //    bit 5:  power level (0=1, 1=2, 0=3, 1=4)
                                 //    bit 6:  power level (0    0    1    1  )
                                 //    bit 7:  synchout position (0=middle of sample, 1=end of sample (Vector))
                                 //    bit 8:  sample on synch (0=disabled, 1=enabled, rising edge)
                                 //    bit 9:  start on synch (0=disabled, 1=enabled, rising edge)
                                 //    bit 10: synch master (0=slave, 1=master)
                                 //    bit 11: not used
                                 //    bit 12: not used
                                 //    bit 13: not used
                                 //    bit 14: power level ping2 (0=1, 1=2, 0=3, 1=4)
                                 //    bit 15: power level ping2 (0    0    1    1  )
   unsigned short hPwrCtrlReg;   // power control register
                                 //    bit 0:  xmit voltage (0=low, 1=high) (Vectrino)
                                 //    bit 1:  xmit voltage distance (0=low, 1=high) (Vectrino)
                                 //    bit 2:  not used
                                 //    bit 3:  not used
                                 //    bit 4:  not used
                                 //    bit 5:  wave power level (0=1, 1=2, 0=3, 1=4) / range profile (Aquapro)
                                 //    bit 6:  wave power level (0    0    1    1  ) / distance power level (Vectrino)
                                 //    bit 7:  not used
   unsigned short hSpare1[3];    // not used
   unsigned short hCompassUpdateRate;  // compass update rate
   unsigned short hCoordSystem;  // coordinate system (0=ENU, 1=XYZ, 2=BEAM)
   unsigned short nBins;         // number of bins (cells) (Aquadopp = 1 cell)
   unsigned short hBinLength;    // cell size
   unsigned short hMeasInterval; // measurement interval
   char           cDeployName[PD_MAX_DEPLOYNAME];// recorder deployment name (not null terminated)
   unsigned short hWrapMode;     // recorder wrap mode (0=NO WRAP, 1=WRAP WHEN FULL)
   PdClock        clockDeploy;   // date and time
   unsigned long  lDiagInterval; // number of seconds between diagnostics measurements
   unsigned short hMode;         // mode:
                                 //    bit 0: use user specified sound speed (0=no, 1=yes)
                                 //    bit 1: diagnostics/wave mode (0=disable, 1=enable)
                                 //    bit 2: analog output mode (0=disable, 1=enable)
                                 //    bit 3: output format (0=Vector, 1=ADV)
                                 //    bit 4: scaling (0=1 mm, 1=0.1 mm)
                                 //    bit 5: serial output (0=disable, 1=enable)
                                 //    bit 6: reserved EasyQ
                                 //    bit 7: stage (0=disable, 1=enable) and distance output (Aquapro)
                                 //    bit 8: output power for analog input (0=disable, 1=enable)
                                 //    bit 9: serial CT sensor (0=disable, 1=enable)
                                 //    bit 10: override  receiver DACs Vectrino (0=disable, 1=enable)
                                 //    bit 11: gain linearisation Vectrino (0=disable, 1=enable)
                                 //    bit 12: DAC override values interpreted as (0=tenths of dB, 1=counts)
                                 //    bit 13: not used // distance meas in probe check Vectrino (0=disable, 1=enable)
                                 //    bit 14: gain linearisation distance Vectrino (0=disable, 1=enable)
                                 //    bit 15: output distance data in probe check Vectrino (0=disable, 1=enable)
                                 //    bit 15: read current value Continental (0=no, 1=yes)
   unsigned short hAdjSoundSpeed;// user input sound speed adjustment factor
   unsigned short nSampDiag;     // number of samples (AI if EasyQ) in diagnostics mode
   unsigned short nBeamsCellDiag;// number of beams / cell number to measure in diagnostics mode
   unsigned short nPingsDiag;    // number of pings in diagnostics/wave mode
   unsigned short hModeTest;     // mode test:
                                 //    bit 0: correct using DSP filter (0=no filter, 1=filter)
                                 //    bit 1: filter data output (0=total corrected velocity, 1=only correction part)
                                 //    bit 2: not used
                                 //    bit 3: not used
                                 //    bit 4: amp out 1 (00=not used, 01=amp1, 10=amp2, 11=avg)
                                 //    bit 5: amp out 1
                                 //    bit 6: amp out 2 (00=comb, 01=amp1, 10=amp2, 11=avg)
                                 //    bit 7: amp out 2
   unsigned short hAnaInAddr;    // analog input address   
   unsigned short hSWVersion;    // software version (ex: 12005 = 1.20 Beta5)
   unsigned short hSalinity;     // salinity  (0.1 ppt)
   unsigned short hVelAdjTable[PD_MAX_VELTABLE];
   char           cComments[PD_MAX_COMMENTS];// file comments 3 * 60 characters
   union {
      struct {
         unsigned short hPhaseToVel1;  // Phase to velocity for first lag
         unsigned short hUa1;          //
         unsigned short hUah1;         //
         unsigned short hPhaseToVel2;  // Phase to velocity for second lag
         unsigned short hUa2;          //
         unsigned short hUah2;         //
         unsigned short hLag1;         //
         unsigned short hLag2;         //
         unsigned short nMeas;         // 
         unsigned short nPingMiddle;   // 
      } Lag;                           // lag configuration (Vector / Vectrino)
      struct {
         unsigned short hMode;         // wave measurement mode
                                       //    bit 0: data rate (0=1 Hz, 1=2 Hz)
                                       //    bit 1: wave cell position (0=fixed, 1=dynamic)
                                       //    bit 2: type of dynamic position (0=pct of mean pressure, 1=pct of min pressusre)
                                       //    bit 3: peak detection (0=off, 1=on)
                                       //    bit 4: SUV (0=off, 1=on)
         unsigned short hDynPercPos;   // percentage for wave cell positioning (=32767*(%val)/100)
         unsigned short hT1;           // wave transmit pulse
         unsigned short hT2;           // fixed wave blanking distance (counts)
         unsigned short hT3;           // wave measurement cell size
         unsigned short nSamp;         // 
         unsigned short hA1;           // filter / AST quality threshold
         unsigned short hB0;           // filter / AST number of cells
         unsigned short hB1;           // filter
         unsigned short hSpare;        // 
      } Wave;
      struct {
         unsigned short hSpare1[6];    //
         unsigned short hQualThres;    // quality threshold (counts)
         unsigned short hT1;           // beam 4 transmit pulse length (counts)
         unsigned short hSpare2[2];    //
      } Distance;
   } u;
   unsigned short hAnaOutScale;        // analog output scale factor (16384=1.0, max=4.0)
   unsigned short hCorrThresh;         // correlation threshold for resolving ambiguities
   unsigned short hT1Dist;             // transmit pulse length (counts) distance Vectrino
   unsigned short hT1Lag2;             // transmit pulse length (counts) second lag
   unsigned short hStartCmd;           // start command
   unsigned short hSwFlags;            // used by software to flag conditions
                                       //    bit 0: CRC recorder download (0=off, 1=on)
                                       //    bit 1: recorder download (0=dump, 1=file)
                                       //    bit 2: orientation (0=up, 1=down)	// HR-AQP
   unsigned short hRxConst[4];         // override receiver DAC 1-4 
   unsigned short hSpare2[5];
   short          hQualConst[12];      // match filter constants (AQP/EZQ) NOTE: use 4..11 for EasyQ stage
   short          hChecksum;
} PdUserConf;



//////////////////////////////////////////////////////////////////////////////
// Head configuration (224 bytes + 2 byte(checksum))

typedef struct {
   unsigned char  cSync;         // sync = 0xa5
   unsigned char  cId;           // identification = 0x04
   unsigned short hSize;         // total size of structure (words)
   unsigned short hConfig;       // head configuration:
                                 //    bit 0: Pressure sensor (0=no, 1=yes)
                                 //    bit 1: Magnetometer sensor (0=no, 1=yes)
                                 //    bit 2: Tilt sensor (0=no, 1=yes)
                                 //    bit 3: Tilt sensor mounting orientation (0=up, 1=down)
   unsigned short hFrequency;    // head frequency [kHz]
   unsigned short hType;         // head type: 0 - Normal 3D Aquadopp head
   char  acSerialNo[PD_MAX_HEADSERIALNO];          // head serial number
   short hBeamAngles[4];         // beam angles for beam 1, 2, 3, 4 [-32768, 32767>  => [-pi, pi> = [-180, 180>
   short hBeamToXYZ[9];          // beam to XYZ transformation matrix for up orientation
   short hTiltCalibDown[8];      // calibration constants for tilt sensor
   unsigned short hSpare1;
   short hCompAlignUp[9];        // transforms the raw compass magnetic vector to the correct coordinate for the up orientation
   short hCompAlignDown[9];      // transforms the raw compass magnetic vector to the correct coordinate for the down orientation
   short hTiltAlignUp[4];        // transforms the pitch and roll value for the tilt sensor for the up orientation
   short hTiltAlignDown[4];      // transforms the pitch and roll value for the tilt sensor for the down orientation
   unsigned short hPressCalib[4];// calibration constants for pressure sensor
   short hTempCalib[4];          // calibration constants for temperature sensor
   short hTiltCalibUp[8];        // calibration constants for tilt sensor
   short hCompCalib[16];         // calibration constants for compass sensor
   short hDatum[4];              // datum correction values
   short hTiltRange;             // range for tilt sensor
   unsigned short hTiltScale;    // scaling value for tilt sensor
   unsigned short hCompScale;    // scaling value for compass
   unsigned short hTempScale;    // scaling value for temperature
   unsigned short hSpare2[11];   // spare values
   unsigned short nBeams;        // number of beams
   short hChecksum;              // checksum
} PdHeadConf;


//////////////////////////////////////////////////////////////////////////////
// Hardware production configuration (48 bytes)

typedef struct {
   unsigned char  cSync;         // sync = 0xa5
   unsigned char  cId;           // identification = 0x05 (0xfd if in FW upgrade mode)
   unsigned short hSize;         // total size of structure (words)
   char acSerialNo[PD_MAX_SERIALNO];    // instrument type and serial number
   unsigned short hConfig;       // board configuration:
                                 //    bit 0: Recorder installed (0=no, 1=yes)
                                 //    bit 1: Compass installed (0=no, 1=yes)
                                 //    bit 2: New compass (0=no, 1=yes)
   unsigned short hFrequency;    // board frequency [kHz]
   unsigned short hPICversion;   // PIC code version number
   unsigned short hHWrevision;   // Hardware revision
   unsigned short hRecSize;      // Recorder size (65536 bytes)
   unsigned short hSpare[7];     // spare values
   char  cFWversion[PD_MAX_FWVERSION]; // firmware version
   short hChecksum;              // checksum
} PdProdConf;



//////////////////////////////////////////////////////////////////////////////
// HR Aquadopp profile data (32 + f(nBeams) bytes)
typedef struct {
   unsigned char  cSync;         // sync = 0xa5
   unsigned char  cId;           // identification (0x21 = 3 beams, 0x22 = 2 beams, 0x21 = 1 beam)
   unsigned short hSize;         // size of structure (words)
   PdClock        clock;         // date and time
   unsigned short hMilliseconds; // milliseconds
   short          hError;        // error code
   unsigned short hBattery;      // battery voltage (0.1 V)  
   unsigned short hSoundSpeed;   // speed of sound (0.1 m/s)
   short          hHeading;      // compass heading (0.1 deg)
   short          hPitch;        // compass pitch (0.1 deg)
   short          hRoll;         // compass roll (0.1 deg)
   union {
      struct {
         char     cFill;
         char     cStatus;       // status
      } Status;
      struct {
         unsigned char cMSB;     // pressure MSB
         char     cFill;
         unsigned short hLSW;    // pressure LSW
      } Pressure;                // (mm)
   } u1;
   short          hTemperature;  // temperature (0.01 deg C)
   unsigned short hAnaIn1;       // analog input 1
   unsigned short hAnaIn2;       // analog input 2
   short          hSpare[10];
   // actual size of the following = nBeams*nBins*3 + 2
   short          hVel[PD_MAX_BEAMS][PD_MAX_BINS]; // short hVel[nBeams][nCells];   // velocity 
   unsigned char  cAmp[PD_MAX_BEAMS][PD_MAX_BINS]; // char  cAmp[nBeams][nCells];   // amplitude
   unsigned char  cCorr[PD_MAX_BEAMS][PD_MAX_BINS];// char  cCorr[nBeams][nCells];  // correlation  (0-100)
                                                   // char  cFill           // if nCells % 2 != 0
   short          hChecksum;     // checksum
} PdHrProf;


#pragma pack(pop)




int main(int argc, char* argv[])
{
   FILE *pf = fopen(argv[1], "rb");


	// Header
   PdProdConf prod;
   PdUserConf conf;
   PdHeadConf head;

   fread(&prod,sizeof(char),sizeof(PdProdConf),pf);
   fread(&head,sizeof(char),sizeof(PdHeadConf),pf);
   fread(&conf,sizeof(char),sizeof(PdUserConf),pf);

   printf("\nBoard serial number ------ %-14.14s", prod.acSerialNo);
   printf("\nBoard frequency (kHz) ---- %d",       prod.hFrequency);
   printf("\nRecorder size (Mb) ------- %d",       prod.hRecSize*65536/1024/1024);
   printf("\nFirmware version --------- %-4.4s",   prod.cFWversion);

   printf("\nHead frequency (kHz) ----- %d",       head.hFrequency);
   printf("\nHead serial number ------- %-12.12s", head.acSerialNo);
   printf("\nTransformation matrix ---- %8.4f %8.4f %8.4f", head.hBeamToXYZ[0]/4096.0,head.hBeamToXYZ[1]/4096.0,head.hBeamToXYZ[2]/4096.0);
   printf("\nTransformation matrix ---- %8.4f %8.4f %8.4f", head.hBeamToXYZ[3]/4096.0,head.hBeamToXYZ[4]/4096.0,head.hBeamToXYZ[5]/4096.0);
   printf("\nTransformation matrix ---- %8.4f %8.4f %8.4f", head.hBeamToXYZ[6]/4096.0,head.hBeamToXYZ[7]/4096.0,head.hBeamToXYZ[8]/4096.0);
   printf("\nNo. of head beams -------- %d",       head.nBeams);

   // Aquadopp Profiler HR
   printf("\nProfile interval (s) ----- %d",       conf.hMeasInterval);
   printf("\nCell size (m) ------------ %.2f",     cos(DEGTORAD(25.0))*0.186767*conf.hBinLength/head.hFrequency);
   printf("\nNo. of cells ------------- %d",       conf.nBins);
	if (conf.hSwFlags & 0x0004)							printf("\nOrientation -------------- DOWNLOOKING");
   else                                            printf("\nOrientation -------------- UPLOOKING");
   printf("\nAverage interval (s) ----- %d",       conf.hAvgInterval);
   printf("\nBlanking distance (m) ---- %.2f",     cos(DEGTORAD(25.0))*(0.022888*conf.hT2 - 12.0*conf.hT1/head.hFrequency));
   printf("\nCompass update rate (s) -- %d",       conf.hCompassUpdateRate);

   if (conf.u.Lag.nMeas == 0) 							printf("\nBurst sampling ----------- DISABLED");
	else															printf("\nBurst sampling ----------- ENABLED");
   printf("\nNo. of samples ----------- %d",       conf.u.Lag.nMeas);
   printf("\nSampling rate ------------ %d",       512/conf.hT5);

   if      (conf.hCoordSystem == 0)                printf("\nCoordinate system -------- ENU");
   else if (conf.hCoordSystem == 0)                printf("\nCoordinate system -------- XYZ");
   else                                            printf("\nCoordinate system -------- BEAM");
   if (conf.hTimCtrlReg ^ 0x060 == 0)              printf("\nPower level -------------- LOW (4)");
   else if (conf.hTimCtrlReg ^ 0x040 == 0)         printf("\nPower level -------------- LOW+ (3)");
   else if (conf.hTimCtrlReg ^ 0x020 == 0)         printf("\nPower level -------------- HIGH- (2)");
   else                                            printf("\nPower level -------------- HIGH (1)");

	if (conf.hWrapMode == 1)                        printf("\nFile wrapping ------------ ENABLED");
   else                                            printf("\nFile wrapping ------------ DISABLED");
	if (conf.hMode & 0x0001)                        printf("\nSpeed of sound ----------- FIXED");
   else                                            printf("\nSpeed of sound ----------- MEASURED");
   printf("\nSpeed of sound (m/s) ----- %.1f",     1500.0*conf.hAdjSoundSpeed/16384.0);
   printf("\nSalinity (ppt) ----------- %.1f",     0.1*conf.hSalinity);

   if      ((conf.hAnaInAddr        & 0x000f) == 1) printf("\nAnalog input 1 ----------- PROFILE");
   else if (((conf.hAnaInAddr >> 4) & 0x000f) == 1) printf("\nAnalog input 1 ----------- PROFILE");
   else                                             printf("\nAnalog input 1 ----------- NONE");
   if      ((conf.hAnaInAddr        & 0x000f) == 2) printf("\nAnalog input 2 ----------- PROFILE");
   else if (((conf.hAnaInAddr >> 4) & 0x000f) == 2) printf("\nAnalog input 2 ----------- PROFILE");
   else                                             printf("\nAnalog input 2 ----------- NONE");

   printf("\nDeployment time ---------- %2.2x.%2.2x.%2.2x %2.2x:%2.2x:%2.2x",
      conf.clockDeploy.cDay,
      conf.clockDeploy.cMonth,
      conf.clockDeploy.cYear,
      conf.clockDeploy.cHour,
      conf.clockDeploy.cMinute,
      conf.clockDeploy.cSecond);
   printf("\nDeployment name ---------- %-6.6s",    conf.cDeployName);
   printf("\nComments ----------------- %-45.45s",  &conf.cComments[0]);
   printf("\nComments ----------------- %-45.45s",  &conf.cComments[45]);
   printf("\nComments ----------------- %-45.45s",  &conf.cComments[90]);
   printf("\nComments ----------------- %-45.45s",  &conf.cComments[135]);


	// Data (first measurement)
   PdHrProf prof;

   fread(&prof,sizeof(char),sizeof(PdHrProf),pf);


   // Sensor data
   double dPressure = (65536.0*(double)prof.u1.Pressure.cMSB + (double)prof.u1.Pressure.hLSW);

   printf("\n\nMeasurement time --------- %2.2x.%2.2x.%2.2x %2.2x:%2.2x:%2.2x",
      prof.clock.cMonth,
      prof.clock.cDay,
      prof.clock.cYear,
      prof.clock.cHour,
      prof.clock.cMinute,
      prof.clock.cSecond);
   printf("\nError code --------------- %d",		(unsigned char)prof.hError);
   printf("\nStatus code -------------- %d",    (unsigned char)prof.u1.Status.cStatus);
   printf("\nBattery voltage (V) ------ %.1f",  (double)prof.hBattery * 0.1);
   printf("\nSound speed (m/s) -------- %.1f",  (double)prof.hSoundSpeed * 0.1);
   printf("\nHeading (deg) ------------ %.1f",  (double)prof.hHeading * 0.1);
   printf("\nPitch (deg) -------------- %.1f",  (double)prof.hPitch * 0.1);
   printf("\nRoll (deg) --------------- %.1f",  (double)prof.hRoll * 0.1);
   printf("\nPressure (m) ------------- %.3f",  dPressure * 0.001);
   printf("\nTemperature (degC) ------- %.2f",  (double)prof.hTemperature * 0.01);
   printf("\nAnalog in #1 (counts) ---- %d",    prof.hAnaIn1);
   printf("\nAnalog in #2 (counts) ---- %d",    prof.hAnaIn2);

   // Velocity data
   int i,j,k;
	int nCells = conf.nBins;
	int nBeams = conf.nBeams;
   double dVelScale = (conf.hMode & 0x0010) ? 0.0001 : 0.001;

   short *phVel = (short *)&prof.hVel[0][0];
   unsigned char *pcAmp = (unsigned char *)phVel + nBeams * nCells * sizeof(short);
   unsigned char *pcCorr = (unsigned char *)pcAmp + nBeams * nCells * sizeof(char);

   printf("\n\n");
   for (i=0; i<nBeams; i++) {
      for (j=0; j<nCells; j++) {
         k = i*nCells + j;
         if (j == 0)
            printf("Vel (m/s) %d: %8.3f", i+1, (double)phVel[k]*dVelScale);
         else
            printf(" %8.3f",(double)phVel[k]*dVelScale);
      }
      printf("\n");
   }
   for (i=0; i<nBeams; i++) {
      for (j=0; j<nCells; j++) {
         k = i*nCells + j;
         if (j == 0)
            printf("Amp (counts) %d: %5d", i+1, pcAmp[k]);
         else
            printf(" %5d",pcAmp[k]);
      }
		printf("\n");
   }
   for (i=0; i<nBeams; i++) {
      for (j=0; j<nCells; j++) {
         k = i*nCells + j;
         if (j == 0)
            printf("Corr (%%) %d %5d", i+1, pcCorr[k]);
         else
            printf(" %5d",pcCorr[k]);
      }
		printf("\n");
   }

	return 0;
}

