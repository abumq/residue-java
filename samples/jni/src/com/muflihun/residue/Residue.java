package com.muflihun.residue;

import cz.adamh.NativeUtils;
import java.lang.Exception;
import java.io.PrintStream;

/**
 * Residue singleton
 */
public class Residue {
    static {
        try {
            NativeUtils.loadLibraryFromJar("/lib/libresidue-jni.so");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static Residue instance = null;
    private Logger defaultLogger = new Logger("default");
    private PrintStream printStream = new ResiduePrintStream(System.out);
    
    private Residue() {
    }
    
    public static Residue getInstance() {
        if (instance == null) {
            instance = new Residue();
        }
        return instance;
    }
    
    public void setDefaultLogger(Logger logger) {
        this.defaultLogger = logger;
    }
    
    public Logger getDefaultLogger() {
        return this.defaultLogger;
    }
    
    public PrintStream getPrintStream() {
        return printStream;
    }
    
    public void setPrintStream(PrintStream printStream) {
        this.printStream = printStream;
    }
    
    public native void connect(String confPath) throws Exception;
    public native void disconnect();
    
    /* package */ synchronized native void write(String loggerId, String file, int line, String func, String msg, int level, int vl, String threadId);
}
