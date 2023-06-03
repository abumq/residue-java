/**
 * Logger.java
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

import java.lang.Exception;

/**
 * Contains implementations of the dispatch functions that sends log messages
 */
public class Logger {
    private String id;

    private enum LoggingLevels {
        TRACE(2),
        DEBUG(4),
        FATAL(8),
        ERROR(16),
        WARNING(32),
        VERBOSE(64),
        INFO(128);

        private int value;

        LoggingLevels(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

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

    private void log(Object msg, Throwable t, LoggingLevels level, int vlevel) {
        if (t != null) {
            t.printStackTrace(Residue.getInstance().getPrintStream());
        }
        StackTraceElement si = getStackItem(4);
        int lineNumber = si == null ? 0 : si.getLineNumber();
        String filename = si == null ? "" : si.getFileName();
        String func = si == null ? "" : si.getMethodName();
        Residue.getInstance().write(id, filename, lineNumber, func, msg == null ? "null" : msg.toString(), level.getValue(), vlevel, Thread.currentThread().getName());
    }

    public void debug(Object obj) {
        log(obj, null, LoggingLevels.DEBUG, 0);
    }

    public void info(Object obj) {
        log(obj, null, LoggingLevels.INFO, 0);
    }

    public void error(Object obj) {
        log(obj, null, LoggingLevels.ERROR, 0);
    }

    public void warn(Object obj) {
        log(obj, null, LoggingLevels.WARNING, 0);
    }

    public void fatal(Object obj) {
        log(obj, null, LoggingLevels.FATAL, 0);
    }

    public void trace(Object obj) {
        log(obj, null, LoggingLevels.TRACE, 0);
    }

    public void debug(String format, Object... args) {
        String message = String.format(format, args);
        log(message, null, LoggingLevels.DEBUG, 0);
    }

    public void debug(Throwable t, String format, Object... args) {
        String message = String.format(format, args);
        log(message, t, LoggingLevels.DEBUG, 0);
    }

    public void debug(String message, Throwable throwable) {
        log(message, throwable, LoggingLevels.DEBUG, 0);
    }

    public void info(String format, Object... args) {
        String message = String.format(format, args);

        log(message, null, LoggingLevels.INFO, 0);
    }

    public void info(Throwable t, String format, Object... args) {
        String message = String.format(format, args);

        info(message, t);
    }

    public void info(String message, Throwable throwable) {
        log(message, throwable, LoggingLevels.INFO, 0);
    }

    public void warn(String format, Object... args) {
        String message = String.format(format, args);

        log(message, null, LoggingLevels.WARNING, 0);
    }

    public void warn(Throwable t, String format, Object... args) {
        String message = String.format(format, args);

        warn(message, t);
    }

    public void warn(String message, Throwable throwable) {
        log(message, throwable, LoggingLevels.WARNING, 0);
    }

    public void error(String format, Object... args) {
        String message = String.format(format, args);

        log(message, null, LoggingLevels.ERROR, 0);
    }

    public void error(Throwable t, String format, Object... args) {
        String message = String.format(format, args);

        error(message, t);
    }

    public void error(String message, Throwable throwable) {
        log(message, throwable, LoggingLevels.ERROR, 0);
    }

    public void trace(String format, Object... args) {
        String message = String.format(format, args);

        log(message, null, LoggingLevels.TRACE, 0);
    }

    public void trace(Throwable t, String format, Object... args) {
        String message = String.format(format, args);

        trace(message, t);
    }

    public void trace(String message, Throwable throwable) {
        log(message, throwable, LoggingLevels.TRACE, 0);
    }

    public void fatal(String format, Object... args) {
        String message = String.format(format, args);

        log(message, null, LoggingLevels.FATAL, 0);
    }

    public void fatal(Throwable t, String format, Object... args) {
        String message = String.format(format, args);

        fatal(message, t);
    }

    public void fatal(String message, Throwable throwable) {
        log(message, throwable, LoggingLevels.FATAL, 0);
    }

    public void verbose(int vlevel, String format, Object... args) {
        String message = String.format(format, args);

        log(message, null, LoggingLevels.VERBOSE, vlevel);
    }

    public void verbose(int vlevel, Throwable t, String format, Object... args) {
        String message = String.format(format, args);

        verbose(vlevel, message, t);
    }

    public void verbose(int vlevel, String message, Throwable throwable) {
        log(message, throwable, LoggingLevels.VERBOSE, vlevel);
    }
}
