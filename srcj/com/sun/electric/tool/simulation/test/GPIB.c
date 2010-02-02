/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GPIB.c
 *
 * Copyright (c) 2009 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
// basic lib
#include <stdio.h>

// for jni to work
#include <jni.h>

// native function declaration 
#include "com_sun_electric_tool_simulation_test_GPIB.h"
#include "ugpib.h"

// misc definition
#define TRUE 1
#define FALSE 0

/*
   There is no equivalent Java primitive for unsigned variable such as unsigned int.
   Care must be taken if signed Java variable and unsigned C variable are used simultaneously.
*/
JNIEXPORT jint JNICALL Java_com_sun_electric_tool_simulation_test_GPIB_ibwrt
  (JNIEnv *penv, jclass cls, jint ud, jbyteArray data, jint length, jintArray err)
{
  unsigned int ret;
   
  //variable preparation phase
  jbyte *dataP = (*penv)->GetByteArrayElements(penv, data, 0);
  jint *perr = (*penv)->GetIntArrayElements(penv, err, 0);

  //excution phase
  ret = ibwrt(ud, dataP, length);
  *perr = iberr;

  //release  phase
  (*penv)->ReleaseByteArrayElements(penv, data, dataP, 0);
  (*penv)->ReleaseIntArrayElements(penv, err, perr, 0);

  return ret; 
}

JNIEXPORT jint JNICALL Java_com_sun_electric_tool_simulation_test_GPIB_ibrd
  (JNIEnv *penv, jclass cls, jint ud, jbyteArray data, jint length, jintArray err)
{
  unsigned int ret;
   
  //variable preparation phase
  jbyte *dataP = (*penv)->GetByteArrayElements(penv, data, 0);
  jint *perr = (*penv)->GetIntArrayElements(penv, err, 0);


  //excution phase
  ret = ibrd(ud, dataP, length);
  *perr = iberr;

  //release  phase
  (*penv)->ReleaseByteArrayElements(penv, data, dataP, 0);
  (*penv)->ReleaseIntArrayElements(penv, err, perr, 0);

  return ret; 
}

JNIEXPORT jint JNICALL Java_com_sun_electric_tool_simulation_test_GPIB_ibfind
  (JNIEnv *penv, jclass cls, jbyteArray name, jintArray stat_err)
{
  unsigned int ret;
  int i; 

  //variable preparation phase
  jbyte *nameP = (*penv)->GetByteArrayElements(penv, name, 0);
  jint *pstat_err = (*penv)->GetIntArrayElements(penv, stat_err, 0);

  //excution phase
  ret = ibfind(nameP);
  pstat_err[0] = ibsta;
  pstat_err[1] = iberr;

  //release  phase
  (*penv)->ReleaseByteArrayElements(penv, name, nameP, 0);
  (*penv)->ReleaseIntArrayElements(penv, stat_err, pstat_err, 0);

  return ret; 
}

JNIEXPORT jint JNICALL Java_com_sun_electric_tool_simulation_test_GPIB_ibask
  (JNIEnv *penv, jclass cls, jint ud, jint option, jintArray value, jintArray err)
{
  int ret;
  
  //variable preparation phase
  jint *pvalue = (*penv)->GetIntArrayElements(penv, value, 0);
  jint *perr = (*penv)->GetIntArrayElements(penv, err, 0);
   
  //excution phase
  ret = ibask(ud, option, pvalue);
  *perr = iberr;
  //printf("ret = %d, value=%d, err=%d\n", ret, *pvalue, *perr);
  
  // release phase
  (*penv)->ReleaseIntArrayElements(penv, value, pvalue, 0);
  (*penv)->ReleaseIntArrayElements(penv, err, perr, 0);

  return ret;
}

/*
 * Class:     com_sun_electric_tool_simulation_test_GPIB
 * Method:    ibclr
 * Signature: (I[I)I
 */
JNIEXPORT jint JNICALL Java_com_sun_electric_tool_simulation_test_GPIB_ibclr
  (JNIEnv *penv, jclass cls, jint ud, jintArray err) {
  unsigned int ret;

  //variable preparation phase
  jint *perr = (*penv)->GetIntArrayElements(penv, err, 0);
  
  //excution phase
  ret = ibclr(ud);
  *perr = iberr;
  
  // release phase
  (*penv)->ReleaseIntArrayElements(penv, err, perr, 0);
  
  return ret;
}

/*
 * Class:     com_sun_electric_tool_simulation_test_GPIB
 * Method:    ibcmd
 * Signature: (I[BJ[I)I
 */
JNIEXPORT jint JNICALL Java_com_sun_electric_tool_simulation_test_GPIB_ibcmd
  (JNIEnv *penv, jclass cls, jint ud, jbyteArray cmd, jlong cnt, jintArray err) {
  unsigned int ret;
   
  //variable preparation phase
  jbyte *dataP = (*penv)->GetByteArrayElements(penv, cmd, 0);
  jint *perr = (*penv)->GetIntArrayElements(penv, err, 0);

  //excution phase
  ret = ibcmd(ud, dataP, cnt);
  *perr = iberr;

  //release  phase
  (*penv)->ReleaseByteArrayElements(penv, cmd, dataP, 0);
  (*penv)->ReleaseIntArrayElements(penv, err, perr, 0);

  return ret; 
}


