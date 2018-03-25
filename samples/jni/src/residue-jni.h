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
     * Residue.infoWrapper(String loggerId, String file, Integer lineNumber, String func, String msg, Integer vl)
     *
     * Sends info log
     */
    JNIEXPORT void JNICALL Java_com_muflihun_residue_Residue_infoWrapper(JNIEnv *, jobject, jstring, jstring, jint, jstring, jstring, jint);
 
#ifdef __cplusplus
}
#endif
#endif
