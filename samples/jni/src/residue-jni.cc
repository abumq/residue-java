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

JNIEXPORT void JNICALL Java_com_muflihun_residue_Residue_write(JNIEnv *env,
        jobject obj,
        jstring loggerId_,
        jstring file_,
        jint line_,
        jstring func_,
        jstring msg_,
        jint level_,
        jint vl_,
        jstring threadId_
    ) {
    const char* loggerId = env->GetStringUTFChars(loggerId_, NULL);
    const char* file = env->GetStringUTFChars(file_, NULL);
    const char* func = env->GetStringUTFChars(func_, NULL);
    el::base::type::LineNumber line = static_cast<el::base::type::LineNumber>(line_);
    const char* msg = env->GetStringUTFChars(msg_, NULL);
    const char* threadId = env->GetStringUTFChars(threadId_, NULL);
    Residue::setThreadName(threadId);
    el::Level level = static_cast<el::Level>(level_);
    el::base::type::VerboseLevel vl = static_cast<el::base::type::VerboseLevel>(vl_);
    el::base::Writer(level, file, line, func, el::base::DispatchAction::NormalLog, vl).construct(1, loggerId) << msg;
}
