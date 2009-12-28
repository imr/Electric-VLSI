// basic lib
#include <stdio.h>

// for jni to work
#include <jni.h>

// native function declaration 
#include "com_sun_electric_tool_simulation_test_Netscan4JNI.h"

// for netscan library to work
#include "NetUSB.h"

// misc definition
#define TRUE 1
#define FALSE 0

/*
   There is no equivalent Java primitive for unsigned variable such as unsigned int.
   Care must be taken if signed Java variable and unsigned C variable are used simultaneously.
*/

JNIEXPORT jint JNICALL Java_com_sun_electric_tool_simulation_test_Netscan4JNI_netUSB_1Connect
  (JNIEnv *env, jclass cls, jstring jstr)
{
  unsigned int hsock;
  char *str;

  //variable preparation phase
  str = (char *)((*env)->GetStringUTFChars(env, jstr, 0));

  //excution phase
  hsock = NetUSB_Connect(str);
  
  //release  phase
  (*env)->ReleaseStringUTFChars(env, jstr, str);

  return hsock; 
}

JNIEXPORT jint JNICALL Java_com_sun_electric_tool_simulation_test_Netscan4JNI_netUSB_1hard_1reset
  (JNIEnv *env, jclass cls, jlong kHz, jint mV)
{
    int hrresult;
    hrresult = NetUSB_hard_reset(mV, SLEW_SLOW);
    /* printf("hrresult=%d\n", hrresult); */
    return hrresult;
}

JNIEXPORT jint JNICALL Java_com_sun_electric_tool_simulation_test_Netscan4JNI_netUSB_1set_1scan_1clk
  (JNIEnv *env, jclass cls, jlong kHz)
{
    int sscresult;
    sscresult = NetUSB_set_scan_clk(kHz, CLOCK_AUTO, CLOCK_AUTO);
    /* printf("sccresult=%d\n", sscresult); */
    return sscresult;
}


JNIEXPORT jint JNICALL Java_com_sun_electric_tool_simulation_test_Netscan4JNI_netUSB_1set_1trst
  (JNIEnv *env, jclass cls, jint signal)
{
    return NetUSB_set_trst(signal);
}

JNIEXPORT jint JNICALL Java_com_sun_electric_tool_simulation_test_Netscan4JNI_netUSB_1Disconnect
  (JNIEnv *env, jclass cls)
{
    return NetUSB_Disconnect();
}

#undef INCLUDE_UNUSED_FUNCTIONS
#ifdef INCLUDE_UNUSED_FUNCTIONS

JNIEXPORT jint JNICALL Java_com_sun_electric_tool_simulation_test_Netscan4JNI_netUSB_1tms_1reset
  (JNIEnv *env, jclass cls, jint tap)
{
    int result;
    result = NetUSB_tms_reset(tap);
    return result;
}

JNIEXPORT jint JNICALL Java_com_sun_electric_tool_simulation_test_Netscan4JNI_netUSB_1move_1to_1state
  (JNIEnv *env, jclass cls, jint state, jint tap)
{
    int result;
    result = NetUSB_move_to_state(tap,state);
    return result;
}

#endif

JNIEXPORT jint JNICALL Java_com_sun_electric_tool_simulation_test_Netscan4JNI_netUSB_1scan_1ir
  (JNIEnv *env, jclass cls, jshortArray scanIn, jlong bitLength, jshortArray scanOut, jint tap)
{
    //variable preparation phase
    jshort *scanInP = (*env)->GetShortArrayElements(env, scanIn, 0);
    jshort *scanOutP = (*env)->GetShortArrayElements(env, scanOut, 0);
    int result;

    //excution phase
    result = NetUSB_scan_ir(tap, scanInP, bitLength, scanOutP);

    //release  phase
    (*env)->ReleaseShortArrayElements(env, scanIn, scanInP, 0);
    (*env)->ReleaseShortArrayElements(env, scanOut, scanOutP, 0);
    return result;
}

JNIEXPORT jint JNICALL Java_com_sun_electric_tool_simulation_test_Netscan4JNI_netUSB_1scan_1dr
  (JNIEnv *env, jclass cls, jshortArray scanIn, jlong bitLength, jshortArray scanOut, jint tap)
{
    //variable preparation phase
    jshort *scanInP = (*env)->GetShortArrayElements(env, scanIn, 0);
    jshort *scanOutP = (*env)->GetShortArrayElements(env, scanOut, 0);
    int result;

    //excution phase
    result = NetUSB_scan_dr(tap, scanInP, bitLength, scanOutP);

    //release  phase
    (*env)->ReleaseShortArrayElements(env, scanIn, scanInP, 0);
    (*env)->ReleaseShortArrayElements(env, scanOut, scanOutP, 0);
    return result;
}

JNIEXPORT jint JNICALL Java_com_sun_electric_tool_simulation_test_Netscan4JNI_netUSB_1AccessScanGPIO
  (JNIEnv *env, jclass cls, jint tap, jint gpio, jint mode, int value)
{
  return NetUSB_AccessScanGPIO(tap, gpio, mode, value);
}


