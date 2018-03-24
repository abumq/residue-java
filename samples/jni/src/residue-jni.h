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
     * Residue.info(String msg)
     *
     * Sends info log
     */
    JNIEXPORT void JNICALL Java_com_muflihun_residue_Residue_info(JNIEnv *, jobject, jstring);
 
#ifdef __cplusplus
}
#endif
#endif
