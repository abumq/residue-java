#include <jni.h>
#include "residue-jni.h"
#include <residue/residue.h>

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void*)
{
    std::cout << "Residue interface loaded" << std::endl;
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL Java_com_muflihun_residue_Residue_connect(JNIEnv *env, jobject, jstring conf_) {
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

JNIEXPORT void JNICALL Java_com_muflihun_residue_Residue_disconnect(JNIEnv *env, jobject) {
    Residue::disconnect();
}

JNIEXPORT void JNICALL Java_com_muflihun_residue_Residue_infoWrapper(JNIEnv *env,
        jobject obj,
        jstring loggerId_,
        jstring file_,
        jint line_,
        jstring func_,
        jstring msg_,
        jint vl_
    ) {
    const char* loggerId = env->GetStringUTFChars(loggerId_, NULL);
    const char* file = env->GetStringUTFChars(file_, NULL);
    const char* func = env->GetStringUTFChars(func_, NULL);
    el::base::type::LineNumber line = static_cast<el::base::type::LineNumber>(line_);
    const char* msg = env->GetStringUTFChars(msg_, NULL);
    el::base::type::VerboseLevel vl = static_cast<el::base::type::VerboseLevel>(vl_);
    el::base::Writer(el::Level::Info, file, line, func, el::base::DispatchAction::NormalLog, vl).construct(1, loggerId) << msg;
}
