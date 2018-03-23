package com.muflihun.residue;

import java.lang.Exception;

public class Residue {
   static {
       System.loadLibrary("residue-jni");
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
    
    public native void info(String msg);
}
