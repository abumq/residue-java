//
// Part of residue native binding for java
//
// Copyright (C) 2017-present Muflihun Labs
//
// https://muflihun.com
// https://muflihun.github.io/residue
// https://github.com/muflihun/residue-java
//
// Author: @abumusamq
//

#include <jni.h>
#ifndef Residue_JNI_H
#define Residue_JNI_H
#ifdef __cplusplus
extern "C" {
#endif
    
    JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void*);

    /*
     * Method:    connect
     */
    JNIEXPORT void JNICALL Java_com_muflihun_residue_Residue_connect(JNIEnv *, jobject, jstring);
    
    /*
     * Method: disconnect
     */
    JNIEXPORT void JNICALL Java_com_muflihun_residue_Residue_disconnect(JNIEnv *, jobject);
    
    /**
     * Residue.write(String loggerId, String file, int lineNumber, String func, String msg, int level, int vl, jstring threadId)
     *
     * Sends log
     */
    JNIEXPORT void JNICALL Java_com_muflihun_residue_Residue_write(JNIEnv *, jobject, jstring, jstring, jint, jstring, jstring, jint, jint, jstring);
 
#ifdef __cplusplus
}
#endif
#endif
