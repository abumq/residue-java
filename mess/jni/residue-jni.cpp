#include <jni.h>
#include "residue-jni.h"
#include <residue/residue.h>

JNIEXPORT void JNICALL Java_Residue_connect(JNIEnv *env, jobject, jstring conf_) {
    const char* conf = env->GetStringUTFChars(conf_, NULL);
    
    try {
        Residue::loadConfiguration(conf);
        Residue::reconnect();
        std::cout << "Connected to client: " << Residue::instance().clientId() << std::endl;
        std::cout << "Server version: " << Residue::instance().serverVersion() << std::endl;
    } catch (ResidueException& e) {
        env->ThrowNew(env->FindClass("java/lang/Exception"), e.what());
    }
}

JNIEXPORT void JNICALL Java_Residue_disconnect(JNIEnv *env, jobject) {
    Residue::disconnect();
}

JNIEXPORT void JNICALL Java_Residue_info(JNIEnv *env, jobject thisObj, jstring msg_) {
    const char* msg = env->GetStringUTFChars(msg_, NULL);
    CLOG(INFO, "sample-app") << msg;
}
