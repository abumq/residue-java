package com.muflihun.residue;

import cz.adamh.NativeUtils;
import java.lang.Exception;

public class Residue {
   static {
       try {
           NativeUtils.loadLibraryFromJar("/lib/libresidue-jni.so");
       } catch (Exception e) {
           e.printStackTrace();
       }
   }
    
    private static Residue instance = null;
    
    private Residue() {}
    
    public static Residue getInstance() {
        if (instance == null) {
            instance = new Residue();
        }
        return instance;
    }
    
    public native void connect(String confPath) throws Exception;
    public native void disconnect();
    
    /* package */ synchronized native void write(String loggerId, String file, int line, String func, String msg, int level, int vl, String threadId);
}
