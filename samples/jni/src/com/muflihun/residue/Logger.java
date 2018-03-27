package com.muflihun.residue;

import java.lang.Exception;

public class Logger {
    private String id; 

    public Logger(String id) {
        this.id = id;
    }

    private StackTraceElement getStackItem(int baseIdx) {
        String sourceFilename = "";
        StackTraceElement stackItem = null;
        while (sourceFilename.isEmpty() || "Logger.java".equals(sourceFilename)) {
            final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            if (stackTrace != null && stackTrace.length > baseIdx) {
                stackItem = Thread.currentThread().getStackTrace()[baseIdx];
                sourceFilename = stackItem == null ? "" : stackItem.getFileName();
            }
            baseIdx++;
            if (baseIdx >= 10) {
                // too much effort, leave it!
                // technically it should be resolved when baseIdx == 4 or 5 or max 6
                break;
            }
        }
        return stackItem;
    }

    public void info(String msg) {
        StackTraceElement si = getStackItem(3);
        int lineNumber = si == null ? 0 : si.getLineNumber();
        String filename = si == null ? "" : si.getFileName();
        String func = si == null ? "" : si.getMethodName();
        Residue.getInstance().write(id, filename, lineNumber, func, msg, 128 /* Info */, 0, Thread.currentThread().getName()); 
    }
}
