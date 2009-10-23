// basic lib
#include <stdio.h>

// for jni to work
#include <jni.h>

// native function declaration 
#include "com_sun_electric_tool_simulation_test_NetscanJNI.h"

// for netscan library to work
#include "netesfl.h"

// misc definition
#define TRUE 1
#define FALSE 0

/*
   There is no equivalent Java primitive for unsigned variable such as unsigned int.
   Care must be taken if signed Java variable and unsigned C variable are used simultaneously.
*/

JNIEXPORT jint JNICALL Java_com_sun_electric_tool_simulation_test_NetscanJNI_net_1connect
  (JNIEnv *env, jclass cls, jstring jstr)
{
  unsigned int hsock;
  const char *str;
   
  //variable preparation phase
  str = (*env)->GetStringUTFChars(env, jstr, 0);

  //excution phase
  hsock = Net_Connect( str);

  //release  phase
  (*env)->ReleaseStringUTFChars(env, jstr, str);

  return hsock; 
}

JNIEXPORT jint JNICALL Java_com_sun_electric_tool_simulation_test_NetscanJNI_net_1configure
  (JNIEnv *env, jclass cls, jlong kHz, jshort stop_state, jint mV)
{
  return Net_Configure(kHz, stop_state, mV);
}

JNIEXPORT jint JNICALL Java_com_sun_electric_tool_simulation_test_NetscanJNI_net_1set_1trst
  (JNIEnv *env, jclass cls, jint signal)
{
  return Net_Set_TRST(signal);
}

JNIEXPORT jint JNICALL Java_com_sun_electric_tool_simulation_test_NetscanJNI_net_1disconnect
  (JNIEnv *env, jclass cls)
{
  return Net_Disconnect();
}

#undef INCLUDE_UNUSED_FUNCTIONS
#ifdef INCLUDE_UNUSED_FUNCTIONS

JNIEXPORT jint JNICALL Java_com_sun_electric_tool_simulation_test_NetscanJNI_net_1tms_1reset
  (JNIEnv *env, jclass cls)
{
    int result;
    result = Net_TMS_Reset();
    return result;
}

JNIEXPORT jint JNICALL Java_com_sun_electric_tool_simulation_test_NetscanJNI_net_1move_1to_1state
  (JNIEnv *env, jclass cls, jint state)
{
    int result;
    result = Net_Move_to_State(state);
    return result;
}

#endif

JNIEXPORT jint JNICALL Java_com_sun_electric_tool_simulation_test_NetscanJNI_net_1scan_1ir
  (JNIEnv *env, jclass cls, jshortArray scanIn, jlong bitLength, jshortArray scanOut)
{
    //variable preparation phase
    jshort *scanInP = (*env)->GetShortArrayElements(env, scanIn, 0);
    jshort *scanOutP = (*env)->GetShortArrayElements(env, scanOut, 0);
    int result;

    //excution phase
    result = Net_Scan_IR(scanInP, bitLength, scanOutP);

    //release  phase
    (*env)->ReleaseShortArrayElements(env, scanIn, scanInP, 0);
    (*env)->ReleaseShortArrayElements(env, scanOut, scanOutP, 0);
    return result;
}

JNIEXPORT jint JNICALL Java_com_sun_electric_tool_simulation_test_NetscanJNI_net_1scan_1dr
  (JNIEnv *env, jclass cls, jshortArray scanIn, jlong bitLength, jshortArray scanOut)
{
    //variable preparation phase
    jshort *scanInP = (*env)->GetShortArrayElements(env, scanIn, 0);
    jshort *scanOutP = (*env)->GetShortArrayElements(env, scanOut, 0);
    int result;

    //excution phase
    result = Net_Scan_DR(scanInP, bitLength, scanOutP);

    //release  phase
    (*env)->ReleaseShortArrayElements(env, scanIn, scanInP, 0);
    (*env)->ReleaseShortArrayElements(env, scanOut, scanOutP, 0);
    return result;
}

JNIEXPORT jint JNICALL Java_com_sun_electric_tool_simulation_test_NetscanJNI_net_1set_1parallel_1io
  (JNIEnv *env, jclass cls, jshort port_data)
{
  return Net_Set_Parallel_IO(port_data);
}
