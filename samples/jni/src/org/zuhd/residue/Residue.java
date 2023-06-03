/**
 * Residue.java
 *
 * Official Java client library for Residue logging server
 *
 * Copyright (C) 2017-present @abumq (Majid Q.)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amrayn.residue;

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
