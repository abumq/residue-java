package org.zuhd.residue;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;

import java.io.Serializable;

/**
 * Simple Log4j appender to use in existing projects
 */
public class Log4jAppender extends AbstractAppender {

    public Log4jAppender(String name,
                         Filter filter,
                         Layout<? extends Serializable> layout) {
        super(name, filter, layout);
    }

    public Log4jAppender(String name,
                         Filter filter,
                         Layout<? extends Serializable> layout,
                         boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);
    }


    @Override
    public void append(LogEvent record) {
        Residue.LoggingLevels level;
        int vlevel = 0;

        if (record.getLevel() == Level.FATAL) {
            level = Residue.LoggingLevels.ERROR;
        } else if (record.getLevel() == Level.WARN) {
            level = Residue.LoggingLevels.WARNING;
        } else if (record.getLevel() == Level.INFO) {
            level = Residue.LoggingLevels.INFO;
        } else if (record.getLevel() == Level.DEBUG) {
            level = Residue.LoggingLevels.DEBUG;
        } else if (record.getLevel() == Level.TRACE) {
            level = Residue.LoggingLevels.VERBOSE;
            vlevel = 3;
        } else {
            level = Residue.LoggingLevels.INFO;
        }

        Residue.getInstance().log(record.getTimeMillis(), record.getLoggerName(),
                record.getMessage().getFormattedMessage(),
                null /* todo: app name */, level,
                record.getSource().getFileName(), record.getSource().getLineNumber(),
                record.getSource().getMethodName(), record.getThreadName(),
                vlevel);
    }
}

