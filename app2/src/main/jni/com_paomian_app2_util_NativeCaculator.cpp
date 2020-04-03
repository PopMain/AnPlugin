#include "com_paomian_app2_util_NativeCaculator.h"
#include <jni.h>

extern "C" JNIEXPORT jint JNICALL Java_com_paomian_app2_util_NativeCaculator_add
  (JNIEnv *env, jclass obj) {
   return 90;
  }