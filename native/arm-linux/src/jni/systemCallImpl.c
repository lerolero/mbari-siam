/*
Copyright 2013 MBARI, all rights reserved. 
For license and copyright details, see COPYRIGHT.TXT in the SIAM project
home directory.
*/
#include <jni.h>
#include "moos_utils_SystemCall.h"
#include <stdio.h>
#include <stdlib.h>

JNIEXPORT jint JNICALL
Java_moos_utils_SystemCall_call (JNIEnv *env, jobject obj, jstring callStr)
{
  int	rtn;

  const char *str = (*env)->GetStringUTFChars(env, callStr, 0);
//  printf("SystemCallImpl: calling %s\n", str);
  rtn= system(str);
  (*env)->ReleaseStringUTFChars(env, callStr, str);
  return(rtn);
}

